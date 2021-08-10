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

float noise3D(float x, float y, float z) {
    float ptr = 0.0f;
    return fract(sin(x*112.9898f + y*179.233f + z*237.212f) * 43758.5453f, &ptr);
}

float3 cameraShiftVector(int id, int sample, int samplesPerPixel) {
    float p = (float)sample / (float)samplesPerPixel;
    float angle = noise3D((float)id, ((float)sample) * 1.234, p - 12.3456f) * M_PI * 2.0f;
    float l = noise3D((float)id, ((float)sample) * 1.234, p + 12.3456f);
    float x = cos(angle) * l;
    float y = sin(angle) * l;
    return (float3)(x, y, 0.0f);
}

__kernel void brightPixelsToWhite(__global float *inputImage, int size, float k, __global float *outputImage){
    int workId = get_global_id(0);
    if(workId >= size) return;

    float r = inputImage[workId * 3    ] * k;
    float g = inputImage[workId * 3 + 1] * k;
    float b = inputImage[workId * 3 + 2] * k;
    r = r;
    g = g;
    b = b;
    float l = calcLuminance (r, g, b);

    r = pow(r, 1.0f/2.2f);
    g = pow(g, 1.0f/2.2f);
    b = pow(b, 1.0f/2.2f);

    float2 hc = convertRGBToHueChroma(r,g,b);
    float3 rgb = convertHueChromaLuminanceToRGB(hc.x, hc.y, l);

    outputImage[workId * 3    ] = clamp(rgb.x, 0.0f, 1.0f);
    outputImage[workId * 3 + 1] = clamp(rgb.y, 0.0f, 1.0f);
    outputImage[workId * 3 + 2] = clamp(rgb.z, 0.0f, 1.0f);
}

__kernel void prepareForSave(__global float *inputImage, int size, __global float *outputImage){
    int workId = get_global_id(0);
    if(workId >= size) return;

    float color = inputImage[workId];
    outputImage[workId] = clamp(pow(color, 1.0f/2.2f)*255.0f, 0.0f, 255.0f);
}

__kernel void zeroBuffer(__global float *buffer, int size){
    int workId = get_global_id(0);
    if(workId >= size) return;
    buffer[workId] = 0.0f;
}

__kernel void render(__global float *objects, int objectsCount, int width, int height, int equirectangular, int vr, float screenSize, float screenDistance, int samplesPerPixel, int chunkX, int chunkY, __global float *outImage) {
    int id = get_global_id(0);
    int coordX = id % 64 + chunkX;
    int coordY = id / 64 + chunkY;
    float2 uv;
    int left = (id % width) < width / 2;
    if(vr) {
        uv = (float2)((float)(coordX % (width / 2)) / (float)(width / 2), (float)coordY / (float)height);
    } else {
        uv = (float2)((float)coordX / (float)width, (float)coordY / (float)height);
    }
    float2 pixelSize = (float2)(1.0f / (float)width, 1.0f / (float)height);

    if(coordX < width && coordY < height) {
        float r = 0.0f;
        float g = 0.0f;
        float b = 0.0f;

        for(int sample = 0; sample < samplesPerPixel; sample++) {
            float focalLength = 60.0f;
            float aperture = 0.00f;
            float2 uv2 = (float2)(uv.x + pixelSize.x * (noise3D((float)id, ((float)sample) * 1.234, 12.3456f) - 0.5f), uv.y + pixelSize.y * (noise3D((float)id, ((float)sample) * 1.234, 45.6789f) - 0.5f));

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
                float phi = map(-radians(hfov / 2.0f), radians(hfov / 2.0f), uv2.x);
                float theta = map(M_PI / 2.0f - radians(vfov / 2.0f), M_PI / 2.0f + radians(vfov / 2.0f), uv2.y);
                rayDirection = normalize((float3)(
                    sin(phi) * sin(theta),
                    cos(theta),
                    cos(phi) * sin(theta)
                ));
            } else {
                float screenWidth = (float)width / (float)height * screenSize;
                float screenHeight = screenSize;
                rayDirection = normalize((float3)(
                    map(-screenWidth, screenWidth, uv2.x),
                    map(-screenHeight, screenHeight, uv2.y),
                    screenDistance
                ));
            }

            float3 focalPoint = rayOrigin + (rayDirection * focalLength);
            rayOrigin = rayOrigin + cameraShiftVector(id, sample, samplesPerPixel) * aperture;
            rayDirection = normalize(focalPoint - rayOrigin);

            float visionFactor = 1.0;

            for(int i = 0; i < objectsCount; i++) {
                float3 pos = (float3)(objects[i * 7 + 0], objects[i * 7 + 1], objects[i * 7 + 2]);
                float psize = objects[i * 7 + 3];
                float3 color = (float3)(objects[i * 7 + 4], objects[i * 7 + 5], objects[i * 7 + 6]);

                float dt = max(dot(rayDirection, pos), 0.0f);
                float3 projection = rayOrigin + (rayDirection * dt);
                float dist = length(projection - pos);
                if(dist <= psize) {
                    r += color.x * visionFactor;
                    g += color.y * visionFactor;
                    b += color.z * visionFactor;
                    visionFactor *= dist / psize;
                }
            }
        }

        int outputIndex = (coordY * width + coordX) * 3;

        outImage[outputIndex    ] += r / (float)samplesPerPixel;
        outImage[outputIndex + 1] += g / (float)samplesPerPixel;
        outImage[outputIndex + 2] += b / (float)samplesPerPixel;
    }
}