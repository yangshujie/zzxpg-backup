# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is **zgpg_algs** — a satellite on-orbit performance evaluation algorithm service (在轨技改算法库) for microsatellites (微小卫星). It provides a Flask + Celery REST API that dynamically loads and executes various satellite telemetry analysis algorithms.

## Running the Application

```bash
# Install dependencies
pip install -r requirements.txt

# Start the Flask server (port 6090)
python manage.py

# Start the Celery worker (requires Redis on 127.0.0.1:6379)
python -m celery -A manage.celery worker -l info -P eventlet
```

Redis is required as the Celery broker (db 1) and result backend (db 2).

## Architecture

### Request Flow

1. **`manage.py`** — Flask app entry point. Registers the `algsmanagement` blueprint and initializes Celery.
2. **`algsmanagement/views.py`** — REST API endpoints (Blueprint). All computation is dispatched as async Celery tasks.
3. **`algsmanagement/tasks.py`** — Celery task definitions that delegate to service functions.
4. **`algsmanagement/service.py`** — Core business logic. Handles telemetry data parsing, preprocessing (n-sigma outlier removal, three-sigma filtering), and dynamic algorithm loading via `importlib`.
5. **`algsmanagement/models.py`** — Data models including degradation analysis (`Tuihua`), trend prediction (`Qushi`/`Qushi2`), and neural network models (BP, RNN via PyTorch).
6. **`config/config.py`** — Celery configuration class and celery instance.

### API Endpoints (all POST, prefix `/algsmanagement`)

| Endpoint | Purpose |
|---|---|
| `/algs_load` | Generic algorithm execution (dynamically loads algorithm by name) |
| `/status/<task_id>` | Poll async task status/result |
| `/loss` | Degradation model (退化模型) |
| `/network` | Neural network training/inference |
| `/predict` | Trend prediction |
| `/getOrbitPic` | Orbit position visualization |

### Algorithm Plugin System

Algorithms live under `algs/` organized by category:

- **`algs/character/`** — Satellite characteristic indicators (attitude control accuracy, stability, signal stability, energy margin, etc.)
- **`algs/norm/`** — Normalization algorithms (Gaussian mean, sigmoid, residuals)
- **`algs/weight/`** — Weighting algorithms (AHP, entropy method)
- **`algs/dataProcess/`** — Data preprocessing (three-sigma outlier detection, forward-fill alignment)
- **`algs/others/`** — Miscellaneous (AHP validation, Cox model)

Each algorithm follows a convention: `algs/<category>/<alg_name>/<alg_name>.py` with an `algsMain(*args, **kwargs)` entry function that returns `(bool_status, result)`.

Algorithms are loaded dynamically in `service.py` via `importlib.import_module("algs.<category>.<name>.<name>")`. The `algs_name` and `algs_type` fields in the request determine which module to load.

### Data Flow for Telemetry-Based Algorithms

When `dataType == "1"`, the service:
1. Reads telemetry `.txt` files (CSV format: `timestamp,value`) from a storage directory organized by day
2. Applies preprocessing: `ty_name` module for outlier removal (e.g., `three_sigma`), `tc_name` module for alignment (e.g., `data_ffill`)
3. Creates aligned pandas DataFrames across multiple telemetry parameters
4. Passes the processed data to the target algorithm's `algsMain`

### Key Conventions

- Algorithm configs are passed via the `config` key in kwargs
- The `config` dict often contains: `ty_name` (preprocessing algo), `tc_name` (alignment algo), `zgpg_start_time`/`zgpg_end_time`, `sat_type`, `indicatorType`, `k`, `nocancha`, `roundTFlag`
- Telemetry data lists use `"null"` strings for missing values (converted to `None` internally)
- Python 3.6+ compatible (some `.pyc` files indicate 3.6, some 3.9)

## Testing

```bash
# Run the multicore effectiveness test for attitudeControlAccuracy
cd algsmanagement
python test_algs.py
```

The `test_data_generator.py` generates synthetic attitude data for testing. There is no formal test framework — tests are standalone scripts.
