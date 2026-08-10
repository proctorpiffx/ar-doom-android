package com.ardoom.rendering

import android.content.Context
import android.opengl.GLES30
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * Shader for rendering visual effects: muzzle flashes, projectile glows,
 * and hit effects.
 */
class EffectShader(private val context: Context) {

    private var program: Int = 0
    private var mvpMatrixHandle: Int = 0
    private var colorHandle: Int = 0
    private var timeHandle: Int = 0

    private val quadVertices: FloatBuffer

    companion object {
        private const val VERTEX_SHADER = """
            uniform mat4 u_MVPMatrix;
            attribute vec4 a_Position;
            varying vec2 v_UV;

            void main() {
                v_UV = a_Position.xy * 0.5 + 0.5;
                gl_Position = u_MVPMatrix * a_Position;
            }
        """

        private const val FRAGMENT_SHADER = """
            precision mediump float;
            uniform vec3 u_Color;
            uniform float u_Time;
            varying vec2 v_UV;

            void main() {
                float d = distance(v_UV, vec2(0.5, 0.5));
                float intensity = 1.0 - smoothstep(0.0, 0.5, d);
                intensity *= 0.5 + 0.5 * sin(u_Time * 20.0);
                gl_FragColor = vec4(u_Color * intensity, intensity);
            }
        """

        private val QUAD_VERTICES = floatArrayOf(
            -0.5f, -0.5f, 0f,
             0.5f, -0.5f, 0f,
            -0.5f,  0.5f, 0f,
             0.5f,  0.5f, 0f
        )
    }

    init {
        val vertexShader = compileShader(GLES30.GL_VERTEX_SHADER, VERTEX_SHADER)
        val fragmentShader = compileShader(GLES30.GL_FRAGMENT_SHADER, FRAGMENT_SHADER)
        program = GLES30.glCreateProgram()
        GLES30.glAttachShader(program, vertexShader)
        GLES30.glAttachShader(program, fragmentShader)
        GLES30.glLinkProgram(program)

        mvpMatrixHandle = GLES30.glGetUniformLocation(program, "u_MVPMatrix")
        colorHandle = GLES30.glGetUniformLocation(program, "u_Color")
        timeHandle = GLES30.glGetUniformLocation(program, "u_Time")

        quadVertices = createFloatBuffer(QUAD_VERTICES)
    }

    fun drawMuzzleFlash(
        modelMatrix: FloatArray,
        viewMatrix: FloatArray,
        projectionMatrix: FloatArray
    ) {
        draw(modelMatrix, viewMatrix, projectionMatrix, 1.0f, 0.8f, 0.2f)
    }

    fun drawProjectile(
        modelMatrix: FloatArray,
        viewMatrix: FloatArray,
        projectionMatrix: FloatArray,
        r: Float, g: Float, b: Float
    ) {
        draw(modelMatrix, viewMatrix, projectionMatrix, r, g, b)
    }

    private fun draw(
        modelMatrix: FloatArray,
        viewMatrix: FloatArray,
        projectionMatrix: FloatArray,
        r: Float, g: Float, b: Float
    ) {
        val mvpMatrix = FloatArray(16)
        val mvMatrix = FloatArray(16)
        android.opengl.Matrix.multiplyMM(mvMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        android.opengl.Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvMatrix, 0)

        GLES30.glUseProgram(program)

        val positionHandle = GLES30.glGetAttribLocation(program, "a_Position")
        GLES30.glEnableVertexAttribArray(positionHandle)
        GLES30.glVertexAttribPointer(positionHandle, 3, GLES30.GL_FLOAT, false, 0, quadVertices)

        GLES30.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)
        GLES30.glUniform3f(colorHandle, r, g, b)
        GLES30.glUniform1f(timeHandle, System.currentTimeMillis() / 1000f)

        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)

        GLES30.glDisableVertexAttribArray(positionHandle)
    }

    private fun compileShader(type: Int, source: String): Int {
        val shader = GLES30.glCreateShader(type)
        GLES30.glShaderSource(shader, source)
        GLES30.glCompileShader(shader)
        return shader
    }

    private fun createFloatBuffer(array: FloatArray): FloatBuffer {
        val buffer = ByteBuffer.allocateDirect(array.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        buffer.put(array).position(0)
        return buffer
    }
}
