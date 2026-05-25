FROM python:3.12-slim-bookworm

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        libxml2-dev \
        libxslt1-dev \
        zlib1g-dev \
        gcc \
        curl \
        fonts-dejavu-core \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt \
    && playwright install --with-deps chromium

COPY app ./app
COPY static ./static
COPY releases ./releases
COPY data ./data
COPY run.py .

ENV SAYLAT_TELEGRAM_SESSION_PATH=/app/data/telegram

ENV SAYLAT_HOST=0.0.0.0
ENV SAYLAT_PORT=8787

EXPOSE 8787

HEALTHCHECK --interval=30s --timeout=10s --start-period=15s --retries=3 \
    CMD curl -fsS http://127.0.0.1:8787/health || exit 1

# Playwright: один воркер (общий браузер в процессе)
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8787", "--workers", "1"]
