"""
Category normalization pipeline — Phase 2 (improved).

Two-pass approach:
  Pass 1: Sample categories → ask Claude to define a canonical taxonomy
  Pass 2: Batch-classify all unique categories against that taxonomy

Results stored in the category_mappings table.

Requirements:
    pip install anthropic psycopg2-binary tqdm

Environment variables (same as the Java loaders):
    DB_URL, DB_USER, DB_PASSWORD
    ANTHROPIC_API_KEY
"""

import os
import json
import re
import time
import psycopg2
import anthropic
from tqdm import tqdm

# ---------------------------------------------------------------------------
# Config
# ---------------------------------------------------------------------------
SAMPLE_SIZE   = 500  # categories sampled to build the taxonomy
TAXONOMY_SIZE = 75   # number of canonical topics to generate
BATCH_SIZE    = 30   # categories per LLM call in pass 2
MODEL         = "claude-haiku-4-5-20251001"

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------
def parse_json(text):
    """Strip markdown code fences if present, then parse JSON."""
    text = re.sub(r"^```(?:json)?\s*", "", text.strip(), flags=re.MULTILINE)
    text = re.sub(r"```\s*$", "", text.strip(), flags=re.MULTILINE)
    return json.loads(text.strip())

# ---------------------------------------------------------------------------
# Connect
# ---------------------------------------------------------------------------
db_host = os.environ["DB_URL"]
db_user = os.environ["DB_USER"]
db_pass = os.environ["DB_PASSWORD"]

conn   = psycopg2.connect(f"postgresql://{db_user}:{db_pass}@{db_host}")
cursor = conn.cursor()
client = anthropic.Anthropic()
print("Connected.")

# ---------------------------------------------------------------------------
# Pass 1: Generate taxonomy from a random sample of categories
# ---------------------------------------------------------------------------
cursor.execute(
    "SELECT DISTINCT category FROM clues_java ORDER BY RANDOM() LIMIT %s",
    (SAMPLE_SIZE,)
)
sample = [row[0] for row in cursor.fetchall()]
print(f"Sampled {len(sample)} categories. Generating taxonomy...")

taxonomy_prompt = f"""Here are {len(sample)} Jeopardy! category names sampled from a dataset of 500k+ clues:
{chr(10).join(f"- {c}" for c in sample)}

Define a taxonomy of exactly {TAXONOMY_SIZE} canonical topic labels that together cover the full breadth of Jeopardy content.

Rules:
- Labels should be clear, human-readable study topics (e.g. "U.S. Presidents", "Shakespeare", "World Geography")
- Include a "Wordplay & Puzzles" topic for format-specific categories (Before & After, Rhymes With, Starts With, etc.)
- Include an "Other" topic as a catch-all
- Return a JSON array of strings only, no explanation."""

response = client.messages.create(
    model=MODEL,
    max_tokens=1024,
    messages=[{"role": "user", "content": taxonomy_prompt}]
)
taxonomy = parse_json(response.content[0].text)
print(f"\nTaxonomy ({len(taxonomy)} topics):")
for t in taxonomy:
    print(f"  - {t}")

# ---------------------------------------------------------------------------
# Pass 2: Classify all unique categories against the taxonomy
# ---------------------------------------------------------------------------
cursor.execute("SELECT DISTINCT category FROM clues_java")
all_categories = [row[0] for row in cursor.fetchall()]
print(f"\nClassifying {len(all_categories):,} unique categories in batches of {BATCH_SIZE}...")

cursor.execute("TRUNCATE category_mappings")
conn.commit()

taxonomy_str  = json.dumps(taxonomy)
failed_batches = 0

for i in tqdm(range(0, len(all_categories), BATCH_SIZE)):
    batch      = all_categories[i : i + BATCH_SIZE]
    batch_str  = "\n".join(f"- {c}" for c in batch)

    prompt = f"""Canonical topics: {taxonomy_str}

Map each Jeopardy category below to the single best canonical topic from the list above.
Return a JSON object: keys are the exact category names, values are canonical topics from the list.
Use "Other" only if truly nothing fits. No explanation, just JSON.

{batch_str}"""

    try:
        response = client.messages.create(
            model=MODEL,
            max_tokens=2048,
            messages=[{"role": "user", "content": prompt}]
        )
        mappings = parse_json(response.content[0].text)
        data = [(k, v) for k, v in mappings.items()]
        cursor.executemany(
            """INSERT INTO category_mappings (jeopardy_category, canonical_topic)
               VALUES (%s, %s) ON CONFLICT (jeopardy_category) DO NOTHING""",
            data
        )
        conn.commit()
    except Exception as e:
        failed_batches += 1
        tqdm.write(f"Batch {i} failed: {e}")

    time.sleep(0.05)

cursor.close()
conn.close()
print(f"\nDone. {len(all_categories):,} categories processed. {failed_batches} batches failed.")
