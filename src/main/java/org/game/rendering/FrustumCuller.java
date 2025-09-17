package org.game.rendering;

import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class FrustumCuller {
    private final FrustumIntersection frustum = new FrustumIntersection();

    public void update(Matrix4f projView) {
        frustum.set(projView);
    }

    public boolean isVisible(Vector3f center, float radius) {
        return frustum.testSphere(center.x, center.y, center.z, radius);
    }
}