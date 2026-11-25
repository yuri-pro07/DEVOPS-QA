FROM python:3.11-slim AS base
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY *.py ./

FROM base AS teste
COPY requirements-teste.txt .
RUN pip install --no-cache-dir -r requirements-teste.txt
COPY tests ./tests
RUN python -m pytest -q tests

FROM base
ENV PYTHONUNBUFFERED=1
CMD ["python", "worker.py"]
