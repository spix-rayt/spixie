#version 400
in vec2 uv;
in vec4 outColor;

out vec4 frag_colour;

void main(){
    float d = sqrt(uv.x*uv.x + uv.y*uv.y);
    frag_colour = vec4(outColor.x, outColor.y, outColor.z, outColor.w * (1.0-d));
}