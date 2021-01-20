package graphics.renderEngine;

import graphics.shaders.ShaderProgram;
import graphics.shapes.ScreenQuad;

import static org.lwjgl.opengl.GL30.glBindVertexArray;

/**
 * Renderer for rendering a quad/square {@link ScreenQuad} of the size of the screen,
 * which has as 'texture' a greyscale version of the depth map generated by ToDepthMapRenderer...
 */
public class DepthDebugScreenQuadRenderer {
    private ShaderProgram shaderProgram;    // shader to use for rendering
    private ScreenQuad quad;

    public DepthDebugScreenQuadRenderer(ShaderProgram shaderToUse) {
        shaderProgram = shaderToUse;
    }

    /**
     * Prepare {@link ShaderProgram} shaderProgram by binding
     * {@link ScreenQuad} quad mesh's attributes to it & uploading tex handle to uniform.
     */
    public void prepare(ScreenQuad screenQuad) {
        this.quad = screenQuad;
        shaderProgram.use();

        // bind mesh data to shader
        glBindVertexArray(quad.getMesh().getVAOHandle());
        shaderProgram.bindDataToShader(0, quad.getMesh().getVertexVBOHandle(), 2);
        shaderProgram.bindDataToShader(1, quad.getMesh().getTexHandle(), 2);

        shaderProgram.uploadInt("depthMap", 0); // tex at texture unit 0
    }

    /**
     * Render the quad using the {@link ShaderProgram} associated w/the renderer.
     */
    public void render() {
        shaderProgram.use();

        quad.bindTexture();
        quad.getMesh().render();
    }
}
