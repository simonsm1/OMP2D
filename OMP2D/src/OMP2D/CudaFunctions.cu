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

#define colIndex(i,j,ld) (((j)*(ld))+(i))

/**
 *  implementation of the Hillis & Steel inclusive scan reduction method
 */
__device__ void reduce(double *v, int length)
{
	extern __shared__ double sdata[];

	int index = blockIdx.x*threadIdx.x + threadIdx.x;
	int index2 = index * 2 + 1;

	if(index2 < length)
	{
		sdata[index] = v[index2] + v[index2 - 1];
	}
	__syncthreads();

	v[index] = sdata[index];
}

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
__global__ void power(double *v1, int length, double factor)
{
	int index = blockIdx.x*threadIdx.x + threadIdx.x;
	if(index < length)
	{
		v1[index] = pow(v1[index], factor);
	}
}

extern "C"
__global__ void innerProduct(double *v1, double *v2, int length, double *ans)
{
	int index = blockIdx.x*threadIdx.x + threadIdx.x;
	extern __shared__ double products[];

	// Find the product of each value
	if(index < length)
	{
		products[index] = v1[index] * v2[index];
		//printf("product of %f * %f = %f\n", v1[index], v2[index], products[index]);
	}
	__syncthreads();

	// Sum all the products
	while(length != 0)
	{
		reduce(products, length);
		length = length / 2;
		__syncthreads();
	}

	// First thread copies answer to global mem
	if(index == 0) {
		ans[0] = products[0];
	}
}

extern "C"
__global__ void multiply(double *m1, double *m2, int width, int height, double *result)
{
	int index = blockIdx.x*threadIdx.x + threadIdx.x;
	//int corresponding =

	//multiply each row and column and reduce atomic add maybe

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
