package com.patriciocds.audioplayerequalizer;

interface PlaybackCallback {
    void onMusicComplete();
    void onProgress(String musicTitle, String artist, int position);
}