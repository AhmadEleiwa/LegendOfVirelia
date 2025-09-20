package org.game.ui;

import static org.lwjgl.opengl.GL11.GL_SCISSOR_TEST;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glScissor;

import java.util.ArrayList;
import java.util.List;

import org.engine.rendering.UIRenderer;

public class Container extends UIElement {
    protected List<UIElement> children;

    public Container(int x, int y, int width, int height) {
        super(x, y, width, height);
        this.children = new ArrayList<>();
    }

    public void addChild(UIElement child) {
        child.setPosition(child.x + this.x, child.y + this.y);
        this.children.add(child);

    }

    public void removeChild(UIElement child) {
        this.children.remove(child);
    }

    @Override
    public void setPosition(int x, int y) {
        int deltaX = x - this.x;
        int deltaY = y - this.y;
        this.x = x;
        this.y = y;
        recreateMesh();
        for (UIElement child : children) {
            child.setPosition(child.x + deltaX, child.y + deltaY);

        }

    }

    @Override
    public void setSize(int width, int height) {
        // TODO Auto-generated method stub
        super.setSize(width, height);

    }

    @Override
    public void draw(UIRenderer uiRenderer, int width, int height) {
        if (!visible) {
            return;
        }
        glEnable(GL_SCISSOR_TEST);

        uiRenderer.render(this, width, height, true, .7f, 1f, 1f, 1f, false, false);
        glScissor(x, height - (y + this.height), this.width, this.height);
        for (UIElement child : children) {
            if (child.isVisible()) {
                child.draw(uiRenderer, width, height);

            }
        }
        glDisable(GL_SCISSOR_TEST);
    }
}
