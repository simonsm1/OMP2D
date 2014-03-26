package testOMP2D;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import OMP2D.Matrix;
import OMP2D.Matrix.IncompatibleDimensionsException;

public class TestMatrix {
	Matrix m1, m2;
	private final double INNER_PRODUCT = 285;
	private final double MAX_ABS = 9;
	private final double MEAN = 5;
	private final int WIDTH = 3, HEIGHT = 3, SIZE = 9;
	private final double[] SCALE = new double[] {
			2, 4, 6, 8, 10, 12, 14, 16, 18
	};
	private final double[] TRANSPOSED = new double[] {
			1, 4, 7, 2, 5, 8, 3, 6, 9
	};
	private final double[] M2_TRANSPOSED = new double[] {
			3, 1, 4, 4, 5, 3, 7, 8
	};
	private final double[] MULTIPLIED = new double[] {
			30, 36, 42, 66, 81, 96, 102, 126, 150
	};
	
	@Before
	public void setMatrices() {
		//Given
		m1 = new Matrix(3, new double[] {1, 2, 3, 4, 5, 6, 7, 8, 9});
		m2 = new Matrix(4, new double[] {3, 4, 5, 7, 1, 4, 3, 8});
	}

	@Test
	public void scaleTest() {
		//When
		m1.scale(2);
		double[] test = m1.to1DArray();
		//Then
		assertArrayEquals(SCALE, test, 0);
	}
	
	@Test
	public void innerProductTest() throws IncompatibleDimensionsException {
		assertEquals(INNER_PRODUCT, Matrix.innerProduct(m1, m1), 0);
	}
	
	@Test
	public void meanTest() {
		assertEquals(MEAN, m1.getMean(), 0);
	}
	
	@Test
	public void dimensionsTest() {
		assertEquals(WIDTH, m1.getWidth());
		assertEquals(HEIGHT, m1.getHeight());
		assertEquals(SIZE, m1.getSize());
	}
	
	@Test
	public void dotProductTest() throws IncompatibleDimensionsException {
		m1.multiply(m1);
	}
	
	@Test(expected=IncompatibleDimensionsException.class)
	public void dotProductBadDimensionsTest() throws IncompatibleDimensionsException {
		m1.multiply(m2);
	}
	
	@Test
	public void maxAbsTest() {
		m1.updateMaxAbs();
		assertEquals(MAX_ABS, m1.getMaxAbs(), 0);
	}
	
	@Test 
	public void transposeTest() {
		m1.transpose();
		double[] actual;
		/*for(int j = 0; j < m1.getHeight(); j++) {
			for(int i = 0; i < m1.getWidth(); i++) {
				actual[j*i+i] = m1.get(i, j);
				System.out.print(m1.get(i, j) + ", ");
			}
		}*/
		actual = m1.to1DArray();
		assertArrayEquals(TRANSPOSED, actual, 0);
		m2.transpose();
		actual = new double[m2.getSize()];
		/*for(int j = 0; j < m2.getHeight(); j++) {
			for(int i = 0; i < m2.getWidth(); i++) {
				actual[j*i+i] = m2.get(i, j);
				System.out.print(m2.get(i, j) + ", ");
			}
		}*/
		actual = m2.to1DArray();
		assertArrayEquals(M2_TRANSPOSED, actual, 0);
	}
	
	@Test
	public void multiplyTest() throws IncompatibleDimensionsException {
		Matrix m3 = Matrix.matrixMultiply(m1, m1.clone());
		double[] result = m3.to1DArray();
		assertArrayEquals(MULTIPLIED, result, 0);
	}
	
	@Test
	public void frobienusNormTest() {
		assertEquals(INNER_PRODUCT, m1.getFrobeniusNorm(), 0);
	}

}
