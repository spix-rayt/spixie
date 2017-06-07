#version 400
in vec2 position;
in vec2 center;
in float pointSize;
uniform mat4 cameraMatrix;

out vec2 uv;

void main(){
    gl_Position = vec4(position*pointSize+center, 0.0, 1.0)*cameraMatrix;
    uv = position;
}