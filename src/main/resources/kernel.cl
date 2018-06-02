__kernel void renderParticles(__global float *particles, int width, int height, int realWidth, int particlesCount, int ssaa, int depthRender, __global float *outImage) {
    __local float p[256*7];
    __local float validp[256];
    int workGroup = get_group_id(0);
    int localId = get_local_id(0);

    int groupX = (workGroup)%(width/256);
    int imgy = (workGroup)/(width/256);
    int imgx = groupX*256 + localId;

    float2 pixelPos = (float2)( ((imgx/(realWidth-1.0f))-0.5f)*1000.0f*realWidth/height , ((imgy/(height-1.0f))-0.5f)*1000.0f );
    float red = 0.0f;
    float green = 0.0f;
    float blue = 0.0f;
    float alpha = 0.0f;

    int pBlocksCount = particlesCount/256;
    for(int pBlock = 0; pBlock < pBlocksCount; pBlock++){
        barrier(CLK_LOCAL_MEM_FENCE);
        event_t ev1 = async_work_group_copy(p, particles + pBlock*256*7, 256*7, 0);
        wait_group_events(1, &ev1);
        __local int j;
        if(localId == 0){
            j=0;
        }
        barrier(CLK_LOCAL_MEM_FENCE);
        if(fabs(p[localId*7+1] - pixelPos.y) < p[localId*7+2]+1.0f){
            validp[atomic_inc(&j)] = localId;
        }
        barrier(CLK_LOCAL_MEM_FENCE);

        float power=1.0f/(ssaa*ssaa);

        for(int q=0;q<j;q++){
            int i = validp[q];
            if(depthRender){
                float d = fast_distance((float2)(p[i*7], p[i*7+1]), pixelPos)/(p[i*7+2]);
                float k = (1.0f - smoothstep(0.98f, 0.98f, d));
                red = red*(1.0f-k) + p[i*7+3]*k;
                green = green*(1.0f-k) + p[i*7+4]*k;
                blue = blue*(1.0f-k) + p[i*7+5]*k;
                alpha = 1.0;
            }else{
                float sum = 0.0f;
                for(int ssaaX=0; ssaaX<ssaa; ssaaX++){
                    float offsetX = (((ssaaX+0.5f)/ssaa)/2.0f-0.5f);
                    for(int ssaaY=0; ssaaY<ssaa; ssaaY++){
                        float offsetY = (((ssaaY+0.5f)/ssaa)/2.0f-0.5f);
                        float rotatedOffsetX = 0.898f * offsetX - 0.438f * offsetY;
                        float rotatedOffsetY = 0.438f * offsetX + 0.898f * offsetY;
                        float2 pixelPosWithOffset = (float2)( (((imgx + rotatedOffsetX)/(realWidth-1.0f))-0.5f)*1000.0f*realWidth/height , (((imgy + rotatedOffsetY)/(height-1.0f))-0.5f)*1000.0f );
                        float d = fast_distance((float2)(p[i*7], p[i*7+1]), pixelPosWithOffset)/p[i*7+2];
                        sum += (1.0f - smoothstep(0.0f, 1.0f, d)) * p[i*7+6] * power;
                    }
                }
                red = red*(1.0f-sum) + p[i*7+3]*sum;
                green = green*(1.0f-sum) + p[i*7+4]*sum;
                blue = blue*(1.0f-sum) + p[i*7+5]*sum;
                alpha = sum + alpha*(1.0f-sum);
            }
        }
    }

    __local float tile[256*4];

    alpha = clamp(alpha, 0.00001f, 1.0f);

    if(depthRender){
        tile[localId*4] = 1.0f - red;
        tile[localId*4 + 1] = 1.0f - green;
        tile[localId*4 + 2] = 1.0f - blue;
        tile[localId*4 + 3] = 1;
    }else{
        tile[localId*4] = red/alpha;
        tile[localId*4 + 1] = green/alpha;
        tile[localId*4 + 2] = blue/alpha;
        tile[localId*4 + 3] = alpha;
    }

    barrier(CLK_LOCAL_MEM_FENCE);

    int groupX256 = groupX*256;
    int px = (imgy*realWidth + groupX256)*4;
    async_work_group_copy(outImage+px, tile, (clamp(groupX256+256, 0, realWidth)-groupX256)*4, 0);
}