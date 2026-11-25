FROM python:3.11-slim AS base
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY app.py ./
COPY documentos ./documentos

FROM base AS teste
COPY requirements-teste.txt .
RUN pip install --no-cache-dir -r requirements-teste.txt
COPY tests ./tests
RUN python -m pytest -q tests

FROM base
EXPOSE 8000
CMD ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "8000", "--workers", "2"]
