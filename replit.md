# Minecraft SkinCraft Studio

An AI-powered Android app for generating, customizing, and managing Minecraft skins. Built with Kotlin, Jetpack Compose, Firebase, and Google Gemini.

## Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **Navigation:** Navigation Compose
- **Database:** Room (local skin project storage)
- **AI:** Firebase AI / Gemini (skin generation)
- **Auth:** Firebase Auth + Google Sign-In (Credential Manager)
- **Cloud:** Firebase Firestore, Firebase App Check (reCAPTCHA)
- **Camera:** CameraX (camera2)
- **Networking:** Retrofit + Moshi + OkHttp
- **Image loading:** Coil
- **Preferences:** DataStore
- **Testing:** JUnit4, Robolectric, Roborazzi (screenshot tests)

## Project layout

```
app/src/main/java/com/example/
├── MainActivity.kt               # Entry point
├── data/
│   ├── SkinDatabase.kt           # Room database definition
│   ├── SkinProject.kt            # Room entity
│   ├── SkinProjectDao.kt         # DAO queries
│   └── SkinProjectRepository.kt  # Data layer
├── ui/
│   ├── SkinCraftViewModel.kt     # Shared ViewModel
│   └── screens/
│       ├── StudioScreen.kt       # AI skin generation
│       ├── LibraryScreen.kt      # Saved skins
│       └── SettingsScreen.kt     # App settings
└── utils/
    ├── SkinGenerator.kt          # AI/Gemini skin generation logic
    ├── Skin3DRenderer.kt         # 3D skin preview renderer
    ├── SkinTextureMapper.kt      # Texture mapping utilities
    └── SkinExportUtils.kt        # Export skin files
```

## Running the app

This is an Android project — it cannot run directly on Replit. To build and run:

1. Open in **Android Studio**
2. Create a `.env` file with your Gemini API key:
   ```
   GEMINI_API_KEY=your_key_here
   ```
3. Add a `google-services.json` file (from Firebase Console) to the `app/` directory
4. Remove the `signingConfig = signingConfigs.getByName("debugConfig")` line from `app/build.gradle.kts`
5. Run on an emulator or physical Android device

## Required secrets / external services

| Secret | Where to get it |
|---|---|
| `GEMINI_API_KEY` | [Google AI Studio](https://aistudio.google.com) |
| `google-services.json` | Firebase Console → Project Settings |

## User preferences

- Use this project for browsing and editing source code on Replit.
