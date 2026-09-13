from flask import Flask, request, send_file
import io
import numpy as np
from scipy.signal import resample
from TTS.api import TTS
import torch

app = Flask(__name__)

print("🚀 Загрузка XTTS-v2 (онлайн, CPU/GPU)...")

device = "cuda" if torch.cuda.is_available() else "cpu"

# Модель по имени, скачает из интернета при первом запуске
tts = TTS("tts_models/multilingual/multi-dataset/xtts_v2", gpu=(device == "cuda"))

SOURCE_SR = 24000   # у XTTS v2 24 кГц [web:192]
TARGET_SR = 48000
print("✅ XTTS загружен, SOURCE_SR =", SOURCE_SR, "TARGET_SR =", TARGET_SR)


@app.route('/synthesize', methods=['POST'])
def synthesize():
    data = request.get_json()
    text = data.get('text', '').strip()
    if not text:
        return {"error": "Текст пустой"}, 400

    speaker_wav = data.get('speaker_wav')  # опционально
    language = data.get('language', 'ru')

    if speaker_wav:
        wav = tts.tts(text=text, speaker_wav=speaker_wav, language=language)
    else:
        wav = tts.tts(text=text, language=language)

    wav = np.array(wav, dtype=np.float32)

    num_samples = int(len(wav) * TARGET_SR / SOURCE_SR)
    wav_48k = resample(wav, num_samples) if num_samples > 1 else wav
    wav_48k = np.clip(wav_48k, -0.95, 0.95)

    audio = wav_48k * 32767.0
    mono = audio.astype(np.int16)
    stereo = np.stack([mono, mono], axis=1)
    stereo_be = stereo.astype('>i2')
    pcm_bytes = stereo_be.tobytes()

    packet_size = 3840
    aligned_len = (len(pcm_bytes) // packet_size) * packet_size
    pcm_bytes = pcm_bytes[:aligned_len]

    return send_file(io.BytesIO(pcm_bytes),
                     mimetype='application/octet-stream',
                     download_name='speech.pcm')


@app.route('/health', methods=['GET'])
def health():
    return {"status": "ok"}


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5001, debug=False)
