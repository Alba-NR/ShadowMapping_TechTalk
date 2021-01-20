package graphics.renderEngine;

import graphics.scene.Entity;
import graphics.scene.Scene;
import graphics.shaders.ShaderProgram;

import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL13.*;

/**
 * Renderer for rendering entities in the scene using the Phong (or Blinn-phong) illumination model.
 * Also uses shadow mapping for the directional light in the scene.
 */
public class EntityPhongWShadowMapsRenderer extends Renderer {

    private int shadowMapHandle;

    public EntityPhongWShadowMapsRenderer(ShaderProgram phongShaderToUse, int shadowMapHandle) {
        super(phongShaderToUse);
        this.shadowMapHandle = shadowMapHandle;
    }

    /**
     * Must be called after setting the light space projection matrix in RenderContext!
     */
    @Override
    public void prepare(Scene scene) {
        shaderProgram.use();

        shaderProgram.uploadVec3f("I_a", scene.getI_a()); // set ambient illumination intensity
        scene.getDirLight().uploadSpecsToShader(shaderProgram, "dirLight");

        // set light space model matrix
        shaderProgram.uploadMatrix4f("lightSpace_m", RenderContext.getDirLightSpaceMatrix());
    }


    @Override
    public void render(Scene scene) {
        shaderProgram.use();
        shaderProgram.uploadVec3f("wc_cameraPos", RenderContext.getCameraPos());

        // render components
        for(Entity component : scene.getComponents()){
            int offset = component.numOfTexUsedByMaterial();
            shaderProgram.uploadInt("shadowMap", offset);    // shadow map at tex unit 0 todo
            glActiveTexture(GL_TEXTURE0 + offset); // activate appropriate texture unit before binding shadow map todo
            glBindTexture(GL_TEXTURE_2D, shadowMapHandle);  // bind shadow map texture to appropriate texture unit

            component.render(shaderProgram);
        }
    }
}
