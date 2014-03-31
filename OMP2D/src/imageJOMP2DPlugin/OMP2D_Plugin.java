package imageJOMP2DPlugin;

import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.filter.PlugInFilter;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import OMP2D.BadDimensionsException;
import OMP2D.Matrix;
import OMP2D.OMP2D;

public class OMP2D_Plugin implements PlugInFilter {
	private double[] image;
	private double[][] imageBlocks;
	private byte[] imagePixels;
	private String gpuOption;
	private int imageWidth, imageHeight;
	private int numBlocksX, numBlocksY;
	private final int BLOCK_DIM = 16;
	private final double PSS = 49.87;
	public static final int MAX_INTENSITY = 255;
	private double tol;
	ImagePlus imp;
	private final String[] gpuOptions = new String[] {
			"No GPU Acceleration",
			"CUDA Acceleration",
			"CUDA and cuBLAS Acceleration"
	};

	@Override
	public void run(ImageProcessor ip) {
		imagePixels = (byte[]) ip.getPixels();
		imageWidth = ip.getWidth();
		imageHeight = ip.getHeight();
		numBlocksX = (int) Math.ceil(imageWidth / BLOCK_DIM);
		numBlocksY = (int) Math.ceil(imageHeight / BLOCK_DIM);
		
		tol = (MAX_INTENSITY*MAX_INTENSITY)/(Math.pow(10, (PSS/10.0)));
		
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
		options.showDialog();
		this.gpuOption = options.getNextRadioButton();
		return DOES_ALL;
	}
	
	protected void javaOnly(ImageProcessor ip) {
		imageBlocks = makeBlocks();
		double totPSNR = 0;
		for(double[] block: imageBlocks) {
			try {
				OMP2D blockProcessor = new OMP2D(block, BLOCK_DIM, tol);
				blockProcessor.calcBlock();
				totPSNR += blockProcessor.getPSNR();
			} catch (BadDimensionsException e) {
				System.err.println("Uh Oh");
				System.exit(1);
			}
		}
		totPSNR /= numBlocksX*numBlocksY;
		System.out.println(totPSNR);
	}
	
	protected void cudaOnly(ImageProcessor ip) {
		
	}
	
	protected void cudaAndBlas(ImageProcessor ip) {
		imageBlocks = makeBlocks();
		double[] newImage = buildImage();
		boolean ok = true;
		for(int i = 0; i < imageWidth*imageHeight; i++) {
			if(imagePixels[i] != newImage[i] - 128) {
				ok = false;
			}
			//imagePixels[i] = (byte) (newImage[i] - 128);
		}
		if(ok) {
			System.out.println("same image :)");
		}
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

	public double[] buildImage() {
		double[] img = new double[imageWidth*imageHeight];
		for(int bY = 0; bY < numBlocksY; bY++) {
			for(int bX = 0; bX < numBlocksX; bX++) {
				for(int v = 0; v < BLOCK_DIM; v++) {
					for(int u = 0; u < BLOCK_DIM; u++) {
						img[(v+bY*BLOCK_DIM)*imageWidth + bX*BLOCK_DIM + u] = imageBlocks[bY*numBlocksX + bX][v*BLOCK_DIM + u];
					}
				}
			}
		}
		return img;
	}
	
	private static ImagePlus getSampleImage(String title) {
		BufferedImage bi = null;
		try {

		bi = ImageIO.read(new File("Astro_512_grey.jpg"));
		} catch(Exception e) {
			System.out.println("couldn't get image");
			System.exit(1);
		}
		ImageProcessor image = new ByteProcessor(bi);
		ImagePlus imagePlus = new ImagePlus();
		imagePlus.setProcessor(image);
		imagePlus.setTitle(title);
		return imagePlus;
	}
	
	public static void main(String[] args) {
		OMP2D_Plugin operation = new OMP2D_Plugin();
		ImagePlus original = getSampleImage("Orginal Sample Image");
		original.show();
		operation.setup(null, original);
		operation.run(original.getProcessor());
		ImagePlus newImage = getSampleImage("New Compressed Image");
		newImage.show();
	}

}
