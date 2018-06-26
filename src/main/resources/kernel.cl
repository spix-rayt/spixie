typedef struct{
    float x;
    float y;
    float size;
    float r;
    float g;
    float b;
    float a;
    float edge;
    float glow;
} Particle;

#define LOCAL_WORK_SIZE 64
#define SSAA 5
#define SSAA_SQUARED SSAA * SSAA //assert(SSAA_SQUARED<LOCAL_WORK_SIZE)

float linearstep(float edge0, float edge1, float x){
    return clamp((x-edge0)/(edge1-edge0), 0.0f, 1.0f);
}

__kernel void renderParticles(__global float *particles, int width, int height, int realWidth, int particlesCount, __global float *outImage) {
    __local Particle p[LOCAL_WORK_SIZE];
    __local float validp[LOCAL_WORK_SIZE];
    int workGroup = get_group_id(0);
    int localId = get_local_id(0);

    int groupX = (workGroup)%(width/LOCAL_WORK_SIZE);
    int imgy = (workGroup)/(width/LOCAL_WORK_SIZE);
    int imgx = groupX*LOCAL_WORK_SIZE + localId;

    float2 pixelPos = (float2)( ((imgx/(realWidth-1.0f))-0.5f)*1000.0f*realWidth/height , ((imgy/(height-1.0f))-0.5f)*1000.0f );
    float workGroupXLeftPixel = (((groupX*LOCAL_WORK_SIZE)/(realWidth-1.0f))-0.5f)*1000.0f*realWidth/height;
    float workGroupXRightPixel = ((((groupX+1)*LOCAL_WORK_SIZE-1)/(realWidth-1.0f))-0.5f)*1000.0f*realWidth/height;
    float workGroupXLength = workGroupXRightPixel - workGroupXLeftPixel;
    float workGroupXCenter = (workGroupXRightPixel + workGroupXLeftPixel)/2.0f;
    float red = 0.0f;
    float green = 0.0f;
    float blue = 0.0f;
    float alpha = 0.0f;

    float ssaaPixelIntensity = 1.0f/(SSAA_SQUARED);
    __local float2 ssaaOffsets[SSAA_SQUARED];
    if(localId<SSAA_SQUARED){
        int ssaaX = localId%SSAA;
        int ssaaY = localId/SSAA;
        float offsetX = (((ssaaX+0.5f)/SSAA)-0.5f);
        float offsetY = (((ssaaY+0.5f)/SSAA)-0.5f);
        ssaaOffsets[ssaaY*SSAA+ssaaX] = (float2)(0.898f * offsetX - 0.438f * offsetY, 0.438f * offsetX + 0.898f * offsetY); //rotated by 26 degrees
    }

    barrier(CLK_LOCAL_MEM_FENCE);

    int pBlocksCount = particlesCount/LOCAL_WORK_SIZE;
    for(int pBlock = 0; pBlock < pBlocksCount; pBlock++){
        barrier(CLK_LOCAL_MEM_FENCE);
        event_t ev1 = async_work_group_copy((__local float*)p, particles + pBlock*LOCAL_WORK_SIZE*9, LOCAL_WORK_SIZE*9, 0);
        __local int j;
        if(localId == 0){
            j=0;
        }
        barrier(CLK_LOCAL_MEM_FENCE);
        wait_group_events(1, &ev1);
        if(fabs(p[localId].y - pixelPos.y) < (p[localId].size*max(1.0f, p[localId].glow))+1.0f && fabs(p[localId].x - workGroupXCenter) < (p[localId].size*max(1.0f, p[localId].glow))+workGroupXLength+1.0f){
            validp[atomic_inc(&j)] = localId;
        }
        barrier(CLK_LOCAL_MEM_FENCE);

        for(int q=0;q<j;q++){
            int i = validp[q];
            float sum = 0.0f;
            for(int ssaaI=0;ssaaI<SSAA_SQUARED;ssaaI++){
                float2 pixelPosWithOffset = (float2)( (((imgx + ssaaOffsets[ssaaI].x)/(realWidth-1.0f))-0.5f)*1000.0f*realWidth/height , (((imgy + ssaaOffsets[ssaaI].y)/(height-1.0f))-0.5f)*1000.0f );
                float d = fast_distance((float2)(p[i].x, p[i].y), pixelPosWithOffset)/p[i].size;
                sum += (1.0f - linearstep(p[i].edge, 1.0f, d)) * p[i].a * ssaaPixelIntensity;
                float d_glow = d/p[i].glow;
                sum += (1.0f - linearstep(0.0f, 1.0f, d_glow)) * (1.0f - linearstep(0.0f, 1.0f, d_glow)) * p[i].a * ssaaPixelIntensity / p[i].glow / p[i].glow * linearstep(p[i].edge, 1.0f, d);
            }
            sum = clamp(sum, 0.0f, 1.0f);
            red = red*(1.0f-sum) + p[i].r*sum;
            green = green*(1.0f-sum) + p[i].g*sum;
            blue = blue*(1.0f-sum) + p[i].b*sum;
            alpha = sum + alpha*(1.0f-sum);
        }
    }

    __local float tile[LOCAL_WORK_SIZE*4];

    alpha = clamp(alpha, 0.0f, 1.0f);

    tile[localId*4] = clamp(red/alpha, 0.0f, 100000.0f);
    tile[localId*4 + 1] = clamp(green/alpha, 0.0f, 100000.0f);
    tile[localId*4 + 2] = clamp(blue/alpha, 0.0f, 100000.0f);
    tile[localId*4 + 3] = alpha;

    int groupXL = groupX*LOCAL_WORK_SIZE;
    int px = (imgy*realWidth + groupXL)*4;

    barrier(CLK_LOCAL_MEM_FENCE);
    async_work_group_copy(outImage+px, tile, (clamp(groupXL+LOCAL_WORK_SIZE, 0, realWidth)-groupXL)*4, 0);
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