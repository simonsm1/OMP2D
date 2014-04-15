/*
Copyright (c) 2014 Matthew Simons

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

package OMP2D;

public class OMP2D {
	private double[] imageData;
	private Matrix imageBlock, approxBlock;
	private Matrix imageBlockTransposed;
	private BasicMatrix residue;
	private Matrix orthogonal;
	private BasicMatrix dictX, dictY;
	private double[] coefficients;
	
	private final double INITIAL_TOL = 1e-10;
	private final int REORTH_ITERATIONS = 2;
	private final double TOLERANCE;
	private final int MAX_ITERATIONS;
	private final int WIDTH;
	public final int BLOCK_ID;
	
	private int curRowAtom, curColAtom;
	
	/**
	 * Creates a new OMP2D block processor
	 * @param imageData The intensity values of the image
	 * @param width The width of the image
	 * @param tol The tolerance level to be achieved
	 * @param maxIterations The maximum number of iterations to be done without meeting the tolerance level
	 */
	public OMP2D(double[] imageData, int width, int id, double tol, int maxIterations) {
		this.imageData = imageData;

		TOLERANCE = tol;
		WIDTH = width;
		MAX_ITERATIONS = maxIterations;
		BLOCK_ID = id;
	}
	
	/**
	 * Creates a new OMP2D block processor
	 * @param imageBlock A matrix object containing the intensity values of the image
	 * @param tol The tolerance level to be achieved
	 * @param maxIterations The maximum number of iterations to be done without meeting the tolerance level
	 */
	public OMP2D(Matrix imageBlock, int id, double tol, int maxIterations) {
		this.imageData = imageBlock.to1DArray();
		this.imageBlock = imageBlock;

		TOLERANCE = tol;
		WIDTH = imageBlock.getWidth();
		MAX_ITERATIONS = maxIterations;
		BLOCK_ID = id;
	}
	
	/**
	 * Initialiser for the matrices of this object.
	 * @param imageData
	 * @param width
	 */
	private void setup(double[] imageData, int width) {
		imageBlock = new Matrix(width, imageData);
		dictX = new DictionaryX(WIDTH);
		dictY = new DictionaryY(WIDTH);
		
		double[] tData = Matrix.transpose(imageData.clone(), WIDTH, WIDTH);
		this.imageBlockTransposed = new Matrix(width, tData);
		this.residue = new BasicMatrix(WIDTH, tData);
	}
	
	/**
	 * Calculates the approximated block 
	 * @throws BadDimensionsException
	 */
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
		
		//double[] chosenAtom = Matrix.kronecker(dictX.getCol(curColAtom), dictY.getRow(curRowAtom));//They are the same but transposed, use more efficient getrow
		double[] chosenAtom = BasicMatrix.kronecker(dictY, dictY, curColAtom, curRowAtom);
		orthogonal = new Matrix(chosenAtom.length, chosenAtom.clone());
		Matrix beta = new Matrix(chosenAtom.length, chosenAtom.clone());

		double rowNorm = orthogonal.normalizeRow(0); 
		beta.scale(1/rowNorm);
		updateResidual(orthogonal.getRow(orthogonal.getHeight()-1));
		acceptance = residue.getFrobeniusNorm() / (WIDTH*WIDTH);

		if(acceptance < TOLERANCE) {
			processResults(beta);
			return;
		}
		
		for(int k = 1; k < MAX_ITERATIONS; k++) {
			findNextAtom();
			//chosenAtom = Matrix.kronecker(dictX.getCol(curColAtom), dictY.getRow(curRowAtom));
			chosenAtom = BasicMatrix.kronecker(dictY, dictY, curColAtom, curRowAtom);
			
			orthogonalize(chosenAtom.clone());
			reorthogonalize(REORTH_ITERATIONS); 
			
			rowNorm = orthogonal.normalizeRow(k); 

			double[] orthK = orthogonal.getRow(k-1).clone(); //need copy not actual
			getBiorthogonal(beta, chosenAtom, orthK, rowNorm);
			Matrix.scale(orthK, 1/rowNorm);
			beta.addRow(orthK);

			updateResidual(orthogonal.getRow(k));
			acceptance = residue.getFrobeniusNorm() / (WIDTH*WIDTH);

			if(acceptance < TOLERANCE) {
				break;
			}
		}
		processResults(beta);
	}
	
	/**
	 * Finds the approximated block and its coefficients
	 * @param beta
	 * @throws BadDimensionsException
	 */
	private void processResults(Matrix beta) throws BadDimensionsException {
		coefficients = new double[beta.getHeight()];
		double[] ibt = imageBlockTransposed.to1DArray();
		for(int j = 0; j < beta.getHeight(); j++) {
			coefficients[j] = Matrix.innerProduct(beta.getRow(j), ibt);
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
		BasicMatrix temp = BasicMatrix.multiply(dictY, residue);
		BasicMatrix innerProducts = BasicMatrix.multiply(temp, dictX);
		//Matrix innerProducts = multiplyDictX(temp);
		
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
	 * Calculates the biorthogonal for the current iteration
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
		double[] temp = imageData.clone();
		double[] approxData = approxBlock.to1DArray();
		double sum = 0;
		for(int i = 0; i < imageBlock.getSize(); i++) {
			temp[i] -= approxData[i];
			sum += temp[i] * temp[i];
		}
		double mse = sum / (WIDTH*WIDTH);
		return 10*Math.log10((255*255)/mse);
	}
	
	/**
	 * Finds the residue for the current iteration
	 * @param m currentRow
	 * @throws BadDimensionsException
	 */
	public void updateResidual(double[] m) throws BadDimensionsException {
		double scalar = Matrix.innerProduct(imageBlock, m);
		for(int j = 0; j < residue.getHeight(); j++) {
			//double[] row = residue.getRow(j);
			for(int i = 0; i < residue.getWidth(); i++) {
				//row[i] -= m[i*residue.getWidth()+j]*scalar;
				residue.set(i, j, residue.get(i, j) - m[i*residue.getWidth()+j]*scalar);
			}
		}
	}
	
	/**
	 * Orthogonalizes the orthogonal matrix with respect to the current iteration
	 * @param vector 
	 * @throws BadDimensionsException
	 */
	public void orthogonalize(double[] vector) throws BadDimensionsException{
		double scalar = Matrix.innerProduct(orthogonal.getRow(orthogonal.getHeight()-1), vector);
		
		for(int j = 0; j < orthogonal.getHeight(); j++) {
			double[] row = orthogonal.getRow(j);
			for(int i = 0; i < row.length; i++) {
				vector[i] -= scalar*row[i];
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
		int rowId = orthogonal.getHeight()-1;
		int width = orthogonal.getWidth();
		
		double[] lastRow = orthogonal.getRow(rowId);
		for(int r = 0; r < repetitions; r++) {
			for(int j = 0; j < rowId; j++) {
				double[] curRow = orthogonal.getRow(j);
				double scalar = Matrix.innerProduct(curRow, lastRow);
				for(int i = 0; i < width; i++) {
					lastRow[i] -= scalar*curRow[i];
				}
			}
		}
	}
}
