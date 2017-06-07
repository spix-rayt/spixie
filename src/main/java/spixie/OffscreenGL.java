package spixie;

import com.jogamp.opengl.*;
import com.jogamp.opengl.glu.GLU;
import javafx.scene.Node;
import org.apache.commons.io.IOUtils;
import org.joml.Matrix4f;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;

public class OffscreenGL implements GLEventListener {
    private static final GLU glu = new GLU();
    private int shaderProgram;

    @Override
    public void init(GLAutoDrawable drawable) {
        drawable.getContext().makeCurrent();
        shaderProgram = loadShaders(drawable);

        GL4ES3 gl = drawable.getGL().getGL4ES3();
        gl.glEnable(GL.GL_MULTISAMPLE);
        gl.glEnable(GL.GL_BLEND);
        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);




        int[] buffer = new int[2];
        gl.glGenBuffers(2, buffer, 0);
        squareParticleMeshVBO = buffer[0];
        particlesVBO = buffer[1];


        position_attribute = gl.glGetAttribLocation(shaderProgram, "position");
        center_attribute = gl.glGetAttribLocation(shaderProgram, "center");
        pointSize_attribute = gl.glGetAttribLocation(shaderProgram, "pointSize");

        uniformCameraMatrix = gl.glGetUniformLocation(shaderProgram, "cameraMatrix");

        gl.glEnableVertexAttribArray(position_attribute);
        gl.glEnableVertexAttribArray(center_attribute);
        gl.glEnableVertexAttribArray(pointSize_attribute);
        gl.glVertexAttribDivisor(position_attribute, 0);
        gl.glVertexAttribDivisor(center_attribute, 1);
        gl.glVertexAttribDivisor(pointSize_attribute, 1);

        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, squareParticleMeshVBO);
        FloatBuffer vertices = FloatBuffer.wrap(new float[]{-0.5f, -0.5f, 0.5f, -0.5f, -0.5f, 0.5f, 0.5f, 0.5f});
        gl.glBufferData(GL.GL_ARRAY_BUFFER, vertices.capacity()*4, vertices, GL.GL_STATIC_DRAW);
        gl.glVertexAttribPointer(position_attribute, 2, GL.GL_FLOAT, false, 0, 0);


        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, particlesVBO);
        gl.glVertexAttribPointer(center_attribute, 2, GL.GL_FLOAT, false, ParticlesBuilder.PARTICLE_FLOAT_SIZE*4, 0);
        gl.glVertexAttribPointer(pointSize_attribute, 1, GL.GL_FLOAT, false, ParticlesBuilder.PARTICLE_FLOAT_SIZE*4, 8);
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {

    }

    private int squareParticleMeshVBO;
    private int particlesVBO;

    private int position_attribute;
    private int center_attribute;
    private int pointSize_attribute;

    private int uniformCameraMatrix;


    @Override
    public void display(GLAutoDrawable drawable) {
        GL4ES3 gl = drawable.getGL().getGL4ES3();
        gl.glViewport(0,0,drawable.getSurfaceWidth(), drawable.getSurfaceHeight());
        gl.glClearColor(0.0f,0.0f,0.0f,1.0f);

        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        ParticlesBuilder particlesBuilder = new ParticlesBuilder();

        for (Node node : Main.world.root.componentBody.elements.getChildren()) {
            if(node instanceof Multiplier){
                double radius = ((Multiplier) node).radius.get();
                double phase = ((Multiplier) node).phase.get();
                double size = ((Multiplier) node).size.get();
                double count = ((Multiplier) node).count.get();
                for (int i = 0; i < count; i++) {
                    particlesBuilder.addParticle(
                            (float)(Math.cos(Math.PI*2/count*i + phase*Math.PI*2)*radius),
                            (float)(Math.sin(Math.PI*2/count*i + phase*Math.PI*2)*radius),
                            (float)size
                    );
                }
            }
        }
        FloatBuffer buffer = particlesBuilder.toFloatBuffer();

        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, particlesVBO);
        gl.glBufferData(GL.GL_ARRAY_BUFFER, buffer.capacity()*4, buffer, GL2ES2.GL_STATIC_DRAW);

        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, squareParticleMeshVBO);
        gl.glDrawArraysInstanced(GL.GL_TRIANGLE_STRIP, 0, 4, particlesBuilder.particlesCount());
        gl.glFlush();
        gl.glFinish();
        int errorCode = gl.glGetError();
        if(errorCode!=GL.GL_NO_ERROR){
            System.out.println(errorCode + " " + glu.gluErrorString(errorCode));
        }
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        GL4ES3 gl = drawable.getGL().getGL4ES3();
        if(height!=0){
            float ratio = (float)width / (float)height;
            float[] floats = new float[16];
            new Matrix4f().identity().ortho(-ratio*1000, ratio*1000,-1000,1000, 0, 1000).get(floats);
            gl.glProgramUniformMatrix4fv(shaderProgram, uniformCameraMatrix, 1, true, FloatBuffer.wrap(floats));
        }
    }

    public int loadShaders(GLAutoDrawable drawable) {
        try {
            String vertexCode = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("vertex.glsl"), StandardCharsets.UTF_8);
            String fragmentCode = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("fragment.glsl"), StandardCharsets.UTF_8);

            GL4ES3 gl = drawable.getGL().getGL4ES3();
            int vShaderID = gl.glCreateShader(GL2ES2.GL_VERTEX_SHADER);
            gl.glShaderSource(vShaderID, 1, new String[]{vertexCode}, null);
            gl.glCompileShader(vShaderID);

            int[] test = new int[1];
            gl.glGetShaderiv(vShaderID, GL2ES2.GL_COMPILE_STATUS, test, 0);
            if(test[0] == 0){
                System.out.println("VERTEX SHADER COMPILE ERROR");
            }

            int fShaderID = gl.glCreateShader(GL2ES2.GL_FRAGMENT_SHADER);
            gl.glShaderSource(fShaderID, 1, new String[]{fragmentCode}, null);
            gl.glCompileShader(fShaderID);

            gl.glGetShaderiv(fShaderID, GL2ES2.GL_COMPILE_STATUS, test, 0);
            if(test[0] == 0){
                System.out.println("FRAGMENT SHADER COMPILE ERROR");
            }

            int shaderProgram = gl.glCreateProgram();
            gl.glAttachShader(shaderProgram, vShaderID);
            gl.glAttachShader(shaderProgram, fShaderID);
            gl.glLinkProgram(shaderProgram);
            gl.glUseProgram(shaderProgram);
            return shaderProgram;
        } catch (IOException e) {
            e.printStackTrace();
        }
        throw new Error("load shaders error");
    }
}
