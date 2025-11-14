/*
package ru.example.service.voice.STT;

import marytts.LocalMaryInterface;
import marytts.MaryInterface;
import org.springframework.stereotype.Service;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioFileFormat;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class MaryTTSService {

    private final MaryInterface mary;

    public MaryTTSService() throws Exception {
        this.mary = new LocalMaryInterface();
        mary.setVoice("dfki-poppy-hsmm"); // Выберите подходящую модель голоса
    }

    public byte[] synthesize(String text) throws IOException {
        try (AudioInputStream audio = mary.generateAudio(text);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            AudioSystem.write(audio, AudioFileFormat.Type.WAVE, baos);
            byte[] wavBytes = baos.toByteArray();

            return stripWavHeader(wavBytes);
        } catch (Exception e) {
            throw new IOException("Ошибка синтеза речи", e);
        }
    }

    private byte[] stripWavHeader(byte[] wav) {
        if (wav.length <= 44) return new byte[0];
        byte[] pcm = new byte[wav.length - 44];
        System.arraycopy(wav, 44, pcm, 0, pcm.length);
        return pcm;
    }
}
*/
