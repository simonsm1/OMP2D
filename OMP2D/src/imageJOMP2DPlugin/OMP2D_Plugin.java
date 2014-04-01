package imageJOMP2DPlugin;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.filter.PlugInFilter;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

import java.io.File;

import OMP2D.OMP2D;

public class OMP2D_Plugin implements PlugInFilter {
	private byte[] imagePixels;
	private double[] image, approx;
	
	private double[][] imageBlocks, approxBlocks;


	private int imageWidth, imageHeight;
	private int numBlocksX, numBlocksY;
	private int BLOCK_DIM;
	private final double PSS = 49.87;
	public static final int MAX_INTENSITY = 255;
	private double TOLERANCE;
	private ImagePlus imp, preview;
	
	private String gpuOption;
	private final String[] gpuOptions = new String[] {
			"No GPU Acceleration",
			"CUDA Acceleration",
			"CUDA and cuBLAS Acceleration"
	};

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
		
		GenericDialog options = new GenericDialog("Processing Options");
		options.addMessage("OMP2D Image Compression Options");
		options.addRadioButtonGroup("GPU Acceleration?", gpuOptions, 3, 1, "No GPU Acceleration");
		options.addChoice("Block Size", new String[] {"8x8", "16x16", "32x32"}, "16x16");
		options.showDialog();
		this.gpuOption = options.getNextRadioButton();
		String blockChoice = options.getNextChoice();
		if(blockChoice.equals("8x8")) {
			BLOCK_DIM = 8;
		} else if(blockChoice.equals("16x16")) {
			BLOCK_DIM = 16;
		} else {
			BLOCK_DIM = 32;
		}
		return DOES_8G;
	}
	
	protected void javaOnly(ImageProcessor ip) {
		long start = System.currentTimeMillis();
		imageBlocks = makeBlocks();
		for(int b = 0; b < imageBlocks.length; b++) {
			try {
				OMP2D blockProcessor = new OMP2D(imageBlocks[b], BLOCK_DIM, TOLERANCE);
				blockProcessor.calcBlock();
				if(blockProcessor.getNumCoefficients() < 2) {
					System.err.println("here:");
				}
				System.out.println("Block " + b + ": " + blockProcessor.getNumCoefficients());
				approxBlocks[b] = blockProcessor.getApproxImage().to1DArray();
			} catch (Exception e) {
				System.err.println("Uh Oh! Block: " + b + " failed");
				e.printStackTrace();
				System.exit(1);
			}
		}
		approx = buildBlocks(approxBlocks);
		long stop = System.currentTimeMillis();
		image = buildBlocks(imageBlocks);
		preview = buildImage(imageWidth, imageHeight, approx);
		preview.show();
		saveImage(preview);
		IJ.showMessageWithCancel("Results", "PSNR: " + getPSNR(image, approx));
		//IJ.showTime(preview, start, stop);
	}

	protected void cudaOnly(ImageProcessor ip) {
		
	}
	
	protected void cudaAndBlas(ImageProcessor ip) {
		imageBlocks = makeBlocks();
		double[] newImage = buildBlocks(imageBlocks);
		boolean ok = true;
		for(int i = 0; i < imageWidth*imageHeight; i++) {
			if(imagePixels[i] != newImage[i] - 128) {
				ok = false;
			}
		}
		if(ok) {
			System.out.println("same image :)");
		}
	}
	
	public double getPSNR(double[] m1, double[] m2) {
		double mse = 0;

		for(int i = 0; i < m1.length; i++) {
			mse += Math.pow(m1[i]-m2[i], 2);
		}
		mse /= (BLOCK_DIM*BLOCK_DIM);
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
    			pix2d[j*n+i] = (double) (imagePixels[offs+x+i]) + 128;
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
		IJ.save(imp, "/tmp/omp2d/" + System.currentTimeMillis() + ".png");
	}
	
	private byte[] doubleToByteArray(double[] d) {
		byte[] b = new byte[d.length];
		for(int i = 0; i < d.length; i++) {
			b[i] = (byte) (d[i] - 127.0);
		}
		return b;
	}

}
