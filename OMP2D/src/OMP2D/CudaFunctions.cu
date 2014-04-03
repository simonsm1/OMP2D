extern "C"
__global__ void scale(double *v1, int length, double factor)
{
	int index = blockIdx.x*threadIdx.x + threadIdx.x;
	if(index < length)
	{
		v1[index] = v1[index] * factor;
	}
}

extern "C"
__global__ void innerProduct(double *v1, double *v2, int length)
{
	int index = blockIdx.x*threadIdx.x + threadIdx.x;
	if(index < length)
	{
		v1[index] = v1[index] * v2[index];
	}
	__syncthreads();

	reduce(v1, length);
}



extern "C"
__global__ void vectorDot(double *v1, double *v2, int length, double *v3)
{
	int index = blockIdx.x*threadIdx.x + threadIdx.x;
	if(index < length)
	{
		v3[index] = v1[index] * v2[index];
	}
	// returns *v3
}

__device__ void reduce(double *v1, int length)
{
	__shared__ double sdata[1024];
	int index = blockIdx.x*threadIdx.x + threadIdx.x;
	int index2 = index * 2 + 1;
	//int index2 = length/2+index;
	if(index2 < length)
	{
		sdata[index] = v1[index2] + v1[index2 - 1];
		//v1[index] = v1[index] + v1[index2];
	}
	__syncthreads();
	v1[index] = sdata[index];
	//printf("%f ", v1[index]);
}

extern "C"
__global__ void shmem_reduce_kernel(double *d_out, double *d_in)
{

    // sdata is allocated in the kernel call: 3rd arg to <<<b, t, shmem>>>
    extern __shared__ double sdata[];

    int myId = threadIdx.x + blockDim.x * blockIdx.x;
    int tid  = threadIdx.x;

    // load shared mem from global mem
    sdata[tid] = d_in[myId];

    __syncthreads();            // make sure entire block is loaded!

    // do reduction in shared mem
    for (unsigned int s = blockDim.x / 2; s > 0; s >>= 1)
    {
        if (tid < s)
        {
            sdata[tid] += sdata[tid + s];

        }
        __syncthreads();        // make sure all adds at one stage are done!
    }

    // only thread 0 writes result for this block back to global mem
    if (tid == 0)
    {
        d_out[blockIdx.x] = sdata[0];
    }
}

extern "C"
__global__ void global_reduce_kernel(float * d_out, float * d_in)
{
    int myId = threadIdx.x + blockDim.x * blockIdx.x;
    int tid  = threadIdx.x;

    // do reduction in global mem
    for (unsigned int s = blockDim.x / 2; s > 0; s >>= 1)
    {
        if (tid < s)
        {
            d_in[myId] += d_in[myId + s];
        }
        __syncthreads();        // make sure all adds at one stage are done!
    }

    // only thread 0 writes result for this block back to global mem
    if (tid == 0)
    {
        d_out[blockIdx.x] = d_in[myId];
    }
}
