package com.example.mynotes;

import android.net.Uri;

import java.util.Date;

public class NoteModel {
    private Uri imageResource;

    private String itemName;
    private Date date;

    public NoteModel(Uri imageResource, String itemName, Date date) {
        this.imageResource = imageResource;
        this.itemName = itemName;
        this.date = date;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
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
