#!/usr/bin/env python3
"""Build small demo seed files from local public datasets.

The script intentionally uses only the Python standard library. It reads raw
files from data/raw, writes reviewable generated artifacts to data/generated,
and does not require raw files for the default project test path.
"""

from __future__ import annotations

import argparse
import csv
import hashlib
import json
import re
import sys
import zipfile
from datetime import datetime, timedelta
from pathlib import Path
from typing import Iterable
from xml.etree import ElementTree


PROJECT_ROOT = Path(__file__).resolve().parents[2]
DEFAULT_RAW_DIR = PROJECT_ROOT / "data" / "raw"
DEFAULT_OUTPUT_DIR = PROJECT_ROOT / "data" / "generated"
FIXED_CREATED_AT = "2026-05-01 00:00:00.000000"


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Generate small MySQL demo seed SQL from local raw public datasets.",
    )
    parser.add_argument("--raw-dir", default=str(DEFAULT_RAW_DIR), help="Root directory containing raw datasets.")
    parser.add_argument("--output-dir", default=str(DEFAULT_OUTPUT_DIR), help="Directory for generated artifacts.")
    parser.add_argument("--max-orders", type=int, default=50, help="Maximum generated orders.")
    parser.add_argument("--max-products", type=int, default=50, help="Maximum generated products.")
    parser.add_argument("--max-order-items", type=int, default=150, help="Maximum generated order items.")
    parser.add_argument("--max-tickets", type=int, default=50, help="Maximum ticket-like samples considered.")
    parser.add_argument("--max-evaluation-cases", type=int, default=20, help="Maximum generated JSONL cases.")
    args = parser.parse_args()

    raw_dir = Path(args.raw_dir)
    output_dir = Path(args.output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)

    orders = normalize_orders(read_table_dir(raw_dir / "orders"), args.max_orders)
    products, clothing_reviews = normalize_clothing(read_table_dir(raw_dir / "clothing_reviews"), args.max_products)
    chinese_reviews = list(read_table_dir(raw_dir / "chinese_reviews"))

    if not products:
        products = fallback_products()
    if not orders:
        orders = fallback_orders()

    order_items = build_order_items(orders, products, args.max_order_items)
    evaluation_cases = build_evaluation_cases(
        chinese_reviews,
        clothing_reviews,
        args.max_tickets,
        args.max_evaluation_cases,
    )

    write_sql(output_dir / "demo_seed_extra.sql", orders, products, order_items)
    write_jsonl(output_dir / "demo_evaluation_cases.jsonl", evaluation_cases)

    print(f"Generated {output_dir / 'demo_seed_extra.sql'}")
    print(f"Generated {output_dir / 'demo_evaluation_cases.jsonl'}")
    print(
        "Counts: "
        f"orders={len(orders)}, products={len(products)}, "
        f"order_items={len(order_items)}, evaluation_cases={len(evaluation_cases)}"
    )
    return 0


def read_table_dir(directory: Path) -> Iterable[dict[str, str]]:
    if not directory.exists():
        return []

    rows: list[dict[str, str]] = []
    for file in sorted(directory.iterdir()):
        if file.name.startswith("~$") or file.name.startswith("."):
            continue
        suffix = file.suffix.lower()
        if suffix == ".csv":
            rows.extend(read_csv(file))
        elif suffix == ".xlsx":
            rows.extend(read_xlsx(file))
    return rows


def read_csv(path: Path) -> list[dict[str, str]]:
    for encoding in ("utf-8-sig", "utf-8"):
        try:
            with path.open("r", encoding=encoding, newline="") as handle:
                return [normalize_row(row) for row in csv.DictReader(handle)]
        except UnicodeDecodeError:
            continue
    print(f"Skipped unreadable CSV: {path.name}", file=sys.stderr)
    return []


def read_xlsx(path: Path) -> list[dict[str, str]]:
    try:
        with zipfile.ZipFile(path) as workbook:
            shared_strings = read_shared_strings(workbook)
            sheet_name = first_sheet_name(workbook)
            xml = workbook.read(sheet_name)
    except (KeyError, zipfile.BadZipFile):
        print(f"Skipped unsupported XLSX: {path.name}", file=sys.stderr)
        return []

    namespace = {"a": "http://schemas.openxmlformats.org/spreadsheetml/2006/main"}
    root = ElementTree.fromstring(xml)
    records: list[list[str]] = []
    for row in root.findall(".//a:sheetData/a:row", namespace):
        values_by_column: dict[int, str] = {}
        for cell in row.findall("a:c", namespace):
            cell_ref = cell.attrib.get("r", "")
            column_index = excel_column_index(cell_ref)
            values_by_column[column_index] = cell_value(cell, shared_strings, namespace)
        if values_by_column:
            max_index = max(values_by_column)
            records.append([values_by_column.get(index, "") for index in range(max_index + 1)])

    if not records:
        return []
    headers = [clean_text(value) for value in records[0]]
    rows = []
    for record in records[1:]:
        row = {headers[index]: clean_text(value) for index, value in enumerate(record) if index < len(headers)}
        if any(row.values()):
            rows.append(normalize_row(row))
    return rows


def read_shared_strings(workbook: zipfile.ZipFile) -> list[str]:
    try:
        xml = workbook.read("xl/sharedStrings.xml")
    except KeyError:
        return []
    namespace = {"a": "http://schemas.openxmlformats.org/spreadsheetml/2006/main"}
    root = ElementTree.fromstring(xml)
    strings = []
    for item in root.findall(".//a:si", namespace):
        strings.append("".join(text.text or "" for text in item.findall(".//a:t", namespace)))
    return strings


def first_sheet_name(workbook: zipfile.ZipFile) -> str:
    names = sorted(name for name in workbook.namelist() if name.startswith("xl/worksheets/sheet"))
    return names[0]


def excel_column_index(cell_ref: str) -> int:
    letters = "".join(ch for ch in cell_ref if ch.isalpha())
    value = 0
    for letter in letters:
        value = value * 26 + ord(letter.upper()) - ord("A") + 1
    return max(value - 1, 0)


def cell_value(cell: ElementTree.Element, shared_strings: list[str], namespace: dict[str, str]) -> str:
    cell_type = cell.attrib.get("t")
    value = cell.find("a:v", namespace)
    if cell_type == "inlineStr":
        return "".join(text.text or "" for text in cell.findall(".//a:t", namespace))
    if value is None or value.text is None:
        return ""
    if cell_type == "s":
        index = int(value.text)
        return shared_strings[index] if index < len(shared_strings) else ""
    return value.text


def normalize_row(row: dict[str, str]) -> dict[str, str]:
    return {clean_text(key): clean_text(value) for key, value in row.items() if key is not None}


def normalize_orders(rows: Iterable[dict[str, str]], limit: int) -> list[dict[str, str]]:
    orders = []
    seen = set()
    for row in rows:
        order_id = row.get("order_id", "")
        if not order_id or order_id in seen:
            continue
        seen.add(order_id)
        paid_at = mysql_timestamp(row.get("order_approved_at")) or mysql_timestamp(row.get("order_purchase_timestamp"))
        delivered_at = mysql_timestamp(row.get("order_delivered_customer_date"))
        estimated = mysql_timestamp(row.get("order_estimated_delivery_date"))
        deadline = aftersale_deadline(delivered_at, estimated, paid_at)
        orders.append(
            {
                "order_id": truncate_id(order_id, "O-GEN"),
                "user_id": truncate_id(row.get("customer_id", "U-GENERATED"), "U-GEN"),
                "product_id": "",
                "product_name": "",
                "order_status": normalize_order_status(row.get("order_status", "")),
                "paid_amount": "99.00",
                "paid_at": paid_at or FIXED_CREATED_AT,
                "delivered_at": delivered_at,
                "aftersale_deadline": deadline,
            }
        )
        if len(orders) >= limit:
            break
    return orders


def normalize_clothing(rows: Iterable[dict[str, str]], limit: int) -> tuple[list[dict[str, str]], list[dict[str, str]]]:
    products = []
    reviews = []
    seen = set()
    for row in rows:
        clothing_id = row.get("Clothing ID", "")
        review_text = row.get("Review Text", "")
        if review_text:
            reviews.append(row)
        if not clothing_id:
            continue
        product_id = truncate_id(f"P-CLOTHING-{safe_identifier(clothing_id)}", "P-CLOTHING")
        if product_id in seen:
            continue
        seen.add(product_id)
        category = row.get("Class Name") or "Clothing"
        product_name = row.get("Title") or category or f"Clothing {clothing_id}"
        products.append(
            {
                "product_id": product_id,
                "product_name": truncate_text(product_name, 255),
                "category": truncate_text(category, 255),
                "department": truncate_text(row.get("Department Name", ""), 255),
                "division": truncate_text(row.get("Division Name", ""), 255),
                "source_dataset": "clothing_reviews",
                "created_at": FIXED_CREATED_AT,
            }
        )
        if len(products) >= limit:
            break
    return products, reviews


def fallback_products() -> list[dict[str, str]]:
    return [
        {
            "product_id": "P-GEN-CLOTHING-001",
            "product_name": "Demo Clothing Product",
            "category": "Clothing",
            "department": "Women",
            "division": "General",
            "source_dataset": "fallback-demo",
            "created_at": FIXED_CREATED_AT,
        }
    ]


def fallback_orders() -> list[dict[str, str]]:
    return [
        {
            "order_id": "O-GEN-DEMO-001",
            "user_id": "U-GEN-DEMO",
            "product_id": "",
            "product_name": "",
            "order_status": "DELIVERED",
            "paid_amount": "99.00",
            "paid_at": "2026-05-01 10:00:00.000000",
            "delivered_at": "2026-05-03 10:00:00.000000",
            "aftersale_deadline": "2026-05-10 10:00:00.000000",
        }
    ]


def build_order_items(
    orders: list[dict[str, str]],
    products: list[dict[str, str]],
    limit: int,
) -> list[dict[str, str]]:
    items = []
    for index, order in enumerate(orders):
        product = products[index % len(products)]
        order["product_id"] = product["product_id"]
        order["product_name"] = product["product_name"]
        items.append(
            {
                "order_item_id": truncate_id(f"OI-{safe_identifier(order['order_id'])}-1", "OI-GEN"),
                "order_id": order["order_id"],
                "product_id": product["product_id"],
                "product_name": product["product_name"],
                "quantity": "1",
                "unit_price": "99.00",
                "category": product["category"],
                "created_at": order["paid_at"],
            }
        )
        if len(items) >= limit:
            break
    return items


def build_evaluation_cases(
    chinese_rows: Iterable[dict[str, str]],
    clothing_rows: Iterable[dict[str, str]],
    max_tickets: int,
    limit: int,
) -> list[dict[str, object]]:
    cases = []
    ticket_budget = max_tickets
    for row in chinese_rows:
        if ticket_budget <= 0 or len(cases) >= limit:
            break
        review = row.get("review", "")
        if not review:
            continue
        label = row.get("label", "")
        if label == "0" or (label == "1" and len(cases) % 5 == 0):
            cases.append(evaluation_case("chinese_reviews", row.get("cat", ""), review, label))
            ticket_budget -= 1

    for row in clothing_rows:
        if ticket_budget <= 0 or len(cases) >= limit:
            break
        review = row.get("Review Text", "")
        if not review:
            continue
        rating = to_int(row.get("Rating"))
        recommended = row.get("Recommended IND", "")
        if rating <= 2 or recommended == "0":
            cases.append(evaluation_case("clothing_reviews", row.get("Class Name", ""), review, "0"))
            ticket_budget -= 1
    return cases


def evaluation_case(source: str, category: str, text: str, label: str) -> dict[str, object]:
    return {
        "source": source,
        "sourceCategory": category,
        "input": truncate_text(text, 240),
        "expected": {
            "sentimentLabel": label,
            "datasetGenerated": True,
        },
    }


def write_sql(path: Path, orders: list[dict[str, str]], products: list[dict[str, str]], items: list[dict[str, str]]) -> None:
    lines = [
        "-- Generated by scripts/data/build_demo_seed.py.",
        "-- Optional MySQL demo enrichment. Import after schema-mysql.sql and data-mysql.sql.",
        "",
    ]
    lines.extend(insert_statement("products", [
        "product_id",
        "product_name",
        "category",
        "department",
        "division",
        "source_dataset",
        "created_at",
    ], products, [
        "product_name",
        "category",
        "department",
        "division",
        "source_dataset",
        "created_at",
    ]))
    lines.extend(insert_statement("orders", [
        "order_id",
        "user_id",
        "product_id",
        "product_name",
        "order_status",
        "paid_amount",
        "paid_at",
        "delivered_at",
        "aftersale_deadline",
    ], orders, [
        "user_id",
        "product_id",
        "product_name",
        "order_status",
        "paid_amount",
        "paid_at",
        "delivered_at",
        "aftersale_deadline",
    ]))
    lines.extend(insert_statement("order_items", [
        "order_item_id",
        "order_id",
        "product_id",
        "product_name",
        "quantity",
        "unit_price",
        "category",
        "created_at",
    ], items, [
        "order_id",
        "product_id",
        "product_name",
        "quantity",
        "unit_price",
        "category",
        "created_at",
    ]))
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def insert_statement(
    table: str,
    columns: list[str],
    rows: list[dict[str, str]],
    update_columns: list[str],
) -> list[str]:
    if not rows:
        return []
    lines = [f"INSERT INTO {table} (", "    " + ", ".join(columns), ") VALUES"]
    values = []
    for row in rows:
        values.append("    (" + ", ".join(sql_value(row.get(column, "")) for column in columns) + ")")
    lines.append(",\n".join(values))
    lines.append("ON DUPLICATE KEY UPDATE")
    lines.append(",\n".join(f"    {column} = VALUES({column})" for column in update_columns) + ";")
    lines.append("")
    return lines


def write_jsonl(path: Path, cases: list[dict[str, object]]) -> None:
    with path.open("w", encoding="utf-8") as handle:
        for case in cases:
            handle.write(json.dumps(case, ensure_ascii=False) + "\n")


def normalize_order_status(value: str) -> str:
    status = clean_text(value).lower()
    mapping = {
        "delivered": "DELIVERED",
        "shipped": "SHIPPED",
        "canceled": "CANCELLED",
        "cancelled": "CANCELLED",
        "approved": "PAID",
        "invoiced": "PAID",
        "processing": "PAID",
        "created": "CREATED",
        "unavailable": "CANCELLED",
    }
    return mapping.get(status, "PAID")


def aftersale_deadline(delivered_at: str, estimated_at: str, paid_at: str) -> str:
    base = parse_timestamp(delivered_at)
    if base:
        return format_timestamp(base + timedelta(days=7))
    if estimated_at:
        return estimated_at
    paid = parse_timestamp(paid_at)
    if paid:
        return format_timestamp(paid + timedelta(days=30))
    return "2026-06-01 00:00:00.000000"


def mysql_timestamp(value: str | None) -> str:
    parsed = parse_timestamp(value)
    return format_timestamp(parsed) if parsed else ""


def parse_timestamp(value: str | None) -> datetime | None:
    value = clean_text(value)
    if not value:
        return None
    if re.fullmatch(r"\d+(\.\d+)?", value):
        return excel_serial_to_datetime(float(value))
    for pattern in ("%Y-%m-%d %H:%M:%S", "%Y-%m-%dT%H:%M:%S", "%Y-%m-%d"):
        try:
            return datetime.strptime(value[:19] if "T" in pattern else value[:10 if pattern == "%Y-%m-%d" else 19], pattern)
        except ValueError:
            continue
    return None


def excel_serial_to_datetime(value: float) -> datetime:
    return datetime(1899, 12, 30) + timedelta(days=value)


def format_timestamp(value: datetime) -> str:
    return value.strftime("%Y-%m-%d %H:%M:%S") + ".000000"


def sql_value(value: str) -> str:
    if value is None or value == "":
        return "NULL"
    return "'" + str(value).replace("'", "''") + "'"


def clean_text(value: object) -> str:
    return str(value).replace("\ufeff", "").strip() if value is not None else ""


def truncate_text(value: str, max_length: int) -> str:
    value = clean_text(value).replace("\r", " ").replace("\n", " ")
    return value[:max_length]


def safe_identifier(value: str) -> str:
    cleaned = re.sub(r"[^0-9A-Za-z_-]+", "-", clean_text(value))
    return cleaned.strip("-") or "UNKNOWN"


def truncate_id(value: str, prefix: str) -> str:
    value = safe_identifier(value)
    if len(value) <= 64:
        return value
    digest = hashlib.sha1(value.encode("utf-8")).hexdigest()[:12]
    return f"{prefix}-{digest}"


def to_int(value: str) -> int:
    try:
        return int(float(clean_text(value)))
    except ValueError:
        return 0


if __name__ == "__main__":
    raise SystemExit(main())
