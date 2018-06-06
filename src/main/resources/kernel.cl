typedef struct{
    float x;
    float y;
    float size;
    float r;
    float g;
    float b;
    float a;
} Particle;

#define LOCAL_WORK_SIZE 64
#define SSAA 6
#define SSAA_SQUARED SSAA * SSAA

__kernel void renderParticles(__global float *particles, int width, int height, int realWidth, int particlesCount, int depthRender, __global float *outImage) {
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
    {
        int ssaaX = localId%SSAA;
        int ssaaY = localId/SSAA;
        float offsetX = (((ssaaX+0.5f)/SSAA)/2.0f-0.5f);
        float offsetY = (((ssaaY+0.5f)/SSAA)/2.0f-0.5f);
        ssaaOffsets[ssaaY*SSAA+ssaaX] = (float2)(0.898f * offsetX - 0.438f * offsetY, 0.438f * offsetX + 0.898f * offsetY); //rotated by 26 degrees
    }

    barrier(CLK_LOCAL_MEM_FENCE);

    int pBlocksCount = particlesCount/LOCAL_WORK_SIZE;
    for(int pBlock = 0; pBlock < pBlocksCount; pBlock++){
        barrier(CLK_LOCAL_MEM_FENCE);
        event_t ev1 = async_work_group_copy((__local float*)p, particles + pBlock*LOCAL_WORK_SIZE*7, LOCAL_WORK_SIZE*7, 0);
        __local int j;
        if(localId == 0){
            j=0;
        }
        barrier(CLK_LOCAL_MEM_FENCE);
        wait_group_events(1, &ev1);
        if(fabs(p[localId].y - pixelPos.y) < p[localId].size+1.0f && fabs(p[localId].x - workGroupXCenter) < p[localId].size+workGroupXLength+1.0f){
            validp[atomic_inc(&j)] = localId;
        }
        barrier(CLK_LOCAL_MEM_FENCE);

        for(int q=0;q<j;q++){
            int i = validp[q];
            if(depthRender){
                float d = fast_distance((float2)(p[i].x, p[i].y), pixelPos)/(p[i].size);
                float k = (1.0f - smoothstep(0.98f, 0.98f, d));
                red = red*(1.0f-k) + p[i].r*k;
                green = green*(1.0f-k) + p[i].g*k;
                blue = blue*(1.0f-k) + p[i].b*k;
                alpha = 1.0;
            }else{
                float sum = 0.0f;
                for(int ssaaI=0;ssaaI<SSAA_SQUARED;ssaaI++){
                    float2 pixelPosWithOffset = (float2)( (((imgx + ssaaOffsets[ssaaI].x)/(realWidth-1.0f))-0.5f)*1000.0f*realWidth/height , (((imgy + ssaaOffsets[ssaaI].y)/(height-1.0f))-0.5f)*1000.0f );
                    float d = fast_distance((float2)(p[i].x, p[i].y), pixelPosWithOffset)/p[i].size;
                    sum += (1.0f - smoothstep(0.0f, 1.0f, d)) * p[i].a * ssaaPixelIntensity;
                }
                red = red*(1.0f-sum) + p[i].r*sum;
                green = green*(1.0f-sum) + p[i].g*sum;
                blue = blue*(1.0f-sum) + p[i].b*sum;
                alpha = sum + alpha*(1.0f-sum);
            }
        }
    }

    __local float tile[LOCAL_WORK_SIZE*4];

    alpha = clamp(alpha, 0.0f, 1.0f);

    if(depthRender){
        tile[localId*4] = 1.0f - red;
        tile[localId*4 + 1] = 1.0f - green;
        tile[localId*4 + 2] = 1.0f - blue;
        tile[localId*4 + 3] = 1;
    }else{
        tile[localId*4] = clamp(red/alpha, 0.0f, 100000.0f);
        tile[localId*4 + 1] = clamp(green/alpha, 0.0f, 100000.0f);
        tile[localId*4 + 2] = clamp(blue/alpha, 0.0f, 100000.0f);
        tile[localId*4 + 3] = alpha;
    }

    int groupXL = groupX*LOCAL_WORK_SIZE;
    int px = (imgy*realWidth + groupXL)*4;

    barrier(CLK_LOCAL_MEM_FENCE);
    async_work_group_copy(outImage+px, tile, (clamp(groupXL+LOCAL_WORK_SIZE, 0, realWidth)-groupXL)*4, 0);
}