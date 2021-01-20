package graphics.scene;

import graphics.lights.*;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class Scene {

    private List<Entity> components;
    private DirLight dirLight;
    private Vector3f I_a;

    public Scene(List<Entity> components, DirLight dirLight, Vector3f ambient_intensity) {
        if(components != null) this.components = components;
        else this.components = new ArrayList<>();
        this.dirLight = dirLight;
        I_a = ambient_intensity;
    }

    /**
     * Deallocate the mesh resources of the SceneNodes that form this scene
     */
    public void deallocateMeshResources(){
        components.forEach(Entity::deallocateMeshResources);
    }

    public List<Entity> getComponents() {
        return components;
    }
    public DirLight getDirLight() {
        return dirLight;
    }
    public Vector3f getI_a() {
        return I_a;
    }
}
