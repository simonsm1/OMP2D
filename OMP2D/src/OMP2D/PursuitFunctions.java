package OMP2D;
import static OMP2D.MatrixOperations.*;
import OMP2D.MatrixOperations.IncompatibleDimensionsException;

public class PursuitFunctions {

	/**
	 * Selects an Atom from a given dictionary
	 * @param dictY
	 * @param dictX
	 * @param residule
	 * @param numAtomsY
	 * @param numAtomsX
	 * @return An Atom QUESTION not sure what's being returned, there seems to be multi values. If so this function should exist in omp2d
	 * 			returns the index of the atom not the atom itself, x and y
	 * @throws IncompatibleDimensionsException
	 */
	public static double chooseAtomOMP2D(double[][] dictY, double[][] dictX, double[][] residule, int numAtomsY, int numAtomsX) throws IncompatibleDimensionsException {
		
		double[][] temp = matrixMultiply(dictY, residule, 'T', 'N');
		double[][] innerProducts = matrixMultiply(temp, dictX, 'N', 'N');
		
		double maxValue = maxAbs(innerProducts);
		calcIndexYandX();
		return maxValue;
	}
	
	/**
	 * QUESTION This function should really be accepting matrices. Maybe create calcResiduleOMP2D? Yes
	 * @param signal
	 * @param orthogonal
	 * @return
	 * @throws IncompatibleDimensionsException 
	 */
	public static double[] calcResiduleOMP(double[] signal, double[] orthogonal) throws IncompatibleDimensionsException {
		double scalar = realInnerProduct(orthogonal, signal);
		return addVectors(signal, orthogonal, -scalar);
	}
	
	/**
	 * 
	 * @param orthogonalDict
	 * @param vector
	 * @throws IncompatibleDimensionsException
	 */
	public static void orthogonalizeOMP(double[][] orthogonalDict, double[] vector) throws IncompatibleDimensionsException {
		int length = vector.length;
		double[] temp = multiplyMatrixVector(orthogonalDict, vector, 'T');
		
		for(int m = 0; m < length; m++) {
			for(int n = 0; n < length; n++) {
				orthogonalDict[m][n] = temp[m] - orthogonalDict[m][n];
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
	 */
	public static void calcBiorthogonal(double[][] biorthogonal, double[] newAtom, double[] orthogonalAtom, double normAtom) throws IncompatibleDimensionsException {
		int length = newAtom.length;
		if(length > 0) {
			double[] vector = multiplyMatrixVector(biorthogonal, newAtom, 'T');
			scaleVector(vector, 1/normAtom);
			outerProduct(biorthogonal, orthogonalAtom, vector);
		}
		
		//TODO implement the copy elements part
		
	}
	
	/*public static void validateIndex() {
		
	}*/
	
	/**
	 * Finds of mean of all values in a vector. TODO should exist in MatrixOperations
	 * @param vector
	 * @return The mean value
	 */
	public static double mean(double[] vector) {
		double sum = 0;
		for(double d: vector) {
			sum += d;
		}
		return sum/vector.length;
	}
	
	/**
	 * 
	 * @param orthogonalDict
	 * @param row
	 * @param repetitions
	 * @throws IncompatibleDimensionsException
	 */
	public static void reorthogonalize(double[][] orthogonalDict, int row, int repetitions) throws IncompatibleDimensionsException {
		for(int i = 0; i < repetitions; i++) {
			double[] temp = multiplyMatrixVector(orthogonalDict, orthogonalDict[row], 'T');
			for(int j = 0; j < row; j++) {
				addVectors(orthogonalDict[row], orthogonalDict[row], -temp[j]);
			}
		}
	}
	
	/**
	 * Seems to return the index of the atom
	 */
	public static void calcIndexYandX() {
		
	}
	
	/**
	 * Finds the smallest of two values. TODO move to MatrixOperations
	 * @param a
	 * @param b
	 * @return The smaller value
	 */
	public static int min(int a, int b) {
		if(a < b) {
			return a;
		} 
		return b;
	}
}
