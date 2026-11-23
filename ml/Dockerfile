FROM python:3.11-slim AS base
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY *.py ./

FROM base AS teste
COPY requirements-teste.txt .
RUN pip install --no-cache-dir -r requirements-teste.txt
COPY tests ./tests
RUN PASTA_DOS_MODELOS=/tmp/modelos python -m pytest -q tests

FROM base
ENV PASTA_DOS_MODELOS=/models
ENV WORKERS=4
EXPOSE 8000
CMD ["sh", "-c", "python preparar_modelo.py && exec uvicorn app:app --host 0.0.0.0 --port 8000 --workers $WORKERS"]
