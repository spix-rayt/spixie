#version 400
in vec2 uv;

out vec4 frag_colour;

void main(){
    float d = uv.x*uv.x + uv.y*uv.y;
    if(d<0.1){
        frag_colour = vec4(0.0,1.0,0.0,1.0);
    }else{
        frag_colour = vec4(0.0,1.0,0.0,0.0);
    }

}