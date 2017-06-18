#version 400
in vec2 uv;

out vec4 frag_colour;

void main(){
    float d = sqrt(uv.x*uv.x + uv.y*uv.y);
    frag_colour = vec4(0.0,1.0,0.0,1.0-d);
}