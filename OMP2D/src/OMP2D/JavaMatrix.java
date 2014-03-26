package OMP2D;

import java.util.Arrays;

public class JavaMatrix extends AbstractMatrix
{
	protected double[] matrix;
	protected int width, height;
	protected boolean transposed = false;
	protected double maxAbs;
	protected int maxAbsRow, maxAbsCol;
	
	public JavaMatrix(int width, int height) {
		this.width = width;
		this.height = height;
		matrix = new double[height*width];
	}
	
	public JavaMatrix(int width, double... vals) {
		this.width = width;
		if(vals.length % width == 0) {
			this.height = vals.length / width;
		} else {
			this.height = (vals.length / width) + 1;
			//TODO add 0 padding
		}
		matrix = vals;
	}
	
	public Matrix clone() {
		return new Matrix(width, matrix.clone());
	}
	
	/* Old method which didn't actually transpose
	public void transpose() {/*
		int temp = width;
		width = height;
		height = temp;
		transposed = !transposed;
	}*/
	
	public void transpose() {
		double[] newMatrix = new double[matrix.length];
		for(int j = 0; j < height; j++) {
			for(int i = 0; i < width; i++) {
				if(width*i+j >= 1279) {
					System.out.println("close");
				}
				newMatrix[j*height+i] = matrix[j*i+j];
			}
		}
		int temp = width;
		width = height;
		height = temp;
		matrix = newMatrix;
		transposed = !transposed;
	}
	
	/* for use with old transpose
	public double get(int x, int y) {
		int index;
		if(!transposed) {
			index = x*y + x;
		} else {
			int temp = x;
			x = y;
			y = temp;
			index = y*width + x;
		}
		return matrix[index];
	}*/
	
	public double get(int x, int y) {
		int index = y*width + x;
		return matrix[index];
	}
	
	public double get(int i) {
		return matrix[i];
	}
	
	public Vector getRow(int r) {
		double[] temp = Arrays.copyOfRange(matrix, width*r, width*(r+1));
		return new Vector(temp);
	}
	
	public Matrix getRow(int start, int end) {
		double[] temp = Arrays.copyOfRange(matrix, width*start, width*end);
		return new Matrix((end - start), temp);
	}
	
	public void add(Matrix m) {
		for(int j = 0; j < m.getHeight(); j++) {
			for(int i = 0; i < m.getWidth(); i++) {
				matrix[j*i + i] += m.get(i, j);
			}
		}
	}
	
	public double getFrobeniusNorm() {
		double norm = 0;
		try {
			//norm = Math.sqrt(Vector.innerProduct((Vector) this, (Vector) this));
			norm = Math.sqrt(JavaMatrix.innerProduct(this, this));
		} catch (IncompatibleDimensionsException e) {
			e.printStackTrace();
		}
		return norm;
	}
	
	public Matrix getSubMatrix(int startX, int startY, int endX, int endY) {
		int deltaX = endX - startX; int deltaY = endY - startY;
		double[] subMatrix = new double[deltaX*deltaY];
		for(int y = 0; y < deltaY; y++) {
			System.arraycopy(matrix, startX+(startY*y), subMatrix, y*deltaY, deltaX);
		}
		return new Matrix(deltaX, subMatrix);
	}
	
	public Matrix getSubMatrix(int blockSize, int iteration) {
		int start = blockSize*iteration;
		return getSubMatrix(start, start, start+blockSize, start+blockSize);
	}
	
	/**
	 * Applies the Kronecker product of two vectors (QUESTION should this be matrices as well? No)
	 * @param vector1
	 * @param vector2
	 * @return
	 * @throws IncompatibleDimensionsException 
	 */
	/*public static Matrix kronecker(Matrix matrix1, Matrix matrix2) throws IncompatibleDimensionsException {
		if(matrix1.getSize() != matrix2.getSize()) {
			throw new IncompatibleDimensionsException();
		}
		int size = matrix1.getSize();
		Date d;
		
		double[] result = new double[size*size];

		for(int i = 0; i < size; i++) {
			double scaleFactor = vector1.get(i);
			for(int j = 0; j < size; j++) {
				result[j*i + i] = scaleFactor*vector2.get(j); 
			}
		}
		return new Vector(result);
	}*/
	
	/**
	 * Performs the dot product of two matrices
	 * DOES NOT WORK for matrices which are wider TODO copy across values not acted on 
	 * @param matrix1 A matrix defined as \f$M^{x \times y}\f$
	 * @param matrix2 A matrix defined as \f$N^{y \times z}\f$
	 * @param thing1 (QUESTION not sure what this is)
	 * @param thing2
	 * @return The resulting matrix
	 * @throws IncompatibleDimensionsException 
	 */
	public void multiply(Matrix matrix) throws IncompatibleDimensionsException {
		//matrix1[m][n] matrix2[p][q]
		int mMax = this.getHeight(); int nMax = this.getWidth();
		int pMax = matrix.getHeight(); int qMax = matrix.getWidth();
		
		if(nMax != pMax) {
			throw new IncompatibleDimensionsException("Expected matrices of (m,n)x(n,q)\n" +
					"Recieved (" + mMax + "," + nMax + ")x(" + pMax + "," + qMax + ")");
		}

		double[] result = new double[mMax*qMax];
		
		for(int m = 0; m < mMax; m++) {
			for(int q = 0; q < qMax; q++) {			
				for(int product = 0; product < pMax; product++){
					result[q*qMax + m] += this.get(product, m) * matrix.get(q, product);
				}
			}
		}
		this.matrix = result;
		this.width = qMax;
		this.height = pMax;
	}
	
	/**
	 * Multiplies a given matrix and vector
	 * @param matrix A matrix of dimensions (m,n) 
	 * @param vector A vector of dimension m
	 * @param thing (QUESTION I'm not sure what this is...)
	 * @return the resulting vector
	 * @throws IncompatibleDimensionsException 
	 */
	public void multiply(Vector vector) throws IncompatibleDimensionsException {
		int n = vector.getSize();
		int m = this.width;
		
		if(n != m) {
			throw new IncompatibleDimensionsException();
		}
		
		double result[] = new double[n];
		
		for(int i = 0; i < m; i++) {
			result[i] = 0;
			for(int j = 0; j < n; j++) {
				result[i] += matrix[m*j + i] * vector.get(j);
			}
		}
		
		matrix = result;
		width = 1;
		height = m;
	}
	
	public void set(int x, int y, double value) {
		matrix[x*y + x] = value;
	}
	
	/**
	 * Scales a vector by a given factor
	 * @param vector
	 * @param factor
	 * @return The vector scaled
	 */
	public void scale(double factor) {
		for(int i = 0; i < matrix.length; i++) {
			matrix[i] = matrix[i] * factor;
		}
	}
	
	/**
	 * Finds the largest absolute value of a given vector
	 * @return the largest absolute value
	 *
	private double updateMaxAbs() {
		double maxAbsValue = 0;
		int maxAbsIndex = 0;
		for(int i = 0; i < matrix.length; i++) {
			double abs = Math.abs(matrix[i]);
			if(abs > maxAbsValue) {
				maxAbsValue = abs;
				maxAbsIndex = i;
			}
		}
		return maxAbsValue;
	}	*/

	public void updateMaxAbs() {
		int maxAbsIndex = 0;
		for(int j = 0; j < height; j++) {
			for(int i = 0; i < width; i++) {
				double abs = Math.abs(matrix[j*width+i]);
				if(abs > maxAbs) {
					maxAbs = abs;
					maxAbsRow = j;
					maxAbsCol = i;
				}
			}
		}
	}
	
	public double abs(int x, int y) {
		return Math.abs(matrix[x*y]);
	}
	
	public double[] to1DArray() {
		return matrix;
	}
	
	public double[][] to2DArray() {
		double[][] temp = new double[height][width];
		for(int j = 0; j < height; j++) {
			for(int i = 0; i < width; i++) {
				temp[j][i] = matrix[j*i + 1];
			}
		}
		return temp;
	}
	
	/**
	 * Performs the dot product of two matrices 
	 * @param javaMatrix A matrix defined as \f$M^{x \times y}\f$
	 * @param javaMatrix2 A matrix defined as \f$N^{y \times z}\f$
	 * @param thing1 (QUESTION not sure what this is)
	 * @param thing2
	 * @return The resulting matrix
	 * @throws IncompatibleDimensionsException 
	 */
	public static Matrix matrixMultiply(JavaMatrix javaMatrix, JavaMatrix javaMatrix2) throws IncompatibleDimensionsException {
		//matrix1[m][n] matrix2[p][q]
		int mMax = javaMatrix.getHeight(); int nMax = javaMatrix.getWidth();
		int pMax = javaMatrix2.getHeight(); int qMax = javaMatrix2.getWidth();
		
		if(nMax != pMax) {
			throw new IncompatibleDimensionsException("Expected matrices of (m,n)x(n,q)\n" +
					"Recieved (" + mMax + "," + nMax + ")x(" + pMax + "," + qMax + ")");
		}

		double[] result = new double[mMax*qMax];
		/*
		for(int q = 0; q < qMax; q++) {
			for(int m = 0; m < mMax; m++) {			
				for(int product = 0; product < nMax; product++) {
					result[q*qMax + m] += matrix1.get(product, m) * matrix2.get(q, product);
				}
			}
		}*/
		for(int m = 0; m < mMax; m++) {		
			for(int q = 0; q < qMax; q++) {
				for(int product = 0; product < nMax; product++) {
					result[m*qMax + q] += javaMatrix.get(product, m) * javaMatrix2.get(q, product);
				}
			}
		}
		
		Matrix m = new Matrix(qMax, result);
		//m.transpose();//There's got to be another way... TODO
		return m;
	}
	
	 /**
	  * Performs the (QUESTION is there an official name for this?) Sandwich
	  * @param vector1
	  * @param matrix
	  * @param vector2
	  * @return The resulting value
	  * @throws IncompatibleDimensionsException 
	  *
	public static double vectorMatrixVector(Vector vector1, Matrix matrix, Vector vector2) throws IncompatibleDimensionsException {
		//vector1[n] X matrix[n][m] X vector2[m]
		if(vector1.getSize() != matrix.getHeight()) {
			throw new IncompatibleDimensionsException("The length and height of vector1 and the matrix do not match.");
		} else if(matrix.getWidth() != vector2.getSize()) {
			throw new IncompatibleDimensionsException("The length and width of vector2 and the matrix do not match.");
		}

		matrix.multiply(vector1);
		return Vector.innerProduct((Vector) matrix, vector2);
	}*/
	
	/**
	 * Applies the Kronecker product of two vectors (QUESTION should this be matrices as well? No)
	 * @param vector1
	 * @param vector2
	 * @return
	 * @throws IncompatibleDimensionsException 
	 */
	public static Matrix kronecker(Matrix matrix1, Matrix matrix2) throws IncompatibleDimensionsException {
		if(matrix1.getSize() != matrix2.getSize()) {
			throw new IncompatibleDimensionsException();
		}//need to check other dims too
		int width = matrix1.getWidth();
		width = width * width;
		
		double[] result = new double[width*width];

		for(int i = 0; i < width; i++) {
			double scaleFactor = matrix1.get(i);
			for(int j = 0; j < width; j++) {
				result[j*i + i] = scaleFactor*matrix2.get(j); 
			}
		}
		return new Matrix(width, result);
	}
	
	/**
	 * Finds of mean of all values in a vector.
	 * @param vector
	 * @return The mean value
	 */
	public double getMean() {
		double sum = 0;
		for(double d: matrix) {
			sum += d;
		}
		return sum/getSize();
	}
	
	public int getWidth() {
		return width;
	}
	
	public int getHeight() {
		return height;
	}
	
	public int getSize() {
		return matrix.length;
	}
	
	public double getSum() {
		double sum = 0;
		for(double i : matrix) {
			sum += i;
		}
		return sum;
	}
	
	public double getMaxAbs() {
		return maxAbs;
	}

	public int getMaxAbsRow() {
		return maxAbsRow;
	}

	public int getMaxAbsCol() {
		return maxAbsCol;
	}
	
	public static double innerProduct(JavaMatrix javaMatrix, JavaMatrix javaMatrix2) throws IncompatibleDimensionsException {
		return JavaMatrix.matrixMultiply(javaMatrix, javaMatrix2).getSum();
	}

	@SuppressWarnings("serial")
	public static class IncompatibleDimensionsException extends Exception {

		public IncompatibleDimensionsException(String msg) {
			super(msg);
		}
		
		public IncompatibleDimensionsException() {
			super("Expected a combination of vectors and/or matrices of equal length");
		}
		
		public IncompatibleDimensionsException(String msg, int expected, int actual) {
			super(msg + "\nExpected Dimension: " + expected + "\nActual Dimension: " + actual);
		}
	}
}
