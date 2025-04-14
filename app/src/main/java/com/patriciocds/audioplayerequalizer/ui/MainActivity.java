package com.patriciocds.audioplayerequalizer.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.patriciocds.audioplayerequalizer.R;
import com.patriciocds.audioplayerequalizer.service.MusicData;
import com.patriciocds.audioplayerequalizer.util.Playback;
import com.patriciocds.audioplayerequalizer.util.PlaybackUtils;

public class MainActivity extends BaseActivity implements Playback.PlaybackEvents {

    private TextView txtMusicTimeProgress;
    private TextView txtMusicTimeDuration;
    private TextView txtMusicTitle;
    private TextView txtMusicArtist;
    private SeekBar sbrMusicProgress;
    private SeekBar sbrMusicVolume;

    Playback playback = new Playback(this, this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        txtMusicTimeProgress = findViewById(R.id.txtMusicTimeProgress);
        txtMusicTimeDuration = findViewById(R.id.txtMusicTimeDuration);
        txtMusicTitle = findViewById(R.id.txtMusicTitle);
        txtMusicArtist = findViewById(R.id.txtMusicArtist);

        sbrMusicProgress = findViewById(R.id.sbrMusicProgress);
        sbrMusicProgress.setOnSeekBarChangeListener(getOnMusicProgressChangeListener());

        sbrMusicVolume = findViewById(R.id.sbrMusicVolume);
        sbrMusicVolume.setOnSeekBarChangeListener(getOnMusicVolumeChangeListener());

        findViewById(R.id.btnPlay).setOnClickListener(v -> onPlay());
        findViewById(R.id.btnPause).setOnClickListener(v -> onPausePlayback());
        findViewById(R.id.btnStop).setOnClickListener(v -> onStopPlayback());

        requestPermissions();
    }

    @Override
    protected int requestContentView() {
        return R.layout.activity_main;
    }

    @Override
    protected void onStart() {
        super.onStart();
        playback.bindService();
    }

    @Override
    protected void onStop() {
        super.onStop();
        playback.unbindService();
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 123);
            }
        }
    }

    private void onPlay() {
        String path = "android.resource://" + getPackageName() + "/" + R.raw.ketsa_if_you_waited;
        MusicData musicData = new MusicData("If you waited", "Ketsa", path);

        txtMusicTitle.setText(musicData.getTitle());
        txtMusicArtist.setText(musicData.getArtist());

        playback.play(musicData.toString());
    }

    private void onPausePlayback() {
        playback.pause();
    }

    private void onStopPlayback() {
        playback.stop();
        clearMusicInfo();
    }

    private void clearMusicInfo() {
        txtMusicTitle.setText(getString(R.string.unknown_info));
        txtMusicArtist.setText(getString(R.string.unknown_info));
        txtMusicTimeProgress.setText(R.string.music_stopped_time);
        txtMusicTimeDuration.setText(R.string.music_stopped_time);
        sbrMusicProgress.setProgress(0);
    }

    private SeekBar.OnSeekBarChangeListener getOnMusicProgressChangeListener() {
        return new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) { /* without implementation */ }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { /* without implementation */ }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int newPosition = (int)((seekBar.getProgress() / (float) seekBar.getMax()) * playback.getDuration());
                playback.seekTo(newPosition);
            }
        };
    }

    private SeekBar.OnSeekBarChangeListener getOnMusicVolumeChangeListener() {
        return new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    int newVolume = (int) ((progress / (float) seekBar.getMax()) * playback.getMusicMaxVolume());
                    playback.setVolume(newVolume);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { /* without implementation */ }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { /* without implementation */ }
        };
    }

    private void updateVolume() {
        int volume = (int) ((playback.getMusicVolume() / (float) playback.getMusicMaxVolume()) * 100);
        sbrMusicVolume.setProgress(volume);
    }

    @Override
    public void onMusicServiceConnected() {
        updateVolume();
    }

    @Override
    public void onMusicComplete() {
        runOnUiThread(this::clearMusicInfo);
    }

    @Override
    public void onMusicProgress(String musicTitle, String artist, int position) {
        int duration = playback.getDuration();

        txtMusicTitle.setText(musicTitle);
        txtMusicArtist.setText(artist);
        txtMusicTimeProgress.setText(PlaybackUtils.formatMusicTime(position));
        txtMusicTimeDuration.setText(PlaybackUtils.formatMusicTime(duration));

        if (duration > 0) {
            int progress = (int) ((position / (float) duration) * 100);
            sbrMusicProgress.setProgress(progress);
        } else {
            sbrMusicProgress.setProgress(0);
        }
    }
}