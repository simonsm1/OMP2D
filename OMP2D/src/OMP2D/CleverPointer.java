package OMP2D;

import static jcuda.runtime.JCuda.cudaFree;
import static jcuda.runtime.JCuda.cudaMalloc;
import static jcuda.runtime.JCuda.cudaMemcpy;
import static jcuda.runtime.JCuda.cudaMemcpyAsync;
import static jcuda.runtime.JCuda.cudaMemGetInfo;
import static jcuda.runtime.cudaMemcpyKind.cudaMemcpyDeviceToHost;
import static jcuda.runtime.cudaMemcpyKind.cudaMemcpyHostToDevice;
import static jcuda.runtime.cudaMemcpyKind.cudaMemcpyDeviceToDevice;
import static jcuda.runtime.cudaError.cudaSuccess;

import java.util.WeakHashMap;

import jcuda.Pointer;
import jcuda.Sizeof;
import jcuda.runtime.cudaStream_t;

public class CleverPointer<V> extends Pointer {
	private V array;
	private boolean allocated = false;
	private final int SIZE;
	private final int LENGTH;
	private final Class<?> TYPE;
	
	private static final WeakHashMap<String, CleverPointer<?>> allPointers = 
			new WeakHashMap<String, CleverPointer<?>>();
	
	private static boolean hasShutdownHook = false;
	
	private static class freeAllPointers extends Thread {
		public void run() {
			for(CleverPointer<?> p: allPointers.values()) {
				if(p != null) {
					p.free();	
				}
			}
		}
	}
	
	private static void createShutdownHook() {
		if(!hasShutdownHook) {
			Runtime.getRuntime().addShutdownHook(new freeAllPointers());
			hasShutdownHook = true;
		}
	}
	
	public static void showMemUsage() {
		long[] free = new long[1], total = new long[1];
		cudaMemGetInfo(free, total);
		System.out.println("Total Memory:" + total[0]);
		System.out.println("Free  Memory:" + free[0]);
		double percent = (double) free[0]/total[0]*100;
		System.out.println("Percent Free:" + percent);
	}

	private CleverPointer(V array, int size, Class<?> type, int length) {
		SIZE = size;
		TYPE = type;
		LENGTH = length;
		this.array = array;
	}
	
	private CleverPointer(int size, Class<?> type, int length) {
		SIZE = size;
		TYPE = type;
		LENGTH = length;
	}
	
	private static void setup(CleverPointer<?> cp) {
		if(cudaMalloc(cp, cp.SIZE) == cudaSuccess){
			cp.allocated = true;
		}
		allPointers.put(null, cp);
		createShutdownHook();
	}
	
	public CleverPointer<V> clone() {
		CleverPointer<V> cp = new CleverPointer<V>(this.SIZE, this.TYPE, this.LENGTH);
		cudaMemcpy(cp, this, SIZE, cudaMemcpyDeviceToDevice);
		return cp;
	}
	
	public CleverPointer<V> clone(cudaStream_t stream) {
		CleverPointer<V> cp = new CleverPointer<V>(this.SIZE, this.TYPE, this.LENGTH);
		cudaMemcpyAsync(cp, this, SIZE, cudaMemcpyDeviceToDevice, stream);
		return cp;
	}
	
	public static CleverPointer<byte[]> copyByte(byte[] array) {
		CleverPointer<byte[]> cp = 
				new CleverPointer<byte[]>(array, array.length*Sizeof.BYTE, byte[].class, array.length);
		setup(cp);
		cudaMemcpy(cp, Pointer.to(array), cp.SIZE, cudaMemcpyHostToDevice);
		return cp;
	}
	
	public static CleverPointer<char[]> copyChar(char[] array) {
		CleverPointer<char[]> cp = 
				new CleverPointer<char[]>(array, array.length*Sizeof.CHAR, char[].class, array.length);
		setup(cp);
		cudaMemcpy(cp, Pointer.to(array), cp.SIZE, cudaMemcpyHostToDevice);
		return cp;
	}
	
	public static CleverPointer<double[]> copyDouble(double[] array) {
		CleverPointer<double[]> cp = 
				new CleverPointer<double[]>(array, array.length*Sizeof.DOUBLE, double[].class, array.length);
		setup(cp);
		cudaMemcpy(cp, Pointer.to(array), cp.SIZE, cudaMemcpyHostToDevice);
		return cp;
	}
	
	public static CleverPointer<float[]> copyFloat(float[] array) {
		CleverPointer<float[]> cp = 
				new CleverPointer<float[]>(array, array.length*Sizeof.FLOAT, float[].class, array.length);
		setup(cp);
		cudaMemcpy(cp, Pointer.to(array), cp.SIZE, cudaMemcpyHostToDevice);
		return cp;
	}
	
	public static CleverPointer<int[]> copyInt(int[] array) {
		CleverPointer<int[]> cp = 
				new CleverPointer<int[]>(array, array.length*Sizeof.INT, int[].class, array.length);
		setup(cp);
		cudaMemcpy(cp, Pointer.to(array), cp.SIZE, cudaMemcpyHostToDevice);
		return cp;
	}
	
	public static CleverPointer<long[]> copyLong(long[] array) {
		CleverPointer<long[]> cp = 
				new CleverPointer<long[]>(array, array.length*Sizeof.LONG, char[].class, array.length);
		setup(cp);
		cudaMemcpy(cp, Pointer.to(array), cp.SIZE, cudaMemcpyHostToDevice);
		return cp;
	}
	
	public static CleverPointer<short[]> copyShort(short[] array) {
		CleverPointer<short[]> cp = 
				new CleverPointer<short[]>(array, array.length*Sizeof.SHORT, short[].class, array.length);
		setup(cp);
		cudaMemcpy(cp, Pointer.to(array), cp.SIZE, cudaMemcpyHostToDevice);
		return cp;
	}
	
	public static CleverPointer<byte[]> createByte(int length) {
		CleverPointer<byte[]> cp = 
				new CleverPointer<byte[]>(length*Sizeof.BYTE, byte[].class, length);
		setup(cp);
		return cp;
	}
	
	public static CleverPointer<char[]> createChar(int length) {
		CleverPointer<char[]> cp = 
				new CleverPointer<char[]>(length*Sizeof.CHAR, char[].class, length);
		setup(cp);
		return cp;
	}
	
	public static CleverPointer<double[]> createDouble(int length) {
		CleverPointer<double[]> cp = 
				new CleverPointer<double[]>(length*Sizeof.DOUBLE, double[].class, length);
		setup(cp);
		return cp;
	}
	
	public static CleverPointer<float[]> createFloat(int length) {
		CleverPointer<float[]> cp = 
				new CleverPointer<float[]>(length*Sizeof.FLOAT, float[].class, length);
		setup(cp);
		return cp;
	}
	
	public static CleverPointer<int[]> createInt(int length) {
		CleverPointer<int[]> cp = 
				new CleverPointer<int[]>(length*Sizeof.INT, int[].class, length);
		setup(cp);
		return cp;
	}
	
	public static CleverPointer<long[]> createLong(int length) {
		CleverPointer<long[]> cp = 
				new CleverPointer<long[]>(length*Sizeof.LONG, long[].class, length);
		setup(cp);
		return cp;
	}
	
	public static CleverPointer<short[]> createShort(int length) {
		CleverPointer<short[]> cp = 
				new CleverPointer<short[]>(length*Sizeof.SHORT, short[].class, length);
		setup(cp);
		return cp;
	}
	
	@SuppressWarnings("unchecked")
	public V getArray() {
		if(TYPE == char[].class) {
			char[] temp = new char[LENGTH];
			cudaMemcpy(to(temp), this, SIZE, cudaMemcpyDeviceToHost);
			return (V) temp;
		} else if(TYPE == double[].class) {
			double[] temp = new double[LENGTH];
			cudaMemcpy(to(temp), this, SIZE, cudaMemcpyDeviceToHost);
			return (V) temp;
		} else if(TYPE == float[].class) {
			float[] temp = new float[LENGTH];
			cudaMemcpy(to(temp), this, SIZE, cudaMemcpyDeviceToHost);
			return (V) temp;
		} else if(TYPE == int[].class) {
			int[] temp = new int[LENGTH];
			cudaMemcpy(to(temp), this, SIZE, cudaMemcpyDeviceToHost);
			return (V) temp;
		} else if(TYPE == long[].class) {
			long[] temp = new long[LENGTH];
			cudaMemcpy(to(temp), this, SIZE, cudaMemcpyDeviceToHost);
			return (V) temp;
		} else if(TYPE == byte[].class) {
			byte[] temp = new byte[LENGTH];
			cudaMemcpy(to(temp), this, SIZE, cudaMemcpyDeviceToHost);
			return (V) temp;
		} else if(TYPE == short[].class) {
			short[] temp = new short[LENGTH];
			cudaMemcpy(to(temp), this, SIZE, cudaMemcpyDeviceToHost);
			return (V) temp;
		}
		return null;
	}
	
	public V getOriginal() {
		return array;
	}
	
	public boolean isAllocated() {
		return allocated;
	}
	
	public int free() {
		int freed = cudaFree(this);
		if(freed == cudaSuccess) {
			allocated = false;
		}
		return freed;
	}
	
	@Override
	protected void finalize() throws Throwable {
		allPointers.remove(this);
		free();
	}

	public int getSize() {
		return SIZE;
	}
	
}
