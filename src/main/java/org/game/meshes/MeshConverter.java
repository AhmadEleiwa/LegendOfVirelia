package org.game.meshes;

import org.joml.Vector3f;
import org.engine.utils.Debug;
import org.game.utils.AtlasBuilder;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;

public class MeshConverter {

    public static ArrayList<Mesh> processModel(Model modelJson) {
        if (modelJson == null) return null;

        Debug.log("Model type: " + modelJson.type);

        switch (modelJson.type) {
            case ModelTypes.CUBE_ALL:
                return processCubeAllModel(modelJson);
            case ModelTypes.ORIENTABLE_WITH_BOTTOM:
                return processOrientableWithBottom(modelJson);
            case ModelTypes.GENERIC:
            default:
                return processGenericModel(modelJson);
        }
    }

    // Simple cube
    private static ArrayList<Mesh> processCubeAllModel(Model modelJson) {
        ArrayList<Mesh> meshes = new ArrayList<>();
        meshes.add(Mesh.createCube(1)); // 1 unit cube
        return meshes;
    }

    private static ArrayList<Mesh> processOrientableWithBottom(Model modelJson) {
        return processCubeAllModel(modelJson);
    }

    private static ArrayList<Mesh> processGenericModel(Model modelJson) {
        List<Float> positions = new ArrayList<>();
        List<Float> texCoords = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        ArrayList<Mesh> meshes = new ArrayList<>();

        int currentIndex = 0;

        for (Model.Element element : modelJson.elements) {
            // Get atlas rectangle for this element’s texture
            String texName = modelJson.textures.get("0").replace("#", "");
            float[] atlasCoords = AtlasBuilder.getDefault().getUV(texName);
            if (atlasCoords == null) {
                Debug.log("Missing atlas UV for: " + texName);
                continue;
            }

            Vector3f from = new Vector3f(element.from[0], element.from[1], element.from[2]).div(16f);
            Vector3f to = new Vector3f(element.to[0], element.to[1], element.to[2]).div(16f);

            // Process each face
            String[] faces = {"north","south","up","down","east","west"};
            for (String faceName : faces) {
                Model.Face face = element.faces.get(faceName);
                currentIndex = processCubeFace(faceName, face, from, to, atlasCoords,
                                               positions, texCoords, indices, currentIndex);
            }
        }

        // Build a single mesh from all elements
        Mesh mesh = new Mesh(listToFloatArray(positions),
                             listToFloatArray(texCoords),
                             listToIntArray(indices));
        meshes.add(mesh);
        return meshes;
    }

    // -------------------- Cube Face Builder --------------------
    private static int processCubeFace(String faceName,
                                       Model.Face face,
                                       Vector3f from,
                                       Vector3f to,
                                       float[] atlasCoords,
                                       List<Float> positions,
                                       List<Float> texCoords,
                                       List<Integer> indices,
                                       int startIndex) {

        if (face == null || atlasCoords == null) return startIndex;

        Vector3f[] vertices = new Vector3f[4];
        Vector2f[] uvs = new Vector2f[4];

        // Atlas rectangle
        float uMin = atlasCoords[0], vMin = atlasCoords[1], uMax = atlasCoords[2], vMax = atlasCoords[3];

        // Face UVs in pixel coordinates (0-16) or default
        float subU0, subV0, subU1, subV1;
        if (face.uv != null && face.uv.length == 4) {
            subU0 = face.uv[0] / 16f;
            subV0 = face.uv[1] / 16f;
            subU1 = face.uv[2] / 16f;
            subV1 = face.uv[3] / 16f;
        } else {
            subU0 = 0f; subV0 = 0f; subU1 = 1f; subV1 = 1f;
        }

        // Map into atlas
        float u0 = uMin + subU0 * (uMax - uMin);
        float v0 = vMin + subV0 * (vMax - vMin);
        float u1 = uMin + subU1 * (uMax - uMin);
        float v1 = vMin + subV1 * (vMax - vMin);

        uvs[0] = new Vector2f(u0, v1); // bottom-left
        uvs[1] = new Vector2f(u1, v1); // bottom-right
        uvs[2] = new Vector2f(u1, v0); // top-right
        uvs[3] = new Vector2f(u0, v0); // top-left

        // Vertices
        switch (faceName) {
            case "north": vertices[0]=new Vector3f(from.x,from.y,to.z); vertices[1]=new Vector3f(to.x,from.y,to.z); vertices[2]=new Vector3f(to.x,to.y,to.z); vertices[3]=new Vector3f(from.x,to.y,to.z); break;
            case "south": vertices[0]=new Vector3f(to.x,from.y,from.z); vertices[1]=new Vector3f(from.x,from.y,from.z); vertices[2]=new Vector3f(from.x,to.y,from.z); vertices[3]=new Vector3f(to.x,to.y,from.z); break;
            case "up":    vertices[0]=new Vector3f(from.x,to.y,to.z); vertices[1]=new Vector3f(to.x,to.y,to.z); vertices[2]=new Vector3f(to.x,to.y,from.z); vertices[3]=new Vector3f(from.x,to.y,from.z); break;
            case "down":  vertices[0]=new Vector3f(from.x,from.y,from.z); vertices[1]=new Vector3f(to.x,from.y,from.z); vertices[2]=new Vector3f(to.x,from.y,to.z); vertices[3]=new Vector3f(from.x,from.y,to.z); break;
            case "east":  vertices[0]=new Vector3f(to.x,from.y,to.z); vertices[1]=new Vector3f(to.x,from.y,from.z); vertices[2]=new Vector3f(to.x,to.y,from.z); vertices[3]=new Vector3f(to.x,to.y,to.z); break;
            case "west":  vertices[0]=new Vector3f(from.x,from.y,from.z); vertices[1]=new Vector3f(from.x,from.y,to.z); vertices[2]=new Vector3f(from.x,to.y,to.z); vertices[3]=new Vector3f(from.x,to.y,from.z); break;
            default: return startIndex;
        }

        // Add vertices and UVs
        for (int i = 0; i < 4; i++) {
            positions.add(vertices[i].x);
            positions.add(vertices[i].y);
            positions.add(vertices[i].z);
            texCoords.add(uvs[i].x);
            texCoords.add(uvs[i].y);
        }

        // Indices
        indices.add(startIndex + 0);
        indices.add(startIndex + 1);
        indices.add(startIndex + 2);

        indices.add(startIndex + 2);
        indices.add(startIndex + 3);
        indices.add(startIndex + 0);

        return startIndex + 4;
    }

    // -------------------- Helpers --------------------
    private static float[] listToFloatArray(List<Float> list) {
        float[] arr = new float[list.size()];
        for (int i = 0; i < list.size(); i++) arr[i] = list.get(i);
        return arr;
    }

    private static int[] listToIntArray(List<Integer> list) {
        int[] arr = new int[list.size()];
        for (int i = 0; i < list.size(); i++) arr[i] = list.get(i);
        return arr;
    }
}
