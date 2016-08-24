package com.camachoyury.applergy;

import com.google.firebase.auth.FirebaseUser;

/**
 * Created by yury on 8/14/16.
 */

public class Food {


    private String urlImage;
    private String foodName;
    private String brand;
    private String foodType;
    private String allergies;
    private String ingredients;
    private FirebaseUser user;

    public Food() {
    }

    public Food(String urlImage, String foodName, String brand, String foodType, String allergies, String ingredients, FirebaseUser user) {
        this.urlImage = urlImage;
        this.foodName = foodName;
        this.brand = brand;
        this.foodType = foodType;
        this.allergies = allergies;
        this.ingredients = ingredients;
        this.user = user;
    }

    public String getUrlImage() {
        return urlImage;
    }

    public String getFoodName() {
        return foodName;
    }

    public String getBrand() {
        return brand;
    }

    public String getFoodType() {
        return foodType;
    }

    public String getAllergies() {
        return allergies;
    }

    public String getIngredients() {
        return ingredients;
    }

    public FirebaseUser getUser() {
        return user;
    }

    public void setUrlImage(String urlImage) {
        this.urlImage = urlImage;
    }

    public void setFoodName(String foodName) {
        this.foodName = foodName;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public void setFoodType(String foodType) {
        this.foodType = foodType;
    }

    public void setAllergies(String allergies) {
        this.allergies = allergies;
    }

    public void setIngredients(String ingredients) {
        this.ingredients = ingredients;
    }

    public void setUser(FirebaseUser user) {
        this.user = user;
    }
}
