package graphics.renderEngine;

import graphics.renderEngine.renderOptionsManager.RenderOptions;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Stores the rendering context needed by the renderers to correctly set-up and render the scene.
 * Includes:
 *      - view and projection matrices (calculated using the camera's data in the main program)
 *      - the camera's position and camera front vector
 * Also includes:
 *      - post-processing effect to use
 */
public class RenderContext {
    private static Matrix4f viewMatrix, projMatrix;
    private static Vector3f cameraPos, cameraFront;

    private static Matrix4f dirLightViewMatrix, dirLightProjMatrix, dirLightSpaceMatrix;

    private static RenderOptions renderOption = RenderOptions.NORMAL;

    public static void setContext(Matrix4f view_m, Matrix4f projection_m, Vector3f camera_pos, Vector3f camera_front){
        viewMatrix = view_m;
        projMatrix = projection_m;
        cameraPos = camera_pos;
        cameraFront = camera_front;
    }

    public static void setDirLightViewMatrix(Matrix4f dirLightViewMatrix) {
        RenderContext.dirLightViewMatrix = dirLightViewMatrix;
    }

    public static void setDirLightProjMatrix(Matrix4f dirLightProjMatrix) {
        RenderContext.dirLightProjMatrix = dirLightProjMatrix;
    }

    public static void setRenderOption(RenderOptions renderOption) {
        RenderContext.renderOption = renderOption;
    }

    public static Matrix4f getViewMatrix(){
        return viewMatrix;
    }

    public static Matrix4f getProjMatrix(){
        return projMatrix;
    }

    public static Vector3f getCameraPos() {
        return cameraPos;
    }

    public static Vector3f getCameraFront() {
        return cameraFront;
    }

    public static Matrix4f getDirLightSpaceMatrix() {
        if(dirLightSpaceMatrix == null){
            dirLightSpaceMatrix = new Matrix4f(dirLightProjMatrix);
            dirLightSpaceMatrix.mul(dirLightViewMatrix);
        }
        return dirLightSpaceMatrix;
    }

    public static Matrix4f getDirLightViewMatrix() {
        return dirLightViewMatrix;
    }

    public static Matrix4f getDirLightProjMatrix() {
        return dirLightProjMatrix;
    }

    public static RenderOptions getRenderOption() {
        return renderOption;
    }
}
