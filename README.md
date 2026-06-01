# trakie 

trakie is a beautiful, private, and fully offline-first personal dashboard built with Jetpack Compose, Material Design 3, and Kotlin Coroutines. It blends real-time activity logging, Notion-style document drafting, custom reminder sequences, and rich local telemetry charts into one unified application. 

---

## 🎨 Design Philosophy & Visuals

- **Modern Slate & Grayscale Identity**: Supports system dark theme, Google's Material 3 Dynamic colors, and a brutalist **High-Contrast Monochrome Grayscale Mode** for a focused, distraction-free environment.
- **Micro-interactions & Visual Pace**: Smooth rotary and fade animations, infinite loading waves for active tracking, custom high-contrast notes highlighter schemes, and beautiful negative-spaced vector iconography.

### 📱 Screenshots

<p align="center">
  <img src="assets/.aistudio/Screenshot%202026-06-01%20183648.png" width="220" alt="Activity Tracking Screen" style="margin: 10px;" />
  <img src="assets/.aistudio/Screenshot%202026-06-01%20183706.png" width="220" alt="Daily Logs & Ratings" style="margin: 10px;" />
  <img src="assets/.aistudio/Screenshot%202026-06-01%20183715.png" width="220" alt="Smart Notifier & Alarms" style="margin: 10px;" />
  <img src="assets/.aistudio/Screenshot%202026-06-01%20183729.png" width="220" alt="Notion-style Notes Editor" style="margin: 10px;" />
</p>
<p align="center">
  <img src="assets/.aistudio/Screenshot%202026-06-01%20183742.png" width="220" alt="Analytics Graphs" style="margin: 10px;" />
  <img src="assets/.aistudio/Screenshot%202026-06-01%20183804.png" width="220" alt="Home Screen Widget" style="margin: 10px;" />
  <img src="assets/.aistudio/Screenshot%202026-06-01%20165701.png" width="220" alt="Theme Toggle" style="margin: 10px;" />
  <img src="assets/.aistudio/Screenshot%202026-06-01%20165715.png" width="220" alt="Stats Screen" style="margin: 10px;" />
</p>

---

## Key Functional Modules

### 1. Real-Time Activity Tracking Engine
* **Flexible Presets**: Track activities like Studying, Working, Sleeping, Traveling, Exercising, Meditating, or Leisure instantly.
* **Persistent Counter**: Seamless real-time calculation of active duration with high-performance coroutine tick states.
* **Smart Homescreen Integration**: Uses a modern Android App Widget to keep track of your performance direct from the launcher screen.

### 2. Tab 3: Dual-View Explorer (Notion Notes & Statistics)
* **Notion-Style Notes Document Editor**:
  * Rich physical styling toggles (Bold, Italic, Underline).
  * Auto-adjusting font-sizing presets and typography families (Sans-Serif, Serif, Monospace).
  * Direct bullet list hooks and Uri image photo attachments.
  * Debounced asynchronous backing database auto-save with a fully mapped back-button capture.
  * Live document analytics: Real-time word and character counters.
* **Daily Rating & Evaluation Logs**:
  * Complete score analysis on a 1-to-10 scale per calendar date.
  * Quick one-sentence summaries logged safely on local storage.
* **24-Hour Segment Visualizer**:
  * Chronological color-coded segment grids dividing any calendar day into active segments.
  * Instant automatic calculation and warning flags highlighting "Unaccounted Free Time Gaps."
* **Advanced Analytics & Multi-Line Plotting**:
  * Customized drawing canvas rendering local multi-line comparison graphs for the past 30 days.
  * Streak metrics showing consecutive days logged per activity category.

### 3. Smart Interval Notifier (Reminders)
* Configure multiple concurrent alerts with unique repeat frequencies (1 Min, 5 Mins, 15 Mins, 30 Mins, 1 Hour).
* Toggle custom alarm keywords (e.g., "Are you focused?, Stretch, Drink Water") to maintain physical and cognitive wellness.

---

## ⚙️ Architecture & Local Stack

- **UI Framework**: Jetpack Compose (Kotlin) styled with full edge-to-edge system insets, custom scaffold paddings, and cohesive M3 attributes.
- **Local Persistence Layer**: Fully client-side Room Database using Kotlin Symbol Processing (KSP) and safe background Coroutine Flows to query note files, active logs, daily ratings, and reminders in real-time.
- **Verification Engine**: Tested via standard unit testing, Android Roborazzi, and local JVM Robolectric controllers.

---

## 📁 Customizing Visual Assets

To customize the default vector identity next to 'trakie', replace this resource path inside your Android project:
```path
app/src/main/res/drawable/ic_launcher_foreground.xml

**Note:** The developer of this application is me; Chetraj and gemini.