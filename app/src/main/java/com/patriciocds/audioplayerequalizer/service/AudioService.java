package com.patriciocds.audioplayerequalizer.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.widget.RemoteViews;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.app.NotificationCompat;

import com.patriciocds.audioplayerequalizer.PlaybackCallback;
import com.patriciocds.audioplayerequalizer.PlaybackInterface;
import com.patriciocds.audioplayerequalizer.R;
import com.patriciocds.audioplayerequalizer.ui.MainActivity;
import com.patriciocds.audioplayerequalizer.util.ExceptionUtils;

import java.io.IOException;

public class AudioService extends Service {

    public static final String ACTION_PLAY = "com.patriciocds.audioplayerequalizer.action.PLAY";
    public static final String ACTION_PAUSE = "com.patriciocds.audioplayerequalizer.action.PAUSE";
    public static final String ACTION_STOP = "com.patriciocds.audioplayerequalizer.action.STOP";
    public static final String ACTION_SEEK = "com.patriciocds.audioplayerequalizer.action.SEEK";
    public static final String ACTION_UPDATE_VOLUME = "com.patriciocds.audioplayerequalizer.action.UPDATE_VOLUME";

    public static final String EXTRA_AUDIO_PATH = "audioPath";
    public static final String EXTRA_SEEK_POSITION = "seekPosition";
    public static final String EXTRA_VOLUME = "newVolume";

    @VisibleForTesting
    static final String CHANNEL_ID = "AUDIO_SERVICE_CHANNEL_A";

    @VisibleForTesting
    MediaPlayer mediaPlayer;
    @VisibleForTesting
    AudioManager audioManager;

    private MusicData musicData;

    private final Handler progressHandler = new Handler(Looper.getMainLooper());

    private final Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            if (isPlaying()) {
                int position = mediaPlayer.getCurrentPosition();
                notifyOnMusicProgressUpdate(position);
                progressHandler.postDelayed(this, 1000);
            } else {
                progressHandler.removeCallbacks(this);
            }
        }
    };

    private final RemoteCallbackList<PlaybackCallback> playbackCallback = new RemoteCallbackList<>();

    @VisibleForTesting
    final PlaybackInterface.Stub binder = new PlaybackInterface.Stub() {

        @Override
        public boolean isPlaying() {
            return AudioService.this.isPlaying();
        }

        @Override
        public int getDuration() {
            return mediaPlayer.getDuration();
        }

        @Override
        public int getMusicVolume() {
            return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        }

        @Override
        public int getMusicMaxVolume() {
            return audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        }

        @Override
        public void registerCallback(PlaybackCallback callback) {
            playbackCallback.register(callback);
        }

        @Override
        public void unregisterCallback(PlaybackCallback callback) {
            playbackCallback.unregister(callback);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(mp -> notifyOnMusicComplete());

        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();

            switch (action) {
                case ACTION_PLAY:
                    play(intent.getStringExtra(EXTRA_AUDIO_PATH));
                    break;
                case ACTION_PAUSE:
                    if (isPlaying()) {
                        pausePlayback();
                    } else {
                        resumePlayback();
                    }
                    break;
                case ACTION_STOP:
                    stopPlayback();
                    break;
                case ACTION_SEEK:
                    seekTo(intent.getIntExtra(EXTRA_SEEK_POSITION, 0));
                    break;
                case ACTION_UPDATE_VOLUME:
                    setStreamVolume(intent.getIntExtra(EXTRA_VOLUME, 0));
                    break;
            }
        }

        /*
          START_STICKY: Para manter o serviço ativo
          O serviço é configurado com START_STICKY, o que indica que ele deve ser reiniciado
          caso seja encerrado pelo sistema.
         */
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (mediaPlayer != null) {
            stopPlayback();

            mediaPlayer.release();
            mediaPlayer = null;
        }

        super.onDestroy();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        stopService();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Music Player",
                NotificationManager.IMPORTANCE_DEFAULT
        );

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    @VisibleForTesting
    Notification createNotification(String musicTitle, String musicArtist) {
        Intent notificationIntent = new Intent(this, MainActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        RemoteViews notificationLayout = new RemoteViews(getPackageName(), R.layout.notification_player);
        notificationLayout.setTextViewText(R.id.txtMusicTitle, musicTitle);
        notificationLayout.setTextViewText(R.id.txtArtist, musicArtist);
        notificationLayout.setOnClickPendingIntent(R.id.btnPlay, getPlayPendingIntent());
        notificationLayout.setOnClickPendingIntent(R.id.btnPause, getPausePendingIntent());
        notificationLayout.setOnClickPendingIntent(R.id.btnStop, getStopPendingIntent());

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_audio_file)
                .setCustomContentView(notificationLayout)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setContentIntent(pendingIntent)
                .setOngoing(true);

        return builder.build();
    }

    private PendingIntent getPlayPendingIntent() {
        Intent playIntent = new Intent(this, AudioService.class);
        playIntent.setAction(ACTION_PLAY);

        return PendingIntent.getService(
                this, 0, playIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private PendingIntent getPausePendingIntent() {
        Intent pauseIntent = new Intent(this, AudioService.class);
        pauseIntent.setAction(ACTION_PAUSE);

        return PendingIntent.getService(
                this, 1, pauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private PendingIntent getStopPendingIntent() {
        Intent stopIntent = new Intent(this, AudioService.class);
        stopIntent.setAction(ACTION_STOP);

        return PendingIntent.getService(
                this, 2, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private void play(String data) {
        try {
            if (mediaPlayer != null) {
                if (isPlaying()) {
                    mediaPlayer.stop();
                }

                mediaPlayer.reset();

                if (data != null) {
                    musicData = MusicData.make(data);
                }

                if (musicData != null) {
                    if (musicData.getPath().contains("android.resource://")) {
                        Uri uri = Uri.parse(musicData.getPath());
                        mediaPlayer.setDataSource(getApplicationContext(), uri);
                    } else {
                        mediaPlayer.setDataSource(musicData.getPath());
                    }

                    startForeground(1, createNotification(musicData.getTitle(), musicData.getArtist()));

                    mediaPlayer.prepare();
                    mediaPlayer.start();

                    progressHandler.post(progressRunnable);
                }
            }
        } catch (IOException e) {
            ExceptionUtils.printStackTrace(e);
        }
    }

    private void pausePlayback() {
        if (isPlaying()) {
            progressHandler.removeCallbacks(progressRunnable);
            mediaPlayer.pause();
        }
    }

    private void stopPlayback() {
        if (isPlaying()) {
            progressHandler.removeCallbacks(progressRunnable);
            mediaPlayer.stop();
        }
    }

    private void stopService() {
        stopForeground(true);
        stopSelf();
    }

    private void seekTo(int position) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(position);
        }
    }

    private void resumePlayback() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            progressHandler.post(progressRunnable);
        }
    }

    @VisibleForTesting
    boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    private void setStreamVolume(int volume) {
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, AudioManager.FLAG_SHOW_UI);
    }

    private void notifyOnMusicComplete() {
        int items = playbackCallback.beginBroadcast();

        for (int i = 0; i < items; i++) {
            try {
                playbackCallback.getBroadcastItem(i).onMusicComplete();
            } catch (RemoteException e) {
                ExceptionUtils.printStackTrace(e);
            }
        }

        playbackCallback.finishBroadcast();
    }

    private void notifyOnMusicProgressUpdate(int position) {
        int items = playbackCallback.beginBroadcast();

        for (int i = 0; i < items; i++) {
            try {
                playbackCallback.getBroadcastItem(i).onProgress(
                        musicData.getTitle(),
                        musicData.getArtist(),
                        position
                );
            } catch (RemoteException e) {
                ExceptionUtils.printStackTrace(e);
            }
        }

        playbackCallback.finishBroadcast();
    }
}
