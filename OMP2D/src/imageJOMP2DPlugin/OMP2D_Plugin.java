package imageJOMP2DPlugin;

import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.filter.PlugInFilter;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

public class OMP2D_Plugin implements PlugInFilter {
	private double[] image;
	private double[][] imageBlocks;
	private byte[] imagePixels;
	private String gpuOption;
	private int imageWidth, imageHeight;
	private int numBlocksX, numBlocksY;
	private final int BLOCK_DIM = 16;
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
		GenericDialog options = new GenericDialog("Processing Options");
		options.addMessage("OMP2D Image Compression Options");
		options.addRadioButtonGroup("GPU Acceleration?", gpuOptions, 3, 1, "No GPU Acceleration");
		options.showDialog();
		this.gpuOption = options.getNextRadioButton();
		return DOES_ALL;
	}
	
	protected void javaOnly(ImageProcessor ip) {
		
	}
	
	protected void cudaOnly(ImageProcessor ip) {
		
	}
	
	protected void cudaAndBlas(ImageProcessor ip) {
		imageBlocks = makeBlocks();
		double[] newImage = buildImage();
		
	}
	
	public double[][] makeBlocks() {/*
		double[][] blocks = new double[numBlocksX*numBlocksY][BLOCK_DIM*BLOCK_DIM];
		for(int bY = 0; bY < numBlocksY; bY++) {
			for(int v = 0; v < BLOCK_DIM; v++) {
				for(int bX = 0; bX < numBlocksX; bX++) {
					double[] block = new double[BLOCK_DIM*BLOCK_DIM];
					for(int u = 0; u < BLOCK_DIM; u++) {
						block[v*BLOCK_DIM + u] = imagePixels[bY*numBlocksX*BLOCK_DIM*BLOCK_DIM + v*BLOCK_DIM*numBlocksX + bX*BLOCK_DIM];
					}
				}
			}
		}*/
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
    			pix2d[j*n+i] = (double) (imagePixels[offs+x+i]);
    			//pix2d[j*n+i]= (pix2d[j*n+i]-128)/128.0;
    		}
    	}
    	return pix2d;
    }

	public double[] buildImage() {
		double[] img = new double[imageWidth*imageHeight];
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
