import base64
import os
import threading
import time
import urllib.request
from typing import Any, Dict, Tuple

import cv2
import mediapipe as mp
import numpy as np
from flask import Flask, jsonify, request

app = Flask(__name__)

MODEL_URL = (
    "https://storage.googleapis.com/mediapipe-models/face_landmarker/"
    "face_landmarker/float16/latest/face_landmarker.task"
)
MODEL_PATH = os.path.join(os.path.dirname(__file__), "face_landmarker.task")

FACE_MESH: Any = None
FACE_LANDMARKER: Any = None

_DETECTOR_READY = False
_DETECTOR_ERROR = ""
_DETECTOR_LOCK = threading.Lock()

LEFT_EYE_INDICES = [33, 133, 159, 145, 160, 144, 158, 153, 173, 157]
RIGHT_EYE_INDICES = [362, 263, 386, 374, 387, 373, 385, 380, 398, 384]

_LAST_STATE: Dict[str, Any] = {"center": None, "ts": 0.0}
_STATE_LOCK = threading.Lock()


def _ensure_landmarker_model() -> str:
    if os.path.exists(MODEL_PATH) and os.path.getsize(MODEL_PATH) > 0:
        return MODEL_PATH

    urllib.request.urlretrieve(MODEL_URL, MODEL_PATH)
    if not os.path.exists(MODEL_PATH) or os.path.getsize(MODEL_PATH) <= 0:
        raise RuntimeError("Could not download Face Landmarker model.")

    return MODEL_PATH


def _ensure_detector() -> None:
    global FACE_MESH, FACE_LANDMARKER, _DETECTOR_READY, _DETECTOR_ERROR

    if _DETECTOR_READY and (FACE_MESH is not None or FACE_LANDMARKER is not None):
        return

    with _DETECTOR_LOCK:
        if _DETECTOR_READY and (FACE_MESH is not None or FACE_LANDMARKER is not None):
            return

        try:
            if hasattr(mp, "solutions") and hasattr(mp.solutions, "face_mesh"):
                FACE_MESH = mp.solutions.face_mesh.FaceMesh(
                    static_image_mode=True,
                    max_num_faces=2,
                    refine_landmarks=True,
                    min_detection_confidence=0.5,
                    min_tracking_confidence=0.5,
                )
            else:
                from mediapipe.tasks import python as mp_python
                from mediapipe.tasks.python import vision as mp_vision

                model_path = _ensure_landmarker_model()
                options = mp_vision.FaceLandmarkerOptions(
                    base_options=mp_python.BaseOptions(model_asset_path=model_path),
                    running_mode=mp_vision.RunningMode.IMAGE,
                    num_faces=2,
                    min_face_detection_confidence=0.5,
                    min_face_presence_confidence=0.5,
                    min_tracking_confidence=0.5,
                    output_face_blendshapes=False,
                    output_facial_transformation_matrixes=False,
                )
                FACE_LANDMARKER = mp_vision.FaceLandmarker.create_from_options(options)

            _DETECTOR_READY = True
            _DETECTOR_ERROR = ""
        except Exception as error:
            _DETECTOR_READY = False
            _DETECTOR_ERROR = str(error)
            raise


def _detect_faces(frame: np.ndarray) -> list[Any]:
    rgb_frame = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)

    if FACE_MESH is not None:
        result = FACE_MESH.process(rgb_frame)
        return list(result.multi_face_landmarks or [])

    if FACE_LANDMARKER is not None:
        mp_image = mp.Image(image_format=mp.ImageFormat.SRGB, data=rgb_frame)
        result = FACE_LANDMARKER.detect(mp_image)
        return list(result.face_landmarks or [])

    raise RuntimeError("Vision detector is not initialized.")


def _clamp(value: float, min_value: float, max_value: float) -> float:
    return max(min_value, min(max_value, value))


def _decode_base64_image(image_payload: str) -> np.ndarray:
    raw = image_payload.split(",", 1)[-1] if "," in image_payload else image_payload
    decoded = base64.b64decode(raw, validate=False)
    frame_array = np.frombuffer(decoded, dtype=np.uint8)
    frame = cv2.imdecode(frame_array, cv2.IMREAD_COLOR)
    if frame is None:
        raise ValueError("Unable to decode image.")
    return frame


def _edge_density(gray_image: np.ndarray) -> float:
    edges = cv2.Canny(gray_image, 60, 180)
    non_zero = float(np.count_nonzero(edges))
    total = float(max(1, edges.size))
    return non_zero / total


def _extract_face_box(landmarks: Any) -> Tuple[float, float, float, float]:
    xs = [lm.x for lm in landmarks]
    ys = [lm.y for lm in landmarks]
    return min(xs), min(ys), max(xs), max(ys)


def _is_centered(center_x: float, center_y: float) -> bool:
    return abs(center_x - 0.5) <= 0.18 and abs(center_y - 0.5) <= 0.22


def _eye_visible(landmarks: Any, indices: list[int]) -> bool:
    points = [landmarks[idx] for idx in indices if 0 <= idx < len(landmarks)]
    if len(points) < 6:
        return False

    for point in points:
        if point.x < 0 or point.x > 1 or point.y < 0 or point.y > 1:
            return False

    x_values = [point.x for point in points]
    y_values = [point.y for point in points]
    eye_width = max(x_values) - min(x_values)
    eye_height = max(y_values) - min(y_values)

    return eye_width > 0.015 and eye_height > 0.003


def _stable_presence(center: Tuple[float, float]) -> bool:
    now = time.time()
    stable = False

    with _STATE_LOCK:
        previous_center = _LAST_STATE.get("center")
        previous_ts = float(_LAST_STATE.get("ts") or 0.0)

        if previous_center is not None and now - previous_ts <= 6.0:
            dx = abs(center[0] - previous_center[0])
            dy = abs(center[1] - previous_center[1])
            stable = dx <= 0.09 and dy <= 0.10

        _LAST_STATE["center"] = center
        _LAST_STATE["ts"] = now

    return stable


def _wall_or_ceiling_hint(frame: np.ndarray) -> Tuple[int, str]:
    gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
    density = _edge_density(gray)

    if density < 0.02:
        return 8, "Camera appears pointed at wall/ceiling"

    return 12, "No face detected"


@app.get("/health")
def health() -> Any:
    return jsonify({
        "ok": True,
        "service": "vision-python-mediapipe",
        "detectorReady": _DETECTOR_READY,
        "detectorError": _DETECTOR_ERROR,
    })


@app.post("/validate-face")
def validate_face() -> Any:
    payload = request.get_json(silent=True) or {}
    image_payload = str(payload.get("image") or "").strip()

    if not image_payload:
        return jsonify({"message": "image is required"}), 400

    try:
        frame = _decode_base64_image(image_payload)
    except Exception:
        return jsonify({
            "faceDetected": False,
            "faceCount": 0,
            "eyeDetected": False,
            "score": 0,
            "hint": "Invalid image payload",
        }), 422

    try:
        _ensure_detector()
        faces = _detect_faces(frame)
    except Exception as error:
        return jsonify({
            "faceDetected": False,
            "faceCount": 0,
            "eyeDetected": False,
            "score": 0,
            "hint": "Vision service unavailable",
            "message": str(error),
        }), 503

    face_count = len(faces)

    if face_count == 0:
        score, hint = _wall_or_ceiling_hint(frame)
        return jsonify({
            "faceDetected": False,
            "faceCount": 0,
            "eyeDetected": False,
            "score": score,
            "hint": hint,
        })

    if face_count > 1:
        return jsonify({
            "faceDetected": True,
            "faceCount": face_count,
            "eyeDetected": False,
            "score": 15,
            "hint": "Only one person should be visible",
        })

    first_face = faces[0]
    landmarks = first_face.landmark if hasattr(first_face, "landmark") else first_face
    min_x, min_y, max_x, max_y = _extract_face_box(landmarks)
    center_x = (min_x + max_x) / 2.0
    center_y = (min_y + max_y) / 2.0

    centered = _is_centered(center_x, center_y)
    left_eye_visible = _eye_visible(landmarks, LEFT_EYE_INDICES)
    right_eye_visible = _eye_visible(landmarks, RIGHT_EYE_INDICES)
    eye_detected = left_eye_visible and right_eye_visible
    stable = _stable_presence((center_x, center_y))

    score = 40
    if centered:
        score += 25
    if eye_detected:
        score += 25
    if stable:
        score += 10

    score = int(_clamp(score, 0, 100))

    if not centered:
        hint = "Center your face"
    elif not eye_detected:
        hint = "Keep your face fully visible"
    elif stable:
        hint = "Face centered and eyes visible"
    else:
        hint = "Hold still for stable validation"

    return jsonify({
        "faceDetected": True,
        "faceCount": 1,
        "eyeDetected": eye_detected,
        "score": score,
        "hint": hint,
    })


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5001, debug=False)
