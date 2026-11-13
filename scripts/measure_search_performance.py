#!/usr/bin/env python3
"""Measure the agreed MARKET-69 search target against an already running server."""

import argparse
import concurrent.futures
import datetime as dt
import json
import math
import os
import platform
import subprocess
import sys
import time
import urllib.error
import urllib.parse
import urllib.request

REQUESTS = 50
CONCURRENT_CLIENTS = 5
TARGET_SECONDS = 3.0
MIN_ACTIVE_LISTINGS = 10_000


def request_json(url, timeout=90):
    request = urllib.request.Request(url, headers={"Accept": "application/json"})
    with urllib.request.urlopen(request, timeout=timeout) as response:
        return response.status, json.load(response)


def search_url(base, city, checkin, checkout):
    query = urllib.parse.urlencode({"city": city, "checkIn": checkin, "checkOut": checkout, "guests": 2})
    return f"{base}/api/listings?{query}"


def sample(base, city, checkin, checkout):
    started = time.perf_counter()
    try:
        status, body = request_json(search_url(base, city, checkin, checkout))
        elapsed = time.perf_counter() - started
        total = body.get("total") if isinstance(body, dict) else None
        ok = status == 200 and isinstance(total, int) and total > 0 and isinstance(body.get("items"), list)
        return {"city": city, "seconds": round(elapsed, 6), "http_status": status,
                "total": total, "ok": ok, "error": None if ok else "Respuesta vacía o inválida"}
    except (OSError, ValueError, urllib.error.URLError) as exc:
        return {"city": city, "seconds": round(time.perf_counter() - started, 6),
                "http_status": getattr(exc, "code", None), "total": None, "ok": False,
                "error": str(exc)}


def git_revision():
    try:
        root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
        revision = subprocess.run(["git", "rev-parse", "HEAD"], cwd=root, check=True,
                                  capture_output=True, text=True).stdout.strip()
        dirty = bool(subprocess.run(["git", "status", "--porcelain", "--untracked-files=no"],
                                    cwd=root, check=True, capture_output=True, text=True).stdout.strip())
        return {"commit": revision, "tracked_changes_present": dirty}
    except (OSError, subprocess.CalledProcessError):
        return {"commit": None, "tracked_changes_present": None}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base-url", default="http://127.0.0.1:8080", help="URL base del servidor ya iniciado")
    parser.add_argument("--output", help="Archivo JSON de resultados; también se escribe en pantalla")
    args = parser.parse_args()
    parsed = urllib.parse.urlsplit(args.base_url)
    if parsed.scheme not in {"http", "https"} or not parsed.hostname or parsed.username or parsed.password or parsed.query or parsed.fragment:
        parser.error("Indica una URL HTTP(S) sin credenciales, parámetros ni fragmentos")
    base = args.base_url.rstrip("/")
    bogota_now = dt.datetime.now(dt.timezone(dt.timedelta(hours=-5)))
    checkin = (bogota_now.date() + dt.timedelta(days=7)).isoformat()
    checkout = (bogota_now.date() + dt.timedelta(days=9)).isoformat()
    result = {
        "scenario": "MARKET-69: búsqueda por ciudad, fechas y dos huéspedes",
        "timestamp_bogota": bogota_now.isoformat(timespec="seconds"),
        "base_url": base,
        "target": {"requests": REQUESTS, "max_concurrent_clients": CONCURRENT_CLIENTS,
                   "p95_seconds_at_most": TARGET_SECONDS, "required_active_listings": MIN_ACTIVE_LISTINGS,
                   "percentile_method": "nearest rank: ceil(0.95 * n), 1-indexado"},
        "environment": {"platform": platform.platform(), "processor": platform.processor(),
                        "logical_cpus": os.cpu_count(), "python": platform.python_version(), **git_revision()},
        "check_in": checkin, "check_out": checkout,
    }
    try:
        # Outside the timed experiment: verify the public active catalogue and derive
        # actual destinations rather than asserting that the seed is present.
        _, catalog = request_json(f"{base}/api/listings")
        listings = catalog.get("items", [])
        result["observed_active_listings"] = catalog.get("total")
        if not isinstance(result["observed_active_listings"], int) or result["observed_active_listings"] < MIN_ACTIVE_LISTINGS:
            raise ValueError("El catálogo activo observado tiene menos de 10.000 alojamientos.")
        if not isinstance(listings, list) or len(listings) != result["observed_active_listings"]:
            raise ValueError("El catálogo público no coincide con su total declarado.")
        cities = sorted({item.get("city") for item in listings if isinstance(item, dict) and isinstance(item.get("city"), str) and item.get("capacity", 0) >= 2})
        del listings, catalog
        selected = []
        warmup_failures = []
        for city in cities:
            warmup = sample(base, city, checkin, checkout)
            if warmup["ok"]:
                selected.append(city)
            else:
                warmup_failures.append({"city": city, "error": warmup["error"], "http_status": warmup["http_status"]})
            if len(selected) == 10:
                break
        result["warmup_failures"] = warmup_failures
        if len(selected) < 10:
            raise ValueError("No se encontraron diez ciudades con resultados para las fechas de prueba.")
        result["cities"] = selected
        start = time.perf_counter()
        with concurrent.futures.ThreadPoolExecutor(max_workers=CONCURRENT_CLIENTS) as executor:
            results = list(executor.map(lambda i: sample(base, selected[i % len(selected)], checkin, checkout), range(REQUESTS)))
        result["wall_seconds"] = round(time.perf_counter() - start, 6)
        result["samples"] = results
        result["successful_requests"] = sum(item["ok"] for item in results)
        result["failed_requests"] = REQUESTS - result["successful_requests"]
        # All observed latencies are included; errors independently fail the experiment.
        durations = sorted(item["seconds"] for item in results)
        result["p50_seconds"] = durations[math.ceil(0.50 * len(durations)) - 1]
        result["p95_seconds"] = durations[math.ceil(0.95 * len(durations)) - 1]
        result["max_seconds"] = durations[-1]
        result["passed"] = result["failed_requests"] == 0 and result["p95_seconds"] <= TARGET_SECONDS
    except (OSError, ValueError, urllib.error.URLError) as exc:
        result["passed"] = False
        result["setup_error"] = str(exc)
    report = json.dumps(result, ensure_ascii=False, indent=2)
    if args.output:
        with open(args.output, "w", encoding="utf-8") as handle:
            handle.write(report + "\n")
    print(report)
    return 0 if result["passed"] else 1


if __name__ == "__main__":
    sys.exit(main())
