float linearstep(float edge0, float edge1, float x){
    return clamp((x-edge0)/(edge1-edge0), 0.0f, 1.0f);
}

__kernel void renderParticles(__global float *particles, __global int *tiles, __global int *tileStart, __global int *tileSize, int width, int height, int globalParticlesCount, __global int *atomicTileIndex, __global float *outImage) {
    __local int tileIndexArray[8];
    int threadIdx = get_local_id(0);
    int tileIndex = -1;

    float tileXStart = -500.0f*width/height;
    float tileXStep = ((8.0f/(width-1.0f))-0.5f)*1000.0f*width/height - tileXStart;
    float tileYStart = -500.0f;
    float tileYStep = ((8.0f/(height-1.0f))-0.5f)*1000.0f - tileYStart;

    float lowcostAntialiasing = native_sqrt(tileXStep*tileXStep+tileYStep*tileYStep)/8.0f; // :D

    for(;;) {
        if(threadIdx%64==0) {
            tileIndexArray[threadIdx/64] = atomic_add(&atomicTileIndex[0], 1);
        }
        barrier(CLK_LOCAL_MEM_FENCE);
        //while(tileIndexArray[threadIdx/64] == tileIndex);
        tileIndex = tileIndexArray[threadIdx/64];
        if(tileIndex>=((width+7)/8) * ((height+7)/8)){
            break;
        }

        int tileX = tileIndex % ((width+7)/8);
        int tileY = tileIndex / ((width+7)/8);
        int x = (threadIdx % 64) % 8;
        int y = (threadIdx % 64) / 8;
        __global int *particleIds = tiles + tileStart[tileIndex];
        int particlesCount = tileSize[tileIndex];
        if(tileStart[tileIndex] + particlesCount >= 10000000) {
            break; //TODO: fix
        }

        float pixelPosX = (((tileX*8+x)/(width-1.0f))-0.5f)*1000.0f*width/height;
        float pixelPosY = (((tileY*8+y)/(height-1.0f))-0.5f)*1000.0f;

        float colorR = 0.0f;
        float colorG = 0.0f;
        float colorB = 0.0f;
        float colorA = 1.0f;

        for(int i=0;i<particlesCount;i++){
            float particleX = particles[particleIds[i]*8];
            float particleY = particles[particleIds[i]*8+1];
            float particleSize = particles[particleIds[i]*8+2];
            float particleR = particles[particleIds[i]*8+3];
            float particleG = particles[particleIds[i]*8+4];
            float particleB = particles[particleIds[i]*8+5];
            float particleA = particles[particleIds[i]*8+6];
            float particleEdge = particles[particleIds[i]*8+7];

            float xoff=pixelPosX-particleX;
            float yoff=pixelPosY-particleY;
            float dist = native_sqrt(xoff*xoff+yoff*yoff);
            float sum = (1.0f - linearstep(particleSize*particleEdge - lowcostAntialiasing, particleSize, dist + lowcostAntialiasing/2)) * particleA;
            sum = clamp(sum, 0.0f, 1.0f);
            float invertSum = 1.0f-sum;
            colorR = colorR + (particleR - colorR)*sum;
            colorG = colorG + (particleG - colorG)*sum;
            colorB = colorB + (particleB - colorB)*sum;

            colorA = colorA + (particleA - colorA)*sum;
            //colorA = 1.0f;
        }

        if((tileY*8+y) < height && (tileX*8+x) < width) {
            outImage[((tileY*8+y)*width + (tileX*8+x))*4] = colorR;
            outImage[((tileY*8+y)*width + (tileX*8+x))*4+1] = colorG;
            outImage[((tileY*8+y)*width + (tileX*8+x))*4+2] = colorB;
            outImage[((tileY*8+y)*width + (tileX*8+x))*4+3] = colorA;
        }
    }
}

__kernel void clearTileSize(__global int *tileSize, int width, int height) {
    int tilesCount = ((width+7)/8) * ((height+7)/8);
    for(int tileIndex=0; tileIndex<tilesCount; tileIndex++) {
        tileSize[tileIndex] = 0;
    }
}

#define FILL_TILE_SIZE_PARTICLE_BATCH_SIZE 512
#define FILL_TILE_SIZE_BLOCK_THREAD_SIZE 512

__kernel void fillTileSize(__global float *particles, __global int *particleBox, __global int *tileSize, int width, int height, int particlesCount, __global int *atomicParticleIndex) {
    __local int sharedParticles[FILL_TILE_SIZE_PARTICLE_BATCH_SIZE];
    __local int particlesStart;
    int tilesCount = ((width+7)/8) * ((height+7)/8);
    int threadIdx = get_local_id(0);

    float tileXStart = -500.0f*width/height;
    float tileXStep = ((8.0f/(width-1.0f))-0.5f)*1000.0f*width/height - tileXStart;
    float tileYStart = -500.0f;
    float tileYStep = ((8.0f/(height-1.0f))-0.5f)*1000.0f - tileYStart;

    for(;;) {
        if(threadIdx == 0){
            particlesStart = atomic_add(&atomicParticleIndex[0], FILL_TILE_SIZE_PARTICLE_BATCH_SIZE);
        }
        barrier(CLK_LOCAL_MEM_FENCE);
        if(particlesStart >= particlesCount) {
            break;
        }
        for(int particleIndex = threadIdx; particleIndex < FILL_TILE_SIZE_PARTICLE_BATCH_SIZE && (particlesStart + particleIndex) < particlesCount; particleIndex+=FILL_TILE_SIZE_BLOCK_THREAD_SIZE) {
            float px = particles[(particlesStart+particleIndex)*8];
            float py = particles[(particlesStart+particleIndex)*8+1];
            float ps = particles[(particlesStart+particleIndex)*8+2];

            int tileLeft = ((px-ps)-tileXStart)/tileXStep;
            int tileRight = ((px+ps)-tileXStart)/tileXStep;
            int tileTop = ((py-ps)-tileYStart)/tileYStep;
            int tileBottom = ((py+ps)-tileYStart)/tileYStep;
            tileLeft = max(0, min(0xFF, tileLeft));
            tileRight = max(0, min(0xFF, tileRight));
            tileTop = max(0, min(0xFF, tileTop));
            tileBottom = max(0, min(0xFF, tileBottom));
            int pbox = (tileLeft<<24) | (tileRight<<16) | (tileTop<<8) | tileBottom;
            sharedParticles[particleIndex] = pbox;
            particleBox[particlesStart+particleIndex] = pbox;
        }
        barrier(CLK_LOCAL_MEM_FENCE);
        for(int tileIndex = threadIdx; tileIndex<tilesCount; tileIndex+=FILL_TILE_SIZE_BLOCK_THREAD_SIZE) {
            int result = 0;
            int tileX = tileIndex % ((width+7)/8);
            int tileY = tileIndex / ((width+7)/8);
            for(int particleIndex = 0; particleIndex < FILL_TILE_SIZE_PARTICLE_BATCH_SIZE && (particlesStart + particleIndex) < particlesCount; particleIndex+=1) {
                int pbox = sharedParticles[particleIndex];
                result += (tileX>=((pbox>>24) & 0x000000FF) && tileX<=((pbox>>16) & 0x000000FF) && tileY>=((pbox>>8) & 0x000000FF) && tileY <= (pbox & 0x000000FF));
            }
            atomic_add(&tileSize[tileIndex], result);
        }
        barrier(CLK_LOCAL_MEM_FENCE);
    }
}

__kernel void fillTileStart(__global int *tileStart, __global int *tileSize, int width, int height) {
    int tilesCount = ((width+7)/8) * ((height+7)/8);
    int accum = 0;
    for(int tileIndex=0; tileIndex<tilesCount; tileIndex++) {
        tileStart[tileIndex] = accum;
        accum+=tileSize[tileIndex];
    }
}

__kernel void fillTiles(__global int *particleBox, __global int *tiles, __global int *tileStart, int width, int height, int particlesCount, __global int *atomicTileIndex) {
    int tilesCount = ((width+7)/8) * ((height+7)/8);

    float tileXStart = -500.0f*width/height;
    float tileXStep = ((8.0f/(width-1.0f))-0.5f)*1000.0f*width/height - tileXStart;
    float tileYStart = -500.0f;
    float tileYStep = ((8.0f/(height-1.0f))-0.5f)*1000.0f - tileYStart;

    for(;;) {
        int tileIndex = atomic_add(&atomicTileIndex[0], 1);
        if(tileIndex >= tilesCount) {
            break;
        }

        int tileX = tileIndex % ((width+7)/8);
        int tileY = tileIndex / ((width+7)/8);
        int tileStartCurrent = tileStart[tileIndex];

        for(int particleIndex = 0; particleIndex < particlesCount && tileStartCurrent<10000000; particleIndex++) {
            int pbox = particleBox[particleIndex];
            if(tileX>=((pbox>>24) & 0x000000FF) && tileX<=((pbox>>16) & 0x000000FF) && tileY>=((pbox>>8) & 0x000000FF) && tileY <= (pbox & 0x000000FF)){
                tiles[tileStartCurrent] = particleIndex;
                tileStartCurrent++;
            }
        }
    }
}

float hue2rgb(float p, float q, float t){
    if(t<0.0f) t+=1.0f;
    if(t>1.0f) t-=1.0f;
    if(t<1.0f/6.0f) return p+(q-p)*6.0*t;
    else if(t<1.0f/2.0f) return q;
    else if(t<2.0f/3.0f) return p+(q-p)*(2.0f/3.0f-t)*6.0f;
    else return p;
}

float calcLuminance(float r, float g, float b){
    float Pr = 0.2126;
    float Pg = 0.7152;
    float Pb = 0.0722;
    return r*Pr + g*Pg + b*Pb;
}

float3 convertHueChromaLuminanceToRGB(float h, float c, float l){
    float rangeA = 0.0f;
    float rangeB = 1.0f;
    float r = 0.0f;
    float g = 0.0f;
    float b = 0.0f;

    for(int i=0;i<20;i++){
        float m = (rangeA+rangeB)/2.0f;
        float q;
        if(m<0.5f) q=m*(1+c);
        else q=m+c-m*c;

        float p = 2*m-q;
        r = pow(hue2rgb(p,q,h+1.0f/3.0), 2.2f);
        g = pow(hue2rgb(p,q,h), 2.2f);
        b = pow(hue2rgb(p,q,h-1.0f/3.0), 2.2f);
        if(calcLuminance(r,g,b)>l){
            rangeB = m;
        }else{
            rangeA = m;
        }
    }

    return (float3)(r,g,b);
}

float2 convertRGBToHueChroma(float r, float g, float b){
    float max = fmax(r,fmax(g,b));
    float min = fmin(r,fmin(g,b));
    float h = (max + min)/2.0f;
    float d = max-min;
    float s = d/(max+min);
    if(max == r){
        h = (g-b)/d + (g<b ? 6.0f : 0.0f);
    }
    if(max == g){
        h = (b-r)/d+2.0f;
    }
    if(max == b){
        h = (r-g)/d+4.0f;
    }
    return (float2)(h/6.0f, s);
}

__kernel void brightPixelsToWhite(__global float *inputImage, int size, __global float *outputImage){
    int workId = get_global_id(0);
    if(workId >= size) return;

    float r = inputImage[workId*4];
    float g = inputImage[workId*4+1];
    float b = inputImage[workId*4+2];
    float a = clamp(inputImage[workId*4+3], 0.0f, 1.0f);
    r = r*a;
    g = g*a;
    b = b*a;
    float l = calcLuminance (r, g, b);

    r = pow(r, 1.0f/2.2f);
    g = pow(g, 1.0f/2.2f);
    b = pow(b, 1.0f/2.2f);

    float2 hc = convertRGBToHueChroma(r,g,b);
    float3 rgb = convertHueChromaLuminanceToRGB(hc.x, hc.y, l);

    outputImage[workId*3  ] = clamp(rgb.x, 0.0f, 1.0f);
    outputImage[workId*3+1] = clamp(rgb.y, 0.0f, 1.0f);
    outputImage[workId*3+2] = clamp(rgb.z, 0.0f, 1.0f);
}

__kernel void forSave(__global float *inputImage, int size, __global float *outputImage){
    int workId = get_global_id(0);
    if(workId >= size) return;

    float color = inputImage[workId];
    outputImage[workId] = clamp(pow(color, 1.0f/2.2f)*255.0f, 0.0f, 255.0f);
}

__kernel void pixelSum(__global float *accumImage, __global float *addImage, float k, int size){
    int workId = get_global_id(0);
    if(workId >= size) return;

    float accumColor = accumImage[workId];
    float addColor = addImage[workId];
    accumImage[workId] = accumColor + addColor*k;
}

__kernel void zeroBuffer(__global float *buffer, int size){
    int workId = get_global_id(0);
    if(workId >= size) return;
    buffer[workId] = 0.0f;
}