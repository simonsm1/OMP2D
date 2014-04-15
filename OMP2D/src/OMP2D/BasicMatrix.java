package OMP2D;

public class BasicMatrix 
{
	
	/**
	 * Performs the dot product of two matrices 
	 * @param matrix1 A matrix defined as \f$M^{x \times y}\f$
	 * @param matrix2 A matrix defined as \f$N^{y \times z}\f$
	 * @return The resulting matrix
	 * @throws BadDimensionsException
	 */
	public static BasicMatrix multiply(BasicMatrix matrix1, BasicMatrix matrix2) throws BadDimensionsException{
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
		
		return new BasicMatrix(qMax, result);
	}
	
	/**
	 * Applies the Kronecker product of two vectors
	 * @param vector1
	 * @param vector2
	 * @param curRowAtom 
	 * @param curColAtom 
	 * @return
	 * @throws BadDimensionsException 
	 */
	public static double[] kronecker(BasicMatrix vector1, BasicMatrix vector2, int curColAtom, int curRowAtom) throws BadDimensionsException {
		if(vector1.getWidth() != vector2.getWidth()) {
			throw new BadDimensionsException(); 
		}
		int size = vector1.getWidth();
		
		double[] result = new double[size*size];

		for(int j = 0; j < size; j++) {
			double scaleFactor = vector1.get(j, curColAtom);
			for(int i = 0; i < size; i++) {
				result[j*size + i] = scaleFactor*vector2.get(i, curRowAtom); 
			}
		}
		return result;
	}
	
	protected double[] matrix;
	
	protected int width, height;
	
	protected boolean transposed = false;
	
	protected double maxAbs;
	protected int maxAbsRow, maxAbsCol;
	
	public BasicMatrix(int width, double... vals) {
		this.width = width;
		if(vals.length % width == 0) {
			this.height = vals.length / width;
		} else {
			this.height = (vals.length / width) + 1;
			//TODO add 0 padding
		}
		matrix = vals;
	}
	
	public BasicMatrix(int width, int height) {
		this.width = width;
		this.height = height;
		matrix = new double[height*width];
	}
	
	public double abs(int x, int y) {
		return Math.abs(matrix[x*y]);
	}
	
	public void add(BasicMatrix m) {
		for(int j = 0; j < m.getHeight(); j++) {
			for(int i = 0; i < m.getWidth(); i++) {
				matrix[j*width + i] += m.get(i, j);
			}
		}
	}
	
	public BasicMatrix clone() {
		BasicMatrix m = new BasicMatrix(width, matrix.clone());
		m.transposed = transposed;
		return m;
	}
	
	public double get(int i) {
		return matrix[i];
	}
	
	public double get(int x, int y) {
		return matrix[y*width + x];
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
	
	public int getSize() {
		return matrix.length;
	}
	
	public int getWidth() {
		return width;
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
	
	public void subtract(BasicMatrix m) {
		for(int j = 0; j < m.getHeight(); j++) {
			for(int i = 0; i < m.getWidth(); i++) {
				matrix[j*width + i] -= m.get(i, j);
			}
		}
	}
	
	public double[] to1DArray() {
		return matrix;
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
