package org.game.ui;

import org.engine.rendering.UIRenderer;
import org.engine.rendering.UIText;

public class Label extends UIElement {
    private String text;
    private int fontSize;
    private UIText uiText;

    // You might want to add a font reference here

    public Label(int x, int y, int width, int height, String text, int fontSize) {
        super(x, y, width, height);
        this.text = text;
        this.fontSize = fontSize;
        this.uiText = new UIText();
        this.mesh = null;
        uiText.loadFont("fonts/Minecraft.ttf", fontSize);

        recreateMesh();
        // Initialize font and other resources as needed
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
        recreateMesh();
        // Update mesh or other resources if necessary
    }

    public int getFontSize() {
        return fontSize;
    }

    public void setFontSize(int fontSize) {
        this.fontSize = fontSize;
        recreateMesh();
        // Update mesh or other resources if necessary
    }

    @Override
    public void recreateMesh() {
        int centerdPosX = x + width / 2 - (text.length() * fontSize) / 4;
        int centerdPosY = y + height / 2 + fontSize / 4;
        this.mesh = uiText.buildTextMesh(text, centerdPosX, centerdPosY, 1);
    }

    @Override
    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
        recreateMesh();
        // Update mesh or other resources if necessary
    }

    @Override
    public void draw(UIRenderer uiRenderer, int windowWidth, int windowHeight) {
        if (!visible) {
            return;
        }

        uiRenderer.render(this, windowWidth, windowHeight, 1f, 1f, 1f, 1f, uiText.getTexture(), false);

    }

}
