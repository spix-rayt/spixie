#version 400
in vec2 position;
in vec2 center;
in float pointSize;
in vec4 color;
uniform mat4 cameraMatrix;

out vec2 uv;
out vec4 outColor;

void main(){
    gl_Position = vec4(position*pointSize+center, 0.0, 1.0)*cameraMatrix;
    uv = position;
    outColor = color;
}
