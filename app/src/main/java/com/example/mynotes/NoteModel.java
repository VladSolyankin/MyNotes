package com.example.mynotes;

import android.net.Uri;

public class NoteModel {
    private Uri imageResource;

    private String itemName;

    public NoteModel(Uri imageResource, String itemName) {
        this.imageResource = imageResource;
        this.itemName = itemName;
    }

    public Uri getImageResource() {
        return imageResource;
    }

    public String getItemName() {
        return itemName;
    }

    public void setImageResource(Uri imageResource) {
        this.imageResource = imageResource;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }
}
