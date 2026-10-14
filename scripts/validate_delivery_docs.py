#!/usr/bin/env python3
"""Valida la estructura de los artefactos versionados de la Entrega 1."""
from __future__ import annotations

import csv
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path
from urllib.parse import unquote, urlparse

import yaml

ROOT = Path(__file__).resolve().parents[1]
DOCS = ROOT / "docs" / "entrega-1"
ERRORS: list[str] = []


def fail(message: str) -> None:
    ERRORS.append(message)


def validate_required_files() -> None:
    required = [
        "README.md",
        "01-vision-alcance.md",
        "02-actores-interesados.md",
        "03-procesos-negocio.md",
        "04-catalogo-requerimientos.md",
        "05-backlog-historias.md",
        "06-casos-de-uso.md",
        "07-modelo-dominio.md",
        "08-modelo-datos.md",
        "09-arquitectura-servicios.md",
        "10-openapi.yaml",
        "11-prototipo-validacion.md",
        "12-planificacion-riesgos.md",
        "13-checklist-entrega.md",
        "14-secuencias.md",
    ]
    for relative in required:
        if not (DOCS / relative).is_file():
            fail(f"Falta artefacto obligatorio: docs/entrega-1/{relative}")


def validate_openapi() -> None:
    path = DOCS / "10-openapi.yaml"
    try:
        spec = yaml.safe_load(path.read_text(encoding="utf-8"))
    except Exception as exc:  # YAML parsing errors should be visible in CI.
        fail(f"OpenAPI YAML no se puede leer: {exc}")
        return

    if not isinstance(spec, dict):
        fail("OpenAPI debe ser un objeto YAML de nivel superior")
        return
    if spec.get("openapi") != "3.1.0":
        fail("El contrato debe declarar openapi: 3.1.0")
    if not isinstance(spec.get("info"), dict) or not spec["info"].get("title"):
        fail("El contrato OpenAPI necesita info.title")
    paths = spec.get("paths")
    if not isinstance(paths, dict) or not paths:
        fail("El contrato OpenAPI debe declarar al menos una ruta")
        return

    operation_ids: set[str] = set()
    supported_methods = {"get", "post", "put", "patch", "delete", "options", "head", "trace"}
    for route, item in paths.items():
        if not isinstance(item, dict):
            fail(f"Elemento de ruta inválido en OpenAPI: {route}")
            continue
        for method, operation in item.items():
            if method.lower() not in supported_methods:
                continue
            if not isinstance(operation, dict):
                fail(f"Operación inválida: {method.upper()} {route}")
                continue
            operation_id = operation.get("operationId")
            if not operation_id:
                fail(f"Falta operationId en {method.upper()} {route}")
            elif operation_id in operation_ids:
                fail(f"operationId duplicado: {operation_id}")
            else:
                operation_ids.add(operation_id)
            if not isinstance(operation.get("responses"), dict) or not operation["responses"]:
                fail(f"Faltan responses en {method.upper()} {route}")
            placeholders = set(re.findall(r"\{([^{}]+)\}", route))
            params = []
            for parameter in item.get("parameters", []):
                if isinstance(parameter, dict):
                    params.append(parameter.get("name"))
            for parameter in operation.get("parameters", []):
                if isinstance(parameter, dict):
                    params.append(parameter.get("name"))
            declared = set(params)
            for name in placeholders - declared:
                # $ref path-level parameters need not be expanded here; this spec uses explicit path params.
                fail(f"Parámetro de ruta {{{name}}} no declarado en {method.upper()} {route}")

    print(f"OpenAPI: YAML válido; {len(paths)} rutas y {len(operation_ids)} operaciones inspeccionadas")


def validate_bpmn() -> None:
    bpmn_files = sorted((DOCS / "bpmn").glob("*.bpmn"))
    if len(bpmn_files) < 3:
        fail("Se esperan al menos tres fuentes BPMN: publicación, reserva y cancelación")
    for path in bpmn_files:
        try:
            root = ET.parse(path).getroot()
        except ET.ParseError as exc:
            fail(f"BPMN/XML inválido en {path.relative_to(ROOT)}: {exc}")
            continue
        ids: set[str] = set()
        for element in root.iter():
            element_id = element.attrib.get("id")
            if element_id:
                if element_id in ids:
                    fail(f"ID duplicado {element_id} en {path.relative_to(ROOT)}")
                ids.add(element_id)
        refs = {"sourceRef", "targetRef", "processRef", "default", "attachedToRef", "dataObjectRef"}
        for element in root.iter():
            for key, value in element.attrib.items():
                if key.split("}")[-1] in refs and value not in ids:
                    fail(f"Referencia {key}='{value}' sin ID correspondiente en {path.relative_to(ROOT)}")
        if not any(element.tag.endswith("process") for element in root.iter()):
            fail(f"No se encontró proceso BPMN en {path.relative_to(ROOT)}")
        print(f"BPMN: XML válido; {path.relative_to(ROOT)} ({len(ids)} IDs)")


def validate_traceability() -> None:
    path = ROOT / "docs" / "TRACEABILITY_TEMPLATE.csv"
    try:
        with path.open(encoding="utf-8-sig", newline="") as handle:
            rows = list(csv.reader(handle))
    except Exception as exc:
        fail(f"No se puede leer la matriz de trazabilidad: {exc}")
        return
    if not rows:
        fail("La matriz de trazabilidad está vacía")
        return
    width = len(rows[0])
    expected_columns = {"Requirement_ID", "Jira_Issue", "Acceptance_Criteria", "Design_Artifact", "Branch", "Commit", "Pull_Request", "Test_or_Evidence", "Release", "Status", "Owner"}
    missing = expected_columns - set(rows[0])
    if missing:
        fail(f"Faltan columnas de trazabilidad: {', '.join(sorted(missing))}")
    for number, row in enumerate(rows[1:], start=2):
        if len(row) != width:
            fail(f"Fila CSV {number} tiene {len(row)} campos, se esperaban {width}")
        if row and row[0] and not re.match(r"^(RF|RNF|RN)-\d+$", row[0]):
            fail(f"Identificador de requisito no reconocido en fila {number}: {row[0]}")
    if len(rows) < 10:
        fail("La matriz debe incluir el mapeo inicial de requisitos y artefactos")
    print(f"Trazabilidad CSV: {max(len(rows) - 1, 0)} filas de mapeo inspeccionadas")


def validate_relative_markdown_links() -> None:
    markdown_files = [ROOT / "README.md", *ROOT.glob("docs/**/*.md")]
    link_pattern = re.compile(r"(?<!!)\[[^\]]+\]\(([^)]+)\)")
    skipped_schemes = ("https://", "http://", "mailto:", "tel:", "data:")
    for file in markdown_files:
        if not file.is_file():
            continue
        text = file.read_text(encoding="utf-8")
        for raw_target in link_pattern.findall(text):
            target = raw_target.strip().split(" ", 1)[0].strip("<>")
            if not target or target.startswith("#") or target.startswith(skipped_schemes):
                continue
            target_path = unquote(urlparse(target).path).split("#", 1)[0]
            if not target_path:
                continue
            resolved = (file.parent / target_path).resolve()
            if not resolved.exists():
                fail(f"Enlace local roto en {file.relative_to(ROOT)}: {raw_target}")
    print(f"Enlaces Markdown: revisados en {len(markdown_files)} archivos potenciales")


def main() -> int:
    validate_required_files()
    validate_openapi()
    validate_bpmn()
    validate_traceability()
    validate_relative_markdown_links()
    if ERRORS:
        print("\nERRORES DE VALIDACIÓN DOCUMENTAL:", file=sys.stderr)
        for error in ERRORS:
            print(f"- {error}", file=sys.stderr)
        return 1
    print("Validación documental de Entrega 1 completada correctamente.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
