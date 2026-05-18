# Generated Demo Data

This directory stores small, reviewable artifacts generated from local raw public datasets.

Generated files may include:

```text
demo_seed_extra.sql
demo_evaluation_cases.jsonl
```

Regenerate them with:

```bash
python scripts/data/build_demo_seed.py
```

Generated SQL is optional enrichment for the explicit MySQL profile. The default in-memory startup and default
`mvn test` path do not depend on these generated files or on `data/raw`.

