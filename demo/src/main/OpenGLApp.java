package main;

import graphics.camera.Camera;
import graphics.camera.CameraMovement;
import graphics.core.WindowManager;
import graphics.core.io.ScreenshotMaker;
import graphics.lights.DirLight;
import graphics.materials.Material;
import graphics.renderEngine.*;
import graphics.renderEngine.RenderContext;
import graphics.renderEngine.renderOptionsManager.RenderOptionsManager;
import graphics.scene.DrawableEntity;
import graphics.scene.Entity;
import graphics.scene.Scene;
import graphics.shapes.*;
import graphics.shaders.Shader;
import graphics.shaders.ShaderProgram;
import graphics.textures.Texture;
import graphics.textures.TextureType;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * The main app program.
 */
class OpenGLApp {

    private ShaderProgram phongShaderProgram;           // phong shader program using diff & spec textures or colours
    private ShaderProgram phongWShadowsShaderProgram;   // phong shader prog w/shadows
    private ShaderProgram quadShaderProgram;            // shader prog to use for quad
    private ShaderProgram toDepthTexShaderProgram;      // shader prog to use for rendering to depth texture
    private ShaderProgram depthGreyShaderProgram;       // shader prog to render depth map in greyscale
    private Scene scene;                                // scene to render
    private ScreenQuad screenQuad;                      // quad filling entire screen (scene displayed as it's colour texture...)
    private ScreenQuad screenQuadForSM;                 // quad for greyscale depth map....

    final private int SCR_WIDTH = WindowManager.getScrWidth();  // screen size settings
    final private int SCR_HEIGHT = WindowManager.getScrHeight();

    private Camera camera = new Camera();   // camera & mouse
    private double lastX = SCR_WIDTH / 2.0f, lastY = SCR_HEIGHT / 2.0f;
    private boolean firstMouse = true;


    /**
     * Initialise GLFW & window for rendering
     */
    void init() {
        // --- init & config GLFW ---
        if (!glfwInit()) throw new IllegalStateException("Unable to initialize GLFW");

        // --- GLFW window creation (& init GLFW context)---
        WindowManager.createWindow();

        glEnable(GL_MULTISAMPLE);   // enable MSAA
        glEnable(GL_DEPTH_TEST);    // enable depth testing
        glEnable(GL_CULL_FACE);     // enable culling
        glCullFace(GL_BACK);        // cull back faces
        glFrontFace(GL_CCW);        // initially set front faces as those w/counter clockwise winding

        // --- callback functions registered after window is created & before render loop is init ---
        setCallbacks();

        // make window visible
        WindowManager.makeWindowVisible();

        // --- set up shaders ---
        setUpShaders();
    }

    /**
     * Create any shaders here.
     */
    private void setUpShaders() {
        // create (blinn-)phong shaders
        Shader phong_vs = new Shader(GL_VERTEX_SHADER, "./resources/shaders/phong_vs.glsl");
        Shader phong_fs = new Shader(GL_FRAGMENT_SHADER, "./resources/shaders/blinnPhong_fs.glsl");
        phongShaderProgram = new ShaderProgram(phong_vs, phong_fs);

        // create (blinn-)phong shaders w/shadow mapping
        Shader phongWS_vs = new Shader(GL_VERTEX_SHADER, "./resources/shaders/phong_shadowMaps_vs.glsl");
        Shader phongWS_fs = new Shader(GL_FRAGMENT_SHADER, "./resources/shaders/blinnPhong_wShadowMaps_fs.glsl");
        phongWShadowsShaderProgram = new ShaderProgram(phongWS_vs, phongWS_fs);

        // create quad shaders
        Shader quad_vs = new Shader(GL_VERTEX_SHADER, "./resources/shaders/quad_vs.glsl");
        Shader quad_fs = new Shader(GL_FRAGMENT_SHADER, "./resources/shaders/quad_fs.glsl");
        quadShaderProgram = new ShaderProgram(quad_vs, quad_fs);

        // create to depth texture shaders
        Shader toDepthMap_vs = new Shader(GL_VERTEX_SHADER, "./resources/shaders/toDepthMap_vs.glsl");
        Shader toDepthMap_fs = new Shader(GL_FRAGMENT_SHADER, "./resources/shaders/toDepthMap_fs.glsl");
        toDepthTexShaderProgram = new ShaderProgram(toDepthMap_vs, toDepthMap_fs);

        // create depth greyscale shaders
        Shader depthMapDebug_fs = new Shader(GL_FRAGMENT_SHADER, "./resources/shaders/depthDebug_fs.glsl");
        depthGreyShaderProgram = new ShaderProgram(quad_vs, depthMapDebug_fs);
    }

    /**
     * Set-up the scene to render here.
     */
    private void setUpScene() {

        // --- SET UP LIGHTS ---

        // directional light
        DirLight dirLight = new DirLight(new Vector3f(1.0f, 1.0f, 1.0f), 2.0f, new Vector3f(-0.2f, -1.0f, -0.3f));
        Vector3f ambientIntensity = new Vector3f(0.7f,0.7f,1.0f);

        // --- SET UP ENTITIES ---
        // WOODEN CUBES
        List<Texture> woodenCube_texList = Arrays.asList(
                new Texture("./resources/textures/container2.png", false, TextureType.DIFFUSE),
                new Texture("./resources/textures/container2_specular.png", false, TextureType.SPECULAR)
        );
        Material cubeMaterial1 = new Material(woodenCube_texList);
        cubeMaterial1.setK_spec(0.5f);
        Shape cube1 = new Cube(cubeMaterial1);

        Material cubeMaterial2 = new Material(Arrays.asList(
                new Texture("./resources/textures/circuitry-albedo.png", false, TextureType.DIFFUSE),
                new Texture("./resources/textures/circuitry-metallic.png", false, TextureType.SPECULAR)
        ));
        Shape cube2 = new Cube(cubeMaterial2);

        // calc local transform matrix for cube 1
        Matrix4f cube1_local_transform = new Matrix4f();
        cube1_local_transform.translate(-2.0f, 0.0f, -2.0f)
                .rotate((float) Math.toRadians(45), 0.0f, 1.0f, 0.0f);

        // create 1st cube entity
        Entity cube1_entity = new DrawableEntity(null, cube1_local_transform, new Vector3f(2.0f), cube1);

        // calc local transform for 2nd cube entity
        Matrix4f cube2_local_transform = new Matrix4f();
        cube2_local_transform.translate(0.0f, 0.75f, 0.0f)
                .rotate((float) Math.toRadians(30), 0.0f, 1.0f, 0.0f);

        // create 2nd cube entity, child of 1st cube entity
        Entity cube2_entity = new DrawableEntity(cube1_entity, cube2_local_transform, new Vector3f(0.5f), cube1);
        cube1_entity.addChild(cube2_entity);

        // calc local transform for 3rd cube entity
        Matrix4f cube3_local_transform = new Matrix4f();
        cube3_local_transform.translate(2.0f, -0.2f, 0.0f)
                .rotate((float) Math.toRadians(60), 0.0f, 1.0f, 0.0f);

        // create 3rd cube entity, child of 1st cube entity
        Entity cube3_entity = new DrawableEntity(cube1_entity, cube3_local_transform, new Vector3f(0.6f), cube2);
        cube1_entity.addChild(cube3_entity);

        // FLOOR PLANE
        Material floorMaterial = new Material();
        floorMaterial.setK_spec(0);
        floorMaterial.setDiffColour(new Vector3f(30/255f, 5/255f, 5/255f));
        Shape square = new Square(floorMaterial);

        // calc local transform matrix for square
        Matrix4f floor_local_transform = new Matrix4f();
        floor_local_transform.translate(0f, -1.0f, 0f)
                .rotate((float) Math.toRadians(90), 1.0f, 0.0f, 0.0f);

        // create floor entity
        Entity floor = new DrawableEntity(null, floor_local_transform, new Vector3f(25), square);

        // DRAGON
        Shape dragonShape = new ShapeFromOBJ("./resources/models/dragon.obj",
                new Material(new Vector3f(255/255f, 30/255f, 30/255f), new Vector3f(212/255f, 175/255f, 55/255f)),
                true); // red glass dragon

        // calc local transform matrix for dragon
        Matrix4f dragon_local_transform = new Matrix4f();
        dragon_local_transform.translate(2f, -1.0f, 2f)
                .rotateAffine((float) -Math.toRadians(135), 0f, 1f, 0f);

        // create dragon entity
        Entity dragon = new DrawableEntity(null, dragon_local_transform, new Vector3f(0.25f), dragonShape);


        // --- COMPONENTS LIST: add entities to components list
        List<Entity> components = new ArrayList<>(Arrays.asList(cube1_entity, dragon, floor));


        // --- CREATE SCENE ---
        scene = new Scene(components, dirLight, ambientIntensity);

    }

    /**
     * Rendering loop
     */
    void renderLoop(){

        // --- create renderers ---
        EntityPhongRenderer entityNormalRenderer = new EntityPhongRenderer(phongShaderProgram);
        EntityPhongWShadowMapsRenderer entityWShadowsRenderer;    // created after preparing toDepthTextureRenderer (bc uses depth tex handle)
        ScreenQuadRenderer screenQuadRenderer = new ScreenQuadRenderer(quadShaderProgram);
        ToColourTextureRenderer toColourTextureRenderer = new ToColourTextureRenderer();
        ToDepthTextureRenderer toDepthTextureRenderer = new ToDepthTextureRenderer(toDepthTexShaderProgram, 1024, 1024);
        DepthDebugScreenQuadRenderer depthGreyScreenQuadRenderer = new DepthDebugScreenQuadRenderer(depthGreyShaderProgram);

        // --------- SET UP SCENE ---------
        setUpScene();

        // --------- RENDER LOOP ---------

        //--- directional light's light space matrix, for shadow mapping ---
        Matrix4f lightProjection = new Matrix4f().ortho(-10.0f, 10.0f, -10.0f, 10.0f, 1.0f, 20f);
        Matrix4f lightView = new Matrix4f().lookAt(
                scene.getDirLight().getLightPosForSMRender(),
                new Vector3f(0),
                new Vector3f(0.0f, 1.0f, 0.0f)
        );

        RenderContext.setDirLightViewMatrix(lightView);
        RenderContext.setDirLightProjMatrix(lightProjection);


        // --- prepare renderers ---
        toDepthTextureRenderer.prepare(scene);

        screenQuadForSM = new ScreenQuad(toDepthTextureRenderer.getDepthTex());
        depthGreyScreenQuadRenderer.prepare(screenQuadForSM);

        entityWShadowsRenderer = new EntityPhongWShadowMapsRenderer(phongWShadowsShaderProgram, toDepthTextureRenderer.getDepthTex());
        entityWShadowsRenderer.prepare(scene);

        entityNormalRenderer.prepare(scene);

        toColourTextureRenderer.prepare();
        screenQuad = new ScreenQuad(toColourTextureRenderer.getColourTex());
        screenQuadRenderer.prepare(screenQuad);


        // --- (per frame info...) ---
        float deltaTime;	        // Time between current frame and last frame
        float lastFrameT = 0.0f;    // Time of last frame


        // --- repeat while GLFW isn't instructed to close ---
        while(!WindowManager.windowShouldClose()){
            // --- per-frame time logic ---
            float currentFrameT = (float) glfwGetTime();
            deltaTime = currentFrameT - lastFrameT;
            lastFrameT = currentFrameT;

            // --- process keyboard arrows input --
            processAWSDInput(deltaTime);

            // --- clear screen ---
            WindowManager.clearScreen();

            switch (RenderContext.getRenderOption()){
                case NORMAL:
                    renderNormal(toColourTextureRenderer, entityNormalRenderer, screenQuadRenderer);
                    break;
                case FROM_LIGHT_POV:
                    renderFromLight(toColourTextureRenderer, entityNormalRenderer, screenQuadRenderer);
                    break;
                case DEPTH_MAP:
                    renderDepthMap(toDepthTextureRenderer, depthGreyScreenQuadRenderer);
                    break;
                case WITH_SHADOWS:
                    renderWithShadows(toDepthTextureRenderer, toColourTextureRenderer, entityWShadowsRenderer,screenQuadRenderer);
                    break;
            }

            // --- check events & swap buffers ---
            WindowManager.updateWindow();
            glfwPollEvents(); // checks if any events are triggered, updates window state, & calls corresponding funcs
        }

        glBindBuffer(GL_ARRAY_BUFFER, 0);    // unbind any VBO
        glBindVertexArray(0);                       // unbind any VAO
    }

    // --------------------------------------------------------------------------------------------------------------------------

    private void renderNormal(ToColourTextureRenderer toColourTextureRenderer, EntityPhongRenderer entityRenderer, ScreenQuadRenderer screenQuadRenderer){
        // --- bind fbo to which to render ---
        toColourTextureRenderer.bindFBOtoUse();
        WindowManager.clearColourDepthBuffers();

        // --- render commands ---

        Matrix4f view = camera.calcLookAt(); // calc view matrix
        Matrix4f projection = new Matrix4f(); // create projection matrix
        projection.setPerspective((float) Math.toRadians(camera.getFOV()), (float) SCR_WIDTH / SCR_HEIGHT, 0.1f, 100.0f);

        RenderContext.setContext(view, projection, camera.getCameraPos(), camera.getCameraFront());

        entityRenderer.render(scene);

        // bind default framebuffer & render quad
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glDisable(GL_DEPTH_TEST);       // so that screen-space quad isn't discarded bc of depth test
        // clear relevant buffers
        WindowManager.clearColour(1.0f, 1.0f, 1.0f); // optional, to correctly see quad in wireframe mode
        WindowManager.clearColourBuffer();

        screenQuadRenderer.render();    // render screen quad
        glEnable(GL_DEPTH_TEST);
    }

    private void renderFromLight(ToColourTextureRenderer toColourTextureRenderer, EntityPhongRenderer entityRenderer, ScreenQuadRenderer screenQuadRenderer){
        // --- bind fbo to which to render ---
        toColourTextureRenderer.bindFBOtoUse();
        WindowManager.clearColourDepthBuffers();

        // --- render commands ---

        Matrix4f view = RenderContext.getDirLightViewMatrix();          // get view matrix
        Matrix4f projection = RenderContext.getDirLightProjMatrix();    // get proj matrix

        RenderContext.setContext(view, projection, scene.getDirLight().getLightPosForSMRender(), scene.getDirLight().getDirection());

        entityRenderer.render(scene);

        // bind default framebuffer & render quad
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glDisable(GL_DEPTH_TEST);       // so that screen-space quad isn't discarded bc of depth test
        // clear relevant buffers
        WindowManager.clearColour(1.0f, 1.0f, 1.0f); // optional, to correctly see quad in wireframe mode
        WindowManager.clearColourBuffer();

        screenQuadRenderer.render();    // render screen quad
        glEnable(GL_DEPTH_TEST);
    }

    private void renderDepthMap(ToDepthTextureRenderer toDepthTextureRenderer, DepthDebugScreenQuadRenderer depthGreyScreenQuadRenderer){

        //--- render to depth map ---
        toDepthTextureRenderer.render(scene);

        // --- bind fbo to which to render ---
        depthGreyScreenQuadRenderer.render();
        glEnable(GL_DEPTH_TEST);
    }

    private void renderWithShadows(ToDepthTextureRenderer toDepthTextureRenderer, ToColourTextureRenderer toColourTextureRenderer, EntityPhongWShadowMapsRenderer entityRenderer, ScreenQuadRenderer screenQuadRenderer){
        //--- render to depth map ---
        toDepthTextureRenderer.render(scene);

        // --- bind fbo to which to render ---
        toColourTextureRenderer.bindFBOtoUse();
        WindowManager.clearColourDepthBuffers();

        // --- render commands ---

        Matrix4f view = camera.calcLookAt(); // calc view matrix
        Matrix4f projection = new Matrix4f(); // create projection matrix
        projection.setPerspective((float) Math.toRadians(camera.getFOV()), (float) SCR_WIDTH / SCR_HEIGHT, 0.1f, 100.0f);

        RenderContext.setContext(view, projection, camera.getCameraPos(), camera.getCameraFront());

        entityRenderer.render(scene);

        // bind default framebuffer & render quad
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glDisable(GL_DEPTH_TEST);       // so that screen-space quad isn't discarded bc of depth test
        // clear relevant buffers
        WindowManager.clearColour(1.0f, 1.0f, 1.0f); // optional, to correctly see quad in wireframe mode
        WindowManager.clearColourBuffer();

        screenQuadRenderer.render();    // render screen quad
        glEnable(GL_DEPTH_TEST);
    }

    // --------------------------------------------------------------------------------------------------------------------------

    /**
     * Set the window callbacks.
     */
    private void setCallbacks(){
        long win = WindowManager.getWindowHandle();

        // whenever window is resized, call given funct -- adjusts viewport
        glfwSetFramebufferSizeCallback(win, (long window, int width, int height) -> glViewport(0, 0, width, height));

        // whenever key is pressed, repeated or released.
        glfwSetKeyCallback(win, (window, key, scancode, action, mods) -> {
            // close window when esc key is released
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE)
                glfwSetWindowShouldClose(window, true);
            // view in wireframe mode whilst E is pressed
            if (key == GLFW_KEY_E) {
                if (action == GLFW_PRESS) glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
                else if (action == GLFW_RELEASE) glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
            }
            // -> AWSD used to move camera (in processArrowsInput() method)
            // number keys used to choose what to render (4 options)
            for(int i = 1; i <= RenderOptionsManager.getNumOfOptions(); i++){
                if (key == GLFW_KEY_0 + i) RenderContext.setRenderOption(RenderOptionsManager.getEffectByIntID(i));
            }
            // take 'screenshot' when press F
            if (key == GLFW_KEY_F && action == GLFW_RELEASE)
                ScreenshotMaker.takeScreenshot(); // TODO
        });

        // mouse-related callbacks
        glfwSetInputMode(win, GLFW_CURSOR, GLFW_CURSOR_DISABLED); // use mouse
        glfwSetCursorPosCallback(win, (long window, double xpos, double ypos) -> {
            if (firstMouse) {
                lastX = xpos;
                lastY = ypos;
                firstMouse = false;
            }
            double xoffset = (xpos - lastX);
            double yoffset = (lastY - ypos); // reversed since y-coord range from bottom to top
            lastX = xpos;
            lastY = ypos;

            camera.processMouseMovement(xoffset, yoffset, true);
        });
        glfwSetScrollCallback(win, (long window, double xoffset, double yoffset) -> { // 'zoom' illusion when scroll w/mouse
            camera.processMouseScroll(yoffset);
        });
    }

    /**
     * Called in render loop to continually process input from keyboard AWSD keys in each frame.
     */
    private void processAWSDInput(float deltaTime){
        // camera movement using AWSD
        if (WindowManager.getKeyState(GLFW_KEY_W) == GLFW_PRESS) camera.processKeyboardInput(CameraMovement.FORWARD, deltaTime);
        if (WindowManager.getKeyState(GLFW_KEY_S) == GLFW_PRESS) camera.processKeyboardInput(CameraMovement.BACKWARD, deltaTime);
        if (WindowManager.getKeyState(GLFW_KEY_A) == GLFW_PRESS) camera.processKeyboardInput(CameraMovement.LEFT, deltaTime);
        if (WindowManager.getKeyState(GLFW_KEY_D) == GLFW_PRESS) camera.processKeyboardInput(CameraMovement.RIGHT, deltaTime);
        if (WindowManager.getKeyState(GLFW_KEY_LEFT_CONTROL) == GLFW_PRESS) camera.processKeyboardInput(CameraMovement.DOWNWARD, deltaTime);
        if (WindowManager.getKeyState(GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS) camera.processKeyboardInput(CameraMovement.UPWARD, deltaTime);
    }

    /**
     * Terminate GLFW & window
     */
    void terminate(){

        WindowManager.closeWindow();

        // de-allocate all resources
        scene.deallocateMeshResources();
        screenQuad.getMesh().deallocateResources();
        phongShaderProgram.delete();
        quadShaderProgram.delete();
        toDepthTexShaderProgram.delete();

        // clean/delete all other GLFW's resources
        glfwTerminate();
    }
}
