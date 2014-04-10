package OMP2D;

public class OMP2D {
	private double[] imageBlockData;
	private double[] approxBlockData;
	private Matrix imageBlock, approxBlock;
	private Matrix imageBlockTransposed;
	private Matrix residue;
	private Matrix orthogonal;
	private Matrix dictX, dictY;
	private double[] coefficients;
	
	private final double INITIAL_TOL = 1e-10;
	private final double TOLERANCE;
	private final int MAX_ITERATIONS;
	private final int REORTH_ITERATIONS = 2;
	private final int WIDTH;
	
	private int curRowAtom, curColAtom;


	public OMP2D(double[] imageData, int width, double tol) throws BadDimensionsException{
		this.imageBlock = new Matrix(width, imageData);
		this.imageBlockData = imageData;
		this.approxBlockData = imageData.clone();
		
		double[] tData = Matrix.transpose(imageData.clone(), width, width);
		this.imageBlockTransposed = new Matrix(width, tData);

		TOLERANCE = tol;
		WIDTH = width;
		MAX_ITERATIONS = 250;
		setup();
	}
	
	public OMP2D(double[] imageData, int width, double tol, int maxIterations) throws BadDimensionsException{
		this.imageBlock = new Matrix(width, imageData);
		this.imageBlockData = imageData;
		this.approxBlockData = imageData.clone();
		
		double[] tData = Matrix.transpose(imageData.clone(), width, width);
		this.imageBlockTransposed = new Matrix(width, tData);

		TOLERANCE = tol;
		WIDTH = width;
		MAX_ITERATIONS = maxIterations;
		setup();
	}
	
	public OMP2D(Matrix imageBlock, double tol) throws BadDimensionsException{
		this.imageBlock = imageBlock;
		this.imageBlockData = imageBlock.to1DArray();
		//imageBlockTransposed.transpose();
		TOLERANCE = tol;
		WIDTH = imageBlock.getWidth();
		MAX_ITERATIONS = 250;
		setup();
	}
	
	public OMP2D(Matrix imageBlock, double tol, int maxIterations) throws BadDimensionsException{
		this.imageBlock = imageBlock;
		this.imageBlockData = imageBlock.to1DArray();
		imageBlockTransposed.transpose();
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
		double acceptance = findNextAtom();
		if(acceptance < INITIAL_TOL) { 
			return;
		}
		
		double[] chosenAtom = Matrix.kronecker(dictX.getCol(curColAtom), dictY.getRow(curRowAtom));
		orthogonal = new Matrix(chosenAtom.length, chosenAtom.clone());
		Matrix beta = new Matrix(chosenAtom.length, chosenAtom.clone());

		//both same at this point might as well only do it once
		double rowNorm = orthogonal.normalizeRow(0); 
		beta.scale(1/rowNorm);
		updateResidual(orthogonal.getRow(orthogonal.getHeight()-1));
		acceptance = residue.getFrobeniusNorm() / (WIDTH*WIDTH);

		if(acceptance < TOLERANCE) {
			return;
		}
		
		
		for(int k = 1; k < MAX_ITERATIONS; k++) {
			findNextAtom();
			
			chosenAtom = Matrix.kronecker(dictX.getCol(curColAtom), dictY.getRow(curRowAtom));
			
			orthogonalize(chosenAtom.clone());
			reorthogonalize(REORTH_ITERATIONS); 
			
			rowNorm = orthogonal.normalizeRow(k); 

			double[] orthK = orthogonal.getRow(k-1).clone();//need copy not actual
			getBiorthogonal(beta, chosenAtom, orthK, rowNorm);
			Matrix.scale(orthK, 1/rowNorm);
			beta.addRow(orthK);

			updateResidual(orthogonal.getRow(orthogonal.getHeight()-1));
			acceptance = residue.getFrobeniusNorm() / (WIDTH*WIDTH);

			//As cuda memcpy is very slow get the acceptance in a separate thread and continue running the 
			//next iteration to prevent slow down while waiting for result to come back
			if(acceptance < TOLERANCE) {
				break;
			}
		}
		
		//imageBlock.transpose();
		//Vector v = new Vector(imageBlock.to1DArray());
		//v.transpose();
		coefficients = new double[beta.getHeight()];
		for(int j = 0; j < beta.getHeight(); j++) {
			coefficients[j] = Matrix.innerProduct(beta.getRow(j), imageBlockTransposed.to1DArray());
		}

		//imageBlock.transpose();
		approxBlock = imageBlockTransposed;
		approxBlock.subtract(residue);
		approxBlock = new Matrix(WIDTH, Matrix.transpose(approxBlock.to1DArray(), WIDTH, WIDTH));
	}
	
	/**
	 * Selects an Atom from the current dictionary
	 * @return The maximum absolute value found in the dictionaries given the residue,
	 *  a.k.a the initial tolerance level
	 */
	public double findNextAtom() throws BadDimensionsException{
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
	public void getBiorthogonal(Matrix beta, double[] newAtom, double[] orthogonalAtom, double rowNorm) throws BadDimensionsException{
		
		double[] alpha = new double[beta.getHeight()];
		for(int j = 0; j < beta.getHeight(); j++) {
			alpha[j] = Matrix.innerProduct(beta.getRow(j), newAtom);
		}

		Matrix.scale(alpha, 1/rowNorm);

		for(int j = 0; j < alpha.length; j++) {
			double[] row = beta.getRow(j); //new bit
			for(int i = 0; i < beta.getWidth(); i++) {
				row[i] -= alpha[j]*orthogonalAtom[i];
				//beta.set(i, j, beta.get(i, j) - alpha[j]*orthogonalAtom[i]);
			}
		}
	}
	
	/**
	 * Returns the coefficients used to approximate this block as a Vector
	 * @return the coefficients
	 */
	public double[] getCoefficients() {
		return coefficients;
	}
	
	/**
	 * 
	 * @return The number of coefficients used to approximate this block
	 */
	public double getNumCoefficients() {
		return coefficients.length;
	}
	
	/**
	 * Calculates and returns the PSNR of this block
	 * @return PSNR
	 */
	public double getPSNR() {
		double[] temp = imageBlockData.clone();
		double sum = 0;
		for(int i = 0; i < imageBlock.getSize(); i++) {
			temp[i] -= approxBlockData[i];
			sum += temp[i] * temp[i];
		}
		double mse = sum / (WIDTH*WIDTH);
		return 10*Math.log10((255*255)/mse);
	}
	
	/**
	 * QUESTION is there a way to do this without transposing?
	 * @param residue
	 * @param m1 imageBlock
	 * @param m2 currentRow
	 * @throws BadDimensionsException
	 */
	public void updateResidual(double[] m2) throws BadDimensionsException {
		//imageBlock.transpose();
		//residue.transpose();
		double scalar = Matrix.innerProduct(imageBlock, m2);
		//residue.subtract(m2, scalar);
		for(int j = 0; j < residue.getHeight(); j++) {
			double[] row = residue.getRow(j);
			for(int i = 0; i < residue.getWidth(); i++) {
				row[i] -= m2[i*residue.getWidth()+j]*scalar;
			}
		}
		//imageBlock.transpose();
		//residue.transpose();
	}
	
	/**
	 * 
	 * @param vector 
	 * @throws BadDimensionsException
	 */
	public void orthogonalize(double[] vector) throws BadDimensionsException{
		//vector.transpose();
		double scalar = Matrix.innerProduct(orthogonal.getRow(orthogonal.getHeight()-1), vector);
		//int row = orthogonal.getHeight();
		//Matrix newOrth = new Matrix(orthogonal.getWidth(), orthogonal.to1DArray(), vector);
		
		for(int j = 0; j < orthogonal.getHeight(); j++) {
			double[] row = orthogonal.getRow(j);
			for(int i = 0; i < row.length; i++) {
				vector[i] -= scalar*row[i];
				//newOrth.set(m, row, newOrth.get(m, row) - scalar*orthogonal.get(m,n));
			}
		}
		orthogonal.addRow(vector);
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
		dictX = new DictionaryX(WIDTH);
		dictY = new DictionaryY(WIDTH);
		double[] tData = Matrix.transpose(imageBlockData.clone(), WIDTH, WIDTH);
		this.residue = new Matrix(WIDTH, tData);
		//residue = new Matrix(WIDTH, imageBlockDataTransposed);
	}
}
