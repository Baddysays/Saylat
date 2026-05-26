#!/usr/bin/env python3
"""Img2img Saylat reference logo via ComfyUI API -> transparent PNG kit."""
from __future__ import annotations

import json
import random
import sys
import time
import urllib.request
import uuid
from pathlib import Path

COMFY = "http://127.0.0.1:8188"
INPUT_IMAGE = "saylat-reference.png"
CHECKPOINT = "Juggernaut-XL_v9.safetensors"
OUT_PREFIX = "saylat_ref"
SIZE = 1024

PROMPT = (
    "exact same mobile app logo design, white line art salad bowl with three lettuce leaves, "
    "five small cream yellow round dots, word Saylat in bold white sans-serif below bowl, "
    "centered composition, flat vector icon style, isolated on solid pure white background #FFFFFF, "
    "no teal gradient, no rounded square frame, no shadow, crisp clean edges, 1:1 app icon"
)

NEGATIVE = (
    "teal gradient background, cyan background, green background, colored background, "
    "squircle frame, rounded square border, checkerboard, gray background, shadow, 3d, "
    "photorealistic food photo, blurry, watermark, misspelled text, extra text"
)


def queue_prompt(workflow: dict) -> str:
    payload = json.dumps({"prompt": workflow}).encode()
    req = urllib.request.Request(
        f"{COMFY}/prompt",
        data=payload,
        headers={"Content-Type": "application/json"},
    )
    with urllib.request.urlopen(req, timeout=60) as resp:
        data = json.loads(resp.read())
    return data["prompt_id"]


def wait_prompt(prompt_id: str, timeout: float = 600) -> dict:
    start = time.time()
    while time.time() - start < timeout:
        with urllib.request.urlopen(f"{COMFY}/history/{prompt_id}", timeout=30) as resp:
            hist = json.loads(resp.read())
        if prompt_id in hist:
            entry = hist[prompt_id]
            st = entry.get("status", {})
            if st.get("completed"):
                if st.get("status_str") == "success":
                    return entry
                raise RuntimeError(f"job failed: {entry}")
        time.sleep(2)
    raise TimeoutError(prompt_id)


def build_workflow(seed: int, denoise: float) -> dict:
    return {
        "3": {
            "inputs": {
                "seed": seed,
                "steps": 32,
                "cfg": 7.0,
                "sampler_name": "euler",
                "scheduler": "normal",
                "denoise": denoise,
                "model": ["4", 0],
                "positive": ["6", 0],
                "negative": ["7", 0],
                "latent_image": ["12", 0],
            },
            "class_type": "KSampler",
        },
        "4": {
            "inputs": {"ckpt_name": CHECKPOINT},
            "class_type": "CheckpointLoaderSimple",
        },
        "6": {
            "inputs": {"text": PROMPT, "clip": ["4", 1]},
            "class_type": "CLIPTextEncode",
        },
        "7": {
            "inputs": {"text": NEGATIVE, "clip": ["4", 1]},
            "class_type": "CLIPTextEncode",
        },
        "8": {
            "inputs": {"samples": ["3", 0], "vae": ["4", 2]},
            "class_type": "VAEDecode",
        },
        "9": {
            "inputs": {"filename_prefix": OUT_PREFIX, "images": ["8", 0]},
            "class_type": "SaveImage",
        },
        "10": {
            "inputs": {"image": INPUT_IMAGE},
            "class_type": "LoadImage",
        },
        "11": {
            "inputs": {
                "image": ["10", 0],
                "upscale_method": "lanczos",
                "width": SIZE,
                "height": SIZE,
                "crop": "center",
            },
            "class_type": "ImageScale",
        },
        "12": {
            "inputs": {"pixels": ["11", 0], "vae": ["4", 2]},
            "class_type": "VAEEncode",
        },
    }


def main() -> int:
    seeds = [43001, 43002, 43003, 43004, 43005]
    denoise = float(sys.argv[1]) if len(sys.argv) > 1 else 0.52
    if len(sys.argv) > 2:
        seeds = [int(x) for x in sys.argv[2].split(",")]

    results: list[str] = []
    for seed in seeds:
        wf = build_workflow(seed, denoise)
        pid = queue_prompt(wf)
        print(f"queued seed={seed} denoise={denoise} prompt_id={pid}")
        entry = wait_prompt(pid)
        imgs = entry["outputs"]["9"]["images"]
        name = imgs[0]["filename"]
        results.append(name)
        print(f"done -> {name}")

    print("FILES:", ",".join(results))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
