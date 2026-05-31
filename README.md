<div align="center">
  <img width="1200" height="475" alt="GHBanner" src="https://ai.google.dev/static/site-assets/images/share-ais-513315318.png" />
  <h1>Trakie</h1>
  <p>A modern, intelligent Android application powered by cutting-edge technologies.</p>
</div>

---

## 📖 About the Project

**Trakie** is a robust Android application developed to provide a seamless and intelligent user experience. Built with the latest Android development standards, it leverages modern architecture and powerful libraries to ensure performance, maintainability, and a highly responsive user interface.

## 🛠️ Tech Stack

This project is built using the following modern Android development tools and libraries:

*   **Language:** [Kotlin](https://kotlinlang.org/) - Modern, expressive, and safe programming language.
*   **UI Framework:** [Jetpack Compose](https://developer.android.com/jetpack/compose) - Android’s modern toolkit for building native UI declaratively.
*   **Architecture & Navigation:** 
    *   [Navigation Compose](https://developer.android.com/jetpack/compose/navigation) for seamless transitions and type-safe routing.
    *   [ViewModel & Lifecycle](https://developer.android.com/topic/libraries/architecture/viewmodel) for lifecycle-aware data management.
*   **Asynchronous Programming:** [Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) and [Flow](https://kotlinlang.org/docs/flow.html) for asynchronous operations and reactive streams.
*   **Local Storage:** 
    *   [Room Database](https://developer.android.com/training/data-storage/room) for robust local SQLite data persistence.
    *   [DataStore](https://developer.android.com/topic/libraries/architecture/datastore) for typed and asynchronous preference storage.
*   **Networking & APIs:** 
    *   [Retrofit](https://square.github.io/retrofit/) & [OkHttp](https://square.github.io/okhttp/) for type-safe REST API communication.
    *   [Moshi](https://github.com/square/moshi) for JSON serialization/deserialization.
*   **Image Loading:** [Coil](https://coil-kt.github.io/coil/) - Image loading for Android backed by Kotlin Coroutines.
*   **Hardware & Sensors:**
    *   [CameraX](https://developer.android.com/training/camerax) for consistent and reliable camera experiences.
    *   [Play Services Location](https://developers.google.com/android/reference/com/google/android/gms/location/package-summary) for advanced location tracking.
*   **AI Integration:** [Firebase AI / BoM](https://firebase.google.com/) for intelligent, machine-learning-driven features.
*   **Permissions:** [Accompanist Permissions](https://google.github.io/accompanist/permissions/) for declarative permission handling in Compose.
*   **Testing:** JUnit 4, Espresso, Robolectric, and Roborazzi for comprehensive unit, UI, and snapshot testing.

---

## 🚀 Run Locally

**Prerequisites:** [Android Studio](https://developer.android.com/studio) (latest version recommended).

Follow these steps to run the application on your local machine:

1. **Open Android Studio**.
2. Select **Open** and choose the directory containing this project.
3. Allow Android Studio to sync the Gradle project and fix any incompatibilities as it imports.
4. **Configure API Keys:** 
   * Create a file named `.env` in the root project directory.
   * Add your API key: `API_KEY=your_api_key_here` (refer to `.env.example` for guidance).
5. **Adjust Build Settings:** 
   * Open the app-level `build.gradle.kts` file.
   * Remove or comment out this line (if present and causing issues): `signingConfig = signingConfigs.getByName("debugConfig")`
6. **Run the App:** Click the **Run** button (or press `Shift + F10`) to deploy the app on an emulator or a connected physical Android device.

---

<div align="right">
  <sub><i>Note: I used AI in order to complete this project.</i></sub>
</div>
