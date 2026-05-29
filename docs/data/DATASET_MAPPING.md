# Dataset Mapping

## Purpose

V3.5 enriches demo data with public order and review datasets while keeping the default application path deterministic
and offline. Raw source files stay local in a gitignored raw dataset directory; the repository may commit only small generated demo artifacts
under `data/generated`.

## Source Datasets

1. E-commerce order dataset
   - Used to generate additional `orders` rows and `order_items` links.
   - Expected fields: `order_id`, `customer_id`, `order_status`, `order_purchase_timestamp`, `order_approved_at`,
     `order_delivered_carrier_date`, `order_delivered_customer_date`, `order_estimated_delivery_date`, `timeout`.

2. Chinese e-commerce review dataset
   - Used to generate optional evaluation cases and after-sale ticket messages.
   - Expected fields: `cat`, `label`, `review`.
   - `label=0` is treated as a negative after-sale sample candidate. `label=1` may produce a small number of ordinary
     consultation samples.

3. Women's clothing feedback dataset
   - Treated as the same source as the women's clothing review dataset.
   - Used to generate `products`, `order_items`, optional ticket/evaluation samples, and clothing category metadata.
   - Expected fields: `Clothing ID`, `Age`, `Title`, `Review Text`, `Rating`, `Recommended IND`,
     `Positive Feedback Count`, `Division Name`, `Department Name`, `Class Name`.

## Field Mapping

Order dataset to `orders`:

| Source field | Project field | Rule |
| --- | --- | --- |
| `order_id` | `orders.order_id` | Kept as source ID. |
| `customer_id` | `orders.user_id` | Preserved as demo user ID. |
| `order_status` | `orders.order_status` | Normalized to project enum values. |
| `order_purchase_timestamp` | `orders.created_at` concept | Current `orders` table has no `created_at`; script uses it as fallback for `paid_at`. |
| `order_approved_at` | `orders.paid_at` | Falls back to purchase timestamp when missing. |
| `order_delivered_customer_date` | `orders.delivered_at` | May be `NULL`. |
| `order_estimated_delivery_date` | `orders.aftersale_deadline` fallback | Used when delivered date is missing. |
| `timeout` | not stored directly | May influence generated notes later; current schema does not force a logistics-abnormal column. |

Order status normalization:

| Source status | Project status |
| --- | --- |
| `delivered` | `DELIVERED` |
| `shipped` | `SHIPPED` |
| `canceled`, `cancelled` | `CANCELLED` |
| `approved`, `invoiced`, `processing` | `PAID` |
| `created` | `CREATED` |
| `unavailable` | `CANCELLED` |
| other or blank | `PAID` |

Aftersale deadline:

- If `delivered_at` is present, `aftersale_deadline = delivered_at + 7 days`.
- If `delivered_at` is missing, the script uses `order_estimated_delivery_date` when present.
- If both are missing, the script uses `paid_at + 30 days` to satisfy the current non-null field.

Clothing feedback dataset to `products` / `order_items` / optional cases:

| Source field | Project field | Rule |
| --- | --- | --- |
| `Clothing ID` | `products.product_id` | Prefixed as `P-CLOTHING-{id}`. |
| `Title` | `products.product_name` | Falls back to `Class Name` or generated name when blank. |
| `Class Name` | `products.category`, `order_items.category` | Preserved as product category. |
| `Department Name` | `products.department` | Preserved as demo product metadata. |
| `Division Name` | `products.division` | Preserved as demo product metadata. |
| `Rating` | optional case signal | `Rating <= 2` is negative after-sale signal. |
| `Recommended IND` | optional case signal | `0` is negative after-sale signal. |
| `Review Text` | optional ticket/evaluation input | Truncated in generated optional cases. |
| `Age` | not used | Not stored to avoid building user profiles. |

Chinese review dataset to optional ticket/evaluation cases:

| Source field | Project field | Rule |
| --- | --- | --- |
| `cat` | `sourceCategory` / product category hint | Used only in generated case metadata. |
| `label` | sentiment signal | `0` favors after-sale problem samples; `1` is sampled sparingly as consultation. |
| `review` | ticket message / evaluation input | Truncated in generated optional cases. |

## Cleaning Rules

- Use UTF-8 or UTF-8-SIG CSV decoding.
- Support column names with spaces, such as `Clothing ID` and `Review Text`.
- Escape single quotes in SQL with doubled single quotes.
- Keep generated SQL deterministic and small by default.
- Do not write personal paths, credentials, raw long comments, or private data into generated docs.
- Do not use `Age` for user profile, segmentation, ranking, or personalization.

## Scale Guidance

Default script limits are intentionally small:

```text
--max-orders 50
--max-products 50
--max-order-items 150
--max-tickets 50
--max-evaluation-cases 20
```

Larger local demos can raise limits explicitly. Keep committed generated files reviewable.

## Run Script

```bash
python scripts/data/build_demo_seed.py
```

Optional arguments:

```bash
python scripts/data/build_demo_seed.py \
  --max-orders 1000 \
  --max-products 500 \
  --max-order-items 3000 \
  --max-tickets 500 \
  --max-evaluation-cases 100
```

The script scans:

```text
local raw dataset directory for orders
local raw dataset directory for Chinese reviews
local raw dataset directory for clothing reviews
```

It writes:

```text
data/generated/demo_seed_extra.sql
data/generated/demo_evaluation_cases.jsonl
```

## Import Generated SQL

For local MySQL profile enrichment, import base schema and seed first, then generated seed:

```bash
mysql --default-character-set=utf8mb4 -u <user> -p after_sale_agent < src/main/resources/schema-mysql.sql
mysql --default-character-set=utf8mb4 -u <user> -p after_sale_agent < src/main/resources/data-mysql.sql
mysql --default-character-set=utf8mb4 -u <user> -p after_sale_agent < data/generated/demo_seed_extra.sql
```

Do not store real passwords in shell history, README, docs, or committed config.

## Why Raw Files Are Not Committed

Raw datasets are large, externally versioned, and may have license or redistribution constraints. Keeping them out of
Git prevents repository bloat and avoids mixing public raw data with project source code.

## Current Limitations

- Generated data is demo data, not production truth.
- V3.6 exposes available `products` and `order_items` data through the order query tool output, but it remains demo
  data and does not connect to a production order center.
- `get_user_orders` remains a lightweight lookup even if the shared mapper includes item details.
- XLSX support in the script covers basic worksheet strings and inline strings; CSV is preferred for repeatable local
  generation.
- Optional generated evaluation cases are not part of the default Java evaluation dataset.
