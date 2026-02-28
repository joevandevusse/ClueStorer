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
SAMPLE_SIZE    = 500             # categories sampled to build the taxonomy
TAXONOMY_SIZE  = 75              # number of canonical topics to generate
BATCH_SIZE     = 30              # categories per LLM call in pass 2
MODEL          = "claude-haiku-4-5-20251001"
TAXONOMY_FILE  = os.path.join(os.path.dirname(__file__), "taxonomy.json")

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------
def parse_json(text):
    """
    Robustly extract and parse JSON from LLM output.
    Handles markdown code fences and extra text before/after the JSON.
    """
    text = text.strip()
    # Strip markdown code fences
    text = re.sub(r"^```(?:json)?\s*", "", text, flags=re.MULTILINE)
    text = re.sub(r"```\s*$", "", text, flags=re.MULTILINE)
    text = text.strip()

    # If there's extra text around the JSON, extract the outermost [ ] or { }
    for open_char, close_char in [("[", "]"), ("{", "}")]:
        start = text.find(open_char)
        end   = text.rfind(close_char)
        if start != -1 and end != -1 and end > start:
            try:
                return json.loads(text[start : end + 1])
            except json.JSONDecodeError:
                pass

    return json.loads(text)  # last resort — will raise if truly malformed

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
# Pass 1: Load saved taxonomy or generate a new one
# ---------------------------------------------------------------------------
if os.path.exists(TAXONOMY_FILE):
    with open(TAXONOMY_FILE) as f:
        taxonomy = json.load(f)
    print(f"Loaded existing taxonomy ({len(taxonomy)} topics) from {TAXONOMY_FILE}.")
else:
    cursor.execute(
        "SELECT category FROM (SELECT DISTINCT category FROM clues_java) c ORDER BY RANDOM() LIMIT %s",
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

    with open(TAXONOMY_FILE, "w") as f:
        json.dump(taxonomy, f, indent=2)
    print(f"Taxonomy saved to {TAXONOMY_FILE}.")

print(f"\nTaxonomy ({len(taxonomy)} topics):")
for t in taxonomy:
    print(f"  - {t}")

# ---------------------------------------------------------------------------
# Pass 2: Classify all unique categories against the taxonomy
# ---------------------------------------------------------------------------
# Load all categories, skipping any already mapped (resume support)
cursor.execute("SELECT DISTINCT category FROM clues_java")
all_categories = [row[0] for row in cursor.fetchall()]

cursor.execute("SELECT jeopardy_category FROM category_mappings")
already_mapped = {row[0] for row in cursor.fetchall()}
remaining = [c for c in all_categories if c not in already_mapped]

print(f"\n{len(all_categories):,} unique categories total, "
      f"{len(already_mapped):,} already mapped, "
      f"{len(remaining):,} remaining.")

taxonomy_str  = json.dumps(taxonomy)
failed_batches = 0

for i in tqdm(range(0, len(remaining), BATCH_SIZE)):
    batch = remaining[i : i + BATCH_SIZE]

    # Use a numbered list so special characters in category names
    # never end up as JSON keys (avoids backslash escape failures)
    numbered = "\n".join(f"{j + 1}. {c}" for j, c in enumerate(batch))

    prompt = f"""Canonical topics: {taxonomy_str}

For each numbered Jeopardy category below, return the single best canonical topic from the list above.
Return a JSON array of exactly {len(batch)} strings in the same order as the input.
Use "Other" only if truly nothing fits. No explanation, just the JSON array.

{numbered}"""

    success = False
    for attempt in range(2):  # try twice before giving up
        try:
            response = client.messages.create(
                model=MODEL,
                max_tokens=2048,
                messages=[{"role": "user", "content": prompt}]
            )
            labels = parse_json(response.content[0].text)
            data = [(cat, label) for cat, label in zip(batch, labels)]
            cursor.executemany(
                """INSERT INTO category_mappings (jeopardy_category, canonical_topic)
                   VALUES (%s, %s) ON CONFLICT (jeopardy_category) DO NOTHING""",
                data
            )
            conn.commit()
            success = True
            break
        except Exception as e:
            if attempt == 0:
                time.sleep(1)  # brief pause before retry
            else:
                failed_batches += 1
                tqdm.write(f"Batch {i} failed after retry: {e}")

    if success:
        time.sleep(0.05)

cursor.close()
conn.close()
print(f"\nDone. {len(remaining):,} categories processed. {failed_batches} batches failed.")
