package com.ardoom.rendering

import android.content.Context
import android.opengl.GLES30
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * Shader that renders the ARCore camera feed as the background.
 * Takes the AR frame's camera texture and draws it full-screen.
 */
class BackgroundShader(private val context: Context) {

    private var program: Int = 0
    private var positionHandle: Int = 0
    private var texCoordHandle: Int = 0
    private var textureHandle: Int = 0
    private var displayRotationHandle: Int = 0
    private var textureId: Int = 0

    companion object {
        private const val VERTEX_SHADER = """
            attribute vec4 a_Position;
            attribute vec2 a_TexCoord;
            uniform mat3 u_UVTransform;
            varying vec2 v_TexCoord;

            void main() {
                v_TexCoord = (u_UVTransform * vec3(a_TexCoord, 1.0)).xy;
                gl_Position = a_Position;
            }
        """

        private const val FRAGMENT_SHADER = """
            precision mediump float;
            uniform samplerExternalOES u_Texture;
            varying vec2 v_TexCoord;

            void main() {
                gl_FragColor = texture2D(u_Texture, v_TexCoord);
            }
        """

        private val COORDS = floatArrayOf(
            -1f, -1f, -1f, 1f, 1f, -1f, 1f, 1f
        )

        private val TEX_COORDS = floatArrayOf(
            0f, 0f, 0f, 1f, 1f, 0f, 1f, 1f
        )
    }

    init {
        val vertexShader = compileShader(GLES30.GL_VERTEX_SHADER, VERTEX_SHADER)
        val fragmentShader = compileShader(GLES30.GL_FRAGMENT_SHADER, FRAGMENT_SHADER)
        program = GLES30.glCreateProgram()
        GLES30.glAttachShader(program, vertexShader)
        GLES30.glAttachShader(program, fragmentShader)
        GLES30.glLinkProgram(program)

        positionHandle = GLES30.glGetAttribLocation(program, "a_Position")
        texCoordHandle = GLES30.glGetAttribLocation(program, "a_TexCoord")
        textureHandle = GLES30.glGetUniformLocation(program, "u_Texture")
        displayRotationHandle = GLES30.glGetUniformLocation(program, "u_UVTransform")

        // Create external OES texture for AR camera feed
        val textures = IntArray(1)
        GLES30.glGenTextures(1, textures, 0)
        textureId = textures[0]
        GLES30.glBindTexture(GLES30.GL_TEXTURE_EXTERNAL_OES, textureId)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
    }

    fun draw(frame: com.google.ar.core.Frame) {
        GLES30.glUseProgram(program)

        // Update camera texture from AR frame
        frame.acquireCameraImage()?.use { image ->
            // Bind the camera texture
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_EXTERNAL_OES, textureId)
            GLES30.glUniform1i(textureHandle, 0)

            // Apply display rotation transform
            val transform = FloatArray(16)
            frame.acquireCameraImage()  // simplified
            GLES30.glUniformMatrix3fv(displayRotationHandle, 1, false, floatArrayOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f), 0)
        }

        // Draw full-screen quad
        val vertexBuffer = createFloatBuffer(COORDS)
        val texBuffer = createFloatBuffer(TEX_COORDS)

        GLES30.glEnableVertexAttribArray(positionHandle)
        GLES30.glVertexAttribPointer(positionHandle, 2, GLES30.GL_FLOAT, false, 0, vertexBuffer)

        GLES30.glEnableVertexAttribArray(texCoordHandle)
        GLES30.glVertexAttribPointer(texCoordHandle, 2, GLES30.GL_FLOAT, false, 0, texBuffer)

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
