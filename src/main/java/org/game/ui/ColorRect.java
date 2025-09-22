package org.game.ui;
import org.engine.rendering.UIRenderer;
import org.game.meshes.Mesh2D;

public class ColorRect extends UIElement {
    private final float r, g, b, a;

    public ColorRect(int x, int y, int width, int height, float r, float g, float b, float a) {
        super(x, y, width, height);
        this.r = r; this.g = g; this.b = b; this.a = a;
        this.mesh = Mesh2D.createQuad(x, y, width, height);
    }


    public float getRed() { return r; }
    public float getGreen() { return g; }
    public float getBlue() { return b; }
    public float getAlpha() { return a; }

    @Override
    public void draw(UIRenderer uiRenderer, int windowWidth, int windowHeight) {
        if (visible) {
            uiRenderer.render(this, windowWidth, windowHeight, r, g, b, a, null, false);
        }
    }
}
