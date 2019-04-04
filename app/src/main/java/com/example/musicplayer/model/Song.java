package com.example.musicplayer.model;

import java.io.Serializable;

public class Song implements Serializable {

    private String mName;
    private String mPath;


    public Song(String name, String path) {
        this.mName = name;
        this.mPath = path;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        this.mName = name;
    }

    public String getPath() {
        return mPath;
    }

    public void setPath(String path) {
        this.mPath = path;
    }

}
