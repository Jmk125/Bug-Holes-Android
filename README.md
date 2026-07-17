# Bug Holes Android — MVP

Native Kotlin/Jetpack Compose Android Studio project.

## Implemented
- Team profiles with one or more players
- Automatic player rotation after every recorded turn
- Miss/normal shot (+1)
- Foul (+2 strokes and +1 foul incident)
- Live team and per-player foul counters
- Six tappable pockets
- Tapping an open pocket closes it; tapping a closed pocket reopens it
- Closure/reopening attributed to the current shooter
- Scratch confirmation and immediate recorded loss
- Undo restores turn, strokes, fouls and pocket state
- Complete Game remains disabled until all six pockets are currently closed
- Completed games and their full action histories saved locally with Room/SQLite

## Open it
1. Install the current Android Studio.
2. Extract this folder.
3. In Android Studio choose **Open** and select `BugHolesAndroid`.
4. Allow Gradle sync and SDK downloads.
5. Connect an Android phone with USB debugging or create an emulator.
6. Press Run.

## Rules represented by one action per turn
- MISS: +1 and next player
- FOUL: +2, +1 foul, and next player
- CLOSE_POCKET: +1, closes pocket, and next player
- REOPEN_POCKET: +1, reopens pocket, and next player
- SCRATCH: +1 and immediate loss

## Next development pass
- Game history screen
- Team statistics dashboard (wins/losses, averages, foul totals/rates)
- Player contribution statistics (closures, reopenings, fouls, scratches)
- Seasons
- Edit/delete profiles and games
- Backup/export
- More polished pool-table artwork and app icon

Note: the generated project was prepared as source code in this environment; it was not compiled here because an Android SDK/Gradle runtime is not installed in the container.
