# Phase 4: Knowledge Map (Interactive Bubble Chart)

## Context
The user has accumulated enough stats data (425 rows, 23/46 topics with 5+ data points) to build a meaningful visualization. Phase 4 of the Moneyball Trivia Engine renders an interactive bubble chart that exposes high-value knowledge gaps — topics with high clue dollar value but low accuracy. Clicking a bubble launches a study session for that topic.

---

## 1. Backend — ClueApi

### 1a. New DTO: `BubblePointDto.java`
**Path:** `ClueApi/src/main/java/org/clueapi/model/BubblePointDto.java`

```java
public record BubblePointDto(
    String canonicalTopic,
    long   clueCount,
    double meanValue,
    long   attemptCount,
    Double accuracy       // null when attemptCount == 0
) {}
```
Follow the existing record pattern (no `@JsonProperty` needed — Jackson serializes camelCase by default).

### 1b. New method in `StatsResource.java`
**Path:** `ClueApi/src/main/java/org/clueapi/resource/StatsResource.java`

Add `getBubble(Context ctx)` alongside the existing `record()` method:

```java
public void getBubble(Context ctx) throws Exception {
    String sql = """
        SELECT
          t.canonical_topic,
          t.clue_count,
          t.mean_value,
          COALESCE(s.attempt_count, 0) AS attempt_count,
          s.accuracy
        FROM (
          SELECT
            cm.canonical_topic,
            COUNT(*) AS clue_count,
            AVG(
              CASE
                WHEN c.clue_value ~ '^\\$[0-9,]+$'
                THEN CAST(REPLACE(REPLACE(c.clue_value, '$', ''), ',', '') AS INTEGER)
                ELSE NULL
              END
            ) AS mean_value
          FROM category_mappings cm
          JOIN clues_java c ON c.category = cm.jeopardy_category
          GROUP BY cm.canonical_topic
        ) t
        LEFT JOIN (
          SELECT canonical_topic,
                 COUNT(*) AS attempt_count,
                 AVG(CASE WHEN passed THEN 1.0 ELSE 0.0 END) AS accuracy
          FROM user_stats
          GROUP BY canonical_topic
        ) s ON s.canonical_topic = t.canonical_topic
        ORDER BY t.canonical_topic
        """;

    List<BubblePointDto> points = new ArrayList<>();
    try (Connection conn = db.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
            long attempts = rs.getLong("attempt_count");
            Double accuracy = attempts > 0 ? rs.getDouble("accuracy") : null;
            points.add(new BubblePointDto(
                rs.getString("canonical_topic"),
                rs.getLong("clue_count"),
                rs.getDouble("mean_value"),
                attempts,
                accuracy
            ));
        }
    }
    ctx.json(points);
}
```

### 1c. Register route in `ClueApi.java`
**Path:** `ClueApi/src/main/java/org/clueapi/ClueApi.java`

Add alongside existing routes:
```java
.get("/api/stats/bubble", stats::getBubble)
```

---

## 2. Frontend — clue-study

### 2a. Install D3
```
npm install d3
npm install --save-dev @types/d3
```

### 2b. `types/index.ts` — add `BubblePoint`
```typescript
export interface BubblePoint {
  canonicalTopic: string;
  clueCount:      number;
  meanValue:      number;
  attemptCount:   number;
  accuracy:       number | null;  // null = no data yet
}
```

### 2c. `api/client.ts` — add `fetchBubbleData()`
```typescript
export async function fetchBubbleData(): Promise<BubblePoint[]> {
  const res = await fetch(`${BASE}/api/stats/bubble`);
  if (!res.ok) throw new Error('Failed to fetch bubble data');
  return res.json();
}
```

### 2d. `App.tsx` — add third view state
Add `showMap` boolean alongside `config`:
```tsx
const [config, setConfig]   = useState<StudyConfig | null>(null);
const [showMap, setShowMap] = useState(false);

// Render priority: StudyMode > BubbleChart > TopicPicker
if (config) return <StudyMode ... onExit={() => setConfig(null)} />;

if (showMap) return (
  <BubbleChart
    onExit={() => setShowMap(false)}
    onStudy={(topic) => { setShowMap(false); setConfig({ topic, fromDate: null }); }}
  />
);

return <TopicPicker onSelect={setConfig} onShowMap={() => setShowMap(true)} />;
```

### 2e. `TopicPicker.tsx` — add Knowledge Map button
Add `onShowMap: () => void` to Props, add a secondary button below "Start Studying":
```tsx
<button className="btn-ghost btn-map" onClick={onShowMap}>
  Knowledge Map
</button>
```
Style: uses existing `btn-ghost` class; add `btn-map` for width/margin tweaks in `TopicPicker.css`.

### 2f. New `BubbleChart.tsx`
**Path:** `clue-study/src/components/BubbleChart.tsx`

Key implementation details:
- Fetches data on mount via `fetchBubbleData()`
- SVG rendered via `useRef`, D3 manipulates it imperatively inside `useEffect`
- **D3 force simulation:**
  - `forceX` → targets `xScale(d.meanValue)` with strength 0.5
  - `forceY` → targets `yScale(d.accuracy ?? 0.5)` with strength 0.5 (no-data topics float to vertical center)
  - `forceCollide` → radius + 3px padding to prevent overlap
- **Scales:**
  - `xScale`: `scaleLinear` domain `[0, max(meanValue)]` → `[padding, width - padding]`
  - `yScale`: `scaleLinear` domain `[0, 1]` → `[height - padding, padding]` (inverted: 0% at bottom)
  - `rScale`: `scaleSqrt` domain `[0, max(clueCount)]` → `[8, 38]` (area proportional to count)
- **Colors:**
  - `accuracy === null`: `#555` (gray) — no data yet
  - `accuracy < 0.4`: `#c0392b` (red)
  - `accuracy < 0.7`: `#d4af37` (gold, var(--gold))
  - `accuracy >= 0.7`: `#27ae60` (green)
- **Axes:** X-axis at bottom (dollar labels, e.g. "$400", "$800"), Y-axis at left (percentage labels)
- **Tooltip:** React-managed `div` (absolutely positioned overlay), shown on `mouseover`, hidden on `mouseout`. Displays: topic name, accuracy or "No data yet", attempt count, clue count, mean value.
- **Click:** `d3.on('click', (_, d) => onStudy(d.canonicalTopic))`
- **Header:** `← Back` button + title "Knowledge Map"

### 2g. New `BubbleChart.css`
**Path:** `clue-study/src/components/BubbleChart.css`

- `.bubble-chart` full-height container, flex column, `background: var(--bg)`
- `.bubble-chart-header` matches `.study-header` pattern
- `.bubble-svg` flex-grow SVG, uses `width: 100%`, `height: 100%`
- `.bubble-tooltip` absolutely positioned dark card (`background: var(--surface)`, `border: 1px solid var(--gold)`), pointer-events none
- Axis text: `fill: var(--text-dim)`, small font
- No-data bubbles: `stroke-dasharray: 4 2`

---

## 3. File Change Summary

| File | Change |
|---|---|
| `ClueApi/.../model/BubblePointDto.java` | **NEW** record DTO |
| `ClueApi/.../resource/StatsResource.java` | Add `getBubble()` method |
| `ClueApi/.../ClueApi.java` | Register `GET /api/stats/bubble` |
| `clue-study/src/types/index.ts` | Add `BubblePoint` interface |
| `clue-study/src/api/client.ts` | Add `fetchBubbleData()` |
| `clue-study/src/App.tsx` | Add `showMap` state + BubbleChart branch |
| `clue-study/src/components/TopicPicker.tsx` | Add `onShowMap` prop + button |
| `clue-study/src/components/TopicPicker.css` | Style `btn-map` |
| `clue-study/src/components/BubbleChart.tsx` | **NEW** D3 bubble chart component |
| `clue-study/src/components/BubbleChart.css` | **NEW** styles |

---

## 4. Verification

1. Restart ClueApi, hit `GET http://localhost:7070/api/stats/bubble` — confirm JSON array with all 46 topics, 23 with non-null accuracy
2. `npm run dev` in clue-study, click "Knowledge Map" from home screen
3. Chart loads: bubbles render, axes labeled, no-data topics appear gray/dashed
4. Hover tooltip shows correct data
5. Click a bubble → chart closes, StudyMode opens for that topic with no era filter
