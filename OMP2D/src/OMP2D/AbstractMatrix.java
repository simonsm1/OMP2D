package OMP2D;

import OMP2D.BadDimensionsException;

public abstract class AbstractMatrix 
{
	protected double[] matrix;
	protected int width, height;
	protected boolean transposed = false;
	protected double maxAbs;
	protected int maxAbsRow, maxAbsCol;
	
	
	public Matrix clone() {
		return new Matrix(width, matrix.clone());
	}
	
	public abstract void transpose();
	public abstract double get(int x, int y);
	public abstract double get(int i);
	public abstract AbstractMatrix getRow(int r);	
	public abstract AbstractMatrix getRow(int start, int end);
	
	public abstract void add(Matrix m);
	
	public abstract double getFrobeniusNorm();

	public abstract void multiply(AbstractMatrix matrix) throws BadDimensionsException;
	
	public abstract void set(int x, int y, double value);
	public abstract void scale(double factor);

	public abstract void updateMaxAbs();
	
	public abstract double abs(int x, int y);
	
	public abstract double[] to1DArray();
	
	public abstract double getMean();
	
	public abstract int getWidth();
	
	public abstract int getHeight();
	
	public abstract int getSize();
	
	public abstract double getSum();
	
	public abstract double getMaxAbs();

	public abstract int getMaxAbsRow();

	public abstract int getMaxAbsCol();
}
