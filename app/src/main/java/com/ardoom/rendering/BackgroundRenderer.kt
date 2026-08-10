package com.ardoom.rendering

import android.opengl.GLES30
import com.google.ar.core.Coordinates2d
import com.google.ar.core.Frame
import com.google.ar.core.Session
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * Renders the ARCore camera feed as the fullscreen background.
 * Uses the OES external texture that ARCore updates each frame.
 *
 * This follows the official ARCore HelloAR rendering pattern.
 */
class BackgroundRenderer {

    private var program: Int = 0
    private var positionAttrib: Int = 0
    private var texCoordAttrib: Int = 0
    private var textureUniform: Int = 0
    private var uvTransformUniform: Int = 0
    private var textureId: Int = -1

    private val quadCoords: FloatBuffer
    private val quadTexCoords: FloatBuffer

    private val uvTransform = FloatArray(9)

    companion object {
        private const val VERTEX_SHADER = """
            attribute vec4 a_Position;
            attribute vec2 a_TexCoord;
            uniform mat3 u_UvTransform;
            varying vec2 v_TexCoord;
            void main() {
                v_TexCoord = (u_UvTransform * vec3(a_TexCoord, 1.0)).xy;
                gl_Position = a_Position;
            }
        """

        private const val FRAGMENT_SHADER = """
            #extension GL_OES_EGL_image_external_essl3 : enable
            precision mediump float;
            uniform samplerExternalOES u_Texture;
            varying vec2 v_TexCoord;
            void main() {
                gl_FragColor = texture2D(u_Texture, v_TexCoord);
            }
        """

        // Fullscreen quad in clip space (two triangles as a strip)
        private val COORDS = floatArrayOf(
            -1f, -1f,
             1f, -1f,
            -1f,  1f,
             1f,  1f
        )

        private val TEX_COORDS = floatArrayOf(
            0f, 1f,
            1f, 1f,
            0f, 0f,
            1f, 0f
        )
    }

    init {
        // Create the external OES texture
        val textures = IntArray(1)
        GLES30.glGenTextures(1, textures, 0)
        textureId = textures[0]
        GLES30.glBindTexture(GLES30.GL_TEXTURE_EXTERNAL_OES, textureId)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)

        quadCoords = ByteBuffer.allocateDirect(COORDS.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer().put(COORDS)
        quadTexCoords = ByteBuffer.allocateDirect(TEX_COORDS.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer().put(TEX_COORDS)

        compileProgram()
    }

    fun getTextureId(): Int = textureId

    private fun compileProgram() {
        val vs = compile(GLES30.GL_VERTEX_SHADER, VERTEX_SHADER)
        val fs = compile(GLES30.GL_FRAGMENT_SHADER, FRAGMENT_SHADER)
        program = GLES30.glCreateProgram()
        GLES30.glAttachShader(program, vs)
        GLES30.glAttachShader(program, fs)
        GLES30.glLinkProgram(program)

        positionAttrib = GLES30.glGetAttribLocation(program, "a_Position")
        texCoordAttrib = GLES30.glGetAttribLocation(program, "a_TexCoord")
        textureUniform = GLES30.glGetUniformLocation(program, "u_Texture")
        uvTransformUniform = GLES30.glGetUniformLocation(program, "u_UvTransform")
    }

    fun draw(session: Session, frame: Frame) {
        // Ensure the camera texture is set on the session
        session.setCameraTextureName(textureId)

        // Get the UV transform for display rotation
        val transform = frame.transformCoordinates2d(
            Coordinates2d.OPENGL_NORMALIZED_DEVICE_COORDINATES,
            quadCoords,
            Coordinates2d.TEXTURE_NORMALIZED
        )
        // The transform is a 4x3 matrix but we need 3x3 for the shader
        val transformArray = transform.textureCoordinates
        // Build 3x3 from the 4x3 packed by ARCore (rows of [u, v, w])
        for (i in 0 until 3) {
            uvTransform[i * 3 + 0] = transformArray[i * 4 + 0]
            uvTransform[i * 3 + 1] = transformArray[i * 4 + 1]
            uvTransform[i * 3 + 2] = transformArray[i * 4 + 3]
        }

        GLES30.glDisable(GLES30.GL_DEPTH_TEST)
        GLES30.glDepthMask(false)

        GLES30.glUseProgram(program)

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_EXTERNAL_OES, textureId)
        GLES30.glUniform1i(textureUniform, 0)

        GLES30.glUniformMatrix3fv(uvTransformUniform, 1, false, uvTransform, 0)

        quadCoords.position(0)
        GLES30.glVertexAttribPointer(positionAttrib, 2, GLES30.GL_FLOAT, false, 0, quadCoords)
        GLES30.glEnableVertexAttribArray(positionAttrib)

        quadTexCoords.position(0)
        GLES30.glVertexAttribPointer(texCoordAttrib, 2, GLES30.GL_FLOAT, false, 0, quadTexCoords)
        GLES30.glEnableVertexAttribArray(texCoordAttrib)

        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)

        GLES30.glDisableVertexAttribArray(positionAttrib)
        GLES30.glDisableVertexAttribArray(texCoordAttrib)

        GLES30.glDepthMask(true)
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
    }

    private fun compile(type: Int, source: String): Int {
        val shader = GLES30.glCreateShader(type)
        GLES30.glShaderSource(shader, source)
        GLES30.glCompileShader(shader)
        val status = IntArray(1)
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, status, 0)
        if (status[0] != GLES30.GL_TRUE) {
            val log = GLES30.glGetShaderInfoLog(shader)
            GLES30.glDeleteShader(shader)
            throw RuntimeException("Shader compile failed: $log")
        }
        return shader
    }
}
