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

public class CudaMatrix extends Matrix {
	private CleverPointer<double[]> dMatrix;

	public CudaMatrix(int width, double[] vals) {
		super(width, vals);
		dMatrix = CleverPointer.copyDouble(vals);
	}
	
	public CleverPointer<double[]> getPointer() {
		return dMatrix;
	}
	
	@Override
	public void scale(double factor) {
		CudaDirector.scale(dMatrix, factor);
	}
	
	@Override
	public double[] to1DArray() {
		return dMatrix.getArray();
	}
	
	/**
	 * Performs the dot product of two matrices 
	 * @param matrix1 A matrix defined as \f$M^{x \times y}\f$
	 * @param matrix2 A matrix defined as \f$N^{y \times z}\f$
	 * @return The resulting matrix
	 * @throws BadDimensionsException
	 */
	public static CleverPointer<double[]> multiply(CudaMatrix matrix1, CudaMatrix matrix2) throws BadDimensionsException{
		//matrix1[m][n] matrix2[p][q]
		int mMax = matrix1.getHeight(); int nMax = matrix1.getWidth();
		int pMax = matrix2.getHeight(); int qMax = matrix2.getWidth();
		
		if(nMax != pMax) {
			throw new BadDimensionsException("Expected matrices of (m,n)x(n,q)\n" +
					"Recieved (" + mMax + "," + nMax + ")x(" + pMax + "," + qMax + ")");
		}

		CleverPointer<double[]> result = CleverPointer.createDouble(mMax*qMax);
		
		CudaDirector.multiply(matrix1.getPointer(), matrix1.getWidth(), matrix2.getPointer(), matrix2.getHeight(), result);
		
		return result;
	}
	
	public static CleverPointer<double[]> innerProduct(CudaMatrix matrix1, CudaMatrix matrix2) throws BadDimensionsException {
		if(matrix1.getSize() != matrix2.getSize()) {
			throw new BadDimensionsException("Matrices should be the same dimension");
		}
		int length = matrix1.getSize();
		CleverPointer<double[]> ans = CleverPointer.createDouble(1);
		
		CudaDirector.innerProduct(matrix1.getPointer(), matrix2.getPointer(), length, ans);
		
		return ans;
	}

}
