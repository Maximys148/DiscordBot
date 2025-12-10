package ru.example.service.voice;

import org.springframework.stereotype.Service;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Кодирует аудио данные из Discord в понятные данные для Vosk(STT сервис)
 */
@Service
public class OpusToPcmDecoder {

    public byte[] resample48kTo16k(byte[] audio48k) throws IOException {
        // Исправляем формат: входящие аудио данные - big endian, 2 канала (стерео)
        AudioFormat sourceFormat = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                48000,
                16,
                2,          // 2 канала (стерео)
                4,          // frame size = 2 байта * 2 канала = 4
                48000,
                true        // big endian (важно!)
        );

        AudioFormat intermediateFormat = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                48000,
                16,
                1,          // моно
                2,
                48000,
                false       // little endian для промежуточного
        );

        // Целевой формат: 16000 Hz, 16 бит, моно, signed, little endian
        AudioFormat targetFormat = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                16000,
                16,
                1,
                2,
                16000,
                false
        );

        try (ByteArrayInputStream bais = new ByteArrayInputStream(audio48k);
             AudioInputStream sourceStream = new AudioInputStream(bais, sourceFormat, audio48k.length / sourceFormat.getFrameSize());
             // Конвертируем стерео big endian -> моно little endian 48kHz
             AudioInputStream monoStream = AudioSystem.getAudioInputStream(intermediateFormat, sourceStream);
             // Конвертируем 48kHz моно -> 16kHz моно
             AudioInputStream resampledStream = AudioSystem.getAudioInputStream(targetFormat, monoStream);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = resampledStream.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            return baos.toByteArray();
        }
    }
}
