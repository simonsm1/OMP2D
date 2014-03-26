package OMP2D;

import OMP2D.Matrix.IncompatibleDimensionsException;

public class OMP2D {
	private Matrix imageBlock, approxBlock;
	private Matrix residue;
	private Matrix dictX, dictY;
	private final double INITIAL_TOL = 1e-10;
	private final double TOLERANCE;
	private final int ITERATIONS = 25; //what should this be set to? 25 seems to be max given the size of orthogonal... or maybe the number of rows in dictionary
	private final int MIN_ATOMS = 5; //what should this be set to?
	private final int REORTH_ITERATIONS = 2;
	private int width;
	private int curRowAtom, curColAtom;
	private Matrix orthogonal = null;
	//private final double TOL3 = 1e-7;
	

	public OMP2D(double[] imageBlock, int width, double tol) throws IncompatibleDimensionsException {
		this.imageBlock = new Matrix(width, imageBlock);
		TOLERANCE = tol;
		this.width = width;
		dictX = new Dictionary();
		dictY = dictX.clone();
		dictX.transpose();
		residue = this.imageBlock.clone();
	}
	
	public OMP2D(Matrix imageBlock, double tol) throws IncompatibleDimensionsException {
		this.imageBlock = imageBlock;
		TOLERANCE = tol;
		this.width = imageBlock.getWidth();
		dictX = new Dictionary();
		dictY = dictX.clone();
		dictX.transpose();
		residue = this.imageBlock.clone();
	}
	
	public Matrix getApproxImage() {
		return approxBlock;
	}
	
	/**
	 * 
	 * @param imageBlock
	 * 
	 * @return the index of the the chosen atom to represent this block.
	 * @throws IncompatibleDimensionsException 
	 */
	public void calcBlock() throws IncompatibleDimensionsException {
		int numAtomsX = imageBlock.getWidth();
		int numAtomsY = numAtomsX;
		Matrix beta = null;
		int k;
		double ssResidual;
		
		
		for(k = 0; k < ITERATIONS; k++) {
			//if(k < numInd) {
				//set iChosenAtom X Y XY
			//} else {
			//Chooseatom needs to set curColAtom and curRowAtom
			double acceptance = chooseAtom(numAtomsY, numAtomsX);
			//System.out.println("current acceptance: " + acceptance);
			if(acceptance < INITIAL_TOL) { 
				break;
			}
			
			//the rows which are selected are found in choose atom
			Vector chosenAtom = Vector.kronecker(dictX.getCol(curColAtom), dictY.getRow(curRowAtom));
			if(k == 0) {
				orthogonal = chosenAtom.clone();
			} else {
				orthogonalize(orthogonal, chosenAtom);
				reorthogonalize(orthogonal, REORTH_ITERATIONS); //Magic Number alert!
			}
			
			double normAtom = orthogonal.getRow(k).getNorm();
			orthogonal.normalize(k); // normalize only the kth

			if(k > 0) {
				Vector orthK = orthogonal.getRow(orthogonal.getHeight()-1);
				getBiorthogonal(beta, chosenAtom, orthK, normAtom);
				orthK.scale(1/normAtom);
				beta = new Matrix(beta.getWidth(), beta.matrix, orthK.to1DArray());
			} else {
				beta = chosenAtom.clone();
				beta.scale(1/normAtom);
			}
			getResidual(residue, imageBlock, orthogonal.getRow(orthogonal.getHeight()-1));
			double temp = residue.getFrobeniusNorm();
			temp = temp / (width*width);
			//ssResidual = temp * temp;
			System.out.println("interation " + k + ": " + temp);
			if(temp < TOLERANCE) {
				k++;  //we're using k to direct the next part of assignment
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
		residue.scale(-1); //just make a subtract method
		residue.add(imageBlock);
		approxBlock = residue;

	}
	
	/**
	 * Selects an Atom from a given dictionary
	 * @param numAtomsY
	 * @param numAtomsX
	 * @return iChosenAtomx and y and xy
	 * @throws IncompatibleDimensionsException
	 */
	public double chooseAtom(int numAtomsY, int numAtomsX) throws IncompatibleDimensionsException {
		Matrix temp = Matrix.matrixMultiply(dictY, residue);
		Matrix innerProducts = Matrix.matrixMultiply(temp, dictX);
		
		innerProducts.updateMaxAbs();
		curRowAtom = innerProducts.getMaxAbsRow();
		curColAtom = innerProducts.getMaxAbsCol();
		
		return innerProducts.getMaxAbs();
	}
	
	/**
	 * 
	 * @param orthogonalDict
	 * @param vector
	 * @throws IncompatibleDimensionsException
	 */
	public void orthogonalize(Matrix orthogonal, Vector vector) throws IncompatibleDimensionsException {
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
	public void reorthogonalize(Matrix orthogonal, int repetitions) throws IncompatibleDimensionsException {
		int row = orthogonal.getHeight()-1;
		//System.out.println("row: " + row + " row2: " + row2);
		for(int r = 0; r < repetitions; r++) {
			//Vector rowVector = orthogonal.getRow(row);

			for(int j = 0; j < row; j++) {
				//Vector first = orthogonal.getRow(j);
				//Vector second =  orthogonal.getRow(row);
				double scalar = Matrix.innerProduct(orthogonal.getRow(j), orthogonal.getRow(row));
				for(int i = 0; i < orthogonal.getWidth(); i++) {
					orthogonal.set(i, row, orthogonal.get(i, row) - scalar*orthogonal.get(i, j));
				}
			}
		}
	}
	
	/**
	 * 
	 * @param biorthogonal
	 * @param newAtom
	 * @param orthogonalAtom
	 * @param normAtom
	 * @throws IncompatibleDimensionsException
	 
	public void getBiorthogonal(Matrix beta, Matrix newAtom, Vector orthogonalAtom, double normAtom) throws IncompatibleDimensionsException {
		newAtom.transpose();
		Matrix alpha = Matrix.matrixMultiply(beta, newAtom);
		alpha.scale(1/normAtom);
		//TODO subtract
		alpha.scale(-1);
		beta.add(alpha);
	}*/
	
	/**
	 * 
	 * @param biorthogonal
	 * @param newAtom
	 * @param orthogonalAtom
	 * @param normAtom
	 * @throws IncompatibleDimensionsException
	 */
	public void getBiorthogonal(Matrix beta, Matrix newAtom, Vector orthogonalAtom, double normAtom) throws IncompatibleDimensionsException {
		
		Vector alpha = new Vector(beta.getHeight());
		for(int j = 0; j < beta.getHeight(); j++) {
			alpha.set(j, 0, Matrix.innerProduct(beta.getRow(j), newAtom));
		}

		alpha.scale(1/normAtom);
		//TODO subtract
		//alpha.scale(-1);
		//for each row add the corresponding alpha scale
		for(int j = 0; j < alpha.getSize(); j++) {
			for(int i = 0; i < beta.getWidth(); i++) {
				beta.set(i, j, beta.get(i, j) - alpha.get(j)*orthogonalAtom.get(i));
			}
		}
		//beta.add(alpha);
	}

	/**
	 * QUESTION This function should really be accepting matrices. Maybe create calcResiduleOMP2D? Yes
	 * @param signal
	 * @param orthogonal
	 * @return
	 * @throws IncompatibleDimensionsException 
	 */
	public void getResidual(Matrix residue, Matrix m1, Matrix m2) throws IncompatibleDimensionsException {
		m1.transpose();
		residue.transpose();
		double scalar = Matrix.innerProduct(m1, m2);
		m2.scale(-scalar);
		//residue.scale(-scalar);
		residue.add(m2);
		m1.transpose();
		residue.transpose();
	}
}
