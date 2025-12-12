package ru.example.model;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;

public class OpusPacketProvider {
    private final ByteArrayInputStream stream;
    private final byte[] buffer = new byte[960 * 2 * 2]; // 20ms @ 48kHz stereo
    private int bytesRead;
    
    public OpusPacketProvider(ByteArrayInputStream stream) {
        this.stream = stream;
    }
    
    public boolean canProvide() {
        return stream.available() > 0;
    }
    
    public ByteBuffer provide() {
        bytesRead = stream.read(buffer, 0, Math.min(buffer.length, stream.available()));
        return bytesRead > 0 ? ByteBuffer.wrap(buffer, 0, bytesRead) : null;
    }
}
