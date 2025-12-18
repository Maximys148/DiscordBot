import os
import io
import numpy as np
import torch
from flask import Flask, request, send_file

app = Flask(__name__)

# === НАСТРОЙКА TORCH / МОДЕЛИ ===
device = torch.device("cpu")
torch.set_num_threads(4)

MODEL_PATH = "v5_ru.pt"

# Скачиваем именно v5_ru.pt, минуя hub-кэш
if not os.path.isfile(MODEL_PATH):
    torch.hub.download_url_to_file(
        "https://models.silero.ai/models/tts/ru/v5_ru.pt",
        MODEL_PATH
    )

print("🚀 Загружаем модель из", MODEL_PATH)
model = torch.package.PackageImporter(MODEL_PATH).load_pickle("tts_models", "model")
model.to(device)

speakers = getattr(model, "speakers", [])
print("✅ Спикеры модели:", speakers)

# ---- Определение «версии» по признакам ----
model_type = type(model).__name__
apply_args = model.apply_tts.__code__.co_varnames

if MODEL_PATH == "v5_ru.pt" and set(speakers) == {"aidar", "baya", "kseniya", "eugene", "xenia"}:
    detected_version = "v5_ru (core v3 + v5 фичи)"
elif "v3" in model_type.lower():
    detected_version = "v3 (по имени класса)"
elif "v5" in model_type.lower():
    detected_version = "v5 (по имени класса)"
elif "use_stress" in apply_args or "use_yo" in apply_args or "put_stress_homo" in apply_args:
    detected_version = "v5-подобная (есть параметры ударений/омографов)"
elif "aidar" in speakers and "baya" in speakers and "random" in speakers:
    detected_version = "v4_ru-подобная (speakers + random)"
else:
    detected_version = "неоднозначно (нужна ручная проверка)"

print("🔎 Тип класса модели:", model_type)
print("🔎 Параметры apply_tts:", apply_args)
print("🔎 Грубое определение версии:", detected_version)
# -------------------------------------------


def preprocess_text(text: str) -> str:
    """Простая нормализация и расстановка пауз для лучшей просодии."""
    text = (text or "").strip()
    if not text:
        return text

    # Замена ... на один символ (часто лучше для TTS)
    text = text.replace("...", "…")

    # Паузы после знаков конца фразы
    for ch in [".", "!", "?"]:
        text = text.replace(ch, ch + " ")

    # Удаляем лишние пробелы
    text = " ".join(text.split())
    return text


def time_stretch_tensor(audio: torch.Tensor, speed_factor: float) -> torch.Tensor:
    """
    Простейший time-stretch через линейную интерполяцию.
    speed_factor < 1 => медленнее, > 1 => быстрее.
    Меняется и скорость, и высота голоса немного, но без внешних библиотек.
    """
    if speed_factor <= 0:
        return audio

    if speed_factor == 1.0:
        return audio

    y = audio.numpy()
    n_src = len(y)
    n_tgt = int(n_src / speed_factor)

    if n_tgt < 2 or n_src < 2:
        return audio

    # новые индексы по времени
    src_positions = np.linspace(0, n_src - 1, num=n_tgt)
    src_indices = np.floor(src_positions).astype(int)
    src_indices_next = np.clip(src_indices + 1, 0, n_src - 1)
    frac = src_positions - src_indices

    y_stretched = (1.0 - frac) * y[src_indices] + frac * y[src_indices_next]
    return torch.from_numpy(y_stretched.astype(np.float32))


@app.route("/synthesize", methods=["POST"])
def synthesize():
    data = request.get_json(force=True)
    text = preprocess_text(data.get("text", ""))

    if not text:
        return {"error": "Текст пустой"}, 400

    # здесь можно потом сделать выбор спикера из JSON
    speaker = "xenia"
    print(f"📝 Запрос: '{text[:60]}...' | 🎤 speaker={speaker} | model={detected_version}")

    # Генерация с включёнными фичами v5 (ударения / омографы / ё)
    audio = model.apply_tts(
        text=text,
        ssml_text=None,
        speaker=speaker,
        sample_rate=48000,
        put_accent=True,
        put_stress_homo=True,
        put_yo=True,
        put_yo_homo=True,
        stress_single_vowel=False,
        voice_path=None,
        symbol_durs=None,
        return_ts=False,
    )

    # === ЗАМЕДЛЕНИЕ РЕЧИ ===
    # коэффициент скорости: 0.8 -> медленнее примерно на 20%
    # speed_factor = float(data.get("speed", 0.9))
    # audio = time_stretch_tensor(audio, speed_factor)

    # Лёгкая пост‑обработка: headroom + нормализация + fade-in/out
    audio = torch.clamp(audio, -0.95, 0.95)
    audio = torch.nn.functional.normalize(audio, dim=0)

    fade_samples = int(0.03 * 48000)  # 30 ms
    if len(audio) > fade_samples * 2:
        fade = torch.linspace(0, 1, fade_samples)
        audio[:fade_samples] *= fade
        audio[-fade_samples:] *= torch.linspace(1, 0, fade_samples)

    # float32 [-1,1] -> int16 stereo BIG-ENDIAN (для Discord/JDA)
    mono = (audio.numpy() * 32767).astype(np.int16)
    stereo = np.stack([mono, mono], axis=1)
    stereo_be = stereo.astype(">i2")
    pcm_bytes = stereo_be.tobytes()

    # выравнивание под 20 ms пакеты (3840 байт)
    packet_size = 3840
    aligned_len = (len(pcm_bytes) // packet_size) * packet_size
    pcm_bytes = pcm_bytes[:aligned_len]

    buf = io.BytesIO(pcm_bytes)
    buf.seek(0)
    return send_file(
        buf,
        mimetype="application/octet-stream",
        download_name="speech.pcm"
    )


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000)
