package OMP2D;

public class OMP2D {
	private Matrix imageBlock, approxBlock;
	private Matrix residue;
	private Matrix orthogonal;
	private Matrix dictX, dictY;
	private Vector coefficients;
	
	private final double INITIAL_TOL = 1e-10;
	private final double TOLERANCE;
	private final int MAX_ITERATIONS;
	private final int REORTH_ITERATIONS = 2;
	private final int WIDTH;
	
	private int curRowAtom, curColAtom;


	public OMP2D(double[] imageData, int width, double tol) throws BadDimensionsException{
		this.imageBlock = new Matrix(width, imageData);
		TOLERANCE = tol;
		WIDTH = width;
		MAX_ITERATIONS = 250;
		setup();
	}
	
	public OMP2D(double[] imageData, int width, double tol, int maxIterations) throws BadDimensionsException{
		this.imageBlock = new Matrix(width, imageData);
		TOLERANCE = tol;
		WIDTH = width;
		MAX_ITERATIONS = maxIterations;
		setup();
	}
	
	public OMP2D(Matrix imageBlock, double tol) throws BadDimensionsException{
		this.imageBlock = imageBlock;
		TOLERANCE = tol;
		WIDTH = imageBlock.getWidth();
		MAX_ITERATIONS = 250;
		setup();
	}
	
	public OMP2D(Matrix imageBlock, double tol, int maxIterations) throws BadDimensionsException{
		this.imageBlock = imageBlock;
		TOLERANCE = tol;
		WIDTH = imageBlock.getWidth();
		MAX_ITERATIONS = maxIterations;
		setup();
	}
	
	public OMP2D(CleverPointer<double[]> imagePointer, int width, double tol) {
		TOLERANCE = tol;
		WIDTH = width;
		MAX_ITERATIONS = 250;
	}
	
	public void calcBlock() throws BadDimensionsException {
		
		//First iteration
		double acceptance = chooseAtom();
		if(acceptance < INITIAL_TOL) { 
			return;
		}
		
		Vector chosenAtom = Vector.kronecker(dictX.getCol(curColAtom), dictY.getRow(curRowAtom));
		orthogonal = chosenAtom.clone();

		double rowNorm = orthogonal.normalizeRow(0); 
		Matrix beta = chosenAtom.clone();
		beta.scale(1/rowNorm);
		getResidual(residue, imageBlock, orthogonal.getRow(orthogonal.getHeight()-1));
		acceptance = residue.getFrobeniusNorm() / (WIDTH*WIDTH);

		if(acceptance < TOLERANCE) {
			return;
		}
		
		
		for(int k = 1; k < MAX_ITERATIONS; k++) {
			chooseAtom();
			
			chosenAtom = Vector.kronecker(dictX.getCol(curColAtom), dictY.getRow(curRowAtom));
			
			orthogonalize(chosenAtom);
			reorthogonalize(REORTH_ITERATIONS); 
			
			rowNorm = orthogonal.normalizeRow(k); 

			Vector orthK = orthogonal.getRow(k-1);
			getBiorthogonal(beta, chosenAtom, orthK, rowNorm);
			orthK.scale(1/rowNorm);
			beta = new Matrix(beta.getWidth(), beta.matrix, orthK.to1DArray());

			getResidual(residue, imageBlock, orthogonal.getRow(orthogonal.getHeight()-1));
			acceptance = residue.getFrobeniusNorm() / (WIDTH*WIDTH);

			//As cuda memcpy is very slow get the acceptance in a sperate thread and continue running the 
			//next iteration to prevent slow down while waiting for result to come back
			if(acceptance < TOLERANCE) {
				break;
			}
		}
		
		imageBlock.transpose();
		Vector v = new Vector(imageBlock.to1DArray());
		v.transpose();
		coefficients = new Vector(beta.getHeight());
		for(int j = 0; j < beta.getHeight(); j++) {
			coefficients.set(j, 0, Matrix.innerProduct(beta.getRow(j), v));
		}

		imageBlock.transpose();
		approxBlock = imageBlock.clone();
		approxBlock.subtract(residue);
	}
	
	/**
	 * Selects an Atom from the current dictionary
	 * @return The maximum absolute value found in the dictionaries given the residue
	 */
	public double chooseAtom() throws BadDimensionsException{
		Matrix temp = Matrix.multiply(dictY, residue);
		Matrix innerProducts = Matrix.multiply(temp, dictX);
		
		innerProducts.updateMaxAbs();
		curRowAtom = innerProducts.getMaxAbsRow();
		curColAtom = innerProducts.getMaxAbsCol();
		
		return innerProducts.getMaxAbs();
	}
	
	/**
	 * 
	 * @return The approximated block calculated
	 */
	public Matrix getApproxImage() {
		return approxBlock;
	}
	
	/**
	 * 
	 * @param beta
	 * @param newAtom
	 * @param orthogonalAtom
	 * @param rowNorm
	 * @throws BadDimensionsException
	 */
	public void getBiorthogonal(Matrix beta, Matrix newAtom, Vector orthogonalAtom, double rowNorm) throws BadDimensionsException{
		
		Vector alpha = new Vector(beta.getHeight());
		for(int j = 0; j < beta.getHeight(); j++) {
			alpha.set(j, 0, Matrix.innerProduct(beta.getRow(j), newAtom));
		}

		alpha.scale(1/rowNorm);

		for(int j = 0; j < alpha.getSize(); j++) {
			for(int i = 0; i < beta.getWidth(); i++) {
				beta.set(i, j, beta.get(i, j) - alpha.get(j)*orthogonalAtom.get(i));
			}
		}
	}
	
	/**
	 * Returns the coefficients used to approximate this block as a Vector
	 * @return the coefficients
	 */
	public Vector getCoefficients() {
		return coefficients;
	}
	
	/**
	 * 
	 * @return The number of coefficients used to approximate this block
	 */
	public double getNumCoefficients() {
		return coefficients.getWidth();
	}
	
	/**
	 * Calculates and returns the PSNR of this block
	 * @return PSNR
	 */
	public double getPSNR() {
		Matrix temp = imageBlock.clone();
		temp.subtract(approxBlock);
		for(int i = 0; i < imageBlock.getSize(); i++) {
			temp.set(i, Math.pow(temp.get(i), 2));
		}
		double mse = temp.getSum() / (WIDTH*WIDTH);
		return 10*Math.log10((255*255)/mse);
	}
	
	/**
	 * 
	 * @param residue
	 * @param m1
	 * @param m2
	 * @throws BadDimensionsException
	 */
	public void getResidual(Matrix residue, Matrix m1, Matrix m2) throws BadDimensionsException{
		m1.transpose();
		residue.transpose();
		double scalar = Matrix.innerProduct(m1, m2);
		m2.scale(-scalar);
		//residue.scale(-scalar);
		residue.add(m2);
		m1.transpose();
		residue.transpose();
	}
	
	/**
	 * 
	 * @param vector 
	 * @throws BadDimensionsException
	 */
	public void orthogonalize(Vector vector) throws BadDimensionsException{
		vector.transpose();
		double scalar = Matrix.innerProduct(orthogonal.getRow(orthogonal.getHeight()-1), vector);
		int row = orthogonal.getHeight();
		Matrix newOrth = new Matrix(orthogonal.getWidth(), orthogonal.to1DArray(), vector.to1DArray());
		
		for(int n = 0; n < row; n++) {
			for(int m = 0; m < newOrth.getWidth(); m++) {
				newOrth.set(m, row, newOrth.get(m, row) - scalar*orthogonal.get(m,n));
			}
		}
		this.orthogonal = newOrth;
	}
	
	/**
	 * 
	 * @param repetitions
	 * @throws BadDimensionsException
	 */
	public void reorthogonalize(int repetitions) throws BadDimensionsException{
		int row = orthogonal.getHeight()-1;
		for(int r = 0; r < repetitions; r++) {
			for(int j = 0; j < row; j++) {
				double scalar = Matrix.innerProduct(orthogonal.getRow(j), orthogonal.getRow(row));
				for(int i = 0; i < orthogonal.getWidth(); i++) {
					orthogonal.set(i, row, orthogonal.get(i, row) - scalar*orthogonal.get(i, j));
				}
			}
		}
	}

	/**
	 * Initialisation of Matrices
	 */
	private void setup() {
		dictX = new Dictionary(WIDTH);
		dictY = new Dictionary(WIDTH);
		dictY.transpose();
		residue = this.imageBlock.clone();
	}
}
