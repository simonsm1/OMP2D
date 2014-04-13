/*
Copyright (c) 2014 Matthew Simons

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

package OMP2D;

import jcuda.Pointer;
import jcuda.Sizeof;
import jcuda.driver.CUstream;
import jcuda.jcublas.JCublas;
import jcuda.runtime.JCuda;
import jcuda.runtime.cudaStream_t;
import jcuda.runtime.dim3;
import jcuda.utils.KernelLauncher;
import static jcuda.runtime.cudaMemcpyKind.cudaMemcpyDeviceToHost;

public class CudaDirector {
	private static KernelLauncher kl;
	public static final int MAX_THREADS_PER_BLOCK = 1024;
	
	public static void init(String cuSource) {
		kl = KernelLauncher.create("src/OMP2D/CudaFunctions.cu", "scale", false, "-arch=sm_30");
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
			CleverPointer<double[]> vector2, int length, CleverPointer<double[]> ans) {
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
		
		kl = kl.forFunction("innerProduct");
		kl.setSharedMemSize(Sizeof.DOUBLE*length);
		kl.setup(gridSize, blockSize);
		kl.call(vector1, vector2, length, ans);
	}

	public static void multiply(CleverPointer<double[]> matrix1, int m1Width, CleverPointer<double[]> matrix2,
			int m2Height, CleverPointer<double[]> result) {
		int numThreads = m1Width*m2Height;
		dim3 gridSize;
		dim3 blockSize;
		if(numThreads < MAX_THREADS_PER_BLOCK) {
			gridSize = new dim3(1, 1, 1);
			blockSize = new dim3(numThreads, 1, 1);
		} else {
			gridSize = new dim3((int) Math.ceil(numThreads / MAX_THREADS_PER_BLOCK), 1, 1);
			blockSize = new dim3(MAX_THREADS_PER_BLOCK, 1, 1);
		}
		
		kl = kl.forFunction("multiply");
		kl.setSharedMemSize(Sizeof.DOUBLE*numThreads);
		kl.setup(gridSize, blockSize);
		kl.call(matrix1, matrix2, m1Width, m2Height, result);
	}
	
	public static void testSync(CleverPointer<double[]> matrix, double power) {
		kl = kl.forFunction("power");
		int length = matrix.getArray().length;
		double[][] tempHolder = new double[16][length];
		cudaStream_t[] streams = new cudaStream_t[16];
		
		for(int k = 0; k < 16; k++) {
			streams[k] = new cudaStream_t();
			JCuda.cudaStreamCreate(streams[k]);
		}

		for(int k = 0; k < 16; k++) {

			CUstream s = new CUstream(streams[k]);
			
			kl.setStream(s);
			kl.setBlockSize(256, 1, 1);
			kl.setGridSize(1, 1);
			kl.call(matrix, length, 2);
			
			JCuda.cudaMemcpyAsync(Pointer.to(tempHolder[k]), matrix, matrix.getSize(), cudaMemcpyDeviceToHost, streams[k]);
			matrix = matrix.clone(streams[k]);
		}
		
		for(int k = 0; k < 16; k++) {
			streams[k] = new cudaStream_t();
			JCuda.cudaStreamDestroy(streams[k]);
		}
		
		System.out.println("stop");

	}
	
	public static void innerProduct(Matrix m1, Matrix m2) {
		//JCublas.cublasSetVector(m1.getSize(), Sizeof.DOUBLE, Pointer., incx, y, incy)
		int N = 275;
	    float h_A[];
	    float h_B[];
	    float h_C[];
	    Pointer d_A = new Pointer();
	    Pointer d_B = new Pointer();
	    Pointer d_C = new Pointer();
	    float alpha = 1.0f;
	    float beta = 0.0f;
	    int n2 = N * N;
	    int i;

	    /* Initialize JCublas */
	    JCublas.cublasInit();

	    /* Allocate host memory for the matrices */
	    h_A = new float[n2];
	    h_B = new float[n2];
	    h_C = new float[n2];

	    /* Fill the matrices with test data */
	    for (i = 0; i < n2; i++)
	    {
	      h_A[i] = (float)Math.random();
	      h_B[i] = (float)Math.random();
	      h_C[i] = (float)Math.random();
	    }

	    /* Allocate device memory for the matrices */
	    JCublas.cublasAlloc(n2, Sizeof.FLOAT, d_A);
	    JCublas.cublasAlloc(n2, Sizeof.FLOAT, d_B);
	    JCublas.cublasAlloc(n2, Sizeof.FLOAT, d_C);

	    /* Initialize the device matrices with the host matrices */
	    JCublas.cublasSetVector(n2, Sizeof.FLOAT, Pointer.to(h_A), 1, d_A, 1);
	    JCublas.cublasSetVector(n2, Sizeof.FLOAT, Pointer.to(h_B), 1, d_B, 1);
	    JCublas.cublasSetVector(n2, Sizeof.FLOAT, Pointer.to(h_C), 1, d_C, 1);

	    /* Performs operation using JCublas */
	    JCublas.cublasSgemm('n', 'n', N, N, N, alpha,
	              d_A, N, d_B, N, beta, d_C, N);

	    /* Read the result back */
	    JCublas.cublasGetVector(n2, Sizeof.FLOAT, d_C, 1, Pointer.to(h_C), 1);

	    /* Memory clean up */



	    JCublas.cublasFree(d_A);
	    JCublas.cublasFree(d_B);
	    JCublas.cublasFree(d_C);

	    /* Shutdown */
	    JCublas.cublasShutdown();

	  }

}
