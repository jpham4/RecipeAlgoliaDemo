package com.example.jimmy.recipealgoliademo.io;

import com.example.jimmy.recipealgoliademo.model.Highlight;
import com.example.jimmy.recipealgoliademo.model.HighlightedResult;
import com.example.jimmy.recipealgoliademo.model.Recipe;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SearchResultsJsonParser {
    private RecipeJsonParser recipeParser = new RecipeJsonParser();
    public List<HighlightedResult<Recipe>> parseResults(JSONObject jsonObject)
    {
        if (jsonObject == null)
            return null;

        List<HighlightedResult<Recipe>> results = new ArrayList<>();
        JSONArray hits = jsonObject.optJSONArray("hits");
        if (hits == null)
            return null;

        for (int i = 0; i < hits.length(); ++i) {
            JSONObject hit = hits.optJSONObject(i);
            if (hit == null)
                continue;

            Recipe recipe = recipeParser.parse(hit);
            if (recipe == null)
                continue;

            JSONObject highlightResult = hit.optJSONObject("_highlightResult");
            if (highlightResult == null)
                continue;
            JSONObject highlightName = highlightResult.optJSONObject("name");
            if (highlightName == null)
                continue;
            String value = highlightName.optString("value");
            if (value == null)
                continue;
            HighlightedResult<Recipe> result = new HighlightedResult<>(recipe);
            result.addHighlight("name", new Highlight("name", value));
            results.add(result);
        }
        return results;
    }
}
