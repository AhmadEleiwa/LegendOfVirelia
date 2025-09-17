package org.game.core;

import org.joml.Matrix4f;
import org.joml.Vector3f;



public abstract class GameObject {
   
    public Vector3f position = new Vector3f();
    public Vector3f rotation = new Vector3f();
    public Vector3f scale = new Vector3f(1, 1, 1);
    protected String type;
    protected String name;

    public String getName() {
        return name;
    }

    public Matrix4f getModelMatrix() {
        return new Matrix4f()
                .translate(position)
                .rotateXYZ(rotation.x, rotation.y, rotation.z)
                .scale(scale);
    }

    public String getType() {
        return type;
    }
    public abstract void update(float delta);
    public abstract void render();
    public abstract void delete();
  

}