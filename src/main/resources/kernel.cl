#pragma OPENCL EXTENSION cl_khr_int64_base_atomics : enable

kernel void RenderParticles(global float *particles, int width, int height, int particlesCount, global int *outImage) {
    __local float p[256*7];
    __local float validp[256];
    int workGroup = get_group_id(0);
    int localId = get_local_id(0);

    int groupX = (workGroup)%(width/256);
    int imgy = (workGroup)/(width/256);
    int imgx = groupX*256 + localId;

    float2 pixelPos = (float2)( ((imgx/(width-1.0f))-0.5f)*1000.0f*width/height , ((imgy/(height-1.0f))-0.5f)*1000.0f );
    float red = 0.0f;
    float green = 0.0f;
    float blue = 0.0f;
    float alpha = 0.0f;

    int pBlocksCount = particlesCount/256;
    for(int pBlock = 0; pBlock < pBlocksCount; pBlock++){
        barrier(CLK_LOCAL_MEM_FENCE);
        event_t ev1 = async_work_group_copy(p, particles + pBlock*256*7, 256*7, 0);
        wait_group_events(1, &ev1);
        __local long j;
        if(localId == 0){
            j=0;
        }
        barrier(CLK_LOCAL_MEM_FENCE);
        if(fabs(p[localId*7+1] - pixelPos.y) < p[localId*7+2]){
            validp[atom_inc(&j)] = localId;
        }
        barrier(CLK_LOCAL_MEM_FENCE);

        for(int q=0;q<j;q++){
            int i = validp[q];
            float d = fast_distance((float2)(p[i*7], p[i*7+1]), pixelPos)/p[i*7+2];
            float srca=clamp(p[i*7+6]*(1.0f-d), 0.0f, 1.0f);
            float rsrca = 1.0f-srca;
            red = red*rsrca + p[i*7+3]*srca;
            green = green*rsrca + p[i*7+4]*srca;
            blue = blue*rsrca + p[i*7+5]*srca;
        }
        alpha = 1.0f;
    }

    __local int tile[256*4];
    tile[localId*4] = clamp((int)round(red*255), 0, 255);
    tile[localId*4 + 1] = clamp((int)round(green*255), 0, 255);
    tile[localId*4 + 2] = clamp((int)round(blue*255), 0, 255);
    tile[localId*4 + 3] = clamp((int)round(alpha*255), 0, 255);
    barrier(CLK_LOCAL_MEM_FENCE);

    int px = (imgy*width + groupX*256)*4;
    async_work_group_copy(outImage+px, tile, 256*4, 0);
}
