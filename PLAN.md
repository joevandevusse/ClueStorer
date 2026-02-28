# **Design Document: "Moneyball" Trivia Engine**

**Project Lead:** Joe

**Primary Goal:** Build a data-driven trivia training platform to identify knowledge gaps, optimize study time via statistical probability, and improve recall speed for competitive trivia (and secure future wins for Joey & the Pussycats). **Existing Foundation:** To be integrated with or modeled after the existing `jeopardy-react` repository.

[Roger Craig’s Video](https://vimeo.com/29001512?&login=true&__cf_chl_tk=haYCnESp6mL_Eo8Wj8ok.2L0CkNkp7FyZN7TKnUpOPo-1771993727-1.0.1.1-qmop0CRZPXWAiI0oUtcuuo2QoJGGr7A3l_.K41F2HJw)

## **1\. System Architecture & Tech Stack**

* **Frontend:** React.js (leveraging existing `jeopardy-react` patterns)  
* **Backend/Database:** Google Firebase/Firestore (NoSQL document store for rapid read/writes of JSON clues and user stats)  
* **Data Processing (NLP):** Python (spaCy/NLTK for text processing, scikit-learn for K-Means clustering)  
* **Data Visualization:** D3.js or Visx (for the interactive bubble chart)

## **2\. Project Phases & Milestones**

### **Phase 1: Data Acquisition & ETL Pipeline**

* **Objective:** Secure the raw data and push it to a scalable database.  
* **Tasks:**  
  * Download the open-source `jwolle1/jeopardy_clue_dataset` (538k+ clues).  
  * Write a Python script to clean the JSON/CSV file, stripping out broken links, video/audio clues, and formatting errors.  
  * Configure a Firebase Firestore database.  
  * Batch upload the cleaned dataset into a `Clues` collection in Firestore.

### **Phase 2: NLP & Text Clustering (The Brain)**

* **Objective:** Categorize the raw clues into distinct, semantic study topics rather than relying on the show's arbitrary categories.  
* **Tasks:**  
  * Process the text clues using TF-IDF vectorization or SentenceBERT to capture keyword significance.  
  * Run a K-Means clustering algorithm to group the clues into mathematically similar buckets.  
  * Assign descriptive labels to the top clusters (e.g., "19th Century Literature," "World Capitals").  
  * Update the Firestore `Clues` documents with their new cluster assignments and calculate the mean dollar value for each cluster.

### **Phase 3: The Game Interface (The Web App)**

* **Objective:** Build the UI to study the categorized data and enforce the 3-second recall rule.  
* **Tasks:**  
  * Create a "Study Mode" component in React that queries Firestore for specific clue clusters.  
  * Implement a strict 3-second countdown timer using React hooks (`useEffect`, `useRef`).  
  * Build the state management to log a pass/fail. If the timer hits zero before the answer is revealed, it registers as a failure.  
  * Push the user's pass/fail results to a `UserStats` collection in Firebase, updating the personal accuracy percentage for that specific cluster.

### **Phase 4: Data Visualization (The Map)**

* **Objective:** Render an interactive X/Y bubble chart to expose high-value knowledge gaps.  
* **Tasks:**  
  * Integrate D3.js or Visx into the React frontend.  
  * Map the axes:  
    * **X-Axis:** Cluster Mean Dollar Value (from Phase 2).  
    * **Y-Axis:** User Accuracy % (from Phase 3).  
    * **Bubble Radius:** Total volume of questions in that cluster.  
  * Implement click-events on the bubbles so clicking a weak category immediately launches a Study Mode session for those specific clues.

---

As your PM, my recommendation for Sprint 1 is to lock down the data source. Would you like to start by writing the Python script to clean and parse the open-source dataset, or would you rather sketch out the Firebase schema first?