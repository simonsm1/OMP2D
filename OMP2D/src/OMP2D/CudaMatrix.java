package OMP2D;

public class CudaMatrix extends Matrix {
	private CleverPointer<double[]> dMatrix;

	public CudaMatrix(int width, double[] vals) {
		super(width, vals);
		dMatrix = CleverPointer.copyDouble(vals);
	}
	
	@Override
	public void scale(double factor) {
		CudaDirector.scale(dMatrix, factor);
	}
	
	@Override
	public double[] to1DArray() {
		return dMatrix.getArray();
	}

}
