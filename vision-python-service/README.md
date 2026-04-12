# Vision Python Service (Flask + MediaPipe)

Dedicated microservice for real-time interview camera validation.

## Install

```bash
cd vision-python-service
python -m venv .venv
.venv\Scripts\activate
pip install -r requirements.txt
```

## Run

```bash
python app.py
```

Service starts on:

- http://localhost:5001/health
- http://localhost:5001/validate-face

Notes:

- On Python 3.13, MediaPipe uses the Tasks API and the service auto-downloads `face_landmarker.task` on first detector initialization.
- `/health` returns detector readiness fields (`detectorReady`, `detectorError`) to simplify troubleshooting.

## Request

```json
{
  "image": "<base64>"
}
```

## Response

```json
{
  "faceDetected": true,
  "faceCount": 1,
  "eyeDetected": true,
  "score": 92,
  "hint": "Face centered and eyes visible"
}
```
