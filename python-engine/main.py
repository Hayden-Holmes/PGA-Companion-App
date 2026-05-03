"""
Golf Course Fit Engine
----------------------
FastAPI service that computes per-course SG component weights using
Weighted Ridge Regression with exponential time decay.

Algorithm overview
──────────────────
1. Pull every player-tournament observation at the requested course from
   the database (rounds + leaderboard join).
2. Assign an exponential decay weight to each observation so that recent
   tournaments influence the model more than old ones (half-life = 2 yrs).
3. Standardise the four SG features then fit a weighted Ridge regression
   where the target is -position (higher = better finish).
4. Normalise the absolute coefficient values → weights that sum to 1.
5. Report R² (cross-validated when data allows) as a confidence signal.

Falls back to yardage-based heuristic weights when < 12 observations exist.

Run with:  uvicorn main:app --reload --port 8000
"""

import math
import logging
import os
from datetime import date

import numpy as np
import psycopg2
import psycopg2.extras
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from sklearn.linear_model import Ridge
from sklearn.model_selection import cross_val_score
from sklearn.preprocessing import StandardScaler

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# ── DB connection ─────────────────────────────────────────────────────────────

DB_HOST = os.getenv("DB_HOST", "db.wqsyfsdpgdlpbhkmcmrm.supabase.co")
DB_PORT = os.getenv("DB_PORT", "5432")
DB_NAME = os.getenv("DB_NAME", "postgres")
DB_USER = os.getenv("DB_USER", "postgres")
DB_PASS = os.getenv("DB_PASS", "H6OjSywPxvSg6qeA")

HALF_LIFE_DAYS = 730   # 2-year half-life for exponential decay


def get_conn():
    return psycopg2.connect(
        host=DB_HOST, port=int(DB_PORT), dbname=DB_NAME,
        user=DB_USER, password=DB_PASS, sslmode="require"
    )


# ── Response schema ───────────────────────────────────────────────────────────

class WeightsResponse(BaseModel):
    sg_ott:              float
    sg_app:              float
    sg_arg:              float
    sg_putt:             float
    algorithm:           str          # "ridge_regression" | "heuristic"
    data_points:         int
    r_squared:           float | None
    confidence:          str          # "high" | "medium" | "low"
    decay_half_life_days: int | None


# ── App ───────────────────────────────────────────────────────────────────────

app = FastAPI(title="Golf Course Fit Engine", version="2.0.0")


@app.get("/health")
def health():
    return {"status": "ok"}


@app.get("/weights/{course_id}", response_model=WeightsResponse)
def get_course_weights(course_id: str):
    """
    Compute SG component weights for a course.

    Uses Weighted Ridge Regression with exponential time decay when
    sufficient historical data exists; otherwise falls back to
    yardage-based heuristic weights.
    """
    conn = get_conn()
    try:
        course = _fetch_course(conn, course_id)
        if course is None:
            raise HTTPException(status_code=404, detail=f"Course '{course_id}' not found")

        rows = _fetch_course_history(conn, course_id)
        logger.info("Course %s: %d historical player-tournament observations", course_id, len(rows))

        if len(rows) < 12:
            w = _heuristic_weights(course)
            return WeightsResponse(
                sg_ott=w[0], sg_app=w[1], sg_arg=w[2], sg_putt=w[3],
                algorithm="heuristic",
                data_points=len(rows),
                r_squared=None,
                confidence="low",
                decay_half_life_days=None,
            )

        return _ridge_regression_weights(rows)
    finally:
        conn.close()


# ── Database queries ──────────────────────────────────────────────────────────

def _fetch_course(conn, course_id: str):
    with conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor) as cur:
        cur.execute(
            "SELECT course_id, course_name, yardage FROM courses WHERE course_id = %s",
            (course_id,),
        )
        return cur.fetchone()


def _fetch_course_history(conn, course_id: str) -> list:
    """
    One row per player per tournament at this course, including:
    - Per-tournament average of each SG component
    - Final leaderboard position (FIN only)
    - Tournament end date for decay weighting
    """
    query = """
        SELECT
            r.player_id,
            t.tournament_id,
            t.end_date,
            t.season_year,
            AVG(r.sg_ott)            AS avg_sg_ott,
            AVG(r.sg_app)            AS avg_sg_app,
            AVG(r.sg_arg)            AS avg_sg_arg,
            AVG(r.sg_putt)           AS avg_sg_putt,
            AVG(r.sg_total)          AS avg_sg_total,
            AVG(r.driving_distance)  AS avg_driving_dist,
            AVG(r.gir)               AS avg_gir,
            AVG(r.scrambling)        AS avg_scrambling,
            l.position
        FROM rounds r
        JOIN tournaments t
            ON r.tournament_id = t.tournament_id
        JOIN raw_leaderboard_rows l
            ON l.event_id  = t.event_id
           AND l.player_id = r.player_id
        WHERE t.course_id   = %s
          AND l.position    IS NOT NULL
          AND l.position    > 0
          AND r.sg_ott      IS NOT NULL
          AND r.sg_app      IS NOT NULL
          AND r.sg_arg      IS NOT NULL
          AND r.sg_putt     IS NOT NULL
        GROUP BY r.player_id, t.tournament_id, t.end_date, t.season_year, l.position
        ORDER BY t.end_date DESC NULLS LAST
    """
    with conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor) as cur:
        cur.execute(query, (course_id,))
        return cur.fetchall()


# ── Algorithm ─────────────────────────────────────────────────────────────────

def _ridge_regression_weights(rows: list) -> WeightsResponse:
    """
    Weighted Ridge Regression with exponential time decay.

    Each observation receives a sample weight  w_i = exp(-ln2 * days_ago / T)
    where T = HALF_LIFE_DAYS.  This lets the model emphasise recent
    tournaments at the course over older ones where conditions may differ.

    Features are standardised (zero mean, unit variance) before fitting so
    that Ridge's L2 penalty treats all four SG components fairly regardless
    of their natural scale.

    The returned weights are the absolute Ridge coefficients, normalised to
    sum to 1.
    """
    today = date.today()

    sg_ott  = np.array([float(r["avg_sg_ott"])  for r in rows])
    sg_app  = np.array([float(r["avg_sg_app"])  for r in rows])
    sg_arg  = np.array([float(r["avg_sg_arg"])  for r in rows])
    sg_putt = np.array([float(r["avg_sg_putt"]) for r in rows])
    y       = np.array([-float(r["position"])   for r in rows])  # higher = better

    # Exponential decay sample weights
    sample_weights = np.array([
        math.exp(
            -math.log(2)
            * max(0, (today - r["end_date"]).days if r["end_date"] else 365)
            / HALF_LIFE_DAYS
        )
        for r in rows
    ])

    X = np.column_stack([sg_ott, sg_app, sg_arg, sg_putt])

    # Standardise so Ridge penalises all features equally
    scaler  = StandardScaler()
    X_scaled = scaler.fit_transform(X)

    model = Ridge(alpha=1.0)
    model.fit(X_scaled, y, sample_weight=sample_weights)

    # R²: cross-validated when we have enough data, in-sample otherwise
    r_squared: float | None = None
    n = len(rows)
    if n >= 20:
        n_folds = min(5, n // 4)
        try:
            cv_scores = cross_val_score(
                Ridge(alpha=1.0), X_scaled, y,
                cv=n_folds,
                scoring="r2",
                fit_params={"sample_weight": sample_weights},
            )
            r_squared = float(np.mean(cv_scores))
        except Exception as exc:
            logger.warning("CV failed (%s); using in-sample R²", exc)
            r_squared = float(model.score(X_scaled, y, sample_weight=sample_weights))
    else:
        r_squared = float(model.score(X_scaled, y, sample_weight=sample_weights))

    # Normalise absolute coefficients → weights
    coeffs = np.abs(model.coef_)
    coeff_sum = coeffs.sum()
    if coeff_sum <= 0:
        coeffs = np.full(4, 0.25)
    else:
        coeffs = coeffs / coeff_sum

    if r_squared is not None and r_squared > 0.3:
        confidence = "high"
    elif r_squared is not None and r_squared > 0.1:
        confidence = "medium"
    else:
        confidence = "low"

    logger.info(
        "Ridge weights  OTT=%.3f  App=%.3f  ARG=%.3f  Putt=%.3f  R²=%.3f  n=%d",
        coeffs[0], coeffs[1], coeffs[2], coeffs[3],
        r_squared if r_squared is not None else float("nan"),
        n,
    )

    return WeightsResponse(
        sg_ott=round(float(coeffs[0]), 4),
        sg_app=round(float(coeffs[1]), 4),
        sg_arg=round(float(coeffs[2]), 4),
        sg_putt=round(float(coeffs[3]), 4),
        algorithm="ridge_regression",
        data_points=n,
        r_squared=round(r_squared, 4) if r_squared is not None else None,
        confidence=confidence,
        decay_half_life_days=HALF_LIFE_DAYS,
    )


def _heuristic_weights(course) -> tuple:
    yardage = course.get("yardage") or 7100
    if yardage >= 7300:
        return (0.40, 0.30, 0.10, 0.20)
    elif yardage >= 6900:
        return (0.25, 0.32, 0.13, 0.30)
    else:
        return (0.15, 0.32, 0.20, 0.33)
