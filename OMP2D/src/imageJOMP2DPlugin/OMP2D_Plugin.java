package imageJOMP2DPlugin;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.filter.PlugInFilter;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import OMP2D.BadDimensionsException;
import OMP2D.OMP2D;

public class OMP2D_Plugin implements PlugInFilter {
	
	private double[][] imageBlocks, approxBlocks;
	private byte[] imagePixels;
	private double[] image, approx;
	private int totalCoeffs;
	private int progress;

	private int imageWidth, imageHeight;
	private int numBlocksX, numBlocksY;
	private ImagePlus imp, preview;
	private String threadOption;
	private final String[] threadOptions = new String[] {
			"Single Threaded",
			"Multi-Threaded",
	};
	
	private int BLOCK_DIM;
	private double PSS;
	public static final int MAX_INTENSITY = 255;
	private double TOLERANCE;
	private int MAX_ITERATIONS;
	private boolean debug;

	@Override
	public void run(ImageProcessor ip) {
		imp.show();
		imagePixels = (byte[]) ip.getPixels();
		numBlocksX = (int) Math.ceil(imageWidth / BLOCK_DIM);
		numBlocksY = (int) Math.ceil(imageHeight / BLOCK_DIM);
		
		TOLERANCE = (MAX_INTENSITY*MAX_INTENSITY)/(Math.pow(10, (PSS/10.0)));
		approxBlocks = new double[numBlocksX*numBlocksY][BLOCK_DIM*BLOCK_DIM];
		
		imageBlocks = makeBlocks();
		totalCoeffs = 0;
		
		long start = System.currentTimeMillis();
		
		if(threadOption.equals("Single Threaded")) {
			singleThread(ip);
		} else if(threadOption.equals("Multi-Threaded")) {
			multiThread(ip);
		}
		
		long stop = System.currentTimeMillis();
		
		approx = buildBlocks(approxBlocks);
		image = buildBlocks(imageBlocks);
		
		preview = buildImage(imageWidth, imageHeight, approx);
		IJ.showTime(preview, stop - stop, "");
		preview.show();
		
		if(debug) {
			printApprox();
			saveImage(preview);
		}

		
		double sparsity = (imageWidth*imageHeight)/(totalCoeffs*1.0);
		IJ.showMessageWithCancel("Results", "PSNR: " + getPSNR(image, approx) + 
				"\nTime Taken: " + ((stop - start)/1000.0) + "s" +
				"\nSparsity: " + sparsity);
	}

	@Override
	public int setup(String arg, ImagePlus imp) {
		imageWidth = imp.getWidth();
		imageHeight = imp.getHeight();
		if(imageWidth % 32 != 0 || imageHeight % 32 != 0) {
			IJ.error("Only greyscale images which are exactly divisible into blocks of size 32 are currently supported");
			return DONE;
		}
		
		this.imp = imp;
		int result = displayOptions();
		return result;
	}
	
	private int displayOptions() {
		boolean validOptions = false;
		do {
			GenericDialog options = new GenericDialog("OMP2D Image Compression Options");
			options.addRadioButtonGroup("Single or Multi-Threaded", threadOptions, 3, 1, "Multi-Threaded");
			options.addChoice("Block Size", new String[] {"8x8", "16x16", "32x32"}, "16x16");
			options.addNumericField("PSS", 50.0, 2, 4, "");
			options.addNumericField("Maximum Iterations", 250, 0, 4, "");
			options.addCheckbox("Debug", false);
			options.showDialog();

			if(options.wasCanceled()) {
				return DONE;
			}
			
			this.threadOption = options.getNextRadioButton();
			
			String blockChoice = options.getNextChoice();
			if(blockChoice.equals("8x8")) {
				BLOCK_DIM = 8;
			} else if(blockChoice.equals("16x16")) {
				BLOCK_DIM = 16;
			} else if(blockChoice.equals("32x32")){
				BLOCK_DIM = 32;
			} else {
				IJ.error("Invalid Options", "The block size must be 8, 16 or 32");
				continue;
			}
			
			if(Double.isNaN(PSS = options.getNextNumber())) {
				IJ.error("Invalid Options", "The number entered for PSS must be a positive number");
				continue;
			}
			
			if(Double.isNaN(MAX_ITERATIONS = (int) options.getNextNumber())) {
				IJ.error("Invalid Options", "The number entered for Max Iterations must be a positive integer");
				continue;
			}
			
			debug = options.getNextBoolean();
			validOptions = true;
			
		} while(!validOptions);
		
		return DOES_8G+NO_CHANGES+NO_UNDO;
	}
	
	/**
	 * Prints the approximated image pixel values to file
	 */
	private void printApprox() {
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(getTempDir() + "H.txt", "UTF-8");
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		for(double d : approx) {
			pw.println(d);
		}

		pw.close();
	}

	/**
	 * Approximates the image in a sequential manner
	 * @param ip
	 */
	private void singleThread(ImageProcessor ip) {
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
	}

	protected void multiThread(ImageProcessor ip) {
		List<OMP2D> blockProcessors = new ArrayList<OMP2D>();
		
		for(int b = 0; b < imageBlocks.length; b++) {
			OMP2D blockProcessor = new OMP2D(imageBlocks[b], BLOCK_DIM, TOLERANCE, MAX_ITERATIONS);
			blockProcessors.add(blockProcessor);
		}
		
		try {
			processBlocks(blockProcessors);
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Processes an array of blocks asynchronously. The number of concurrently running 
	 * threads is set to the maximum number available for the given platform  
	 * @param blocks The OMP2D blocks to be processed
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	public void processBlocks(List<OMP2D> blocks) throws InterruptedException, ExecutionException {
		progress = 0;
	    int threads = Runtime.getRuntime().availableProcessors();
	    ExecutorService service = Executors.newFixedThreadPool(threads);

	    List<Future<OMP2D>> futures = new ArrayList<Future<OMP2D>>();
	    for(final OMP2D block : blocks) {
	        Callable<OMP2D> callable = new Callable<OMP2D>() {
	            public OMP2D call() throws BadDimensionsException {
					block.calcBlock();
					IJ.showProgress(++progress, imageBlocks.length);
	                return block;
	            }
	        };
	        futures.add(service.submit(callable));
	    }

	    service.shutdown();
	    int blockId = 0;
	    
	    for(Future<OMP2D> future : futures) {
	    	OMP2D block = future.get();
			totalCoeffs += block.getNumCoefficients();
			approxBlocks[blockId] = block.getApproxImage().to1DArray();
	        blockId++;
	    }
	}
	
	/**
	 * Calculates the Peak Signal-to-Noise Ratio between one matrix and another
	 * @param m1 A matrix
	 * @param m2 The approximated matrix
	 * @return
	 */
	public double getPSNR(double[] m1, double[] m2) {
		if(m1.length != m2.length) {
			IJ.showMessage("Cannot calculate PSNR; lengths of the original and approximated matrices differ");
			return -1;
		}
		double mse = 0;
		for(int i = 0; i < m1.length; i++) {
			mse += Math.pow(m1[i]-m2[i], 2);
		}
		mse /= (imageWidth*imageHeight);
		return 10.0*Math.log10((MAX_INTENSITY*MAX_INTENSITY)/mse);
	}
	
	/**
	 * Divides the inputed image into blocks of dimension NxN
	 * @return An array of blocks corresponding to the image.
	 */
	private double[][] makeBlocks() {
		double[][] blocks = new double[numBlocksX*numBlocksY][BLOCK_DIM*BLOCK_DIM];
		for(int bY = 0; bY < numBlocksY; bY++) {
			for(int bX = 0; bX < numBlocksX; bX++) {
				blocks[bY*numBlocksX + bX] = getBytePixels(bX, bY, BLOCK_DIM);
			}
		}
		return blocks;
	}
	
	/**
	 * Gets the pixel values 0-255 of an n dimensional block at position I(x, y) 
	 * @param x The x co-ordinate of the block
	 * @param y The y co-ordinate of the block
	 * @param n The block width and height in pixels
	 * @return The block
	 */
    private double[] getBytePixels(int x, int y, int n) {
    	x *= n; y *= n;
    	double[] pix2d = new double[n*n];
    	for(int j = 0; j < n; j++) {
    		int offs = (y + j)*imageHeight;
    		for(int i = 0; i < n; i++) {
    			pix2d[j*n+i] = (double) (imagePixels[offs+x+i]&0xff);
    		}
    	}
    	return pix2d;
    }

    /**
     * Reassembles the blocks into a 1D array compatible with ImageJ
     * @param blocks 
     * @return Image array
     */
	private double[] buildBlocks(double[][] blocks) {
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
	
	/**
	 * Creates a new ImagePlus object given some pixel values and dimensions
	 * @param width The width of the image
	 * @param height The height of the image
	 * @param pixels The pixel values 0-255
	 * @return
	 */
	private ImagePlus buildImage(int width, int height, double[] pixels) {
		byte[] imgPixels = doubleToByteArray(pixels);
		ByteProcessor bp = new ByteProcessor(width, height, imgPixels);
		return new ImagePlus("Preview", bp);
	}
	
	/**
	 * Saves a given image to file
	 * @param img
	 */
	private void saveImage(ImagePlus img) {
		IJ.save(img, getTempDir() + System.currentTimeMillis() + ".png");
	}
	
	/**
	 * Gets the current platform's temporary directory and appends the omp2d directory
	 * @return The location
	 */
	private String getTempDir() {
		String path = System.getProperty("java.io.tmpdir") + "/omp2d/";
		File dir = new File(path);
		if(!dir.exists()) {
			dir.mkdir();
		}
		return path;
	}
	
	/**
	 * Converts a double array to byte array
	 * @param d double array
	 * @return byte array
	 */
	private byte[] doubleToByteArray(double[] d) {
		byte[] b = new byte[d.length];
		for(int i = 0; i < d.length; i++) {
			b[i] = (byte) d[i];
		}
		return b;
	}

}
