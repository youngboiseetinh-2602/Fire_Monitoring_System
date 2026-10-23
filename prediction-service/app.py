import os
from contextlib import asynccontextmanager
from pathlib import Path

import joblib
import pandas as pd
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field

MODEL_PATH = Path(os.getenv("MODEL_PATH", "/app/model/best_model.joblib"))
FEATURE_NAMES = ["NhietDo_DHT22", "DoAm_DHT22", "KhiGas_MQ2_ADC", "CamBienLua_Flame"]
LABELS = {0: "NORMAL", 1: "UNNORMAL", 2: "DANGER", 3: "FIRE", 4: "EXTREME_FIRE"}
model = None


class SensorReading(BaseModel):
    temperature: float
    humidity: float = Field(ge=0, le=100)
    gas: int = Field(ge=0)
    flame: bool


class Prediction(BaseModel):
    label: str
    class_id: int
    confidence: float


@asynccontextmanager
async def lifespan(_: FastAPI):
    global model
    # joblib uses pickle internally. Only deploy the trusted, locally trained model.
    model = joblib.load(MODEL_PATH)
    actual_features = list(getattr(model, "feature_names_in_", []))
    if actual_features and actual_features != FEATURE_NAMES:
        raise RuntimeError(f"Unexpected model features: {actual_features}")
    actual_classes = [int(value) for value in getattr(model, "classes_", [])]
    if actual_classes and actual_classes != list(LABELS):
        raise RuntimeError(f"Unexpected model classes: {actual_classes}")
    yield


app = FastAPI(title="Fire prediction service", lifespan=lifespan)


@app.get("/health")
def health():
    return {"status": "up", "modelLoaded": model is not None}


@app.post("/predict", response_model=Prediction)
def predict(reading: SensorReading):
    frame = pd.DataFrame([[
        reading.temperature, reading.humidity, reading.gas, int(reading.flame)
    ]], columns=FEATURE_NAMES)
    class_id = int(model.predict(frame)[0])
    label = LABELS.get(class_id)
    if label is None:
        raise HTTPException(status_code=500, detail="Model returned an unknown class")
    confidence = 1.0
    if hasattr(model, "predict_proba"):
        probabilities = model.predict_proba(frame)[0]
        class_index = list(model.classes_).index(class_id)
        confidence = float(probabilities[class_index])
    return Prediction(label=label, class_id=class_id, confidence=confidence)
