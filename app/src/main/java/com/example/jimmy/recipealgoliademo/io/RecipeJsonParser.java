package com.example.jimmy.recipealgoliademo.io;

import com.example.jimmy.recipealgoliademo.model.Recipe;

import org.json.JSONObject;

public class RecipeJsonParser {
    public Recipe parse(JSONObject jsonObject)
    {
        if (jsonObject == null)
            return null;

        String name = jsonObject.optString("name");
        String url = jsonObject.optString("url");
        String image = jsonObject.optString("image");
        int cookTime = jsonObject.optInt("cookTime", 0);
        if (name != null && url != null && image != null && cookTime != 0) {
            return new Recipe(name, url, image, cookTime);
        }
        return null;
    }
}
