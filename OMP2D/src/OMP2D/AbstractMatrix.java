package OMP2D;

import java.util.Arrays;

import OMP2D.JavaMatrix.IncompatibleDimensionsException;

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
	public abstract Vector getRow(int r);	
	public abstract Matrix getRow(int start, int end);
	
	public abstract void add(Matrix m);
	
	public abstract double getFrobeniusNorm();
	
	public abstract Matrix getSubMatrix(int startX, int startY, int endX, int endY);
	
	public abstract Matrix getSubMatrix(int blockSize, int iteration);
	

	public abstract void multiply(Matrix matrix) throws IncompatibleDimensionsException;
	public abstract void multiply(Vector vector) throws IncompatibleDimensionsException;
	
	public abstract void set(int x, int y, double value);
	public abstract void scale(double factor);

	public abstract void updateMaxAbs();
	
	public abstract double abs(int x, int y);
	
	public abstract double[] to1DArray();
	
	public abstract double[][] to2DArray();
	
	public abstract double getMean();
	
	public abstract int getWidth();
	
	public abstract int getHeight();
	
	public abstract int getSize();
	
	public abstract double getSum();
	
	public abstract double getMaxAbs();

	public abstract int getMaxAbsRow();

	public abstract int getMaxAbsCol();
}
