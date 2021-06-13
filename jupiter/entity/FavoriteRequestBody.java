package com.laioffer.jupiter.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;

public class FavoriteRequestBody {
    private final Item favoriteItem;

    @JsonCreator
    //java to json string也用@annotation 用@JsonCreator（"favourite"）
    //这里我们只需要把json string convert to java class 还因此我们就只用写@JsonCreator
    public FavoriteRequestBody(@JsonProperty("favorite") Item favoriteItem) {
        this.favoriteItem = favoriteItem;
    }

    public Item getFavoriteItem() {
        return favoriteItem;
    }
}

