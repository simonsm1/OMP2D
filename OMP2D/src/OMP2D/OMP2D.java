package OMP2D;

public class OMP2D {
	private Matrix imageBlock, approxBlock;
	private Matrix residue;
	private Matrix dictX, dictY;
	private final double INITIAL_TOL = 1e-10;
	private final double TOLERANCE;
	private final int ITERATIONS = 25;
	private final int MIN_ATOMS = 5;
	private final int REORTH_ITERATIONS = 2;
	private final int WIDTH;
	private int curRowAtom, curColAtom;
	private Matrix orthogonal = null;

	public OMP2D(double[] imageBlock, int width, double tol) throws BadDimensionsException{
		this.imageBlock = new Matrix(width, imageBlock);
		TOLERANCE = tol;
		WIDTH = width;
		
		dictX = new Dictionary();
		dictY = new Dictionary();
		dictY.transpose();
		residue = this.imageBlock.clone();
	}
	
	public OMP2D(Matrix imageBlock, double tol) throws BadDimensionsException{
		this.imageBlock = imageBlock;
		TOLERANCE = tol;
		WIDTH = imageBlock.getWidth();
		
		dictX = new Dictionary();
		dictY = new Dictionary();
		dictY.transpose();
		residue = this.imageBlock.clone();
	}
	
	/**
	 * 
	 * @param imageBlock
	 * 
	 * @return the index of the the chosen atom to represent this block.
	 * @throws BadDimensionsException
	 */
	public void calcBlock() throws BadDimensionsException{
		
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
		
		
		for(int k = 1; k < ITERATIONS; k++) {
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

			if(acceptance < TOLERANCE) {
				break;
			}
		}
		
		imageBlock.transpose();
		Vector v = new Vector(imageBlock.to1DArray());
		v.transpose();
		Vector result = new Vector(beta.getHeight());
		for(int j = 0; j < beta.getHeight(); j++) {
			result.set(j, 0, Matrix.innerProduct(beta.getRow(j), v));
		}

		imageBlock.transpose();
		approxBlock = imageBlock.clone();
		approxBlock.subtract(residue);
	}
	
	/**
	 * Selects an Atom from a given dictionary
	 * @param numAtomsY
	 * @param numAtomsX
	 * @return iChosenAtomx and y and xy
	 * @throws IncompatibleDimensionsException
	 */
	public double chooseAtom() throws BadDimensionsException{
		Matrix temp = Matrix.multiply(dictY, residue);
		Matrix innerProducts = Matrix.multiply(temp, dictX);
		
		innerProducts.updateMaxAbs();
		curRowAtom = innerProducts.getMaxAbsRow();
		curColAtom = innerProducts.getMaxAbsCol();
		
		return innerProducts.getMaxAbs();
	}
	
	public Matrix getApproxImage() {
		return approxBlock;
	}
	
	/**
	 * 
	 * @param biorthogonal
	 * @param newAtom
	 * @param orthogonalAtom
	 * @param normAtom
	 * @throws IncompatibleDimensionsException
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
	 * @param signal
	 * @param orthogonal
	 * @return
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
	 * @param orthogonalDict
	 * @param vector
	 * @throws IncompatibleDimensionsException
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
	 * @param orthogonalDict
	 * @param row
	 * @param repetitions
	 * @throws IncompatibleDimensionsException
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
}
