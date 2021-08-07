float linearstep(float start, float end, float value){
    return clamp((value - start) / (end - start), 0.0f, 1.0f);
}

float map(float start, float end, float value) {
    return start + (end - start) * value;
}

float mod(float a, float b) {
    return fmod(fmod(a, b) + b, b);
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

__kernel void render(__global float *objects, int objectsCount, int width, int height, int equirectangular, int vr, float screenWidth, float screenHeight, float screenDistance, __global float *outImage) {
    int id = get_global_id(0);
    float2 uv;
    int left = (id % width) < width / 2;
    if(vr) {
        uv = (float2)((float)((id % width) % (width / 2)) / (float)(width / 2), (float)(id / width) / (float)height);
    } else {
        uv = (float2)((float)(id % width) / (float)width, (float)(id / width) / (float)height);
    }

    if(id < width * height) {
        float r = 0.0f;
        float g = 0.0f;
        float b = 0.0f;

        float3 rayOrigin = (float3)(0.0f, 0.0f, 0.0f);
        if(vr) {
            if(left) {
                rayOrigin.x -= 0.063f / 2.0f;
            } else {
                rayOrigin.x += 0.063f / 2.0f;
            }
        }
        float3 rayDirection;

        if(equirectangular != 0) {
            float hfov = 180.0f;
            float vfov = 180.0f;
            float phi = map(-radians(hfov / 2.0f), radians(hfov / 2.0f), uv.x);
            float theta = map(M_PI / 2.0f - radians(vfov / 2.0f), M_PI / 2.0f + radians(vfov / 2.0f), uv.y);
            rayDirection = normalize((float3)(
                sin(phi) * sin(theta),
                cos(theta),
                cos(phi) * sin(theta)
            ));
        } else {
            rayDirection = normalize((float3)(
                map(-screenWidth, screenWidth, uv.x),
                map(-screenHeight, screenHeight, uv.y),
                screenDistance
            ));
        }

        for(int i = 0; i < objectsCount; i++) {
            float3 pos = (float3)(objects[i * 7 + 0], objects[i * 7 + 1], objects[i * 7 + 2]);
            float psize = objects[i * 7 + 3];
            float3 color = (float3)(objects[i * 7 + 4], objects[i * 7 + 5], objects[i * 7 + 6]);

            float dt = dot(rayDirection, pos);
            float3 projection = rayOrigin + (rayDirection * dt);
            float dist = length(projection - pos);
            if(dist <= psize) {
                r = color.x;
                g = color.y;
                b = color.z;
                i = objectsCount;
            }
        }

        outImage[id * 4    ] = r;
        outImage[id * 4 + 1] = g;
        outImage[id * 4 + 2] = b;
        outImage[id * 4 + 3] = 1.0f;
    }
}