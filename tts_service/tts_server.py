# tts_service/tts_server.py
from flask import Flask, request, send_file
from transformers import VitsModel, AutoTokenizer
import torch
import scipy.io.wavfile
import io
from ruaccent import RUAccent
import numpy as np

app = Flask(__name__)

# Имя модели с Hugging Face (меняйте здесь, если нужно)
MODEL_NAME = "utrobinmv/tts_ru_free_hf_vits_high_multispeaker"
# MODEL_NAME = "facebook/mms-tts-rus"  # Альтернатива от Meta

# Загрузка модели и токенизатора один раз при старте
print(f"Загрузка модели {MODEL_NAME}...")
device = "cuda" if torch.cuda.is_available() else "cpu"
model = VitsModel.from_pretrained(MODEL_NAME).to(device)
tokenizer = AutoTokenizer.from_pretrained(MODEL_NAME)
accentizer = RUAccent()
accentizer.load(omograph_model_size='turbo', use_dictionary=True)
model.eval()
print("Модель готова к работе.")

@app.route('/synthesize', methods=['POST'])
def synthesize():
    """Эндпоинт для синтеза речи. Ждёт JSON с 'text' и опционально 'speaker_id'."""
    data = request.get_json()
    text = data.get('text', '').strip().lower()  # Модель ожидает нижний регистр [citation:5]
    speaker_id = int(data.get('speaker_id', 0))  # 0-женщина, 1-мужчина [citation:5]

    if not text:
        return {"error": "Текст не может быть пустым"}, 400

    # 1. Расстановка ударений для лучшего качества [citation:5][citation:10]
    try:
        text_with_accents = accentizer.process_all(text)
    except Exception:
        text_with_accents = text

    # 2. Токенизация и синтез
    inputs = tokenizer(text_with_accents, return_tensors="pt")
    with torch.no_grad():
        output = model(inputs["input_ids"].to(device), speaker_id=speaker_id).waveform
        audio_array = output.cpu().numpy().squeeze()

    # 3. Возврат аудио в виде WAV-файла
    sampling_rate = model.config.sampling_rate
    buffer = io.BytesIO()
    scipy.io.wavfile.write(buffer, rate=sampling_rate, data=audio_array)
    buffer.seek(0)

    return send_file(
        buffer,
        mimetype='audio/wav',
        as_attachment=True,
        download_name='speech.wav'
    )

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)