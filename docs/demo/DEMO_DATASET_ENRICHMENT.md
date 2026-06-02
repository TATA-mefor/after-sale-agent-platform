## Demo Dataset Enrichment

V3.5 adds optional demo data enrichment for public local datasets. The default app startup and default `mvn test` path
do not require these raw files.

Place downloaded raw datasets in your local gitignored raw dataset directory. Keep only local public downloads there;
do not commit raw large files, personal paths, credentials, or private customer data.

Generate small reviewable seed artifacts:

```bash
python scripts/data/build_demo_seed.py
```

Optional scale controls:

```bash
python scripts/data/build_demo_seed.py \
  --max-orders 1000 \
  --max-products 500 \
  --max-order-items 3000 \
  --max-tickets 500 \
  --max-evaluation-cases 100
```

The script writes:

```text
data/generated/demo_seed_extra.sql
data/generated/demo_evaluation_cases.jsonl
```

Import generated enrichment after the base MySQL schema and seed:

```bash
mysql --default-character-set=utf8mb4 -u <user> -p after_sale_agent < src/main/resources/schema-mysql.sql
mysql --default-character-set=utf8mb4 -u <user> -p after_sale_agent < src/main/resources/data-mysql.sql
mysql --default-character-set=utf8mb4 -u <user> -p after_sale_agent < data/generated/demo_seed_extra.sql
```

The base `data-mysql.sql` already includes minimal `products` and `order_items` rows, so the MySQL demo remains usable
even when the optional generation script is not run.

See `docs/data/DATASET_MAPPING.md` for dataset field mapping, cleaning rules, limits, and current boundaries.

V3.6 wires these product and order-item records into the order query tool output. `get_order_by_id` now returns
structured `orderItems` with product name, category, quantity, price, item status, return/exchange support flags, and
the special-item flag. The default in-memory repository also includes matching demo item data, so this behavior does
not require MySQL or generated raw datasets.

The MySQL `products` and `order_items` tables intentionally store only demo product and line-item fields. The
`supportReturn`, `supportExchange`, and `isSpecialItem` values in Java tool output are deterministic demo-rule
derivations from existing product/category fields; they are not separate MySQL columns.

