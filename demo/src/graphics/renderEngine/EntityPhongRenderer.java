package graphics.renderEngine;

import graphics.scene.Entity;
import graphics.scene.Scene;
import graphics.shaders.ShaderProgram;

/**
 * Renderer for rendering entities in the scene using the Phong (or Blinn-phong) illumination model.
 */
public class EntityPhongRenderer extends Renderer{

    public EntityPhongRenderer(ShaderProgram phongShaderToUse) {
        super(phongShaderToUse);
    }

    @Override
    public void prepare(Scene scene) {
        shaderProgram.use();

        shaderProgram.uploadVec3f("I_a", scene.getI_a()); // set ambient illumination intensity
        scene.getDirLight().uploadSpecsToShader(shaderProgram, "dirLight");
    }


    @Override
    public void render(Scene scene) {
        shaderProgram.use();
        shaderProgram.uploadVec3f("wc_cameraPos", RenderContext.getCameraPos());

        // render components
        for(Entity component : scene.getComponents()) component.render(shaderProgram);
    }
}
