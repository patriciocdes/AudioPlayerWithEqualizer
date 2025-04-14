package com.patriciocds.audioplayerequalizer.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;

import com.patriciocds.audioplayerequalizer.PlaybackCallback;
import com.patriciocds.audioplayerequalizer.PlaybackInterface;
import com.patriciocds.audioplayerequalizer.service.AudioService;

public class Playback {

    private final Context context;
    private final PlaybackEvents playbackEvents;

    private PlaybackInterface mediaPlayer;
    private boolean isBound = false;

    private final PlaybackCallback callback = new PlaybackCallback.Stub() {
        @Override
        public void onMusicComplete() {
            playbackEvents.onMusicComplete();
        }

        @Override
        public void onProgress(String musicTitle, String artist, int position) {
            playbackEvents.onMusicProgress(musicTitle, artist, position);
        }
    };

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mediaPlayer = PlaybackInterface.Stub.asInterface(service);
            isBound = true;

            try {
                mediaPlayer.registerCallback(callback);
            } catch (RemoteException e) {
                ExceptionUtils.printStackTrace(e);
            }

            playbackEvents.onMusicServiceConnected();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
            mediaPlayer = null;
        }
    };

    public Playback(Context context, PlaybackEvents playbackEvents) {
        this.context = context;
        this.playbackEvents = playbackEvents;
    }

    public boolean isConnected() {
        return isBound && mediaPlayer != null;
    }

    public void bindService() {
        Intent intent = new Intent(context, AudioService.class);

        context.startForegroundService(intent);
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    public void unbindService() {
        if (isBound && mediaPlayer != null) {

            try {
                mediaPlayer.unregisterCallback(callback);
            } catch (RemoteException e) {
                ExceptionUtils.printStackTrace(e);
            }

            context.unbindService(serviceConnection);
            isBound = false;
        }
    }

    public void play(String path) {
        if (isConnected()) {
            Intent intent = new Intent(context, AudioService.class);
            intent.setAction(AudioService.ACTION_PLAY);
            intent.putExtra(AudioService.EXTRA_AUDIO_PATH, path);

            context.startService(intent);
        }
    }

    public void pause() {
        if (isConnected()) {
            Intent intent = new Intent(context, AudioService.class);
            intent.setAction(AudioService.ACTION_PAUSE);

            context.startService(intent);
        }
    }

    public void stop() {
        if (isConnected()) {
            Intent intent = new Intent(context, AudioService.class);
            intent.setAction(AudioService.ACTION_STOP);

            context.startService(intent);
        }
    }

    public void seekTo(int position) {
        if (isConnected()) {
            Intent intent = new Intent(context, AudioService.class);
            intent.setAction(AudioService.ACTION_SEEK);
            intent.putExtra(AudioService.EXTRA_SEEK_POSITION, position);

            context.startService(intent);
        }
    }

    public void setVolume(int volume) {
        if (isConnected()) {
            Intent intent = new Intent(context, AudioService.class);
            intent.setAction(AudioService.ACTION_UPDATE_VOLUME);
            intent.putExtra(AudioService.EXTRA_VOLUME, volume);

            context.startService(intent);
        }
    }

    public int getDuration() {
        try {
            return isConnected() ? mediaPlayer.getDuration() : 0;
        } catch (RemoteException e) {
            return 0;
        }
    }

    public int getMusicMaxVolume() {
        try {
            return isConnected() ? mediaPlayer.getMusicMaxVolume() : 0;
        } catch (RemoteException e) {
            return 0;
        }
    }

    public int getMusicVolume() {
        try {
            return isConnected() ? mediaPlayer.getMusicVolume() : 0;
        } catch (RemoteException e) {
            return 0;
        }
    }

    public interface PlaybackEvents {
        void onMusicServiceConnected();
        void onMusicComplete();
        void onMusicProgress(String musicTitle, String artist, int position);
    }
}
