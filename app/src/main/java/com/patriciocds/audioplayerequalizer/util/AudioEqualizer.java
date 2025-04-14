package com.patriciocds.audioplayerequalizer.util;

public class AudioEqualizer {

    static {
        System.loadLibrary("audio-equalizer-lib");
    }

    public native int applyEqualization(short[] audioData, int[] gains);
}
