package OMP2D;

import java.util.Arrays;

public class Matrix 
{
	
	/**
	 * Returns the inner product of two matrices
	 * @param matrix1
	 * @param matrix2
	 * @return
	 * @throws IncompatibleDimensionsException
	 */
	public static double innerProduct(Matrix matrix1, Matrix matrix2) throws BadDimensionsException {
		if(matrix1.getSize() != matrix2.getSize()) {
			throw new BadDimensionsException("Matrices should be the same dimension");
		}
		
		double innerProduct = 0;
		for (int i = 0; i < matrix1.getSize(); i++) {
			innerProduct += matrix1.get(i)*matrix2.get(i);
		}
		return innerProduct;
	}
	
	/**
	 * Performs the dot product of two matrices 
	 * @param matrix1 A matrix defined as \f$M^{x \times y}\f$
	 * @param matrix2 A matrix defined as \f$N^{y \times z}\f$
	 * @return The resulting matrix
	 * @throws BadDimensionsException
	 */
	public static Matrix multiply(Matrix matrix1, Matrix matrix2) throws BadDimensionsException{
		//matrix1[m][n] matrix2[p][q]
		int mMax = matrix1.getHeight(); int nMax = matrix1.getWidth();
		int pMax = matrix2.getHeight(); int qMax = matrix2.getWidth();
		
		if(nMax != pMax) {
			throw new BadDimensionsException("Expected matrices of (m,n)x(n,q)\n" +
					"Recieved (" + mMax + "," + nMax + ")x(" + pMax + "," + qMax + ")");
		}

		double[] result = new double[mMax*qMax];

		for(int m = 0; m < mMax; m++) {		
			for(int q = 0; q < qMax; q++) {
				for(int product = 0; product < nMax; product++) {
					result[m*qMax + q] += matrix1.get(product, m) * matrix2.get(q, product);
				}
			}
		}
		
		return new Matrix(qMax, result);
	}
	protected double[] matrix;
	
	protected int width, height;
	
	protected boolean transposed = false;
	
	protected double maxAbs;
	protected int maxAbsRow, maxAbsCol;
	
	public Matrix(int width, double... vals) {
		this.width = width;
		if(vals.length % width == 0) {
			this.height = vals.length / width;
		} else {
			this.height = (vals.length / width) + 1;
			//TODO add 0 padding
		}
		matrix = vals;
	}
	
	public Matrix(int width, double[] vals, double[] vals2) {
		this.width = width;
		int length = vals.length+vals2.length;
		if(length % width == 0) {
			this.height = length / width;
		} else {
			this.height = (length / width) + 1;
		} 
		matrix = new double[length];
		System.arraycopy(vals, 0, matrix, 0, vals.length);
		System.arraycopy(vals2, 0, matrix, vals.length, vals2.length);
	}
	
	public Matrix(int width, int height) {
		this.width = width;
		this.height = height;
		matrix = new double[height*width];
	}
	
	public double abs(int x, int y) {
		return Math.abs(matrix[x*y]);
	}
	
	public void add(Matrix m) {
		for(int j = 0; j < m.getHeight(); j++) {
			for(int i = 0; i < m.getWidth(); i++) {
				matrix[j*width + i] += m.get(i, j);
			}
		}
	}
	
	public Matrix clone() {
		Matrix m = new Matrix(width, matrix.clone());
		m.transposed = transposed;
		return m;
	}
	
	public double get(int i) {
		return matrix[i];
	}
	
	public double get(int x, int y) {
		int index = y*width + x;
		return matrix[index];
	}
	
	public Vector getCol(int c) {
		this.transpose();
		Vector v = getRow(c);
		this.transpose();
		return v;
	}
	
	public double getFrobeniusNorm() {
		double norm = 0;
		for(int i = 0; i < this.getSize(); i++) {
			norm += this.get(i)*this.get(i);
		}
		return norm;
	}
	
	public int getHeight() {
		return height;
	}
	
	public double getMaxAbs() {
		return maxAbs;
	}
	
	public int getMaxAbsCol() {
		return maxAbsCol;
	}
	
	public int getMaxAbsRow() {
		return maxAbsRow;
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
	
	/**
	 * Performs the Euclidean norm operation on this vector
	 * @param vector
	 * @return The Euclidean norm 
	 * @throws BadDimensionsException
	 */
	public double getNorm() throws BadDimensionsException{
		return Math.sqrt(innerProduct(this, this));
	}
	
	/**
	 * Performs the Euclidean norm operation on this vector
	 * @param vector
	 * @return The Euclidean norm 
	 * @throws BadDimensionsException
	 */
	public double getNorm(int row) throws BadDimensionsException{
		return Math.sqrt(innerProduct(this.getRow(row), this.getRow(row)));
	}
	
	public Vector getRow(int r) {
		double[] temp = Arrays.copyOfRange(matrix, width*r, width*(r+1));
		return new Vector(temp);
	}
	
	public Matrix getRow(int start, int end) {
		double[] temp = Arrays.copyOfRange(matrix, width*start, width*end);
		return new Matrix((end - start), temp);
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
	
	public int getWidth() {
		return width;
	}
	
	/**
	 * Multiplies this matrix with another. Performs the dot product of two matrices
	 * @param matrix1 A matrix defined as \f$M^{x \times y}\f$
	 * @param matrix2 A matrix defined as \f$N^{y \times z}\f$
	 * @return The resulting matrix
	 * @throws BadDimensionsException
	 */
	public void multiply(Matrix matrix) throws BadDimensionsException{
		//matrix1[m][n] matrix2[p][q]
		int mMax = this.getHeight(); int nMax = this.getWidth();
		int pMax = matrix.getHeight(); int qMax = matrix.getWidth();
		
		if(nMax != pMax) {
			throw new BadDimensionsException("Expected matrices of (m,n)x(n,q)\n" +
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
	 * Multiplies this matrix and vector
	 * @param vector A vector of dimension m
	 * @return the resulting vector
	 * @throws BadDimensionsException
	 */
	public void multiply(Vector vector) throws BadDimensionsException{
		int n = vector.getSize();
		int m = this.width;
		
		if(n != m) {
			throw new BadDimensionsException();
		}
		
		double result[] = new double[this.height];
		
		for(int j = 0; j < this.height; j++) {
			result[j] = 0;
			for(int i = 0; i < n; i++) {
				result[j] += matrix[m*j + i] * vector.get(i);
			}
		}
		
		matrix = result;
		width = 1;
		height = m;
	}

	public double normalizeRow(int row) throws BadDimensionsException{
		double normVector = getNorm(row);
		for(int i = 0; i < width; i++) {
			set(i, row, get(i, row)/normVector);
		}
		return normVector;
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
	
	public void set(int x, double value) {
		matrix[x] = value;
	}
	
	public void set(int x, int y, double value) {
		matrix[width*y + x] = value;
	}
	
	public void subtract(Matrix m) {
		for(int j = 0; j < m.getHeight(); j++) {
			for(int i = 0; i < m.getWidth(); i++) {
				matrix[j*width + i] -= m.get(i, j);
			}
		}
	}
	
	public double[] to1DArray() {
		return matrix;
	}
	
	public void transpose() {
		double[] newMatrix = new double[matrix.length];
		for(int j = 0; j < height; j++) {
			for(int i = 0; i < width; i++) {
				if(width > height) {
					newMatrix[i*height+j] = matrix[j*width+i];	
				} else {
					newMatrix[i*height+j] = matrix[j*width+i];
				}
			}
		}
		int temp = width;
		width = height;
		height = temp;
		matrix = newMatrix;
		transposed = !transposed;
	}

	/**
	 * Finds the largest absolute value of a given vector
	 * @return the largest absolute value
	 */
	public void updateMaxAbs() {
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
}
