package com.ardoom.rendering

import android.content.Context
import android.opengl.GLES30
import com.ardoom.game.EnemyState
import com.ardoom.game.EnemyType
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * Shader for rendering billboarded DOOM enemy sprites in AR space.
 * Draws textured quads that always face the camera.
 */
class SpriteShader(private val context: Context) {

    private var program: Int = 0
    private var mvpMatrixHandle: Int = 0
    private var textureHandle: Int = 0
    private var alphaHandle: Int = 0

    private val quadVertices: FloatBuffer
    private val quadTexCoords: FloatBuffer

    companion object {
        private const val VERTEX_SHADER = """
            uniform mat4 u_MVPMatrix;
            attribute vec4 a_Position;
            attribute vec2 a_TexCoord;
            varying vec2 v_TexCoord;

            void main() {
                v_TexCoord = a_TexCoord;
                gl_Position = u_MVPMatrix * a_Position;
            }
        """

        private const val FRAGMENT_SHADER = """
            precision mediump float;
            uniform sampler2D u_Texture;
            uniform float u_Alpha;
            varying vec2 v_TexCoord;

            void main() {
                vec4 color = texture2D(u_Texture, v_TexCoord);
                gl_FragColor = vec4(color.rgb, color.a * u_Alpha);
            }
        """

        // Unit quad vertices (1x1 centered at origin)
        private val QUAD_VERTICES = floatArrayOf(
            -0.5f, -0.5f, 0f,
             0.5f, -0.5f, 0f,
            -0.5f,  0.5f, 0f,
             0.5f,  0.5f, 0f
        )

        private val QUAD_TEX_COORDS = floatArrayOf(
            0f, 1f,
            1f, 1f,
            0f, 0f,
            1f, 0f
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
        textureHandle = GLES30.glGetUniformLocation(program, "u_Texture")
        alphaHandle = GLES30.glGetUniformLocation(program, "u_Alpha")

        quadVertices = createFloatBuffer(QUAD_VERTICES)
        quadTexCoords = createFloatBuffer(QUAD_TEX_COORDS)
    }

    fun draw(
        modelMatrix: FloatArray,
        viewMatrix: FloatArray,
        projectionMatrix: FloatArray,
        texture: Int,
        alpha: Float
    ) {
        // MVP = Projection * View * Model
        val mvpMatrix = FloatArray(16)
        val mvMatrix = FloatArray(16)
        android.opengl.Matrix.multiplyMM(mvMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        android.opengl.Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvMatrix, 0)

        GLES30.glUseProgram(program)

        val positionHandle = GLES30.glGetAttribLocation(program, "a_Position")
        val texCoordHandle = GLES30.glGetAttribLocation(program, "a_TexCoord")

        GLES30.glEnableVertexAttribArray(positionHandle)
        GLES30.glVertexAttribPointer(positionHandle, 3, GLES30.GL_FLOAT, false, 0, quadVertices)

        GLES30.glEnableVertexAttribArray(texCoordHandle)
        GLES30.glVertexAttribPointer(texCoordHandle, 2, GLES30.GL_FLOAT, false, 0, quadTexCoords)

        GLES30.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)
        GLES30.glUniform1f(alphaHandle, alpha)

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texture)
        GLES30.glUniform1i(textureHandle, 0)

        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)

        GLES30.glDisableVertexAttribArray(positionHandle)
        GLES30.glDisableVertexAttribArray(texCoordHandle)
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
