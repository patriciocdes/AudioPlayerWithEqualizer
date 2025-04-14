package com.patriciocds.audioplayerequalizer.util;

import android.annotation.SuppressLint;

public class PlaybackUtils {

    private PlaybackUtils() { }

    @SuppressLint("DefaultLocale")
    public static String formatMusicTime(long millis) {
        long totalSeconds = millis / 1000;
        long hour = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hour > 0) {
            return String.format("%02d:%02d:%02d", hour, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }
}
