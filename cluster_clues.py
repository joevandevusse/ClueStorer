"""
Phase 2: NLP clustering pipeline.

Reads clues from PostgreSQL, clusters them by topic using TF-IDF + K-Means,
and writes the results to the cluster_assignments table.

Requirements:
    pip install psycopg2-binary pandas scikit-learn

Environment variables (same as the Java loaders):
    DB_URL      e.g. localhost/jeopardy
    DB_USER
    DB_PASSWORD
"""

import os
import psycopg2
import pandas as pd
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.cluster import KMeans

# ---------------------------------------------------------------------------
# Config
# ---------------------------------------------------------------------------
NUM_CLUSTERS = 50      # Broad starting point — tune up or down as needed
TOP_WORDS    = 3       # Words used to auto-label each cluster
MIN_DATE     = None    # Optional: filter by date e.g. "2000-01-01"

# ---------------------------------------------------------------------------
# Connect
# ---------------------------------------------------------------------------
db_host = os.environ.get("DB_URL", "localhost/jeopardy")
db_user = os.environ.get("DB_USER")
db_pass = os.environ.get("DB_PASSWORD")

conn = psycopg2.connect(f"postgresql://{db_user}:{db_pass}@{db_host}")
print("Connected to database.")

# ---------------------------------------------------------------------------
# Load clues
# ---------------------------------------------------------------------------
date_filter = f"AND game_date >= '{MIN_DATE}'" if MIN_DATE else ""
query = f"""
    SELECT id, question
    FROM clues_java
    WHERE question IS NOT NULL
    {date_filter}
"""
print("Loading clues...")
df = pd.read_sql_query(query, conn)
print(f"Loaded {len(df):,} clues.")

# ---------------------------------------------------------------------------
# Vectorize
# ---------------------------------------------------------------------------
print("Vectorizing text...")
vectorizer = TfidfVectorizer(stop_words="english", max_features=5000)
X = vectorizer.fit_transform(df["question"])

# ---------------------------------------------------------------------------
# Cluster
# ---------------------------------------------------------------------------
print(f"Running K-Means with {NUM_CLUSTERS} clusters...")
kmeans = KMeans(n_clusters=NUM_CLUSTERS, random_state=42, n_init="auto")
df["cluster_id"] = kmeans.fit_predict(X)

# ---------------------------------------------------------------------------
# Auto-label each cluster from its top centroid words
# ---------------------------------------------------------------------------
print("Labelling clusters...")
order_centroids = kmeans.cluster_centers_.argsort()[:, ::-1]
terms = vectorizer.get_feature_names_out()

cluster_labels = {}
for i in range(NUM_CLUSTERS):
    top_words = [terms[ind] for ind in order_centroids[i, :TOP_WORDS]]
    cluster_labels[i] = " | ".join(top_words).title()
    print(f"  Cluster {i:>2}: {cluster_labels[i]}")

df["cluster_label"] = df["cluster_id"].map(cluster_labels)

# ---------------------------------------------------------------------------
# Write to cluster_assignments (replace previous run)
# ---------------------------------------------------------------------------
print("Writing to cluster_assignments...")
cursor = conn.cursor()
cursor.execute("TRUNCATE cluster_assignments")

insert_data = list(zip(df["id"], df["cluster_id"], df["cluster_label"]))
cursor.executemany(
    "INSERT INTO cluster_assignments (clue_id, cluster_id, cluster_label) VALUES (%s, %s, %s)",
    insert_data
)

conn.commit()
conn.close()
print(f"Done. {len(df):,} rows written to cluster_assignments.")
