package imageJOMP2DPlugin;

import static org.junit.Assert.*;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

import org.junit.Before;
import org.junit.Test;

public class OMP2DPluginTest {
	
	private final double[] BLOCK_DATA = new double[] {
			1.1200000e+02, 1.1000000e+02, 1.0800000e+02, 1.0200000e+02, 1.0100000e+02, 1.0300000e+02, 1.0300000e+02, 1.0300000e+02, 1.0300000e+02, 1.0000000e+02, 1.0600000e+02, 1.0500000e+02, 1.0800000e+02, 1.0900000e+02, 1.0900000e+02, 1.0600000e+02
			, 1.1300000e+02, 1.1000000e+02, 1.0700000e+02, 1.0100000e+02, 9.8000000e+01, 9.9000000e+01, 1.0000000e+02, 1.0200000e+02, 1.0300000e+02, 1.0500000e+02, 1.2800000e+02, 1.1200000e+02, 1.0200000e+02, 1.1000000e+02, 1.0600000e+02, 1.0800000e+02
			, 1.1100000e+02, 1.0700000e+02, 1.0400000e+02, 1.0000000e+02, 9.9000000e+01, 9.9000000e+01, 9.8000000e+01, 1.0100000e+02, 1.0300000e+02, 1.0400000e+02, 1.1500000e+02, 1.0800000e+02, 1.0500000e+02, 1.0900000e+02, 1.0800000e+02, 1.0500000e+02
			, 1.1500000e+02, 1.1100000e+02, 1.0800000e+02, 1.0500000e+02, 1.0300000e+02, 1.0200000e+02, 9.9000000e+01, 9.9000000e+01, 1.0400000e+02, 1.0700000e+02, 1.0200000e+02, 1.0500000e+02, 1.0900000e+02, 1.0400000e+02, 1.0800000e+02, 1.0600000e+02
			, 1.1600000e+02, 1.1400000e+02, 1.1200000e+02, 1.0800000e+02, 1.0600000e+02, 1.0600000e+02, 1.0400000e+02, 1.0400000e+02, 1.0400000e+02, 1.0800000e+02, 1.1000000e+02, 1.0800000e+02, 1.0800000e+02, 1.0600000e+02, 1.0600000e+02, 1.0800000e+02
			, 1.1400000e+02, 1.1500000e+02, 1.1500000e+02, 1.1200000e+02, 1.0800000e+02, 1.0700000e+02, 1.0700000e+02, 1.0800000e+02, 1.0600000e+02, 1.0400000e+02, 1.0900000e+02, 1.0700000e+02, 1.0600000e+02, 1.0900000e+02, 1.0700000e+02, 1.0600000e+02
			, 1.1200000e+02, 1.1300000e+02, 1.1400000e+02, 1.1300000e+02, 1.1200000e+02, 1.1000000e+02, 1.1100000e+02, 1.1300000e+02, 1.1000000e+02, 1.0700000e+02, 1.0600000e+02, 1.0700000e+02, 1.0500000e+02, 1.0600000e+02, 1.1000000e+02, 1.0800000e+02
			, 1.1600000e+02, 1.1300000e+02, 1.1100000e+02, 1.1100000e+02, 1.1100000e+02, 1.1000000e+02, 1.1100000e+02, 1.1300000e+02, 1.1300000e+02, 1.1100000e+02, 1.0800000e+02, 1.0500000e+02, 1.0600000e+02, 1.1100000e+02, 1.1300000e+02, 1.1300000e+02
			, 1.1900000e+02, 1.1600000e+02, 1.1200000e+02, 1.1200000e+02, 1.1100000e+02, 1.0900000e+02, 1.0900000e+02, 1.1300000e+02, 1.1400000e+02, 1.1100000e+02, 1.0800000e+02, 1.0700000e+02, 1.0700000e+02, 1.1200000e+02, 1.1500000e+02, 1.1400000e+02
			, 1.1800000e+02, 1.1700000e+02, 1.1400000e+02, 1.1200000e+02, 1.1100000e+02, 1.0900000e+02, 1.0800000e+02, 1.1000000e+02, 1.1100000e+02, 1.0800000e+02, 1.0800000e+02, 1.1100000e+02, 1.1300000e+02, 1.1300000e+02, 1.1400000e+02, 1.1300000e+02
			, 1.1700000e+02, 1.1600000e+02, 1.1400000e+02, 1.1200000e+02, 1.1100000e+02, 1.1100000e+02, 1.1200000e+02, 1.1300000e+02, 1.0900000e+02, 1.0500000e+02, 1.0600000e+02, 1.1000000e+02, 1.1100000e+02, 1.1000000e+02, 1.1000000e+02, 1.0900000e+02
			, 1.1700000e+02, 1.1700000e+02, 1.1500000e+02, 1.1300000e+02, 1.1300000e+02, 1.1400000e+02, 1.1400000e+02, 1.1300000e+02, 1.1000000e+02, 1.0900000e+02, 1.0800000e+02, 1.0900000e+02, 1.0900000e+02, 1.1000000e+02, 1.1000000e+02, 1.0900000e+02
			, 1.1300000e+02, 1.1400000e+02, 1.1500000e+02, 1.1500000e+02, 1.1500000e+02, 1.1600000e+02, 1.1400000e+02, 1.1000000e+02, 1.0800000e+02, 1.0800000e+02, 1.0800000e+02, 1.0900000e+02, 1.0900000e+02, 1.1000000e+02, 1.1000000e+02, 1.1000000e+02
			, 1.0900000e+02, 1.1000000e+02, 1.1100000e+02, 1.1200000e+02, 1.1300000e+02, 1.1500000e+02, 1.1400000e+02, 1.1100000e+02, 1.1000000e+02, 1.0800000e+02, 1.0800000e+02, 1.0900000e+02, 1.1000000e+02, 1.1000000e+02, 1.1100000e+02, 1.1400000e+02
			, 1.0800000e+02, 1.0800000e+02, 1.0800000e+02, 1.0900000e+02, 1.1000000e+02, 1.1200000e+02, 1.1400000e+02, 1.1400000e+02, 1.1300000e+02, 1.1000000e+02, 1.0800000e+02, 1.0900000e+02, 1.1000000e+02, 1.1100000e+02, 1.1300000e+02, 1.1500000e+02
			, 1.0800000e+02, 1.0700000e+02, 1.0800000e+02, 1.1000000e+02, 1.1000000e+02, 1.1200000e+02, 1.1500000e+02, 1.1700000e+02, 1.1300000e+02, 1.1300000e+02, 1.1100000e+02, 1.1000000e+02, 1.1300000e+02, 1.1600000e+02, 1.1600000e+02, 1.1500000e+02
	};
	
	

	@Before
	public void setUp() throws Exception {

	}

	@Test
	public void astroTest() throws IOException {
		OMP2D_Plugin operation = new OMP2D_Plugin();
		ImagePlus original = getSampleImage("Orginal Sample Image");
		operation.setup(null, original);
		operation.run(original.getProcessor());
	}
	
	private static ImagePlus getSampleImage(String title) throws IOException {
		ImageProcessor pro = new ByteProcessor(ImageIO.read(new File("Astro_512_grey.jpg")));
		return new ImagePlus("Original Image", pro);
	}

}
