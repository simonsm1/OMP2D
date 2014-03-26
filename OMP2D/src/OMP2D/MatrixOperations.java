package OMP2D;

public class MatrixOperations {

	/**
	 * Performs the inner product operation on two vectors
	 * @param vector1
	 * @param vector2
	 * @return The inner product
	 * @throws IncompatibleDimensionsException
	 */
	public static double realInnerProduct(double[] vector1, double[] vector2) throws IncompatibleDimensionsException {
		if(vector1.length != vector2.length) {
			throw new IncompatibleDimensionsException();
		}
		
		int length = vector1.length;
        double innerProduct = 0;
        for(int i = 0; i < length; i++) {
            innerProduct += vector1[i] * vector2[i];  
        }
        return innerProduct;
	}
	
	/**
	 * @param vector
	 * @return
	 * @throws IncompatibleDimensionsException 
	 */
	public static double[] normalize(double[] vector) throws IncompatibleDimensionsException {
		double normVector = norm(vector);
		return scaleVector(vector, 1/normVector);
	}
	
	/**
	 * Performs the Euclidean norm operation on a vector
	 * @param vector
	 * @return The Euclidean norm 
	 * @throws IncompatibleDimensionsException 
	 */
	public static double norm(double[] vector) throws IncompatibleDimensionsException {
		return Math.sqrt(realInnerProduct(vector, vector));
	}
	
	/**
	 * Adds and scales the first vector TODO This should be handle outside this class
	 * @param vector1
	 * @param vector2
	 * @param scalar
	 * @return
	 * @throws IncompatibleDimensionsException 
	 */
	public static double[] addVectors(double[] vector1, double[] vector2, double scalar) throws IncompatibleDimensionsException {
		return addVectors(scaleVector(vector1, scalar), vector2);
	}
	
	/**
	 * Adds two vectors of the same length together
	 * @param vector1
	 * @param vector2
	 * @return The resulting vector 
	 * @throws IncompatibleDimensionsException 
	 */
	public static double[] addVectors(double[] vector1, double[] vector2) throws IncompatibleDimensionsException {
		if(vector1.length != vector2.length) {
			throw new IncompatibleDimensionsException();
		}
		
		int length = vector1.length;
		double[] result = new double[length];
		
		for(int i = 0; i < length; i++) {
			result[i] = vector1[i] + vector2[i];
		}
		return result;
	}
	
	/**
	 * Multiplies a given matrix and vector
	 * @param matrix A matrix of dimensions (m,n) 
	 * @param vector A vector of dimension m
	 * @param thing (QUESTION I'm not sure what this is...)
	 * @return the resulting vector
	 * @throws IncompatibleDimensionsException 
	 */
	public static double[] multiplyMatrixVector(double[][] matrix, double[] vector, char thing) throws IncompatibleDimensionsException {
		int n = vector.length;
		int m = matrix[0].length; //width
		
		if(n != m) {
			throw new IncompatibleDimensionsException();
		}
		
		double result[] = new double[n];
		
		if(thing == 'T') {
			for(int i = 0; i < n; i++) {
				result[i] = realInnerProduct(matrix[i], vector);
			}
		} else {
			for(int i = 0; i < m; i++) {
				result[i] = 0;
				for(int j = 0; j < n; j++) {
					result[i] += matrix[i][j] * vector[j];
				}
			}
		}
		
		return result;
	}
	
	/**
	 * Scales a vector by a given factor
	 * @param vector
	 * @param factor
	 * @return The vector scaled
	 */
	public static double[] scaleVector(double[] vector, double factor) {
		for(int i = 0; i < vector.length; i++) {
			vector[i] = vector[i] * factor;
		}
		return vector;
	}
	
	/**
	 * Performs the outer product
	 * @param matrix A transposed vector
	 * @param vector
	 * @param vectorScalar TODO this should be done outside this method
	 * @return
	 * @throws IncompatibleDimensionsException 
	 */
	public static double[][] outerProduct(double[][] matrix, double[] vector, double[] vectorScalar) throws IncompatibleDimensionsException {
		int n = vector.length;
		int m = matrix[0].length;
		
		if(m != 1) {
			throw new IncompatibleDimensionsException("The width of the transposed vector should be 1", 1, m);
		} else if(n != matrix.length) {
			throw new IncompatibleDimensionsException("The length of each vector should be equal");
		}
		
		for(int j = 0; j < m; j++) { //TODO shouldn't need this if width is 1
			for(int i = 0; i < n; i++) {
				matrix[j][i] -= vectorScalar[j] * vector[i]; 
			}
		}
		return matrix;
	}
	
	/**
	 * Applies the Kronecker product of two vectors (QUESTION should this be matrices as well? No)
	 * @param vector1
	 * @param vector2
	 * @return
	 * @throws IncompatibleDimensionsException 
	 */
	public static double[] kronAtom(double[] vector1, double[] vector2) throws IncompatibleDimensionsException {
		if(vector1.length != vector2.length) {
			throw new IncompatibleDimensionsException();
		}

		int length = vector1.length;
		double[] result = new double[length*length];
		for(int i = 0; i < length; i++) {
			double scaleFactor = vector1[i];
			for(int j = 0; j < length; j++) {
				result[j*i] = scaleFactor*vector2[j]; 
			}
		}
		return result;
	}
	
	/**
	 * Finds the largest absolute value of a given vector
	 * @param vector
	 * @return the largest absolute value
	 */
	public static double maxAbs(double[] vector) {
		double maxAbsValue = 0;
		for(int i = 0; i < vector.length; i++) {
			double abs = Math.abs(vector[i]);
			if(abs > maxAbsValue) {
				maxAbsValue = abs;
			}
		}
		return maxAbsValue;
	}
	
	/**
	 * Finds the largest absolute value of a given matrix
	 * @param matrix
	 * @return
	 */
	public static double maxAbs(double[][] matrix) {
		double maxAbsValue = 0;
		for(int i = 0; i < matrix.length; i++) {
			double[] vector = matrix[i];
			double abs = maxAbs(vector);
			if(abs > maxAbsValue) {
				maxAbsValue = abs;
			}
		}
		return maxAbsValue;
	}
	
	/**
	 * Fills a vector with a given value up the <i>n</i>-th element  
	 * Not necessary in Java for we have Array.fill()
	 * @param vector The vector to be filled
	 * @param value The value which will be inserted
	 * @param num The last numbered index which will be filled 0 to num inclusive 
	 */
	@Deprecated
	public static void allocateElements(double[] vector, double value, int num) {
		for(int n = 0; n < num; n++) {
			vector[n] = value;
		}
	}
	
	/**
	 * Performs the dot product of two matrices 
	 * @param matrix1 A matrix defined as \f$M^{x \times y}\f$
	 * @param matrix2 A matrix defined as \f$N^{y \times z}\f$
	 * @param thing1 (QUESTION not sure what this is)
	 * @param thing2
	 * @return The resulting matrix
	 * @throws IncompatibleDimensionsException 
	 */
	public static double[][] matrixMultiply(double[][] matrix1, double[][] matrix2, char thing1, char thing2) throws IncompatibleDimensionsException {
		//matrix1[m][n] matrix2[p][q]
		int mMax = matrix1.length; int nMax = matrix1[0].length;
		int pMax = matrix2.length; int qMax = matrix2[0].length;
		
		if(nMax != pMax) {
			throw new IncompatibleDimensionsException("Expected matrices of (m,n)x(n,q)");
		}

		double[][] result = new double[mMax][qMax];
		
		for(int q = 0; q < pMax; q++) {
			for(int m = 0; m < mMax; m++) {			
				for(int product = 0; product < nMax; product++){
					result[m][q] += matrix1[m][product] * matrix2[product][q];
				}
			}
		}
		return result;
	}
	
	 /**
	  * Performs the (QUESTION is there an official name for this?) Sandwich
	  * @param vector1
	  * @param matrix
	  * @param vector2
	  * @return The resulting value
	  * @throws IncompatibleDimensionsException 
	  */
	public static double vectorMatrixVector(double[] vector1, double[][] matrix, double[] vector2) throws IncompatibleDimensionsException {
		//vector1[n] X matrix[n][m] X vector2[m]
		if(vector1.length != matrix.length) {
			throw new IncompatibleDimensionsException("The length and height of vector1 and the matrix do not match.");
		} else if(matrix[0].length != vector2.length) {
			throw new IncompatibleDimensionsException("The length and width of vector2 and the matrix do not match.");
		}

		double[] temp = multiplyMatrixVector(matrix, vector1, 'N');
		double result = realInnerProduct(temp, vector2);
		return result;
	}
	
	@SuppressWarnings("serial")
	protected static class IncompatibleDimensionsException extends Exception {

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
