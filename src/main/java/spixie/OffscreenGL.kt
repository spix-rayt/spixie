package spixie

import com.jogamp.opengl.GL
import com.jogamp.opengl.GL2ES2
import com.jogamp.opengl.GLAutoDrawable
import com.jogamp.opengl.GLEventListener
import com.jogamp.opengl.glu.GLU
import org.apache.commons.io.IOUtils
import org.joml.Matrix4f
import java.io.IOException
import java.nio.FloatBuffer
import java.nio.charset.StandardCharsets

class OffscreenGL : GLEventListener {
    private var shaderProgram: Int = 0

    override fun init(drawable: GLAutoDrawable) {
        drawable.context.makeCurrent()
        shaderProgram = loadShaders(drawable)

        val gl = drawable.gl.gL4ES3
        gl.glEnable(GL.GL_MULTISAMPLE)
        gl.glEnable(GL.GL_BLEND)
        gl.glBlendEquation(GL.GL_FUNC_ADD)
        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE)


        val buffer = IntArray(2)
        gl.glGenBuffers(2, buffer, 0)
        squareParticleMeshVBO = buffer[0]
        particlesVBO = buffer[1]


        position_attribute = gl.glGetAttribLocation(shaderProgram, "position")
        center_attribute = gl.glGetAttribLocation(shaderProgram, "center")
        pointSize_attribute = gl.glGetAttribLocation(shaderProgram, "pointSize")

        uniformCameraMatrix = gl.glGetUniformLocation(shaderProgram, "cameraMatrix")

        gl.glEnableVertexAttribArray(position_attribute)
        gl.glEnableVertexAttribArray(center_attribute)
        gl.glEnableVertexAttribArray(pointSize_attribute)
        gl.glVertexAttribDivisor(position_attribute, 0)
        gl.glVertexAttribDivisor(center_attribute, 1)
        gl.glVertexAttribDivisor(pointSize_attribute, 1)

        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, squareParticleMeshVBO)
        val vertices = FloatBuffer.wrap(floatArrayOf(-1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f))
        gl.glBufferData(GL.GL_ARRAY_BUFFER, (vertices.capacity() * 4).toLong(), vertices, GL.GL_STATIC_DRAW)
        gl.glVertexAttribPointer(position_attribute, 2, GL.GL_FLOAT, false, 0, 0)


        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, particlesVBO)
        gl.glVertexAttribPointer(center_attribute, 2, GL.GL_FLOAT, false, ParticlesBuilder.PARTICLE_FLOAT_SIZE * 4, 0)
        gl.glVertexAttribPointer(pointSize_attribute, 1, GL.GL_FLOAT, false, ParticlesBuilder.PARTICLE_FLOAT_SIZE * 4, 8)
    }

    override fun dispose(drawable: GLAutoDrawable) {

    }

    private var squareParticleMeshVBO: Int = 0
    private var particlesVBO: Int = 0

    private var position_attribute: Int = 0
    private var center_attribute: Int = 0
    private var pointSize_attribute: Int = 0

    private var uniformCameraMatrix: Int = 0


    override fun display(drawable: GLAutoDrawable) {
        val gl = drawable.gl.gL4ES3
        gl.glViewport(0, 0, drawable.surfaceWidth, drawable.surfaceHeight)
        gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

        gl.glClear(GL.GL_COLOR_BUFFER_BIT or GL.GL_DEPTH_BUFFER_BIT)

        val particlesBuilder = ParticlesBuilder()


        for (child in Main.world.root.children) {
            val value = child.value
            if(value is ComponentObject){
                value.render(particlesBuilder)
            }
        }
        val buffer = particlesBuilder.toFloatBuffer()

        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, particlesVBO)
        gl.glBufferData(GL.GL_ARRAY_BUFFER, (buffer.capacity() * 4).toLong(), buffer, GL2ES2.GL_STATIC_DRAW)

        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, squareParticleMeshVBO)
        gl.glDrawArraysInstanced(GL.GL_TRIANGLE_STRIP, 0, 4, particlesBuilder.particlesCount())
        gl.glFlush()
        gl.glFinish()
        val errorCode = gl.glGetError()
        if (errorCode != GL.GL_NO_ERROR) {
            println(errorCode.toString() + " " + glu.gluErrorString(errorCode))
        }
    }

    override fun reshape(drawable: GLAutoDrawable, x: Int, y: Int, width: Int, height: Int) {
        val gl = drawable.gl.gL4ES3
        if (height != 0) {
            val ratio = width.toFloat() / height.toFloat()
            val floats = FloatArray(16)
            Matrix4f().identity().ortho(-ratio * 1000, ratio * 1000, -1000f, 1000f, 0f, 1000f).get(floats)
            gl.glProgramUniformMatrix4fv(shaderProgram, uniformCameraMatrix, 1, true, FloatBuffer.wrap(floats))
        }
    }

    fun loadShaders(drawable: GLAutoDrawable): Int {
        try {
            val vertexCode = IOUtils.toString(javaClass.getClassLoader().getResourceAsStream("vertex.glsl"), StandardCharsets.UTF_8)
            val fragmentCode = IOUtils.toString(javaClass.getClassLoader().getResourceAsStream("fragment.glsl"), StandardCharsets.UTF_8)

            val gl = drawable.gl.gL4ES3
            val vShaderID = gl.glCreateShader(GL2ES2.GL_VERTEX_SHADER)
            gl.glShaderSource(vShaderID, 1, arrayOf<String>(vertexCode), null)
            gl.glCompileShader(vShaderID)

            val test = IntArray(1)
            gl.glGetShaderiv(vShaderID, GL2ES2.GL_COMPILE_STATUS, test, 0)
            if (test[0] == 0) {
                println("VERTEX SHADER COMPILE ERROR")
            }

            val fShaderID = gl.glCreateShader(GL2ES2.GL_FRAGMENT_SHADER)
            gl.glShaderSource(fShaderID, 1, arrayOf<String>(fragmentCode), null)
            gl.glCompileShader(fShaderID)

            gl.glGetShaderiv(fShaderID, GL2ES2.GL_COMPILE_STATUS, test, 0)
            if (test[0] == 0) {
                println("FRAGMENT SHADER COMPILE ERROR")
            }

            val shaderProgram = gl.glCreateProgram()
            gl.glAttachShader(shaderProgram, vShaderID)
            gl.glAttachShader(shaderProgram, fShaderID)
            gl.glLinkProgram(shaderProgram)
            gl.glUseProgram(shaderProgram)
            return shaderProgram
        } catch (e: IOException) {
            e.printStackTrace()
        }

        throw Error("load shaders error")
    }

    companion object {
        private val glu = GLU()
    }
}
