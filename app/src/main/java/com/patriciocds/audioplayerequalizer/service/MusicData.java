package com.patriciocds.audioplayerequalizer.service;

import androidx.annotation.NonNull;

import com.google.gson.Gson;

public class MusicData {

    private final String title;
    private final String artist;
    private final String path;

    public static MusicData make(String jsonData) {
        return (new Gson()).fromJson(jsonData, MusicData.class);
    }

    public MusicData(String title, String artist, String path) {
        this.title = title;
        this.artist = artist;
        this.path = path;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getPath() {
        return path;
    }

    @NonNull
    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
