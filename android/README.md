# Pigeon for Android

Native Android port of the Pigeon iOS app (Strava for studying). Kotlin + Jetpack Compose.

**Deliverable:** `../Pigeon-1.0.0-android.apk` — signed release APK (12.5 MB, minSdk 26 / Android 8.0+, arm64-v8a + x86_64). Sideload directly on a device.

## Build

```bash
source ~/android-toolchain/env.sh   # JDK 17 + Android SDK 34 + Gradle 8.7 (user-local)
gradle assembleRelease              # -> app/build/outputs/apk/release/app-release.apk
```

Signing key: `keystore/pigeon-release.jks` (alias `pigeon`, store/key password `pigeonflyshome`). Keep it to ship updates as the same app identity.

## What's ported

- Onboarding carousel + email auth against the app's existing Supabase project (same tables/RPCs as iOS: `users`, `timelapses`, `friend_requests`, `study_groups`, `group_invites`, `toggle_like`, `claim_username`, ...), plus a zero-setup "continue without an account" local mode
- 5-tab shell (Home / Friends / Record / Groups / You) with Pigeon paper theme, Glouceser fonts, glass cards
- Session recorder: Continuous / Pomodoro / Set Time / Carrier Pigeon (landmark flights at 70 km/h) modes, multi-subject segments with mid-session switching, app-leave + off-task tracking, lock mode, pause/discard, crash-recovery snapshots, ongoing notification
- CameraX timelapse: periodic stills during the session, played back as an animated flipbook in the feed
- Posts: captions, "pun me" with all 300 puns and rarity tiers (weighted rolls, feed boost, rarity chips), share sheet
- Feed: greeting pools, streak bar, search, unsynced banner, likes (optimistic + cloud), detail sheet, edit/delete
- Stats: Day/Week/Month/Year/Lifetime with stacked bars, subject donut, calendar heatmap, summary cards, Most Studied records
- Profile: stats row, top subjects, personal records, subject management, wallpapers (14 gradients + solids), appearance modes, edit profile
- Friends (search/requests/friend profile), Groups (create, leaderboard, feed), Recaps (weekly/monthly/yearly/wrapped card)

## Known gaps vs iOS

Dynamic Island / Live Activities, Google OAuth (needs an Android OAuth client), Instagram sticker pasteboard, Google Photos backup, video MP4 export (frames are kept as a flipbook instead), content moderation regex pass. All cloud data is shared with the iOS app via the same Supabase project.
