package OMP2D;

public class Vector extends Matrix {

	public Vector(int width) {
		super(width);
		super.height = 1;
		super.matrix = new double[width];
	}
	
	public Vector(double... d) {
		super(d.length, d);
		//super.height = 1;
		super.matrix = d;
	}
	
	public double get(int index) {
		return matrix[index];
	}
	
	/**
	 * @param vector
	 * @return
	 * @throws IncompatibleDimensionsException 
	 */
	public void normalize() throws IncompatibleDimensionsException {
		double normVector = getNorm();
		scale(1/normVector);
	}
	
	/**
	 * Performs the Euclidean norm operation on this vector
	 * @param vector
	 * @return The Euclidean norm 
	 * @throws IncompatibleDimensionsException 
	 */
	public double getNorm() throws IncompatibleDimensionsException {
		return Math.sqrt(innerProduct(this, this));
	}
	
	/**
	 * Performs the inner product operation on two vectors
	 * @param vector1
	 * @param vector2
	 * @return The inner product
	 * @throws IncompatibleDimensionsException
	 */
	public static double innerProduct(Vector vector1, Vector vector2) throws IncompatibleDimensionsException {
		if(vector1.getSize() != vector2.getSize()) {
			throw new IncompatibleDimensionsException();
		}
		
		int length = vector1.getSize();
        double innerProduct = 0;
        for(int i = 0; i < length; i++) {
            innerProduct += vector1.get(i) * vector2.get(i);  
        }
        return innerProduct;
	}
	
	/**
	 * Applies the Kronecker product of two vectors (QUESTION should this be matrices as well? No)
	 * @param vector1
	 * @param vector2
	 * @return
	 * @throws IncompatibleDimensionsException 
	 */
	public static Vector kronecker(Vector vector1, Vector vector2) throws IncompatibleDimensionsException {
		if(vector1.getSize() != vector2.getSize()) {
			throw new IncompatibleDimensionsException();
		}
		int size = vector1.getSize();
		
		double[] result = new double[size*size];

		for(int j = 0; j < size; j++) {
			double scaleFactor = vector1.get(j);
			for(int i = 0; i < size; i++) {
				result[j*size + i] = scaleFactor*vector2.get(i); 
			}
		}
		//return new Vector(result);
		Vector v = new Vector(result);
		v.transpose();
		return v;
	}
	
	@Override
	public Vector clone() {
		Vector v = new Vector(matrix.clone());
		v.transposed = transposed;
		return v;
	}

}
