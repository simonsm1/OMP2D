package testOMP2D;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import OMP2D.Matrix;
import OMP2D.BadDimensionsException;
import OMP2D.OMP2D;
import OMP2D.Vector;

public class TestOMP2D {
	private final int BLOCK_DIM = 16;
	private final int PSS = 44;
	private final double PSNR = 44.05937594149887;
	private final int MAX_INTENSITY = 255;
	
	private final double[] BLOCK_DATA = new double[] {
			1.1200000e+02, 1.1000000e+02, 1.0800000e+02, 1.0200000e+02, 1.0100000e+02, 1.0300000e+02, 1.0300000e+02, 1.0300000e+02, 1.0300000e+02, 1.0000000e+02, 1.0600000e+02, 1.0500000e+02, 1.0800000e+02, 1.0900000e+02, 1.0900000e+02, 1.0600000e+02
			, 1.1300000e+02, 1.1000000e+02, 1.0700000e+02, 1.0100000e+02, 9.8000000e+01, 9.9000000e+01, 1.0000000e+02, 1.0200000e+02, 1.0300000e+02, 1.0500000e+02, 1.2800000e+02, 1.1200000e+02, 1.0200000e+02, 1.1000000e+02, 1.0600000e+02, 1.0800000e+02
			, 1.1100000e+02, 1.0700000e+02, 1.0400000e+02, 1.0000000e+02, 9.9000000e+01, 9.9000000e+01, 9.8000000e+01, 1.0100000e+02, 1.0300000e+02, 1.0400000e+02, 1.1500000e+02, 1.0800000e+02, 1.0500000e+02, 1.0900000e+02, 1.0800000e+02, 1.0500000e+02
			, 1.1500000e+02, 1.1100000e+02, 1.0800000e+02, 1.0500000e+02, 1.0300000e+02, 1.0200000e+02, 9.9000000e+01, 9.9000000e+01, 1.0400000e+02, 1.0700000e+02, 1.0200000e+02, 1.0500000e+02, 1.0900000e+02, 1.0400000e+02, 1.0800000e+02, 1.0600000e+02
			, 1.1600000e+02, 1.1400000e+02, 1.1200000e+02, 1.0800000e+02, 1.0600000e+02, 1.0600000e+02, 1.0400000e+02, 1.0400000e+02, 1.0400000e+02, 1.0800000e+02, 1.1000000e+02, 1.0800000e+02, 1.0800000e+02, 1.0600000e+02, 1.0600000e+02, 1.0800000e+02
			, 1.1400000e+02, 1.1500000e+02, 1.1500000e+02, 1.1200000e+02, 1.0800000e+02, 1.0700000e+02, 1.0700000e+02, 1.0800000e+02, 1.0600000e+02, 1.0400000e+02, 1.0900000e+02, 1.0700000e+02, 1.0600000e+02, 1.0900000e+02, 1.0700000e+02, 1.0600000e+02
			, 1.1200000e+02, 1.1300000e+02, 1.1400000e+02, 1.1300000e+02, 1.1200000e+02, 1.1000000e+02, 1.1100000e+02, 1.1300000e+02, 1.1000000e+02, 1.0700000e+02, 1.0600000e+02, 1.0700000e+02, 1.0500000e+02, 1.0600000e+02, 1.1000000e+02, 1.0800000e+02
			, 1.1600000e+02, 1.1300000e+02, 1.1100000e+02, 1.1100000e+02, 1.1100000e+02, 1.1000000e+02, 1.1100000e+02, 1.1300000e+02, 1.1300000e+02, 1.1100000e+02, 1.0800000e+02, 1.0500000e+02, 1.0600000e+02, 1.1100000e+02, 1.1300000e+02, 1.1300000e+02
			, 1.1900000e+02, 1.1600000e+02, 1.1200000e+02, 1.1200000e+02, 1.1100000e+02, 1.0900000e+02, 1.0900000e+02, 1.1300000e+02, 1.1400000e+02, 1.1100000e+02, 1.0800000e+02, 1.0700000e+02, 1.0700000e+02, 1.1200000e+02, 1.1500000e+02, 1.1400000e+02
			, 1.1800000e+02, 1.1700000e+02, 1.1400000e+02, 1.1200000e+02, 1.1100000e+02, 1.0900000e+02, 1.0800000e+02, 1.1000000e+02, 1.1100000e+02, 1.0800000e+02, 1.0800000e+02, 1.1100000e+02, 1.1300000e+02, 1.1300000e+02, 1.1400000e+02, 1.1300000e+02
			, 1.1700000e+02, 1.1600000e+02, 1.1400000e+02, 1.1200000e+02, 1.1100000e+02, 1.1100000e+02, 1.1200000e+02, 1.1300000e+02, 1.0900000e+02, 1.0500000e+02, 1.0600000e+02, 1.1000000e+02, 1.1100000e+02, 1.1000000e+02, 1.1000000e+02, 1.0900000e+02
			, 1.1700000e+02, 1.1700000e+02, 1.1500000e+02, 1.1300000e+02, 1.1300000e+02, 1.1400000e+02, 1.1400000e+02, 1.1300000e+02, 1.1000000e+02, 1.0900000e+02, 1.0800000e+02, 1.0900000e+02, 1.0900000e+02, 1.1000000e+02, 1.1000000e+02, 1.0900000e+02
			, 1.1300000e+02, 1.1400000e+02, 1.1500000e+02, 1.1500000e+02, 1.1500000e+02, 1.1600000e+02, 1.1400000e+02, 1.1000000e+02, 1.0800000e+02, 1.0800000e+02, 1.0800000e+02, 1.0900000e+02, 1.0900000e+02, 1.1000000e+02, 1.1000000e+02, 1.1000000e+02
			, 1.0900000e+02, 1.1000000e+02, 1.1100000e+02, 1.1200000e+02, 1.1300000e+02, 1.1500000e+02, 1.1400000e+02, 1.1100000e+02, 1.1000000e+02, 1.0800000e+02, 1.0800000e+02, 1.0900000e+02, 1.1000000e+02, 1.1000000e+02, 1.1100000e+02, 1.1400000e+02
			, 1.0800000e+02, 1.0800000e+02, 1.0800000e+02, 1.0900000e+02, 1.1000000e+02, 1.1200000e+02, 1.1400000e+02, 1.1400000e+02, 1.1300000e+02, 1.1000000e+02, 1.0800000e+02, 1.0900000e+02, 1.1000000e+02, 1.1100000e+02, 1.1300000e+02, 1.1500000e+02
			, 1.0800000e+02, 1.0700000e+02, 1.0800000e+02, 1.1000000e+02, 1.1000000e+02, 1.1200000e+02, 1.1500000e+02, 1.1700000e+02, 1.1300000e+02, 1.1300000e+02, 1.1100000e+02, 1.1000000e+02, 1.1300000e+02, 1.1600000e+02, 1.1600000e+02, 1.1500000e+02
	};
	private final double[] OUTPUT_BLOCK_DATA = new double[] {
			110.16576058676968, 108.71793656345484, 105.1801478299222,  101.66291700880643, 100.30455067033661, 101.35503473816888, 103.1630726251323, 104.0477230781629, 103.92043222416154, 103.93282963888915, 104.81476495422181, 106.02582878546968, 106.66529852383526, 106.81994478493678, 107.51752567263142, 109.29938286466647, 
			110.85805606689503, 109.07031531525736, 105.33437914460252, 101.52523364378283, 99.58927546956274, 99.96267773656382, 101.47456610344898, 102.75511784889223, 103.52071820068296, 104.39193536727593, 128.00000000000003, 106.95526867472688, 107.43324445779082, 107.29957513410389, 107.47134310003058, 108.4964584568502, 
			111.78362635999493, 109.75268831175123, 106.0476459941178,  102.08947845073108, 99.41776032969008, 98.72023594930367, 99.6205250914976, 101.29774048944034, 103.18904824818877, 105.09458749909567, 115.00000000000006, 107.93744039379571, 108.21071290861913, 107.86826637992102, 107.53867967441136, 107.66142039599355, 
			113.01085048079186, 111.17758177545038, 108.06256322811757, 104.48832094105659, 101.38463502001527, 99.59072732805193, 99.56948098271383, 101.1427568536703, 103.52232878973105, 105.73534768132323, 107.16297952070087, 107.76413434666813, 107.8494115817091, 107.70114871718778, 107.43004379794188, 107.12889836854187, 
			114.34044559608688, 112.9938601237706,  110.89768411844832, 108.27856380458874, 105.35069738076557, 102.88940969963319, 101.9655745566938, 102.94334235904208, 104.92705516697545, 106.4594081637485, 106.8207477783476, 106.52464759375295, 106.50625692715475, 107.00268788295857, 107.42757244303083, 107.29405531274654, 
			115.3889754822515,  114.2922546166107,  113.10567052553164, 111.63572986880959, 109.45393438894936, 107.05990802572344, 105.72891372931568, 106.09796876623861, 107.25947572505855, 107.62475991708729, 106.66241726863298, 105.47160795079606, 105.4812546094319, 106.7957806291545, 108.12755872677421, 108.34620016482387, 
			115.90249162006364, 114.49312649399216, 113.63658619294195, 113.00356664995364, 111.7867025420051, 110.11938428840013, 109.07603076619543, 109.2421384181036, 109.75375733821149, 109.18597096530554, 107.33098196379302, 105.67267388560563, 105.84005897585136, 107.76368404308434, 109.6835924172224, 110.00708467187663, 
			115.97552277312158, 113.98825025246073, 112.90432008858835, 112.52671890902474, 112.0468234331488, 111.3100648406805, 110.93367821209584, 111.21518333689535, 111.39914725656814, 110.44129711488465, 108.44226935640317, 106.90989953847269, 107.30364523015153, 109.40583313429278, 111.39134735040108, 111.56856374127253, 
			115.92587562266787, 113.80711579605756, 112.40465255575818, 111.80577851281583, 111.56353535203581, 111.43227596618698, 111.51434293538563, 111.74709965811702, 111.59607671872087, 110.56244207290459, 108.97567277468804, 107.99779109490093, 108.59157279145634, 110.46697149946976, 112.17612689512761, 112.32093285679666, 
			118.66312327097653, 116.66271307568668, 114.15975186921803, 111.81883547706158, 110.10801471732464, 109.18725422606656, 108.92387932028453, 109.01213620067661, 109.1682291848248, 109.31847701317075, 109.64438498749512, 110.4035885568992, 111.62090401704344, 112.89162626439031, 113.49358378797614, 112.77314791625852, 
			115.75615936515638, 115.7697866825554,  114.83393024720876, 113.60600334271221, 112.86069771965232, 112.64727196872347, 112.27687627368435, 111.14880897647528, 109.46421457745281, 108.03999055786663, 107.49874973991568, 107.78525342006773, 108.47233337097417, 109.32937743087068, 110.37583469665276, 111.40746941187935, 
			114.94245542053385, 116.09662411364948, 115.63053510390404, 114.4022463184157,  113.65480225565547, 113.5464198972882, 113.07377348982254, 111.42978245861285, 109.12082592757362, 107.50082895412723, 107.31999573125165, 108.04608980504997, 108.70419695983328, 109.09249722826118, 109.81897485207122, 111.15634350155064, 
			113.16835661916149, 114.58059128180696, 114.32250430397252, 113.37153968604821, 113.129170059865, 113.67215908065062, 113.6978133634481, 112.19778373734722, 109.8048967976301, 108.1838119323663, 108.22656109760591, 109.2283522887069, 109.9531544533006, 110.16049528078499, 110.65868435461746, 111.8756826178705, 
			110.5790885036507,  111.42483652652325, 111.17812261597622, 110.79252101204527, 111.45352412342704, 112.97220739888101, 113.8375493137912, 112.91602551845044, 110.79531601401237, 109.19371532114131, 109.16443001753683, 110.20146685249101, 111.16436165253829, 111.7053000801534, 112.37157881194199, 113.3532062586972, 
			107.70371199512209, 107.80245556221061, 107.71289279205207, 108.15073679701264, 109.75639320926902, 112.01155281338518, 113.43072522648647, 112.95520329002547, 111.03401827500207, 109.1998738606017, 108.65090991911259, 109.39862010013117, 110.73672661055379, 112.18745817530402, 113.66391644287391, 114.79154714024925, 
			105.50849153936215, 106.10965609417953, 107.32411221500496, 109.38317527177384, 112.17747439978912, 114.93775427024724, 116.52434525195677, 116.2101014220766, 114.27004603615967, 111.82943541161357, 110.12735427918773, 109.8477008009575, 110.95825636303859, 112.9193802776478, 114.88513631376624, 115.8217718152551
	};
	
	private final int ORTHOGONAL_M_WIDTH = 16;
	private final double[] ORTHOGONAL_MATRIX = new double[0];
	private final double[] ORTHOGONAL_VECTOR = new double[0];
	private final double[] ORTHOGONAL_OUTPUT = new double[0];
	
	private final double[] REORTHOGONALIZE = new double[] {
			1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12 
	};
	private final int REORTHOGONALIZE_WIDTH = 4;
	private final double[] REORTHOGONALIZE_EXPECTED = new double[0];
	
	private final int RESIDUE_WIDTH = 16;
	private final double[] RESIDUE_ORIGINAL = new double[0];
	private final double[] RESIDUE_CHANGED = new double[0];
	private final double[] RESIDUE_DELTA = new double[0];
	
	private final int BIORTHOGONAL_WIDTH = 256;
	private final int BIORTHGOONAL_NA_LENGTH = 256;
	private final int BIORTHOGONAL_OA_LENGTH = 256;
	private final double[] BIORTHOGONAL = new double[0];
	private final double[] BIORTHOGONAL_NEW_ATOM = new double[0];
	private final double[] BIORTHOGONAL_ORTH_ATOM = new double[0];
	private final double BIORTHOGONAL_NORM_ATOM = 0.0;
	
	
	@Before
	public void setUp() {	
	}

	@Test
	public void fullTest() throws BadDimensionsException {
		double tolerance = (MAX_INTENSITY*MAX_INTENSITY)/(Math.pow(10, (PSS/10.0)));
		Matrix imageBlock = new Matrix(BLOCK_DIM, BLOCK_DATA);
		OMP2D blockProcessor = new OMP2D(imageBlock, tolerance);
		
		blockProcessor.calcBlock();

		Matrix approxBlock = blockProcessor.getApproxImage();
		/*
		for(int i = 0; i < approxBlock.getHeight(); i++) {
			for(int j = 0; j < approxBlock.getWidth(); j ++) {
				System.out.print(approxBlock.get(j, i) + ", ");
			}
			System.out.println("");
		}*/
		
		double psnr = getPSNR(imageBlock, approxBlock);
		assertEquals(PSNR, psnr, 0.0);
		//assertArrayEquals(OUTPUT_BLOCK_DATA, approxBlock.to1DArray(), 1e-12);
	}
	
	public double getPSNR(Matrix image1, Matrix image2) {
		image2.scale(-1);
		image1.add(image2);
		for(int i = 0; i < image1.getSize(); i++) {
			image1.set(i, Math.pow(image1.get(i), 2));
		}
		double mse = image1.getSum();
		mse = mse / (BLOCK_DIM*BLOCK_DIM);
		return 10*Math.log10((255*255)/mse);
	}
	
	
	
	/*
	@Test
	public void orthogonalizeTest() throws IncompatibleDimensionsException {
		OMP2D omp = new OMP2D(BLOCK_DATA, BLOCK_DIM, TOLERANCE);
		Matrix m = new Matrix(ORTHOGONAL_M_WIDTH, ORTHOGONAL_MATRIX);
		Vector v = new Vector(ORTHOGONAL_VECTOR);
		omp.orthogonalize(m, v);
		assertArrayEquals(ORTHOGONAL_OUTPUT, m.to1DArray(), 0.0);
	}
	
	@Test
	public void reorthogonalizeTest() throws IncompatibleDimensionsException {
		OMP2D omp = new OMP2D(BLOCK_DATA, BLOCK_DIM, TOLERANCE);
		Matrix m = new Matrix(REORTHOGONALIZE_WIDTH, REORTHOGONALIZE);
		int repetitions = 2;
		omp.reorthogonalize(m, repetitions);
		double[] actual = m.to1DArray();
		assertArrayEquals(REORTHOGONALIZE_EXPECTED, actual, 0);
	}
	
	@Test
	public void getBiorthogonalTest() throws IncompatibleDimensionsException {
		OMP2D omp = new OMP2D(BLOCK_DATA, BLOCK_DIM, TOLERANCE);
		Matrix biorthogonal = new Matrix(BIORTHOGONAL_WIDTH, BIORTHOGONAL);
		Matrix newAtom = new Matrix(BIORTHGOONAL_NA_LENGTH, BIORTHOGONAL_NEW_ATOM);
		Vector orthogonalAtom = new Vector(BIORTHOGONAL_ORTH_ATOM);
		double normAtom = BIORTHOGONAL_NORM_ATOM;
		omp.getBiorthogonal(biorthogonal, newAtom, orthogonalAtom, normAtom);
	}
	/*
	@Test
	public void calcResidualTest() throws IncompatibleDimensionsException {
		OMP2D omp = new OMP2D(BLOCK_DATA, BLOCK_DIM, TOLERANCE);
		Matrix m1 = new Matrix(RESIDUE_WIDTH, RESIDUE_ORIGINAL);
		Matrix m2 = new Matrix(RESIDUE_WIDTH, RESIDUE_CHANGED);
		Matrix delta = omp.getResidual(m1, m2);
		assertArrayEquals(RESIDUE_DELTA, delta.to1DArray(), 0.0);
	}
	
	@Test
	public void chooseAtomTest() throws IncompatibleDimensionsException {
		OMP2D omp = new OMP2D(BLOCK_DATA, BLOCK_DIM, TOLERANCE);
	}*/

}
