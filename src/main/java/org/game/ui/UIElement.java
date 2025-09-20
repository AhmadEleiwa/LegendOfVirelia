package org.game.ui;

import org.engine.rendering.UIRenderer;
import org.game.meshes.Mesh2D;
import org.joml.Matrix4f;

public abstract class UIElement {
    protected int x, y, width, height;
    protected boolean visible;
    protected Mesh2D mesh;

    public UIElement(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.visible = true;
        this.mesh = Mesh2D.createQuad(x, y, width, height);
    }

    public Matrix4f getModelMatrix() {
        return new Matrix4f().identity(); // simple identity, no rotation/scale
    }

    public void recreateMesh() {
        this.mesh = Mesh2D.createQuad(x, y, width, height);
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
        recreateMesh();
    }

    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
        recreateMesh();
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isVisible() {
        return visible;
    }

    public Mesh2D getMesh() {
        return mesh;
    }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public abstract void draw(UIRenderer uiRenderer, int windowWidth, int windowHeight);

}
