# EXEC_PLAN_V3_DEMO_DATASET_ENRICHMENT

Status: completed

Completion Date: 2026-05-18

## Goal

Enrich the local demo dataset foundation with product and order-item seed support, plus a reproducible local cleaning
script for three public datasets, without changing Agent execution semantics or default offline tests.

## What Completed

- Added MySQL `products` and `order_items` tables.
- Added minimal `products` and `order_items` rows to the base MySQL seed.
- Added `data/raw` and `data/generated` documentation.
- Ignored raw public dataset files under `data/raw`.
- Added `scripts/data/build_demo_seed.py` using only the Python standard library.
- Generated optional `data/generated/demo_seed_extra.sql`.
- Generated optional `data/generated/demo_evaluation_cases.jsonl`.
- Added `docs/data/DATASET_MAPPING.md`.
- Added harness tests for schema, seed, script, ignore rules, and mapping docs.
- Updated README, `EXEC_PLAN_V3.md`, and quality score docs.

## Dataset Mapping

Order dataset:

- `order_id` maps to `orders.order_id`.
- `customer_id` maps to `orders.user_id`.
- `order_status` is normalized to existing project status values.
- `order_approved_at` maps to `orders.paid_at`.
- `order_delivered_customer_date` maps to `orders.delivered_at`.
- `order_estimated_delivery_date` is used as an aftersale deadline fallback.
- `timeout` is not stored directly because the current `orders` table has no logistics abnormal field.

Chinese review dataset:

- `cat` is kept as generated case metadata.
- `label=0` is treated as negative after-sale sample signal.
- `label=1` may generate a small number of ordinary consultation samples.
- `review` is truncated for optional generated evaluation cases.

Women's clothing feedback dataset:

- `Clothing ID` maps to `products.product_id`.
- `Title` maps to product name, with category fallback.
- `Class Name`, `Department Name`, and `Division Name` map to product metadata.
- `Rating <= 2` or `Recommended IND = 0` is treated as negative after-sale sample signal.
- `Review Text` may become optional generated case input.
- `Age` is intentionally not used.

## Current System Run Modes

- Default in-memory startup remains available without external data.
- Explicit MySQL profile can use the base schema and seed without generated files.
- Optional generated SQL can be imported after base MySQL schema and seed.
- Default `mvn test` does not require `data/raw`, MySQL, Docker, real LLMs, API keys, or external network.

## Validation Commands

```bash
python scripts/data/build_demo_seed.py --help
python scripts/data/build_demo_seed.py
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

## Known Limitations

- Generated data is demo enrichment, not production truth.
- `products` and `order_items` currently enrich MySQL seed data but do not change Agent tool behavior.
- Optional generated evaluation cases are not part of the curated default Java evaluation dataset.
- XLSX parsing is intentionally basic and intended for simple worksheet exports.
- Raw datasets are not committed and must be supplied locally.

## Follow-ups

- Consider a separate plan to add opt-in import validation against a local MySQL instance.
- Consider a separate plan to evolve order tools to expose order items if the product workflow needs it.
- Consider a separate plan to curate generated cases into the official evaluation dataset after review.

## Completion Signal

TASK_COMPLETE
