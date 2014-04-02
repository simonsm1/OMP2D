package OMP2D;

public class BadDimensionsException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8402683884806051308L;

	public BadDimensionsException() {
		super("Expected a combination of vectors and/or matrices of equal length");
	}
	
	public BadDimensionsException(String msg) {
		super(msg);
	}
	
	public BadDimensionsException(String msg, int expected, int actual) {
		super(msg + "\nExpected Dimension: " + expected + "\nActual Dimension: " + actual);
	}
}

