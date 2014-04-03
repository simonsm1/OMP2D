package OMP2D;

import jcuda.runtime.dim3;
import jcuda.utils.KernelLauncher;

public class CudaDirector {
	private static KernelLauncher kl;
	public static final int MAX_THREADS_PER_BLOCK = 1024;
	
	public static void init(String cuSource) {
		kl = KernelLauncher.create("src/OMP2D/CudaFunctions.cu", "scale", true, "-arch=sm_30");
	}
	
	public static void scale(CleverPointer<double[]> vector, double factor) {
		int length = vector.getOriginal().length;
		int numThreads = length;
		dim3 gridSize;
		dim3 blockSize;
		if(numThreads < MAX_THREADS_PER_BLOCK) {
			gridSize = new dim3(1, 1, 1);
			blockSize = new dim3(length, 1, 1);
		} else {
			gridSize = new dim3((int) Math.ceil(length / MAX_THREADS_PER_BLOCK), 1, 1);
			blockSize = new dim3(MAX_THREADS_PER_BLOCK, 1, 1);
		}
		 
		kl.forFunction("scale").setup(gridSize, blockSize).call(vector, length, factor);
	}

	public static void innerProduct(CleverPointer<double[]> vector1,
			CleverPointer<double[]> vector2, int length) {
		int numThreads = length;
		dim3 gridSize;
		dim3 blockSize;
		if(numThreads < MAX_THREADS_PER_BLOCK) {
			gridSize = new dim3(1, 1, 1);
			blockSize = new dim3(length, 1, 1);
		} else {
			gridSize = new dim3((int) Math.ceil(length / MAX_THREADS_PER_BLOCK), 1, 1);
			blockSize = new dim3(MAX_THREADS_PER_BLOCK, 1, 1);
		}
		
		kl.forFunction("innerProduct").setup(gridSize, blockSize).call(vector1, vector2);
	}

}
