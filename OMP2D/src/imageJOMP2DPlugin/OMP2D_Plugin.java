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
	double[][] image;
	String gpuOption;
	private final String[] gpuOptions = new String[] {
			"No GPU Acceleration",
			"CUDA Acceleration",
			"CUDA and cuBLAS Acceleration"
	};

	@Override
	public void run(ImageProcessor ip) {
		//int[] imagePixelsInt = (int[]) ip.getPixelsCopy();
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
