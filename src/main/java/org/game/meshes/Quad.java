package org.game.meshes;


public class Quad {

    public enum FaceType {
        TOP_FACE,
        BOTTOM_FACE,
        LEFT_FACE,
        RIGHT_FACE,
        FRONT_FACE,
        BACK_FACE
    }

    public final float[] positions;
    public final float[] texCoords;
    public final int[] indices = { 0, 1, 2, 2, 3, 0 }; // Two triangles for a quad

    private Quad(float[] positions, float[] texCoords) {
        this.positions = positions;
        this.texCoords = texCoords;
    }

    // top face ok
    public static final Quad TOP_FACE = new Quad(
            new float[] { 0, 1, 1, 1, 1, 1, 1, 1, 0, 0, 1, 0 },
            new float[] { 0, 1, 1, 1, 1, 0, 0, 0 });

    // bottom face (flipped)
    public static final Quad BOTTOM_FACE = new Quad(
            new float[] { 0, 0, 0, 1, 0, 0, 1, 0, 1, 0, 0, 1 },
            new float[] { 0, 0, 1, 0, 1, 1, 0, 1 } // notice flipped v
    );

    // front face (north)
    public static final Quad FRONT_FACE = new Quad(
            new float[] { 1, 0, 1, 1, 1, 1, 0, 1, 1, 0, 0, 1 },
            new float[] { 0, 1, 0, 0, 1, 0, 1, 1 } // rotated 90°
    );

    // back face (south)
    public static final Quad BACK_FACE = new Quad(

            new float[] { 0, 0, 0, 0, 1, 0, 1, 1, 0, 1, 0, 0 },
            new float[] { 1, 1, 1, 0, 0, 0, 0, 1 });

    // left face (west)
    public static final Quad LEFT_FACE = new Quad(
            new float[] { 0, 0, 1, 0, 1, 1, 0, 1, 0, 0, 0, 0 },
            new float[] { 1, 1, 1, 0, 0, 0, 0, 1 } // rotated
    );

    // right face (east)
    public static final Quad RIGHT_FACE = new Quad(
            new float[] {
                    1.0f, 0.0f, 0.0f,
                    1.0f, 1.0f, 0.0f,
                    1.0f, 1.0f, 1.0f,
                    1.0f, 0.0f, 1.0f
            },
            new float[] { 1, 1, 1, 0, 0, 0, 0, 1 } // rotated
    );

    public static Quad getQuad(FaceType faceType) {
        switch (faceType) {
            case TOP_FACE:
                return TOP_FACE;
            case BOTTOM_FACE:
                return BOTTOM_FACE;
            case LEFT_FACE:
                return LEFT_FACE;
            case RIGHT_FACE:
                return RIGHT_FACE;
            case FRONT_FACE:
                return FRONT_FACE;
            case BACK_FACE:
                return BACK_FACE;
            default:
                throw new IllegalArgumentException("Unknown face type");
        }
    }
}