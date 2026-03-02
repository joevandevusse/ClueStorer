"""
Topic consolidation pipeline — Phase 2b.

The initial normalize_categories.py run produced ~562 canonical topics due to
LLM inconsistency across batches. This script collapses those into a tight
~40-topic taxonomy via a two-pass approach:

  Pass 1: Feed all 562 existing topics to Claude → generate a clean taxonomy
  Pass 2: Map each of the 562 topics to the new taxonomy (batched)
  Pass 3: UPDATE category_mappings in place with the consolidated values

Run with --dry-run to preview mappings without writing to the DB.

Requirements:
    pip install anthropic psycopg2-binary tqdm

Environment variables:
    DB_URL, DB_USER, DB_PASSWORD
    ANTHROPIC_API_KEY
"""

import os
import sys
import json
import re
import time
import psycopg2
import anthropic
from tqdm import tqdm

# ---------------------------------------------------------------------------
# Config
# ---------------------------------------------------------------------------
TARGET_TAXONOMY_SIZE = 40
BATCH_SIZE           = 50        # topics per LLM call in pass 2
MODEL                = "claude-haiku-4-5-20251001"
TAXONOMY_FILE        = os.path.join(os.path.dirname(__file__), "consolidation_taxonomy.json")
DRY_RUN              = "--dry-run" in sys.argv

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------
def parse_json(text):
    text = text.strip()
    text = re.sub(r"^```(?:json)?\s*", "", text, flags=re.MULTILINE)
    text = re.sub(r"```\s*$",           "", text, flags=re.MULTILINE)
    text = text.strip()
    for open_char, close_char in [("[", "]"), ("{", "}")]:
        start = text.find(open_char)
        end   = text.rfind(close_char)
        if start != -1 and end != -1 and end > start:
            try:
                return json.loads(text[start : end + 1])
            except json.JSONDecodeError:
                pass
    return json.loads(text)

# ---------------------------------------------------------------------------
# Connect
# ---------------------------------------------------------------------------
db_host = os.environ["DB_URL"]
db_user = os.environ["DB_USER"]
db_pass = os.environ["DB_PASSWORD"]

conn   = psycopg2.connect(f"postgresql://{db_user}:{db_pass}@{db_host}")
cursor = conn.cursor()
client = anthropic.Anthropic()
print("Connected." + (" (DRY RUN — no DB writes)" if DRY_RUN else ""))

# ---------------------------------------------------------------------------
# Load the 562 existing canonical topics
# ---------------------------------------------------------------------------
cursor.execute("SELECT DISTINCT canonical_topic FROM category_mappings ORDER BY canonical_topic")
existing_topics = [row[0] for row in cursor.fetchall()]
print(f"\nFound {len(existing_topics)} existing canonical topics to consolidate.")

# ---------------------------------------------------------------------------
# Pass 1: Load saved taxonomy or generate a new one
# ---------------------------------------------------------------------------
if os.path.exists(TAXONOMY_FILE):
    with open(TAXONOMY_FILE) as f:
        taxonomy = json.load(f)
    print(f"Loaded existing consolidation taxonomy ({len(taxonomy)} topics) from {TAXONOMY_FILE}.")
else:
    print(f"\nGenerating a {TARGET_TAXONOMY_SIZE}-topic taxonomy from existing topics...")
    taxonomy_prompt = f"""Below are {len(existing_topics)} canonical topic labels that currently exist in a Jeopardy! study database. Many are near-duplicates or overly granular (e.g. "Advertising", "Advertising & Branding", "Advertising & Marketing" should all become one topic).

Existing topics:
{chr(10).join(f"- {t}" for t in existing_topics)}

Define a clean taxonomy of exactly {TARGET_TAXONOMY_SIZE} canonical topic labels that:
- Collapses near-duplicates and overly granular topics into single broad labels
- Covers the full breadth of Jeopardy content
- Uses clear, human-readable names suitable for a study app
- Includes "Wordplay & Puzzles" for format-driven categories (Before & After, Rhymes With, etc.)
- Includes "Other" as a catch-all for anything that doesn't fit

Return a JSON array of exactly {TARGET_TAXONOMY_SIZE} strings only. No explanation."""

    response = client.messages.create(
        model=MODEL,
        max_tokens=1024,
        messages=[{"role": "user", "content": taxonomy_prompt}]
    )
    taxonomy = parse_json(response.content[0].text)

    with open(TAXONOMY_FILE, "w") as f:
        json.dump(taxonomy, f, indent=2)
    print(f"Taxonomy saved to {TAXONOMY_FILE}.")

print(f"\nNew taxonomy ({len(taxonomy)} topics):")
for t in taxonomy:
    print(f"  - {t}")

# ---------------------------------------------------------------------------
# Pass 2: Map each of the 562 existing topics → new taxonomy
# ---------------------------------------------------------------------------
print(f"\nMapping {len(existing_topics)} topics to new taxonomy (batch size {BATCH_SIZE})...")

taxonomy_str   = json.dumps(taxonomy)
mapping        = {}   # old_topic -> new_topic
failed_batches = 0

for i in tqdm(range(0, len(existing_topics), BATCH_SIZE)):
    batch = existing_topics[i : i + BATCH_SIZE]
    numbered = "\n".join(f"{j + 1}. {t}" for j, t in enumerate(batch))

    prompt = f"""New canonical topics: {taxonomy_str}

For each numbered topic below, return the single best matching topic from the new canonical list above.
Collapse near-duplicates into one label. Return a JSON array of exactly {len(batch)} strings in the same order.
Use "Other" only if truly nothing fits. No explanation, just the JSON array.

{numbered}"""

    success = False
    for attempt in range(2):
        try:
            response = client.messages.create(
                model=MODEL,
                max_tokens=2048,
                messages=[{"role": "user", "content": prompt}]
            )
            labels = parse_json(response.content[0].text)
            for old, new in zip(batch, labels):
                mapping[old] = new
            success = True
            break
        except Exception as e:
            if attempt == 0:
                time.sleep(1)
            else:
                failed_batches += 1
                tqdm.write(f"Batch {i} failed after retry: {e}")

    if success:
        time.sleep(0.05)

print(f"\nMapping complete. {failed_batches} batches failed.")

# ---------------------------------------------------------------------------
# Preview: show consolidations (old → new, only where changed)
# ---------------------------------------------------------------------------
changed = {old: new for old, new in mapping.items() if old != new}
print(f"\n{len(changed)} topics will be remapped. Sample:")
for old, new in list(changed.items())[:20]:
    print(f"  '{old}'  →  '{new}'")
if len(changed) > 20:
    print(f"  ... and {len(changed) - 20} more")

# ---------------------------------------------------------------------------
# Pass 3: UPDATE category_mappings in place
# ---------------------------------------------------------------------------
if DRY_RUN:
    print("\nDry run — skipping DB update.")
else:
    print(f"\nUpdating category_mappings for {len(changed)} remapped topics...")
    for old_topic, new_topic in tqdm(changed.items()):
        cursor.execute(
            "UPDATE category_mappings SET canonical_topic = %s WHERE canonical_topic = %s",
            (new_topic, old_topic)
        )
    conn.commit()
    print("Done.")

    # Verify
    cursor.execute("SELECT COUNT(DISTINCT canonical_topic) FROM category_mappings")
    final_count = cursor.fetchone()[0]
    print(f"\nFinal distinct topic count: {final_count}")

cursor.close()
conn.close()
