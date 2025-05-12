package com.patriciocds.audioplayerequalizer.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class AudioEqualizerInstrumentedTest {

    private AudioEqualizer equalizer;

    @Before
    public void setUp() {
        // instancia a classe wrapper; ela já carrega a lib nativa
        equalizer = new AudioEqualizer();
    }

    @Test
    public void applyEqualization_singleBand_gain1000_noChange() {
        // gain 1000 → fator = 1000/1000 = 1.0
        short[] audio = new short[]{100, -200, 300};
        int[] gains = new int[]{1000};

        int returned = equalizer.applyEqualization(audio, gains);

        // deve retornar o número de samples
        assertEquals(3, returned);
        // e os valores não devem mudar
        assertArrayEquals(new short[]{100, -200, 300}, audio);
    }

    @Test
    public void applyEqualization_twoBands_gain2000_doublesEachSampleTwice() {
        // dois gains de 2000 → cada iteração multiplica por 2.0 duas vezes
        short[] audio = new short[]{ 10, -20, 30 };
        int[] gains = new int[]{2000, 2000};

        int returned = equalizer.applyEqualization(audio, gains);

        assertEquals(3, returned);
        // 10 *2 *2 = 40, -20 *2 *2 = -80, 30*2*2=120
        assertArrayEquals(new short[]{40, -80, 120}, audio);
    }

    @Test
    public void applyEqualization_mixedGains_appliesAllBands() {
        // gains {1500, 500} → fator total = 1.5 * 0.5 = 0.75
        short[] audio = new short[]{ 40, 80 };
        int[] gains = new int[]{1500, 500};

        int returned = equalizer.applyEqualization(audio, gains);

        assertEquals(2, returned);
        // 40 * 0.75 = 30, 80*0.75 = 60
        assertArrayEquals(new short[]{30, 60}, audio);
    }
}
