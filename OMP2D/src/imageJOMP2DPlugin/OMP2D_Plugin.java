package imageJOMP2DPlugin;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.filter.PlugInFilter;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

import java.io.File;
import java.io.PrintWriter;

import OMP2D.BadDimensionsException;
import OMP2D.OMP2D;

public class OMP2D_Plugin implements PlugInFilter {
	
	private double[][] imageBlocks, approxBlocks;
	private byte[] imagePixels;
	private double[] image, approx;

	private int imageWidth, imageHeight;
	private int numBlocksX, numBlocksY;
	private ImagePlus imp, preview;
	private String gpuOption;
	private final String[] gpuOptions = new String[] {
			"No GPU Acceleration",
			"CUDA Acceleration",
			"CUDA and cuBLAS Acceleration"
	};
	
	private int BLOCK_DIM;
	private final double PSS = 49.87;
	public static final int MAX_INTENSITY = 255;
	private double TOLERANCE;
	private int MAX_ITERATIONS = 250;

	


	@Override
	public void run(ImageProcessor ip) {
		imp.show();
		imagePixels = (byte[]) ip.getPixels();
		imageWidth = ip.getWidth();
		imageHeight = ip.getHeight();
		numBlocksX = (int) Math.ceil(imageWidth / BLOCK_DIM);
		numBlocksY = (int) Math.ceil(imageHeight / BLOCK_DIM);
		
		TOLERANCE = (MAX_INTENSITY*MAX_INTENSITY)/(Math.pow(10, (PSS/10.0)));
		approxBlocks = new double[numBlocksX*numBlocksY][BLOCK_DIM*BLOCK_DIM];
		
		if(gpuOption.equals("No GPU Acceleration")) {
			javaOnly(ip);
		} else if(gpuOption.equals("CUDA Acceleration")) {
			cudaOnly(ip);
		} else {
			cudaAndBlas(ip);
		}
	}

	@Override
	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;

		
		int result = displayOptions();
		
		
		
		return result;
	}
	
	private int displayOptions() {
		boolean validOptions = false;
		do {
			GenericDialog options = new GenericDialog("Processing Options");
			options.addMessage("OMP2D Image Compression Options");
			options.addRadioButtonGroup("GPU Acceleration?", gpuOptions, 3, 1, "No GPU Acceleration");
			options.addChoice("Block Size", new String[] {"8x8", "16x16", "32x32"}, "16x16");
			options.addStringField("Maximum Iterations", "250");
			options.showDialog();

			if(options.wasCanceled()) {
				return DONE;
			}
			
			this.gpuOption = options.getNextRadioButton();
			
			String blockChoice = options.getNextChoice();
			if(blockChoice.equals("8x8")) {
				BLOCK_DIM = 8;
			} else if(blockChoice.equals("16x16")) {
				BLOCK_DIM = 16;
			} else {
				BLOCK_DIM = 32;
			}
			
			String maxIterations = options.getNextString();
			try {
				MAX_ITERATIONS = Integer.parseInt(maxIterations);
				if(MAX_ITERATIONS < 1) {
					throw new NumberFormatException();
				}
				validOptions = true;
			} catch(NumberFormatException e) {
				IJ.error("Invalid Options", "The number entered for max iterations must be a positive integer");
				continue;
			}
		} while(!validOptions);
		
		return DOES_8G;
	}

	protected void javaOnly(ImageProcessor ip) {
		imageBlocks = makeBlocks();
		PrintWriter pw = null;
		try {
			File dir = new File("/tmp/omp2d");
			if(!dir.exists()) {
				dir.mkdir();
			}
			pw = new PrintWriter("/tmp/omp2d/H.txt", "UTF-8");
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		
		int totalCoeffs = 0;
		long start = System.currentTimeMillis();
		
		for(int b = 0; b < imageBlocks.length; b++) {
			try {
				OMP2D blockProcessor = new OMP2D(imageBlocks[b], BLOCK_DIM, TOLERANCE, MAX_ITERATIONS);
				blockProcessor.calcBlock();
				totalCoeffs += blockProcessor.getNumCoefficients();
				approxBlocks[b] = blockProcessor.getApproxImage().to1DArray();
				IJ.showProgress(b, imageBlocks.length);
			} catch (Exception e) {
				System.err.println("Uh Oh! Block: " + b + " failed");
				e.printStackTrace();
				System.exit(1);
			}
		}
		
		long stop = System.currentTimeMillis();
		
		approx = buildBlocks(approxBlocks);
		
		for(double d : approx) {
			pw.println(d);
		}

		pw.close();

		image = buildBlocks(imageBlocks);
		preview = buildImage(imageWidth, imageHeight, approx);
		preview.show();
		saveImage(preview);
		double sparsity = 262144.0/totalCoeffs;
		
		IJ.showMessageWithCancel("Results", "PSNR: " + getPSNR(image, approx) + 
				"\nTime Taken: " + ((stop - start)/1000.0) + "s" +
				"\nSparsity: " + sparsity);
		IJ.showTime(preview, stop - stop, "");
	}

	protected void cudaOnly(ImageProcessor ip) {
		
	}
	
	protected void cudaAndBlas(ImageProcessor ip) {
		imageBlocks = makeBlocks();
		double[] newImage = buildBlocks(imageBlocks);
		boolean ok = true;
		for(int i = 0; i < imageWidth*imageHeight; i++) {
			if(imagePixels[i] != newImage[i]) {
				ok = false;
			}
		}
		if(ok) {
			System.out.println("same image :)");
		}
	}
	
	public double getPSNR(double[] m1, double[] m2) {
		if(m1.length != m2.length) {
			System.out.println("badpsnr");
		}
		double mse = 0;

		for(int i = 0; i < m1.length; i++) {
			mse += Math.pow(m1[i]-m2[i], 2);
		}
		mse /= (imageWidth*imageHeight);
		return 10*Math.log10((MAX_INTENSITY*MAX_INTENSITY)/mse);
	}
	
	public double[][] makeBlocks() {
		double[][] blocks = new double[numBlocksX*numBlocksY][BLOCK_DIM*BLOCK_DIM];
		for(int bY = 0; bY < numBlocksY; bY++) {
			for(int bX = 0; bX < numBlocksX; bX++) {
				blocks[bY*numBlocksX + bX] = getBytePixels(bX, bY, BLOCK_DIM);
			}
		}
		return blocks;
	}
	
    protected double[] getBytePixels(int x, int y, int n) {
    	x *= n; y *= n;
    	double[] pix2d = new double[n*n];
    	for (int j = 0; j < n; j++) {
    		int offs = (y + j)*imageWidth;
    		for (int i = 0; i < n; i++) {
    			pix2d[j*n+i] = (double) (imagePixels[offs+x+i]&0xff);
    		}
    	}
    	return pix2d;
    }

	public double[] buildBlocks(double[][] blocks) {
		double[] img = new double[imageWidth*imageHeight];
		for(int bY = 0; bY < numBlocksY; bY++) {
			for(int bX = 0; bX < numBlocksX; bX++) {
				for(int v = 0; v < BLOCK_DIM; v++) {
					for(int u = 0; u < BLOCK_DIM; u++) {
						img[(v+bY*BLOCK_DIM)*imageWidth + bX*BLOCK_DIM + u] = blocks[bY*numBlocksX + bX][v*BLOCK_DIM + u];
					}
				}
			}
		}
		return img;
	}
	
	private ImagePlus buildImage(int width, int height, double[] pixels) {
		byte[] imgPixels = doubleToByteArray(pixels);
		ByteProcessor bp = new ByteProcessor(width, height, imgPixels);
		return new ImagePlus("Preview", bp);
	}
	
	private void saveImage(ImagePlus img) {
		File dir = new File("/tmp/omp2d");
		if(!dir.exists()) {
			dir.mkdir();
		}
		IJ.save(img, "/tmp/omp2d/" + System.currentTimeMillis() + ".png");
	}
	
	private byte[] doubleToByteArray(double[] d) {
		byte[] b = new byte[d.length];
		for(int i = 0; i < d.length; i++) {
			b[i] = (byte) d[i];
		}
		return b;
	}

}
