package com.example.jimmy.recipealgoliademo.model;

public class Recipe {
    private String name;
    private String url;
    private String image;
    private int cookTime;

    public Recipe(String name, String url, String image, int cookTime)
    {
        this.name = name;
        this.url = url;
        this.image = image;
        this.cookTime = cookTime;
    }

    public String getName()
    {
        return name;
    }

    public String getUrl()
    {
        return url;
    }
    public String getImage()
    {
        return image;
    }

    public int getCookTime()
    {
        return cookTime;
    }


}
