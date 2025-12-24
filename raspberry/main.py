import time
import threading
from datetime import datetime
from typing import List, Optional
import os, json

import cv2
from fastapi import FastAPI, Response
from fastapi.responses import StreamingResponse
from pydantic import BaseModel, Field

import grovepi

app = FastAPI(title="Bin Raiders API")

# -----------------------------
# SENSOR ULTRASONICO (tolerante)
# -----------------------------
PUERTO_ULTRASONICO = 3  # tu puerto real
distance_cm = -1        # -1 = sin lectura válida
last_sensor_ts = 0.0
_stop_threads = False

def sensor_worker():
    global distance_cm, last_sensor_ts
    fail_count = 0
    print(f"📡 Iniciando ultrasónico en D{PUERTO_ULTRASONICO} (modo tolerante)...")
    while not _stop_threads:
        try:
            d = grovepi.ultrasonicRead(PUERTO_ULTRASONICO)
            if isinstance(d, int) and 0 < d < 450:
                distance_cm = d
                last_sensor_ts = time.time()
                fail_count = 0
            else:
                fail_count += 1
        except (IOError, TypeError):
            fail_count += 1

        if fail_count > 15:
            distance_cm = -1

        time.sleep(0.08)

threading.Thread(target=sensor_worker, daemon=True).start()

# -----------------------------
# CAMARA (captura en hilo)
# -----------------------------
camera_lock = threading.Lock()
latest_frame = None

def iniciar_camara():
    cap = cv2.VideoCapture(0)
    cap.set(cv2.CAP_PROP_BUFFERSIZE, 1)
    cap.set(cv2.CAP_PROP_FRAME_WIDTH, 640)
    cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 480)
    cap.set(cv2.CAP_PROP_FPS, 30)
    return cap

cap = iniciar_camara()

def camera_worker():
    global latest_frame, cap
    while not _stop_threads:
        if not cap.isOpened():
            time.sleep(1)
            cap = iniciar_camara()
            continue

        ret, frame = cap.read()
        if not ret:
            # reconexión suave
            cap.release()
            time.sleep(2)
            cap = iniciar_camara()
            continue

        with camera_lock:
            latest_frame = frame

threading.Thread(target=camera_worker, daemon=True).start()

# -----------------------------
# EVENTOS (para Notificaciones y luego FCM)
# -----------------------------
EVENTS_FILE = "eventos.json"

class EventoIn(BaseModel):
    contenedor_id: int = 1
    nivel: str = Field(default="INFO")  # INFO | WARN | ALERT
    mensaje: str
    distancia_cm: Optional[int] = None

class Evento(EventoIn):
    id: int
    ts: str
    acknowledged: bool = False

_eventos: List[Evento] = []
_next_id = 1

def _load_events():
    global _eventos, _next_id
    if os.path.exists(EVENTS_FILE):
        try:
            with open(EVENTS_FILE, "r", encoding="utf-8") as f:
                data = json.load(f)
            _eventos = [Evento(**x) for x in data]
            _next_id = (max([e.id for e in _eventos]) + 1) if _eventos else 1
        except Exception:
            _eventos = []
            _next_id = 1

def _save_events():
    try:
        with open(EVENTS_FILE, "w", encoding="utf-8") as f:
            json.dump([e.model_dump() for e in _eventos], f, ensure_ascii=False, indent=2)
    except Exception:
        pass

def push_event(nivel: str, msg: str, dist: Optional[int] = None, contenedor_id: int = 1):
    global _next_id
    ev = Evento(
        id=_next_id,
        ts=datetime.now().isoformat(timespec="seconds"),
        contenedor_id=contenedor_id,
        nivel=nivel,
        mensaje=msg,
        distancia_cm=dist,
        acknowledged=False
    )
    _next_id += 1
    _eventos.append(ev)
    _save_events()

_load_events()

# -----------------------------
# LOGICA de porcentaje/estado
# -----------------------------
def calc_fill_percent(dist: int) -> int:
    # calibra a tu maqueta:
    d_vacio = 35
    d_lleno = 8
    if dist >= d_vacio:
        return 0
    if dist <= d_lleno:
        return 100
    pct = (d_vacio - dist) / (d_vacio - d_lleno) * 100
    return int(round(pct))

def calc_estado(pct: int) -> str:
    if pct < 40: return "VACIO"
    if pct < 80: return "MEDIO"
    return "LLENO"

# Para evitar spamear eventos repetidos
_last_estado = None
_last_event_ts = 0.0

def maybe_emit_event(dist: int):
    global _last_estado, _last_event_ts
    pct = calc_fill_percent(dist)
    est = calc_estado(pct)

    # si no cambia estado, no generamos evento
    if est == _last_estado:
        return

    # anti-spam: mínimo 10s entre cambios
    now = time.time()
    if now - _last_event_ts < 10:
        return

    _last_estado = est
    _last_event_ts = now

    nivel = "INFO"
    if est == "LLENO":
        nivel = "ALERT"
        msg = f"⚠️ Contenedor 1 LLENO ({pct}%)"
    elif est == "MEDIO":
        nivel = "WARN"
        msg = f"🟠 Contenedor 1 en nivel MEDIO ({pct}%)"
    else:
        nivel = "INFO"
        msg = f"✅ Contenedor 1 VACIO ({pct}%)"

    push_event(nivel=nivel, msg=msg, dist=dist)

# -----------------------------
# ENDPOINTS
# -----------------------------
@app.get("/estado")
def estado():
    if distance_cm == -1:
        return {"ok": False, "error": "sensor_no_data", "distance_cm": -1}

    pct = calc_fill_percent(distance_cm)
    est = calc_estado(pct)

    maybe_emit_event(distance_cm)

    age_ms = int((time.time() - last_sensor_ts) * 1000) if last_sensor_ts else -1
    return {
        "ok": True,
        "distance_cm": distance_cm,
        "fill_percent": pct,
        "estado": est,
        "sensor_age_ms": age_ms
    }

@app.get("/snapshot")
def snapshot():
    with camera_lock:
        frame = None if latest_frame is None else latest_frame.copy()

    if frame is None:
        return Response(content=b"", media_type="image/jpeg", status_code=503)

    dist = distance_cm
    txt = "Sensor: BUSCANDO..." if dist == -1 else f"Distancia: {dist} cm"
    color = (0,0,255) if dist == -1 else (0,255,0)

    cv2.rectangle(frame, (5, 10), (380, 55), (0, 0, 0), -1)
    cv2.putText(frame, txt, (10, 45), cv2.FONT_HERSHEY_SIMPLEX, 1, color, 2, cv2.LINE_AA)

    ok, jpg = cv2.imencode(".jpg", frame, [int(cv2.IMWRITE_JPEG_QUALITY), 80])
    if not ok:
        return Response(content=b"", media_type="image/jpeg", status_code=500)

    return Response(content=jpg.tobytes(), media_type="image/jpeg")

def mjpeg_generator():
    while True:
        with camera_lock:
            frame = None if latest_frame is None else latest_frame.copy()

        if frame is None:
            time.sleep(0.1)
            continue

        dist = distance_cm
        txt = "Sensor: BUSCANDO..." if dist == -1 else f"Distancia: {dist} cm"
        color = (0,0,255) if dist == -1 else (0,255,0)

        cv2.rectangle(frame, (5, 10), (380, 55), (0, 0, 0), -1)
        cv2.putText(frame, txt, (10, 45), cv2.FONT_HERSHEY_SIMPLEX, 1, color, 2, cv2.LINE_AA)

        ok, jpg = cv2.imencode(".jpg", frame, [int(cv2.IMWRITE_JPEG_QUALITY), 80])
        if not ok:
            continue

        yield (b"--frame\r\n"
               b"Content-Type: image/jpeg\r\n\r\n" + jpg.tobytes() + b"\r\n")

        time.sleep(1/12)  # 12 fps aprox

@app.get("/stream")
def stream():
    return StreamingResponse(
        mjpeg_generator(),
        media_type="multipart/x-mixed-replace; boundary=frame"
    )

@app.get("/eventos", response_model=List[Evento])
def get_eventos(limit: int = 50):
    return list(reversed(_eventos[-limit:]))

@app.post("/eventos", response_model=Evento)
def post_evento(body: EventoIn):
    push_event(body.nivel, body.mensaje, body.distancia_cm, body.contenedor_id)
    return _eventos[-1]

@app.post("/eventos/clear")
def clear_eventos():
    _eventos.clear()
    global _next_id, _last_estado, _last_event_ts
    _next_id = 1
    _last_estado = None
    _last_event_ts = 0.0
    _save_events()
    return {"ok": True}
