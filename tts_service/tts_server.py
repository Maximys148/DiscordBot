from flask import Flask, request, send_file
import torch
import io
import numpy as np
from scipy.io import wavfile

app = Flask(__name__)

print("🚀 Загрузка улучшенного Silero TTS v5...")
device = "cpu"
model, _ = torch.hub.load(
    repo_or_dir='snakers4/silero-models',
    model='silero_tts',
    language='ru',
    speaker='v5_ru'  # ✅ НОВЫЙ v5 — более естественный!
)
speakers = model.speakers
print(f"✅ Доступные спикеры: {speakers}")
print("Рекомендуемые: 'xenia' (мягкий женский), 'eugene' (мужской живой)")

def preprocess_text(text):
    """🎯 Предобработка для лучшей просодии и естественности"""
    if not text:
        return ""

    # ✅ Добавляем паузы после знаков препинания
    text = text.replace('.', '. ').replace('!', '! ').replace('?', '? ')
    text = text.replace('..', '...').replace('...', '... ')

    # ✅ Разбиваем слишком длинные фразы
    if len(text) > 120:
        words = text.split()
        text = ' '.join(words[:20]) + '...'

    # ✅ Убираем лишние пробелы
    return ' '.join(text.split())

@app.route('/synthesize', methods=['POST'])
def synthesize():
    data = request.get_json()
    text = data.get('text', '').strip()
    speaker = data.get('speaker', 'xenia')  # ✅ Лучший по естественности!

    if not text:
        return {"error": "Текст пустой"}, 400

    # ✅ Предобработка текста
    text = preprocess_text(text)
    print(f"📝 Обработанный текст: '{text}' (спикер: {speaker})")

    # ✅ Полный набор параметров Silero v5 для максимальной естественности
    audio = model.apply_tts(
        text=text,
        speaker=speaker,
        sample_rate=48000,
        put_accent=True,           # ✅ Авто-ударения
        put_yo=True,               # ✅ Ё всегда правильно
        put_stress_homo=True,      # ✅ v5: гомографы с ударениями
        put_yo_homo=True           # ✅ v5: Ё в гомографах
    )

    # ✅ Нормализация [-1,1] → int16
    audio = torch.clamp(audio, -1.0, 1.0)
    mono = (audio.numpy() * 32767).astype(np.int16)

    # ✅ Моно → стерео (L=R для Discord)
    stereo = np.stack([mono, mono], axis=1)  # shape: (N_samples, 2_channels)

    # ✅ КРИТИЧЕСКИ ВАЖНО: BIG-ENDIAN для Discord/JDA!
    stereo_big_endian = stereo.astype('>i2')  # NumPy 2.0 совместимо

    # ✅ Raw PCM байты (48kHz, 16bit, stereo, BIG-ENDIAN)
    pcm_bytes = stereo_big_endian.tobytes()

    # ✅ Выравнивание по пакетам Discord (3840 байт = 20ms)
    packet_size = 3840
    aligned_len = (len(pcm_bytes) // packet_size) * packet_size
    pcm_bytes = pcm_bytes[:aligned_len]

    print(f"🎵 PCM BIG-ENDIAN: {len(pcm_bytes)} bytes, {len(pcm_bytes)//packet_size} пакетов")

    buffer = io.BytesIO(pcm_bytes)
    buffer.seek(0)

    # ✅ Готовый Discord PCM (без WAV-обёртки!)
    return send_file(
        buffer,
        mimetype='application/octet-stream',
        as_attachment=True,
        download_name='speech.pcm'
    )

@app.route('/speakers', methods=['GET'])
def get_speakers():
    """🔍 Получить список доступных спикеров"""
    return {"speakers": list(speakers), "recommended": ["xenia", "eugene", "aidar"]}

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=False)
