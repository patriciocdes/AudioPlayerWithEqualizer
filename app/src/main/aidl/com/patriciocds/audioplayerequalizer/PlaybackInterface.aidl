package com.patriciocds.audioplayerequalizer;

import com.patriciocds.audioplayerequalizer.PlaybackCallback;

interface PlaybackInterface {
    boolean isPlaying();

    int getDuration();
    int getMusicVolume();
    int getMusicMaxVolume();

    void registerCallback(PlaybackCallback callback);
    void unregisterCallback(PlaybackCallback callback);
}