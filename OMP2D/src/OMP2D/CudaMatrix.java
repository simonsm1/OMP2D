package OMP2D;

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
	
	public static double innerProduct(CudaMatrix matrix1, CudaMatrix matrix2) throws BadDimensionsException {
		if(matrix1.getSize() != matrix2.getSize()) {
			throw new BadDimensionsException("Matrices should be the same dimension");
		}
		int length = matrix1.getSize();
		
		CudaDirector.innerProduct(matrix1.getPointer(), matrix2.getPointer(), length);
		
		return 0.0;
	}

}
