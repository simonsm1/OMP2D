package OMP2D;

import java.util.ArrayList;
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
	 * Returns the inner product of two matrices
	 * @param matrix1
	 * @param matrix2
	 * @return
	 * @throws IncompatibleDimensionsException
	 */
	public static double innerProduct(Matrix matrix1, double[] matrix2) throws BadDimensionsException {
		if(matrix1.getSize() != matrix2.length) {
			throw new BadDimensionsException("Matrices should be the same dimension");
		}
		
		double innerProduct = 0;
		for (int i = 0; i < matrix1.getSize(); i++) {
			innerProduct += matrix1.get(i)*matrix2[i];
		}
		return innerProduct;
	}
	
	/**
	 * Returns the inner product of two matrices
	 * @param matrix1
	 * @param matrix2
	 * @return
	 * @throws IncompatibleDimensionsException
	 */
	public static double innerProduct(double[] matrix1, double[] matrix2) throws BadDimensionsException {
		if(matrix1.length != matrix2.length) {
			throw new BadDimensionsException("Matrices should be the same dimension");
		}
		
		double innerProduct = 0;
		for (int i = 0; i < matrix1.length; i++) {
			innerProduct += matrix1[i]*matrix2[i];
		}
		return innerProduct;
	}
	
	/**
	 * Applies the Kronecker product of two vectors
	 * @param vector1
	 * @param vector2
	 * @return
	 * @throws BadDimensionsException 
	 */
	public static double[] kronecker(double[] vector1, double[] vector2) throws BadDimensionsException {
		if(vector1.length != vector2.length) {
			throw new BadDimensionsException();
		}
		int size = vector1.length;
		
		double[] result = new double[size*size];

		for(int j = 0; j < size; j++) {
			double scaleFactor = vector1[j];
			for(int i = 0; i < size; i++) {
				result[j*size + i] = scaleFactor*vector2[i]; 
			}
		}
		return result;
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

		Matrix result = new Matrix(qMax);
		
		for(int m = 0; m < mMax; m++) {
			double[] row = new double[qMax];
			for(int q = 0; q < qMax; q++) {			
				for(int product = 0; product < pMax; product++){
					row[q] += matrix1.get(product, m) * matrix2.get(q, product);
				}
			}
			result.addRow(row);
		}

		return result;
	}
	
	public static double[] transpose(double[] matrix, int height, int width) {
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
		return newMatrix;
	}
	
	public static void scale(double[] vector, double factor) {
		for(int i = 0; i < vector.length; i++) {
			vector[i] *= factor;
		}
	}
	
	protected ArrayList<double[]> matrix;
	
	protected int width, height;
	
	protected boolean transposed = false;
	
	protected double maxAbs;
	protected int maxAbsRow, maxAbsCol;
	
	public Matrix(int width, double[]... vals) {
		matrix = new ArrayList<double[]>();
		this.width = width;
		
		for(double[] array : vals) {
			for(int index = 0; index < array.length; index += width) {
				double[] row = new double[width];
				try {
					System.arraycopy(array, index, row, 0, width);
				} catch(IndexOutOfBoundsException e) {
					addPadding(array, index, row);
				}
				matrix.add(row);
			}
		}
		
		height = matrix.size();
	}
	
	private void addPadding(double[] array, int startPos, double[] row) {
		for(int i = 0; i < row.length; i++) {
			if(startPos + i < array.length) {
				row[i] = array[startPos + i];
			} else {
				row[i] = 0;
			}
		}
	}

	public Matrix(int width, int height) {
		this.width = width;
		this.height = height;
		for(int i = 0; i < height; i++) {
			matrix.add(new double[width]);
		}
	}
	
	public void add(Matrix m) {
		for(int j = 0; j < m.getHeight(); j++) {
			double[] row = matrix.get(j);
			for(int i = 0; i < m.getWidth(); i++) {
				row[i] += m.get(i, j);
			}
		}
	}
	
	public void addRow(double[] row) {
		matrix.add(row);
		height++;
	}
	
	public double get(int i) {
		int y = i / width;
		int x = i % width;
		return get(x, y);
	}
	
	public double get(int x, int y) {
		if(!transposed) {
			return matrix.get(y)[x];
		} else {
			return matrix.get(x)[y];
		}

	}
	
	public double[] getCol(int c) {
		double[] col = new double[height];
		for(int i = 0; i < height; i++) {
			col[i] = matrix.get(i)[c];
		}
		
		return col;
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
	
	public ArrayList<double[]> getMatrixValues() {
		return matrix;
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
	 * Finds of mean of all values in this matrix.
	 * @return The mean value
	 */
	public double getMean() {
		return getSum()/getSize();
	}
	
	/**
	 * Performs the Euclidean norm operation on this vector
	 * @param vector
	 * @return The Euclidean norm 
	 * @throws BadDimensionsException
	 */
	public double getRowNorm(int rowIndex) throws BadDimensionsException{
		double innerProduct = 0;
		double[] row = matrix.get(rowIndex);
		for(int i = 0; i < row.length; i++) {
			innerProduct += row[i]*row[i];
		}
		return Math.sqrt(innerProduct);
	}
	
	public double[] getRow(int r) {
		return matrix.get(r);
	}
	
	public int getSize() {
		return width*height;
	}
	
	public double getSum() {
		double sum = 0;
		for(double[] row : matrix) {
			for(double d: row) {
				sum += d;
			}
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

		//Matrix replacement = new Matrix(qMax);
		ArrayList<double[]> replacement = new ArrayList<double[]>();
		
		for(int m = 0; m < mMax; m++) {
			double[] row = new double[qMax];
			for(int q = 0; q < qMax; q++) {			
				for(int product = 0; product < pMax; product++){
					row[q] += this.get(product, m) * matrix.get(q, product);
				}
			}
			replacement.add(row);
			//replacement.addRow(row);
		}
		this.matrix = replacement;
		this.width = qMax;
		this.height = pMax;
	}

	public double normalizeRow(int rowIndex) throws BadDimensionsException{
		double rowNorm = getRowNorm(rowIndex);
		double[] row = matrix.get(rowIndex);
		for(int i = 0; i < width; i++) {
			row[i] /= rowNorm;
		}
		return rowNorm;
	}
	
	/**
	 * Scales the matrix by a given factor
	 * @param factor
	 */
	public void scale(double factor) {
		for(double[] row : matrix) {
			for(int i = 0; i < width; i++) {
				row[i] *= factor;
			}
		}
	}
	
	public void set(int i, double value) {
		int y = i / width;
		int x = i % width;
		set(x, y, value);
	}
	
	public void set(int x, int y, double value) {
		matrix.get(y)[x] = value;
	}
	
	public void subtract(Matrix m) {
		for(int j = 0; j < m.getHeight(); j++) {
			double[] row = matrix.get(j);
			for(int i = 0; i < m.getWidth(); i++) {
				row[i] -= m.get(i, j);
			}
		}
	}
	
	public void subtract(double[] m, double scalar) {
		for(int j = 0; j < this.getHeight(); j++) {
			double[] row = matrix.get(j);
			for(int i = 0; i < this.getWidth(); i++) {
				row[i] -= m[j*width+i]*scalar;
			}
		}
	}
	
	public double[] to1DArray() {
		double[] array = new double[height*width];
		for(int j = 0; j < height; j++) {
			System.arraycopy(matrix.get(j), 0, array, j*width, width);
		}
		return array;
	}
	
	public void transpose() {
		transposed = !transposed;
		int temp = width;
		width = height;
		height = temp;
	}

	/**
	 * Finds the largest absolute value of a given vector
	 */
	public void updateMaxAbs() {
		for(int j = 0; j < height; j++) {
			double[] row = matrix.get(j);
			for(int i = 0; i < width; i++) {
				double abs = Math.abs(row[i]);
				if(abs > maxAbs) {
					maxAbs = abs;
					maxAbsRow = j;
					maxAbsCol = i;
				}
			}
		}
	}
}
