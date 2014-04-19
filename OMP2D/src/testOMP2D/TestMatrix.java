package testOMP2D;


import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import matlabcontrol.MatlabConnectionException;
import matlabcontrol.MatlabInvocationException;
import matlabcontrol.MatlabProxy;
import matlabcontrol.MatlabProxyFactory;
import matlabcontrol.MatlabProxyFactoryOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import OMP2D.BadDimensionsException;
import OMP2D.Matrix;

@RunWith(Enclosed.class)
public class TestMatrix {
	private static MatlabProxy matlab = null;
    private static final String matLoc = System.getProperty("user.dir") + "/matlab";
	
	@BeforeClass
	public static void setMatlabConnection() {
		MatlabProxyFactoryOptions.Builder builder = new MatlabProxyFactoryOptions.Builder();
		builder.setHidden(true);
		builder.setMatlabStartingDirectory(new File(matLoc));
		
		MatlabProxyFactoryOptions options = builder.build();
		MatlabProxyFactory mpf = new MatlabProxyFactory(options);
		try {
			matlab = mpf.getProxy();
		} catch (MatlabConnectionException e) {
			System.err.println("Matlab was not found as an environment variable. Reverting to default JSON expected values");
		}
	}

	@Ignore
	public static class Tests {
		protected static JSONObject testSet;
		protected static JSONArray originalMatrix1;
		protected static JSONArray originalMatrix2;
		protected static JSONArray originalMatrix3;
		
		protected static Matrix 	m1, m2, m3;
		protected static Matrix	 	add, scale;
		protected static int 		width, height, size;
		protected static double	 	scaleFactor;
		protected static Matrix	 	addRow;
		protected static double[]	row1;
		protected static double[] 	row2;
		protected static double[] 	row3;
		protected static double[] 	rowLast;
		protected static double[] 	normalizeRow1;
		protected static double[] 	normalizeRow2;
		protected static double[]	normalizeRow3;
		protected static double[] 	normalizeRowLast;
		protected static Matrix 	subtract;
		protected static Matrix 	subtractScaled;
		protected static double 	frobeniusNorm;
		protected static int 		maxAbsRow;
		protected static int 		maxAbsCol;
		protected static double 	maxAbs;
		protected static Matrix 	incompatibleMatrix;
		protected static Matrix 	multiplied;
		
		public static final double NO_MARGIN = 0.0;
		public static final double TINY_MARGIN = 0.00000000000000100;
		public static final int TO_ZERO_INDEX = 1;
		
		@AfterClass
		public static void clearMatlabVariables() throws MatlabInvocationException {
			if(matlab != null) {
				matlab.eval("clearvars");
			}
		}

		protected static void generateTestSet(int w, int h, double min, double max) throws IOException, JSONException, MatlabConnectionException, MatlabInvocationException {
			int seed = 1;
			String jsonString = null;
			
			if(matlab != null) {
				String fName = "'temp" + w + "x" + h + "-(" + min + "-" + max + ")'";
				matlab.eval("testSet = createTestMatrix(" + w + "," + h + "," +
						min + "," + max + "," + seed + "," + fName + ");");
				jsonString = (String) matlab.getVariable("testSet");
			} else {
				URI alternative = null;
				try {
					alternative = new URI(matLoc + "/alternative.json");
					byte[] encoded = Files.readAllBytes(Paths.get(alternative));
					jsonString =  new String(encoded, StandardCharsets.UTF_8);
				} catch (URISyntaxException e) {
					System.err.println("Was not able to read the supplied alternative expected values at: " + matLoc + "/alternative.json");
					e.printStackTrace();
				}
			}
	
			//System.out.println(result);
			testSet = new JSONObject(jsonString);
			
			width = w;
			height = h;
			size = w*h;
			
			originalMatrix1 = testSet.getJSONArray("matrix1");
			originalMatrix2 = testSet.getJSONArray("matrix2");
			originalMatrix3 = testSet.getJSONArray("matrix3");
			
			m1 					= toMatrix(testSet.getJSONArray("matrix1"));
			m2 					= toMatrix(testSet.getJSONArray("matrix2"));
			m3 					= toMatrix(testSet.getJSONArray("matrix3"), height);
			incompatibleMatrix 	= toMatrix(testSet.getJSONArray("incompatibleMatrix"));
			multiplied			= toMatrix(testSet.getJSONArray("multiplied"), m3.getWidth());
			add 				= toMatrix(testSet.getJSONArray("add"));
			addRow				= toMatrix(testSet.getJSONArray("addRow"));
			row1 				= toRow(testSet.getJSONArray("getRow1"));
			row2 				= toRow(testSet.getJSONArray("getRow2"));
			row3 				= toRow(testSet.getJSONArray("getRow3"));
			rowLast 			= toRow(testSet.getJSONArray("getRowLast"));
			normalizeRow1 		= toRow(testSet.getJSONArray("normalizeRow1"));
			normalizeRow2 		= toRow(testSet.getJSONArray("normalizeRow2"));
			normalizeRow3 		= toRow(testSet.getJSONArray("normalizeRow3"));
			normalizeRowLast 	= toRow(testSet.getJSONArray("normalizeRowLast"));
			scale 				= toMatrix(testSet.getJSONArray("scale"));
			subtract 			= toMatrix(testSet.getJSONArray("subtract"));
			subtractScaled		= toMatrix(testSet.getJSONArray("subtractScaled"));
			scaleFactor 		= testSet.getDouble("scaleFactor");
			
			try {
				maxAbsRow		= testSet.getInt("getMaxAbsRow") - TO_ZERO_INDEX;
				maxAbsCol 		= testSet.getInt("getMaxAbsCol") - TO_ZERO_INDEX;
				maxAbs 			= testSet.getDouble("getMaxAbs");
			} catch(JSONException e) {
				//maxAbsRow = testSet.getJSONArray("getMaxAbsRow").getJSONArray(0).getInt(0) - TO_ZERO_INDEX;
				//maxAbsCol = testSet.getJSONArray("getMaxAbsCol").getJSONArray(0).getInt(0) - TO_ZERO_INDEX;
				maxAbs    = testSet.getJSONArray("getMaxAbs").getJSONArray(0).getDouble(0);
				
				//Hack because Matlab finds the first max locales column-wise vs my Java doing it row wise
				double[] absRowTemp = nestedArrayToRow(testSet.getJSONArray("getMaxAbsRow"));
				int pos = min(absRowTemp);
				maxAbsRow = testSet.getJSONArray("getMaxAbsRow").getJSONArray(pos).getInt(0) - TO_ZERO_INDEX;
				maxAbsCol = testSet.getJSONArray("getMaxAbsCol").getJSONArray(pos).getInt(0) - TO_ZERO_INDEX;
			}
		}
		
		public static void reset() throws JSONException {
			m1 = toMatrix(originalMatrix1);
			m2 = toMatrix(originalMatrix2);
			m3 = toMatrix(originalMatrix3);
		}
		
		private static int min(double[] a) {
			int min = 0;
			for(int i = 1; i < a.length; i++) {
				if(a[i] < a[min]) {
					min = i;
				}
			}
			return min;
		}
		
		private static double[] toRow(JSONArray array) throws JSONException {
			double[] row = new double[array.length()];
			for(int i = 0; i < row.length; i++) {
				row[i] = array.getDouble(i);
			}
			return row;
		}
		
		private static double[] nestedArrayToRow(JSONArray array) throws JSONException {
			double[] row = new double[array.length()];
			for(int i = 0; i < row.length; i++) {
				row[i] = array.getJSONArray(i).getDouble(0);
			}
			return row;
		}
		
		private static Matrix toMatrix(JSONArray array) throws JSONException {
			Matrix m = new Matrix(width);
			for(int row = 0; row < array.length(); row++) {
				m.addRow(toDoubleArray(array.getJSONArray(row)));
			}
			return m;
		}
		
		private static Matrix toMatrix(JSONArray array, int width) throws JSONException {
			Matrix m = new Matrix(width);
			for(int row = 0; row < array.length(); row++) {
				m.addRow(toDoubleArray(array.getJSONArray(row)));
			}
			return m;
		}
		
		public static double[] toDoubleArray(JSONArray array) throws JSONException {
			double[] res = new double[array.length()];
			for(int i = 0; i < array.length(); i++) {
				res[i] = array.getDouble(i);
			}
			return res;
		}
		
		@Test
		public void whenMultiplying() throws BadDimensionsException {
			final Matrix result = Matrix.multiply(m1, m3);
			//Then 
			assertArrayEquals(multiplied.to1DArray(), result.to1DArray(), NO_MARGIN);
		}
		
		@Test(expected=BadDimensionsException.class)
		public void whenMultiplyingAndDimensionsDoNotAgree() throws BadDimensionsException {
			Matrix.multiply(m1, incompatibleMatrix);
			//Then throws BadDimensionsException
		}
		
		@Test
		public void whenAnElementIsChanged() {
			final double VAL_1 = 77;
			final double VAL_2 = 45;
			final double VAL_3 = 56;
			
			final int START_POS = 0;
			final int MID_POS = (int) Math.floor(m1.getSize()/2.0);
			final int END_POS = m1.getSize()-1;
			final int THIRD_POS_X = (int) Math.floor(m1.getWidth()/3);
			final int THIRD_POS_Y = (int) Math.floor(m1.getHeight()/3);
			
			m1.set(START_POS, VAL_1);
			m1.set(MID_POS, VAL_2);
			m1.set(END_POS, VAL_3);
			m1.set(THIRD_POS_X, THIRD_POS_Y, VAL_3);
			
			//Then
			assertEquals(m1.get(0, 0), VAL_1, NO_MARGIN);
			assertEquals(m1.get(MID_POS), VAL_2, NO_MARGIN);
			assertEquals(m1.get(END_POS), VAL_3, NO_MARGIN);
			assertEquals(m1.get(THIRD_POS_X, THIRD_POS_Y), VAL_3, NO_MARGIN);
		}
		
		@Test(expected=ArrayIndexOutOfBoundsException.class)
		public void whenAnElementUnderRangeIsAccessed() {
			m1.get(-1);
			//Then throws ArrayIndexOutOfBoundsException
		}
		
		@Test(expected=IndexOutOfBoundsException.class)
		public void whenAnElementOverRangeIsAccessed() {
			m1.get(m1.getSize());
			//Then throws IndexOutOfBoundsException
		}
		
		@Test
		public void whenSubtracted() {
			m1.subtract(m2);
			//Then
			assertArrayEquals(subtract.to1DArray(), m1.to1DArray(), NO_MARGIN);
		}
		
		@Test
		public void whenRowIsNormalized() throws BadDimensionsException {
			m1.normalizeRow(0);
			m1.normalizeRow(1);
			m1.normalizeRow(2);
			m1.normalizeRow(height-1);
			assertArrayEquals(normalizeRow1, m1.getRow(0), TINY_MARGIN);
			assertArrayEquals(normalizeRow2, m1.getRow(1), TINY_MARGIN);
			assertArrayEquals(normalizeRow3, m1.getRow(2), TINY_MARGIN);
			assertArrayEquals(normalizeRowLast, m1.getRow(height-1), TINY_MARGIN);
		}
		
		@Test 
		public void whenMaxAbsCalculated() {
			m1.updateMaxAbs();
			//Then
			assertEquals(maxAbs, m1.getMaxAbs(), NO_MARGIN);
			//And
			assertEquals(maxAbsRow, m1.getMaxAbsRow());
			//And
			assertEquals(maxAbsCol, m1.getMaxAbsCol());
		}
		
		@Test
		public void whenMatrixAdding() throws IOException, JSONException, MatlabConnectionException, MatlabInvocationException {
			m1.add(m2);
			//Then
			assertArrayEquals(add.to1DArray(), m1.to1DArray(), NO_MARGIN);
		}
		
		@Test
		public void whenMatrixScaling() {
			m1.scale(scaleFactor);
			//Then
			assertArrayEquals(scale.to1DArray(), m1.to1DArray(), NO_MARGIN);
		}
		
		@Test
		public void whenGettingRow() {
			double[] actualRow1 = m1.getRow(0);
			double[] actualRow2 = m1.getRow(1);
			double[] actualRow3 = m1.getRow(2);
			double[] actualRowLast = m1.getRow(height-1);
			
			//Then
			assertArrayEquals(row1, actualRow1, NO_MARGIN);
			assertArrayEquals(row2, actualRow2, NO_MARGIN);
			assertArrayEquals(row3, actualRow3, NO_MARGIN);
			assertArrayEquals(rowLast, actualRowLast, NO_MARGIN);
		}
		
		@Test
		public void whenInsertingRow() {
			m1.addRow(row1);
			assertArrayEquals(addRow.to1DArray(), m1.to1DArray(), NO_MARGIN);
		}
		
		@Test 
		public void whenConvertingToArray() {
			
		}
		
		@Test
		public void whenConstructing() {
			//Then
			assertEquals(height, m1.getHeight());
			assertEquals(width, m1.getWidth());
			assertEquals(size, m1.getSize());
		}
		
	}
	
	public static class GivenAn8x8Matrix extends Tests {
		
		@BeforeClass
		public static void givenAn8x8Matrix() throws IOException, JSONException, MatlabConnectionException, MatlabInvocationException {
			generateTestSet(8,8,0,255);
		}
		
		@After
		public void cleanUp() throws IOException, JSONException, MatlabConnectionException, MatlabInvocationException {
			reset();
		}
	}	
	
	public static class GivenAn8x8Matrix2 extends Tests {
		
		@BeforeClass
		public static void givenAn8x8Matrix() throws IOException, JSONException, MatlabConnectionException, MatlabInvocationException {
			generateTestSet(8,8,-100000,100000);
		}
		
		@After
		public void cleanUp() throws IOException, JSONException, MatlabConnectionException, MatlabInvocationException {
			reset();
		}
	}	
	
	public static class GivenA16x16Matrix extends Tests {
		@BeforeClass
		public static void givenA16x16Matrix() throws IOException, JSONException, MatlabConnectionException, MatlabInvocationException {
			generateTestSet(16,16,0,255);
		}
		
		@After
		public void cleanUp() throws IOException, JSONException, MatlabConnectionException, MatlabInvocationException {
			reset();
		}
	}	
	
	public static class GivenA32x32Matrix extends Tests {
		@BeforeClass
		public static void givenA32x32Matrix() throws IOException, JSONException, MatlabConnectionException, MatlabInvocationException {
			generateTestSet(32,32,0,255);
		}
		
		@After
		public void cleanUp() throws IOException, JSONException, MatlabConnectionException, MatlabInvocationException {
			reset();
		}
	}
	
	public static class GivenA8x40Matrix extends Tests {
		
		@BeforeClass
		public static void givenA8x40Matrix() throws IOException, JSONException, MatlabConnectionException, MatlabInvocationException {
			generateTestSet(8,40,0,255);
		}
		
		@After
		public void cleanUp() throws IOException, JSONException, MatlabConnectionException, MatlabInvocationException {
			reset();
		}
	}	
	
	public static class GivenA40x8Matrix extends Tests {
		
		@BeforeClass
		public static void givenA40x8Matrix() throws IOException, JSONException, MatlabConnectionException, MatlabInvocationException {
			generateTestSet(40,8,0,255);
		}
		
		@After
		public void cleanUp() throws IOException, JSONException, MatlabConnectionException, MatlabInvocationException {
			reset();
		}
	}	

}
	