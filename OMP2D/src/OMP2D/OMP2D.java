package OMP2D;

public class OMP2D {
	private double[] imageData;
	private double[] approxData;
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
	public final int BLOCK_ID;
	
	private int curRowAtom, curColAtom;
	
	public OMP2D(double[] imageData, int width, int id, double tol, int maxIterations) throws BadDimensionsException{
		this.imageData = imageData;
		//this.approxData = imageData.clone();

		TOLERANCE = tol;
		WIDTH = width;
		MAX_ITERATIONS = maxIterations;
		BLOCK_ID = id;
		//setup();
	}
	
	public OMP2D(Matrix imageBlock, int id, double tol, int maxIterations) throws BadDimensionsException{
		this.imageBlock = imageBlock;
		this.imageData = imageBlock.to1DArray();
		imageBlockTransposed.transpose();
		TOLERANCE = tol;
		WIDTH = imageBlock.getWidth();
		MAX_ITERATIONS = maxIterations;
		BLOCK_ID = id;
		//setup();
	}
	
	/**
	 * Initialisation of Matrices
	 */
	private void setup(double[] imageData, int width) {
		imageBlock = new Matrix(width, imageData);
		dictX = new DictionaryX(WIDTH);
		dictY = new DictionaryY(WIDTH);
		
		double[] tData = Matrix.transpose(imageData.clone(), WIDTH, WIDTH);
		this.imageBlockTransposed = new Matrix(width, tData);
		this.residue = new Matrix(WIDTH, tData);
		//residue = new Matrix(WIDTH, imageBlockDataTransposed);
	}
	
	public void calcBlock() throws BadDimensionsException {
		setup(imageData, WIDTH);
		
		
		//First iteration
		double acceptance = findNextAtom();
		if(acceptance < INITIAL_TOL) { 
			//no improvements to be made
			approxBlock = imageBlock;
			coefficients = new double[0];
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
			processResults(beta);
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
		
		processResults(beta);
	}
	
	private void processResults(Matrix beta) throws BadDimensionsException {
		coefficients = new double[beta.getHeight()];
		for(int j = 0; j < beta.getHeight(); j++) {
			coefficients[j] = Matrix.innerProduct(beta.getRow(j), imageBlockTransposed.to1DArray());
		}

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
		Matrix temp = Matrix.multiply2(dictY, residue);
		Matrix innerProducts = Matrix.multiply2(temp, dictX);
		//Matrix innerProducts = multiplyDictX(temp);
		
		innerProducts.updateMaxAbs();
		curRowAtom = innerProducts.getMaxAbsRow();
		curColAtom = innerProducts.getMaxAbsCol();
		
		return innerProducts.getMaxAbs();
	}
	
	private Matrix multiplyDictX(Matrix temp) throws BadDimensionsException {
		//matrix1[m][n] matrix2[p][q]
		int mMax = 16; int nMax = 80;
		int pMax = 80; int qMax = 80;
		
		if(nMax != pMax) {
			throw new BadDimensionsException("Expected matrices of (m,n)x(n,q)\n" +
					"Recieved (" + mMax + "," + nMax + ")x(" + pMax + "," + qMax + ")");
		}
		
		double[] m1 = temp.to1DArray(); double[] m2 = DictionaryX.sixteenByEighty;

		Matrix result = new Matrix(qMax);
		
		for(int m = 0; m < mMax; m++) {
			double[] row = new double[qMax];
			for(int q = 0; q < qMax; q++) {		
				for(int product = 0; product < pMax; product++){
					//row[q] += matrix1.get(product, m) * matrix2.get(q, product);
					//row[q] += m1Row[product] * m2Col[product];
					row[q] += m1[m*nMax+product] * m2[product*qMax+q];
				}
			}
			result.addRow(row);
		}

		return result;
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
	 * Calculates and returns the PSNR of this block TODO fix approxData bug
	 * @return PSNR
	 */
	public double getPSNR() {
		double[] temp = imageData.clone();
		double sum = 0;
		for(int i = 0; i < imageBlock.getSize(); i++) {
			temp[i] -= approxData[i];
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
}
