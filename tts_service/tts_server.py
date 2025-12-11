from flask import Flask, request, send_file
import torch
import io
import numpy as np
from scipy.io import wavfile

app = Flask(__name__)

print("Загрузка Silero TTS...")
device = "cpu"
model, _ = torch.hub.load(
    repo_or_dir='snakers4/silero-models',
    model='silero_tts',
    language='ru',
    speaker='v3_1_ru'
)
speakers = model.speakers
print(f"Спикеры: {speakers}")

@app.route('/synthesize', methods=['POST'])
def synthesize():
    data = request.get_json()
    text = data.get('text', '').strip()
    speaker = data.get('speaker', 'aidar')

    if not text:
        return {"error": "Текст пустой"}, 400

    audio = model.apply_tts(
        text=text,
        speaker=speaker,
        sample_rate=48000,
        put_accent=True,
        put_yo=True
    )

    audio = torch.clamp(audio, -1.0, 1.0)
    mono = (audio.numpy() * 32767).astype(np.int16)

    stereo = np.stack([mono, mono], axis=1)  # (N, 2)

    # ✅ ИСПРАВЛЕНО для NumPy 2.0: BIG-ENDIAN для Discord!
    stereo_big_endian = stereo.astype('>i2')

    pcm_bytes = stereo_big_endian.tobytes()

    # ✅ выравнивание по 3840 байт
    packet_size = 3840
    aligned_len = (len(pcm_bytes) // packet_size) * packet_size
    pcm_bytes = pcm_bytes[:aligned_len]

    print(f"🎵 PCM BIG-ENDIAN: {len(pcm_bytes)} bytes, {len(pcm_bytes)//packet_size} packets")

    buffer = io.BytesIO(pcm_bytes)
    buffer.seek(0)

    return send_file(
        buffer,
        mimetype='application/octet-stream',
        as_attachment=True,
        download_name='speech.pcm'
    )


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=False)
