package com.patriciocds.audioplayerequalizer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.patriciocds.audioplayerequalizer.PlaybackInterface;
import com.patriciocds.audioplayerequalizer.R;
import com.patriciocds.audioplayerequalizer.ui.MainActivity;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ServiceController;
import org.robolectric.annotation.Config;

import java.io.IOException;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {28})
public class AudioServiceTest {

    private MediaPlayer mockPlayer;
    private AudioManager mockAudioManager;
    private AudioService service;
    private PlaybackInterface binder;

    @Before
    public void setUp() {
        mockPlayer = mock(MediaPlayer.class);
        mockAudioManager = mock(AudioManager.class);

        ServiceController<AudioService> controller = Robolectric.buildService(AudioService.class).create();

        service = controller.get();
        service.mediaPlayer = mockPlayer;
        service.audioManager = mockAudioManager;

        binder = (PlaybackInterface) service.onBind(new Intent());
    }

    @Test
    public void isPlaying_deveDelegarParaMediaPlayer() {
        when(mockPlayer.isPlaying()).thenReturn(true);

        assertTrue(service.isPlaying());
        verify(mockPlayer).isPlaying();
    }

    @Test
    public void isPlayingPorBinder_deveDelegarParaMediaPlayer() throws Exception {
        when(mockPlayer.isPlaying()).thenReturn(false);

        assertFalse(binder.isPlaying());
        verify(mockPlayer).isPlaying();
    }

    @Test
    public void getMusicVolume_deveRetornarValorDoAudioManager() throws Exception {
        when(mockAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC)).thenReturn(8);

        assertEquals(8, binder.getMusicVolume());
    }

    @Test
    public void getMusicMaxVolume_deveRetornarMaxDoAudioManager() throws Exception {
        when(mockAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)).thenReturn(15);

        assertEquals(15, binder.getMusicMaxVolume());
    }

    @Test
    public void setStreamVolume_viaIntent_deveChamarAudioManager() {
        // Simula uma chamada ao onStartCommand com ACTION_UPDATE_VOLUME
        Intent intent = new Intent();
        intent.setAction(AudioService.ACTION_UPDATE_VOLUME);
        intent.putExtra(AudioService.EXTRA_VOLUME, 3);

        service.onStartCommand(intent, 0, 0);

        verify(mockAudioManager).setStreamVolume(
                AudioManager.STREAM_MUSIC,
                3,
                AudioManager.FLAG_SHOW_UI
        );
    }

    @Test
    public void play_viaIntent_deveConfigurarMediaPlayerEIniciar() throws IOException {
        // 1. Prepara uma URI de teste e simula que o MediaPlayer não está tocando
        String fakeUri = "android.resource://com.patriciocds.audioplayerequalizer/123";
        MusicData musicData = new MusicData("If you waited", "Ketsa", fakeUri);

        when(mockPlayer.isPlaying()).thenReturn(false);

        // 2. Cria a Intent com ACTION_PLAY e o extra do path
        Intent intent = new Intent();
        intent.setAction(AudioService.ACTION_PLAY);
        intent.putExtra(AudioService.EXTRA_AUDIO_PATH, musicData.toString());

        // 3. Chama onStartCommand para disparar o play
        service.onStartCommand(intent, 0, 0);

        // 4. Verifica se o MediaPlayer foi corretamente resetado e configurado
        verify(mockPlayer).reset();
        verify(mockPlayer).setDataSource(
                eq(service.getApplicationContext()),
                eq(Uri.parse(musicData.getPath()))
        );
        verify(mockPlayer).prepare();
        verify(mockPlayer).start();
    }

    @Test
    public void onStartCommand_withActionPause_whenPlaying_invokesPauseOnMediaPlayer() {
        // 1. Simula que o MediaPlayer está tocando
        when(mockPlayer.isPlaying()).thenReturn(true);

        // 2. Cria a Intent com ACTION_PAUSE
        Intent intent = new Intent();
        intent.setAction(AudioService.ACTION_PAUSE);

        // 3. Chama onStartCommand para disparar o pausePlayback()
        service.onStartCommand(intent, 0, 0);

        // 4. Verifica se pause() foi invocado no MediaPlayer
        verify(mockPlayer).pause();
    }

    @Test
    public void onStartCommand_withActionPause_whenNotPlaying_invokesStartOnMediaPlayer() {
        // 1. Simula que o MediaPlayer NÃO está tocando
        when(mockPlayer.isPlaying()).thenReturn(false);

        // 2. Cria a Intent com ACTION_PAUSE (que, com isPlaying()==false, deve chamar resumePlayback())
        Intent intent = new Intent();
        intent.setAction(AudioService.ACTION_PAUSE);

        // 3. Chama onStartCommand para disparar resumePlayback()
        service.onStartCommand(intent, 0, 0);

        // 4. Verifica se start() foi invocado no MediaPlayer
        verify(mockPlayer).start();
    }

    @Test
    public void onStartCommand_withActionStop_whenPlaying_invokesStopOnMediaPlayer() {
        // 1. Simula que o MediaPlayer está tocando
        when(mockPlayer.isPlaying()).thenReturn(true);

        // 2. Cria a Intent com ACTION_STOP
        Intent intent = new Intent();
        intent.setAction(AudioService.ACTION_STOP);

        // 3. Chama onStartCommand para disparar stopPlayback()
        service.onStartCommand(intent, 0, 0);

        // 4. Verifica se stop() foi invocado no MediaPlayer
        verify(mockPlayer).stop();
    }

    @Test
    public void onStartCommand_withActionSeek_invokesSeekToOnMediaPlayer() {
        // 1. Define a posição de seek que será passada
        int seekPosition = 15000;

        // 2. Cria a Intent com ACTION_SEEK e o extra de posição
        Intent intent = new Intent();
        intent.setAction(AudioService.ACTION_SEEK);
        intent.putExtra(AudioService.EXTRA_SEEK_POSITION, seekPosition);

        // 3. Chama onStartCommand para disparar seekTo()
        service.onStartCommand(intent, 0, 0);

        // 4. Verifica se seekTo() foi invocado no MediaPlayer com o valor correto
        verify(mockPlayer).seekTo(seekPosition);
    }

    @Test
    public void onStartCommand_withActionUpdateVolume_invokesSetStreamVolume() {
        // 1. Define o novo volume que será passado
        int newVolume = 5;

        // 2. Cria a Intent com ACTION_UPDATE_VOLUME e o extra de volume
        Intent intent = new Intent();
        intent.setAction(AudioService.ACTION_UPDATE_VOLUME);
        intent.putExtra(AudioService.EXTRA_VOLUME, newVolume);

        // 3. Chama onStartCommand para disparar setStreamVolume()
        service.onStartCommand(intent, 0, 0);

        // 4. Verifica se setStreamVolume() foi invocado no AudioManager com os parâmetros corretos
        verify(mockAudioManager).setStreamVolume(
                AudioManager.STREAM_MUSIC,
                newVolume,
                AudioManager.FLAG_SHOW_UI
        );
    }

    @Test
    public void createNotificationChannel_registersChannelWithCorrectProperties() {
        // Recupera o NotificationManager do Service
        NotificationManager manager = (NotificationManager)
                service.getSystemService(Context.NOTIFICATION_SERVICE);

        // Obtém o canal criado
        NotificationChannel channel = manager.getNotificationChannel(AudioService.CHANNEL_ID);

        // Verificações
        assertNotNull("O canal não deve ser nulo", channel);
        assertEquals("O nome do canal deve ser 'Music Player'",
                "Music Player", channel.getName());
        assertEquals("A importância do canal deve ser IMPORTANCE_DEFAULT",
                NotificationManager.IMPORTANCE_DEFAULT, channel.getImportance());
    }

    @Test
    public void createNotification_setsCorrectTitleArtistAndPendingIntent() {
        // 1. Define título e artista de teste
        String testTitle = "Test Song";
        String testArtist = "Test Artist";

        // 2. Chama o método createNotification
        Notification notification = service.createNotification(testTitle, testArtist);

        // 3. Renderiza o RemoteViews em uma View real para inspeção
        View contentView = notification.contentView.apply(service, new FrameLayout(service));

        // 4. Verifica que o título e o artista foram colocados corretamente
        TextView titleView = contentView.findViewById(R.id.txtMusicTitle);
        TextView artistView = contentView.findViewById(R.id.txtArtist);
        assertNotNull("Title TextView não deve ser nulo", titleView);
        assertNotNull("Artist TextView não deve ser nulo", artistView);
        assertEquals("O título deve ser exibido corretamente", testTitle, titleView.getText().toString());
        assertEquals("O artista deve ser exibido corretamente", testArtist, artistView.getText().toString());

        // 5. Verifica o PendingIntent de conteúdo que abre a MainActivity
        PendingIntent contentIntent = notification.contentIntent;
        assertNotNull("PendingIntent de conteúdo não deve ser nulo", contentIntent);

        // Usa o ShadowPendingIntent para extrair a Intent internamente salva
        Intent firedIntent = shadowOf(contentIntent).getSavedIntent();
        assertNotNull("Intent salva no PendingIntent não deve ser nula", firedIntent);
        assertEquals("Deve abrir a MainActivity",
                MainActivity.class.getName(),
                firedIntent.getComponent().getClassName());
    }
}
