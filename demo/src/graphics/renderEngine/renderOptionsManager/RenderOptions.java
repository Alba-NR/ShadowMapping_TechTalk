package graphics.renderEngine.renderOptionsManager;

/**
 * Represents the possible options for rendering my demo.
 */
public enum RenderOptions {
    NORMAL,             // op1. render scene as usual
    FROM_LIGHT_POV,     // op2. render scene as seen from the dir light
    DEPTH_MAP,          // op3. render depth map (in greyscale)
    WITH_SHADOWS        // op4. render scene as usual, w/shadows
}