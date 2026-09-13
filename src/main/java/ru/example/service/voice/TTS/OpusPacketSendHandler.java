package ru.example.service.voice.TTS;

import net.dv8tion.jda.api.audio.AudioSendHandler;

import java.nio.ByteBuffer;

public class OpusPacketSendHandler implements AudioSendHandler {
    private final ByteBuffer opusData;
    private boolean played = false;
    
    public OpusPacketSendHandler(byte[] opusBytes) {
        this.opusData = ByteBuffer.wrap(opusBytes);
    }
    
    @Override
    public boolean canProvide() { return !played; }
    
    @Override
    public ByteBuffer provide20MsAudio() {
        if (!played) {
            played = true;
            return opusData;
        }
        return null;
    }
    
    @Override
    public boolean isOpus() { return true; }
}
