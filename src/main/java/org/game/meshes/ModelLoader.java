package org.game.meshes;

import org.engine.utils.Resource;

public class ModelLoader {

    public static Model loadModel(String path) {
        Model model = Resource.loadJson(path, Model.class);
        model.initializeMeshes();
        return model;
    }
}