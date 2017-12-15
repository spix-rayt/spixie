kernel void RenderParticles(global float *particles, int width, int height, int blocksize, int blockX, int blockY, int particlesCount, global float *outImage) {
    int px = get_global_id(0);
    int imgx = px%blocksize + blockX;
    int imgy = px/blocksize + blockY;

    float2 pixelPos = (float2)( ((imgx/(width-1.0f))-0.5f)*1000.0f*width/height , ((imgy/(height-1.0f))-0.5f)*1000.0f );

    float red = 0.0f;
    float green = 0.0f;
    float blue = 0.0f;
    float alpha = 0.0f;

    for(int i=0;i<particlesCount;i++){
        float2 particlePos = (float2)(particles[i*7], particles[i*7+1]);
        float particleSize = particles[i*7+2];
        float4 particleColor = (float4)(particles[i*7+3], particles[i*7+4], particles[i*7+5], particles[i*7+6]);

        float d = length(particlePos - pixelPos)/particleSize;
        float al = max(1.0f-d, 0.0f);
        float srca=clamp(particleColor.w*al, 0.0f, 1.0f);
        red = red*(1-srca) + particleColor.x*srca;
        green = green*(1-srca) + particleColor.y*srca;
        blue = blue*(1-srca) + particleColor.z*srca;
        alpha = 1.0;
    }

    outImage[px*4] = red;
    outImage[px*4+1] = green;
    outImage[px*4+2] = blue;
    outImage[px*4+3] = alpha;
}
