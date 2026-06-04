# InjectBuddy for Android

Native Android client for InjectBuddy — Kotlin, Jetpack Compose + Material 3. App-first:
sign in, land on the cycle-planner dashboard, reach all 14 dosage calculators from one
navigation drawer. Calculator math runs offline; auth and saved protocols sync with the
existing Supabase backend.

## Stack

- Kotlin · Jetpack Compose + Material 3 · min SDK 26 (Android 8)
- MVVM (`ViewModel` + `StateFlow`), coroutines
- Navigation-Compose · `ModalNavigationDrawer` sidebar
- `supabase-kt` (Auth + Postgrest, RLS) · Ktor

## Build

1. Install Android Studio (Ladybug or newer) and open this folder.
2. Copy `local.properties.example` → `local.properties` and set `SUPABASE_URL` /
   `SUPABASE_ANON_KEY` (same Supabase project as the web app).
3. Run on an emulator or device: `./gradlew installDebug` (`gradlew.bat` on Windows).
4. Tests: `./gradlew test`.

## Module layout

```
app/src/main/java/com/injectbuddy/android/
├─ MainActivity.kt · InjectBuddyApp.kt · AppRoot.kt
├─ di/            ServiceLocator (manual DI)
├─ ui/theme/      Material 3 theme (teal #0fbcad, light/dark)
├─ ui/components/ shared Loading / Empty / Error states
├─ nav/           NavItems (single source), MainShell, drawer, NavHost
├─ data/          models, Supabase client, repositories
├─ domain/        repository interfaces
├─ calc/          CalculatorEngine + specs (offline math)
└─ feature/       auth · dashboard · calculator · calendar · settings
```
