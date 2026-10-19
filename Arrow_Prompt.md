# Arrow Tap Puzzle — Complete Claude Opus 5 Build Prompt

> **Purpose:** Give this entire prompt to Claude Opus 5 (Claude Code / coding agent) while the Android Studio project is open. The goal is to build a complete, polished Android puzzle game in Kotlin that reproduces the **gameplay concept, interaction model, progression, and overall visual feel** shown in the supplied screenshots and screen recording, while using original code, original generated levels, and original visual assets/branding.

---

## 1. ROLE

You are a senior Android game engineer, Kotlin architect, UI/UX designer, puzzle-game designer, and QA engineer.

Build the game completely inside the current Android Studio project.

Do not merely give me snippets or a plan. **Actually create and modify all required project files so the project builds and runs.**

Work incrementally:

1. Inspect the existing project.
2. Determine whether it is a usable Android/Kotlin project.
3. If necessary, repair or replace the project structure.
4. Implement the complete game.
5. Build it.
6. Fix compilation/runtime problems.
7. Test the important gameplay paths.
8. Leave the project in a clean, runnable state.

If something is unspecified, make a sensible engineering decision and continue instead of repeatedly asking me questions.

---

# 2. TARGET PLATFORM

Use:

- Language: **Kotlin**
- UI: **Jetpack Compose**
- Rendering: **Jetpack Compose Canvas / custom drawing**
- Minimum SDK: **24**
- Target SDK: **36**
- Compile SDK: **36**
- Portrait-first Android game
- Modern Android architecture
- Offline-first gameplay
- No server required for the core game
- No internet permission unless genuinely required
- No unnecessary third-party libraries

Prefer stable AndroidX/Compose APIs that are compatible with the selected SDK.

The game must run smoothly on ordinary Android phones, not just high-end devices.

---

# 3. IMPORTANT REFERENCE MATERIAL

I have supplied:

- Multiple screenshots of the game.
- A screen recording of the game/trailer.
- The original game's public store description.

**Inspect the supplied screenshots and recording carefully before implementing the UI and gameplay.**

Use them as a visual and behavioral reference.

The screenshots show a game with a minimalist white puzzle board, dark navy arrows, bright blue interaction/highlight states, hearts/lives, level number, difficulty label, hint button, grid button, and a blue level-completion screen.

The reference game is called:

**Arrow Puzzle: Tap Puzzle Games**

Do NOT use that name in the new app.

Create an original app identity and original branding. For example, use a temporary working name such as:

**Arrow Escape Puzzle**

The final code should be easy to rename later.

Do not copy the original game's logo, proprietary artwork, screenshots, sounds, text verbatim, or branded assets. Recreate the underlying game concept and interaction mechanics with original implementation and original level data.

---

# 4. CORE GAME CONCEPT

The game is a relaxing logic puzzle based on directional arrows.

Each level contains multiple arrow-shaped paths/segments on a board.

Every arrow points in one of four directions:

- UP
- DOWN
- LEFT
- RIGHT

The player must tap arrows in the correct order.

An arrow can escape the board only when the space directly ahead of it is completely clear.

If another remaining arrow/path blocks its direction of movement, that arrow is currently blocked and should not be tapped.

The player must reason about which arrows are currently free and remove them one by one.

As arrows disappear, previously blocked arrows become free.

The objective is:

**Remove every arrow from the board.**

When all arrows have escaped:

- Play a satisfying completion animation.
- Show the level as completed.
- Allow the player to continue to the next level or start another game.

---

# 5. GAMEPLAY RULES

Implement these rules carefully.

## 5.1 Arrow movement

Each arrow consists of:

- A path/line segment.
- One arrowhead at its exit/end.
- A direction.

Example:

```text
──────────────►
```

The arrow moves toward the board edge in the direction of its arrowhead.

The arrow should travel smoothly out of the puzzle board when tapped successfully.

---

## 5.2 What makes an arrow free?

An arrow is free if no other remaining arrow geometry blocks the straight path from the arrow's exit point toward the board boundary.

Examples:

- RIGHT arrow → nothing blocks its path to the right edge → FREE.
- RIGHT arrow → another arrow lies in front of it → BLOCKED.
- UP arrow → another arrow is directly in its escape corridor → BLOCKED.

The implementation must use actual geometry/collision logic rather than simply hard-coding whether a tap is valid.

---

# 6. IMPORTANT COLLISION / BLOCKING IMPLEMENTATION

Do NOT implement blocking with a simplistic "same row" or "same column" test only.

Create a robust geometric model.

Represent each arrow as one or more orthogonal line segments:

```kotlin
data class ArrowSegment(
    val start: Point,
    val end: Point
)
```

And represent an arrow as something like:

```kotlin
data class Arrow(
    val id: Int,
    val direction: Direction,
    val segments: List<ArrowSegment>,
    val exitPoint: Point,
    val pathWidth: Float
)
```

Use normalized board coordinates rather than hard-coded screen pixels.

For collision testing:

- UP → inspect geometry above the arrow's exit corridor.
- DOWN → inspect geometry below.
- LEFT → inspect geometry to the left.
- RIGHT → inspect geometry to the right.

Account for stroke width / collision padding.

An arrow that is currently removed must no longer block other arrows.

---

# 7. TAP BEHAVIOR

When the user taps an arrow:

### If the arrow is free:

1. Highlight it blue.
2. Play a short tap feedback animation.
3. Animate it toward and beyond the board edge.
4. Remove it from the active board.
5. Recalculate which remaining arrows are free.
6. Update the remaining-arrow count.
7. Check whether the level is complete.

### If the arrow is blocked:

1. Do not remove it.
2. Show a subtle blocked feedback animation.
3. Reduce the player's life by 1.
4. Briefly indicate that the move was invalid.
5. Do not make the game feel harsh or frustrating.

Avoid excessive shake or aggressive effects.

---

# 8. LIVES SYSTEM

The reference game shows three heart icons.

Use:

```text
❤️ ❤️ ❤️
```

but render them using a clean vector/icon solution rather than requiring external copyrighted artwork.

Default:

- 3 lives per level.

When a blocked arrow is tapped:

- lives -= 1

If lives reaches zero:

- Show a Game Over / Try Again state.
- Allow the player to restart the same level.
- Do not permanently lose progress.

The game should remain relaxing rather than punitive.

---

# 9. HINT SYSTEM

The reference game contains a lightbulb hint button.

Implement:

- Hint button at the bottom.
- Show a small count badge, initially e.g. `2`.
- Tapping Hint finds a currently free arrow.
- Visually highlight that arrow.
- Do not automatically remove the arrow.
- Consume one hint.
- Use a subtle pulsing blue highlight.

The hint system must calculate a genuinely valid move using the puzzle state.

Do NOT simply select a random arrow.

If multiple free arrows exist, choose one intelligently.

---

# 10. GRID BUTTON

The reference UI contains a second circular button with a grid/hash-like icon.

Implement it as a board/grid assistance control.

Preferred behavior:

- Toggle a subtle grid overlay on/off.

The grid should never interfere with puzzle interaction.

Persist the user's grid preference locally.

---

# 11. LEVEL HEADER

The gameplay screen should visually resemble the supplied reference.

Top area:

### Large title

Example:

```text
Tap Away
Arrows
```

or an original equivalent title.

The title should be large, centered, bold, and dark blue/navy.

### Level

Example:

```text
Level 23
```

### Lives

Three heart icons.

### Difficulty

Examples:

```text
Easy
Normal
Hard
Expert
```

### Arrow count

Show the number of remaining arrows.

Use a small arrow icon + count.

The header should adapt to different phone sizes.

Do NOT use fixed absolute screen coordinates.

---

# 12. BOARD

The board is the most important part of the app.

Use a custom Compose Canvas.

Requirements:

- Clean white/light background.
- Dark navy arrows.
- Rounded line caps.
- Rounded line joins.
- Clear arrowheads.
- Subtle spacing.
- Optional faint grid dots.
- Strong visual hierarchy.
- No unnecessary decoration.

Reference visual language:

- Background: near white.
- Main arrow color: deep navy.
- Interaction/highlight: vivid blue.
- Secondary UI surfaces: very light blue/gray.
- Text: dark navy/slate.
- Completion screen: bright blue gradient/radiating background.

Do not copy exact proprietary colors blindly. Use a visually similar but original palette.

---

# 13. ARROW VISUAL STYLE

Arrows should be visually close in spirit to the supplied screenshots:

- Thick dark stroke.
- Rounded corners.
- Clean geometry.
- Large triangular arrowhead.
- Orthogonal 90-degree turns.
- No anti-aliased jagged edges.
- Consistent stroke width.

Use Compose Canvas drawing APIs.

For example:

- `Path`
- `drawPath`
- `drawLine`
- `drawCircle`
- custom arrowhead geometry

Keep rendering efficient.

Do not create thousands of Compose nodes for a level.

---

# 14. BOARD COORDINATE SYSTEM

Use normalized coordinates.

Example:

```text
0.0 ... 1.0
```

for both X and Y.

Convert normalized puzzle coordinates to actual Canvas pixels.

This allows the same level to render correctly on:

- 720p phones
- 1080p phones
- tablets
- different aspect ratios

The playable board should maintain a consistent aspect ratio.

---

# 15. LEVEL DATA ARCHITECTURE

Do NOT hard-code 50 levels directly inside one giant Kotlin function.

Create a clean level model.

For example:

```kotlin
data class Level(
    val id: Int,
    val difficulty: Difficulty,
    val arrows: List<ArrowDefinition>,
    val startingLives: Int = 3,
    val startingHints: Int = 2
)
```

And:

```kotlin
enum class Difficulty {
    EASY,
    NORMAL,
    HARD,
    EXPERT
}
```

Level definitions should be separated from game logic.

Preferred options:

- Kotlin data objects/files
- JSON assets
- or a combination of generated level data + validation

Make it easy to add level 51, 52, 100, etc. later.

---

# 16. 50 LEVEL REQUIREMENT

Create **exactly 50 playable levels** in version 1.

Every level must be solvable.

Every level must have at least one valid solution.

Do NOT generate random levels at runtime without validating them.

The player should never receive an impossible puzzle.

---

# 17. LEVEL DIFFICULTY CURVE

Difficulty must increase gradually.

Do NOT make:

- Level 1 extremely difficult.
- Level 2 suddenly much harder.
- Level 50 identical to Level 1.

Use this progression:

### Levels 1–5 — Tutorial / Very Easy

- Small boards.
- Few arrows.
- Mostly straight arrows.
- Obvious free moves.
- Teach the mechanic.

Approximate arrows:

```text
6–10
```

### Levels 6–10 — Easy

- More arrows.
- Simple turns.
- Basic dependencies.
- 8–14 arrows.

### Levels 11–20 — Normal

- More intersections.
- More chained dependencies.
- More misleading blocked arrows.
- 12–22 arrows.

### Levels 21–30 — Advanced Normal

- Larger layouts.
- Multiple dependency chains.
- More turns.
- 18–30 arrows.

### Levels 31–40 — Hard

- Dense boards.
- Longer chains.
- Several tempting but blocked moves.
- 25–40 arrows.

### Levels 41–47 — Very Hard

- Dense geometry.
- Complex dependencies.
- More compact arrangements.
- 35–55 arrows.

### Levels 48–50 — Expert

- Very dense.
- Long dependency chains.
- Minimal obvious free moves.
- 45–70 arrows where appropriate.
- Still guaranteed solvable.

Difficulty should depend on actual puzzle structure, not just arrow count.

---

# 18. PUZZLE DIFFICULTY METRICS

Implement a level validation/scoring system.

Useful metrics:

- Number of arrows.
- Number of initially free arrows.
- Maximum dependency depth.
- Average number of available moves.
- Number of misleading blocked arrows.
- Number of turns per arrow.
- Board density.
- Solution length.
- Number of possible alternative solutions.

A level with 30 arrows is not automatically harder than one with 20.

Use dependency complexity to determine difficulty.

---

# 19. SOLVER / VALIDATOR

This is extremely important.

Create an internal puzzle solver.

The solver should:

1. Receive a level.
2. Determine all currently free arrows.
3. Simulate removing each possible free arrow.
4. Continue recursively / with BFS/DFS.
5. Determine whether the board can eventually be cleared.
6. Return a valid solution sequence if one exists.

For 50 levels, run validation in development/testing.

Every shipped level must pass validation.

For example:

```kotlin
data class SolutionResult(
    val solvable: Boolean,
    val moveSequence: List<Int>
)
```

Use memoization/state hashing where useful.

Do not run an expensive solver continuously on the Android UI thread.

---

# 20. LEVEL GENERATION

You may create the 50 levels manually, procedurally, or with a hybrid approach.

Preferred approach:

### Generate → Validate → Save

Create a development-side level generator that:

1. Creates valid arrow arrangements.
2. Runs the solver.
3. Rejects unsolvable boards.
4. Calculates difficulty.
5. Keeps levels matching the desired difficulty range.
6. Stores the final validated level definitions in app assets/source.

The shipped app should load the finalized levels quickly.

Do not depend on an online server for levels.

---

# 21. SOLUTION QUALITY

Avoid levels where:

- Every arrow is immediately free.
- There is only one trivial move repeated.
- The solution requires guessing.
- The player must intentionally lose lives.
- The puzzle is mathematically impossible.
- The level is visually cluttered but strategically simple.

Prefer levels with:

- clear initial choices,
- meaningful dependencies,
- satisfying chain reactions,
- increasing complexity,
- multiple valid approaches where appropriate.

---

# 22. LEVEL PROGRESSION

Store progress locally.

Requirements:

- Completed level IDs.
- Highest unlocked level.
- Current level.
- Hint count if appropriate.
- Grid preference.
- Basic settings.

Use DataStore or another modern lightweight local persistence approach.

The player should not lose progress when closing/reopening the app.

---

# 23. LEVEL UNLOCKING

Initially:

- Level 1 unlocked.

After completing Level 1:

- Level 2 unlocks.

Continue until:

- Level 50.

Do not force the player to replay previous levels unless desired.

Provide a level selection screen if practical.

Recommended:

```text
LEVELS

1   ✓
2   ✓
3   ✓
4   🔒
5   🔒
...
```

Use clean circular/square level cards.

Completed levels can display a small check mark.

---

# 24. MAIN MENU

Create a polished main screen.

Possible structure:

```text
        ARROW
        ESCAPE

   A relaxing logic puzzle

       PLAY

     LEVELS

      SETTINGS
```

Keep it minimalist.

Do not overcrowd the screen.

---

# 25. GAME SCREEN

Recommended structure:

```text
┌─────────────────────────────┐
│                             │
│       Tap Away Arrows       │
│                             │
│      Level 12               │
│                             │
│   ↗  18     ❤️ ❤️ ❤️   Normal│
│                             │
│                             │
│       PUZZLE BOARD          │
│                             │
│                             │
│                             │
│      💡 ²        #          │
│                             │
└─────────────────────────────┘
```

Make the actual design visually polished and responsive.

---

# 26. LEVEL COMPLETION SCREEN

The supplied reference shows a bright blue celebratory screen.

Implement an original version with:

- blue gradient/radial background,
- subtle rays,
- small confetti particles,
- "Level Complete!" text,
- miniature completed puzzle preview,
- "Next Level" button,
- "Main Menu" button.

Animation:

1. Board clears.
2. Short pause.
3. Celebration burst.
4. Confetti.
5. Completion card appears.
6. Buttons become interactive.

Keep it smooth and short.

---

# 27. ANIMATIONS

Use Compose animation APIs.

Important animations:

### Arrow tap

- Slight scale/highlight.
- Blue glow.
- Movement toward edge.
- Fade/slide out.

### Blocked tap

- Small shake or pulse.
- Brief red/neutral feedback.
- Heart reduction.

### Hint

- Blue glow.
- Repeating subtle pulse.

### Level completion

- Board clears.
- Confetti.
- Card entrance animation.

Do not use overly dramatic animations.

The game should feel calm and satisfying.

---

# 28. SOUND

If implementing sound:

- subtle tap sound,
- blocked sound,
- arrow escape sound,
- completion sound.

Use original/simple generated sound effects or system-safe tones.

Make sound optional.

Settings:

```text
Sound: ON/OFF
Music: ON/OFF
Haptics: ON/OFF
```

If sound assets would complicate the build, implement the game without external audio first and keep the architecture ready for audio later.

---

# 29. HAPTICS

Optional but recommended:

- Successful arrow: light haptic.
- Blocked arrow: slightly stronger feedback.
- Completion: success haptic.

Respect the user's haptic setting.

---

# 30. SETTINGS SCREEN

Include:

- Sound toggle
- Music toggle if music exists
- Haptic toggle
- Show Grid toggle
- Reset Progress
- About

Add a confirmation dialog before resetting progress.

---

# 31. RESPONSIVE DESIGN

The game must work on different Android screen sizes.

Test at minimum conceptually for:

- 360 × 800
- 393 × 873
- 412 × 915
- 1080 × 2400

Do not assume 810 × 1440 only.

Use:

- `BoxWithConstraints`
- density-independent dimensions
- adaptive sizing
- safe content padding

Keep the puzzle centered.

---

# 32. EDGE-TO-EDGE

Use modern Android edge-to-edge behavior.

Respect:

- status bar
- navigation bar
- display cutouts
- gesture navigation

Do not allow the puzzle to overlap system UI.

---

# 33. ORIENTATION

Portrait only.

If you lock orientation, do it properly in the Android manifest/activity.

The UI must remain stable after lifecycle recreation.

---

# 34. ARCHITECTURE

Use a clean architecture suitable for a small game.

Suggested:

```text
app/
 ├── data/
 │    ├── LevelRepository.kt
 │    ├── LevelData.kt
 │    └── ProgressRepository.kt
 │
 ├── domain/
 │    ├── model/
 │    │    ├── Arrow.kt
 │    │    ├── Level.kt
 │    │    └── Difficulty.kt
 │    │
 │    ├── engine/
 │    │    ├── PuzzleEngine.kt
 │    │    ├── CollisionEngine.kt
 │    │    └── PuzzleSolver.kt
 │    │
 │    └── generator/
 │         └── LevelGenerator.kt
 │
 ├── ui/
 │    ├── navigation/
 │    ├── screens/
 │    │    ├── MainMenuScreen.kt
 │    │    ├── LevelSelectScreen.kt
 │    │    ├── GameScreen.kt
 │    │    ├── LevelCompleteScreen.kt
 │    │    └── SettingsScreen.kt
 │    │
 │    ├── components/
 │    └── theme/
 │
 └── MainActivity.kt
```

You may change the exact structure if there is a better design.

Keep puzzle logic independent from Compose UI.

---

# 35. GAME STATE

Use a predictable state model.

Example:

```kotlin
data class GameState(
    val level: Level,
    val remainingArrows: List<Arrow>,
    val lives: Int,
    val hintsRemaining: Int,
    val highlightedArrowId: Int?,
    val isComplete: Boolean,
    val isGameOver: Boolean
)
```

Use ViewModel/state flow appropriately.

Do not put complex mutable game logic directly inside Composable functions.

---

# 36. TOUCH HANDLING

The board must support accurate tapping.

Use Canvas pointer input.

When the user taps:

1. Convert screen coordinates to normalized board coordinates.
2. Determine which arrow geometry contains the tap.
3. Resolve the selected arrow.
4. Pass it to the game engine.
5. Update state.

Touch hitboxes can be slightly larger than the visible line to make the game comfortable.

Do not require pixel-perfect tapping.

---

# 37. ARROW HIT TESTING

Create a dedicated hit-test function.

It should support:

- horizontal segments,
- vertical segments,
- corners,
- arrowheads.

Use a reasonable touch tolerance.

If two arrows are visually very close, choose the arrow whose geometry is actually closest to the touch location.

Do not make accidental taps frustrating.

---

# 38. BLOCKING LOGIC

Create a dedicated function such as:

```kotlin
fun isArrowFree(
    arrow: Arrow,
    activeArrows: List<Arrow>
): Boolean
```

This function must:

- identify the arrow's exit direction,
- create the escape corridor,
- check all remaining arrow segments,
- ignore the arrow itself,
- ignore already removed arrows,
- return true only when the corridor is clear.

The exact geometric algorithm is up to you, but it must be deterministic and robust.

---

# 39. ESCAPE ANIMATION

When an arrow is successfully tapped:

- animate it along its direction.
- Continue beyond the board boundary.
- Fade slightly near the end.
- Remove it from logical state after the animation or at a carefully chosen point.

Do not let an animated-out arrow continue blocking the board.

---

# 40. GAME LOOP / PERFORMANCE

This is a puzzle game, not a real-time action game.

Do not create a continuous frame loop unless needed.

Use Compose state-driven rendering.

Avoid:

- unnecessary recompositions,
- creating Path objects every frame,
- expensive collision calculations during drawing,
- memory leaks,
- launching coroutines repeatedly from Canvas drawing code.

Cache calculated geometry where appropriate.

---

# 41. ACCESSIBILITY

Add meaningful content descriptions to:

- Back button
- Settings
- Hint
- Grid
- Level selection
- Next level
- Restart

Do not require accessibility features to play the game, but make controls discoverable.

---

# 42. NO NETWORK DEPENDENCY

The game must work completely offline.

Do not make:

- Firebase mandatory,
- remote level downloads,
- API calls,
- online login.

The first version should be fully self-contained.

---

# 43. ADS

Do not implement ads until the core game is completely stable.

If the architecture needs a future ad integration point, create a clean interface/place for it.

Do not let ads interfere with gameplay.

---

# 44. IN-APP PURCHASES

Do not implement real billing in the first build unless explicitly requested later.

The first goal is a complete playable game.

---

# 45. ORIGINAL VISUAL DESIGN

The supplied screenshots are references.

Create an original visual system inspired by their characteristics:

- minimalist,
- clean,
- calm,
- puzzle-focused,
- navy arrows,
- blue interaction,
- white board,
- rounded UI controls.

Do NOT directly copy:

- original logo,
- original developer name,
- original app icon,
- original screenshots,
- original promotional artwork,
- original text descriptions,
- proprietary sound effects.

---

# 46. APP ICON

Create a simple original app icon concept.

Suggested concept:

- rounded blue background,
- white/navy arrow motif,
- simple geometry,
- no text.

Use vector/shape-based assets where possible.

---

# 47. TEXT

Use original app text.

Examples:

```text
Arrow Escape
Clear the board
Find the free path
Tap the arrows in the right order
Level Complete!
Great job!
Next Level
Try Again
Hint
Grid
Settings
```

Keep wording concise.

Do not copy the original store description.

---

# 48. TUTORIAL

Create a short first-time tutorial.

Suggested steps:

### Step 1

"Find an arrow with a clear path."

### Step 2

"Tap it to let it escape."

### Step 3

"Clearing arrows opens paths for others."

### Step 4

"Tap them all in the right order."

Allow the user to skip the tutorial.

Do not show the tutorial on every launch.

---

# 49. FIRST FIVE LEVELS

Make the first five levels especially understandable.

### Level 1

Very small puzzle.

Purpose:
- teach tapping a free arrow.

### Level 2

Introduce one dependency.

### Level 3

Introduce multiple possible free arrows.

### Level 4

Introduce misleading blocked arrows.

### Level 5

Small but satisfying multi-step chain.

The player should understand the complete mechanic before Level 6.

---

# 50. LEVEL DIFFICULTY LABELS

Use:

```text
Levels 1–5: Easy
Levels 6–15: Easy
Levels 16–30: Normal
Levels 31–40: Hard
Levels 41–47: Hard
Levels 48–50: Expert
```

You can refine these labels if the measured puzzle difficulty suggests a better classification.

---

# 51. LEVEL DATA QUALITY

Before finalizing:

- Render every level.
- Validate every level.
- Verify at least one solution exists.
- Verify the solution actually clears every arrow.
- Verify no arrow is outside the board unexpectedly.
- Verify no arrow has invalid geometry.
- Verify no level has accidental impossible overlap.
- Verify hit testing.
- Verify blocked/free state.

Create automated tests for the puzzle engine.

---

# 52. UNIT TESTS

At minimum create tests for:

### Collision

- clear arrow returns true.
- blocked arrow returns false.
- removed arrow does not block.
- perpendicular geometry is handled correctly.

### Solver

- simple level is solvable.
- known impossible level is rejected.
- solver returns a complete solution.
- solution sequence actually clears the board.

### Game engine

- valid tap removes arrow.
- invalid tap reduces life.
- zero lives triggers game over.
- last arrow triggers completion.
- hint selects a free arrow.

### Progress

- completing a level unlocks next level.
- progress survives reload.
- reset clears progress.

---

# 53. DEBUG MODE

Create a development-only debug option if practical.

Useful debug information:

```text
Level: 23
Arrows: 42
Free: 3
Blocked: 39
Lives: 3
Difficulty Score: 67
Solution Length: 42
```

Do not show this in release builds.

A simple build flag or debug menu is acceptable.

---

# 54. LEVEL SOLVER DEBUG VIEW

If practical, create a debug-only "Show Solution" feature.

It should:

- highlight the next valid arrow,
- optionally play the entire solution sequence.

This is extremely useful for validating levels during development.

Never expose it as a normal player feature unless desired.

---

# 55. GAME OVER

If the player loses all lives:

Show a clean modal/card:

```text
Out of Lives

Think carefully and try again.

Restart Level
Main Menu
```

The level remains unlocked.

No progress should be lost.

---

# 56. RESTART LEVEL

Add a restart control, accessible through the game/settings menu.

Restarting:

- restores all arrows,
- restores 3 lives,
- restores starting hints for the level/session,
- clears animations,
- resets the game state.

---

# 57. BACK NAVIGATION

Android back behavior:

Gameplay:
- first back → show confirmation or return to level selection.

Level complete:
- back → level selection/main menu.

Settings:
- back → previous screen.

Do not accidentally terminate the app from inside the game.

---

# 58. STATE RESTORATION

Handle:

- screen rotation if rotation is not locked,
- activity recreation,
- background/foreground,
- process recreation where practical.

At minimum persist the unlocked level and completed levels.

Do not lose the current level unnecessarily.

---

# 59. VISUAL POLISH

Pay close attention to:

- typography,
- spacing,
- button sizes,
- shadows,
- rounded corners,
- animation timing,
- line thickness,
- arrowhead proportions,
- board centering,
- safe areas.

The result should look like a polished Play Store game, not a prototype.

---

# 60. REFERENCE SCREEN DETAILS

From the supplied screenshots, reproduce the following general characteristics:

### Gameplay

- Large title at top.
- Level number below title.
- Arrow count indicator.
- Three heart/life icons.
- Difficulty pill.
- Large white puzzle board.
- Dark navy maze-like arrows.
- Blue active/highlighted arrow.
- Bottom circular hint button.
- Hint count badge.
- Bottom grid control.
- Clean white/light background.

### Completion

- Bright blue background.
- Radial/ray-like visual treatment.
- Large white completion message.
- Confetti.
- Puzzle preview inside a rounded white card.
- Large next-level button.
- Main-menu button.

These should be recreated with original code and visuals.

---

# 61. SCREEN RECORDING OBSERVATIONS

The supplied recording should be inspected for:

- transition timing,
- tap animation,
- arrow movement,
- tutorial/promotional presentation,
- completion animation,
- board density,
- visual pacing.

Use the recording as behavioral/visual guidance.

Do not simply insert the recording into the application.

The game must be a real interactive implementation.

---

# 62. PROMOTIONAL/TRAILER-STYLE SCREENS

The supplied recording appears to show promotional/tutorial-style screens such as:

- "Tap Away Arrows"
- "Find the Free Path"
- "Use Helpful Hints"
- "Escape the Grid"
- "Enjoy Unique Levels"

You may create an optional onboarding/tutorial carousel with original wording.

However, the actual game must remain the priority.

---

# 63. NAVIGATION

Use Compose Navigation or another clean navigation solution.

Suggested routes:

```text
main
levels
game/{levelId}
complete/{levelId}
settings
tutorial
```

Avoid complicated navigation state.

---

# 64. DATA STORAGE

Use DataStore for:

```text
highestUnlockedLevel
completedLevels
showGrid
soundEnabled
musicEnabled
hapticsEnabled
tutorialShown
```

Do not store unnecessary data.

---

# 65. ERROR HANDLING

The app must not crash because of:

- malformed level data,
- unexpected touch coordinates,
- empty arrow lists,
- repeated taps,
- rapid taps,
- activity recreation.

Disable/ignore inappropriate input during transitions.

---

# 66. RAPID TAP PROTECTION

If an arrow is already animating out:

- Ignore additional taps on it.

If level completion animation has started:

- Ignore gameplay taps.

This prevents race conditions.

---

# 67. COROUTINES

Use lifecycle-aware coroutines.

Do not launch uncontrolled coroutines from Composables.

Animation/state operations must be cancelled correctly when the screen disappears.

---

# 68. THREADING

Puzzle validation/solver calculations can be done off the UI thread when appropriate.

Never block the main thread with a large DFS/BFS calculation.

The gameplay interaction itself should feel immediate.

---

# 69. NO OVER-ENGINEERING

Do not build a huge framework.

This is a focused offline puzzle game.

Prefer:

- simple,
- testable,
- maintainable,
- performant,
- readable Kotlin.

---

# 70. BUILD REQUIREMENTS

The final project must:

- compile successfully,
- install successfully,
- launch successfully,
- display the main menu,
- start Level 1,
- allow actual arrow tapping,
- correctly detect blocked arrows,
- reduce lives,
- use hints,
- complete levels,
- unlock levels,
- save progress,
- contain 50 playable levels.

---

# 71. IMPORTANT: DO NOT STOP AT A MOCKUP

Do NOT give me:

- fake buttons,
- placeholder arrows,
- random static screenshots,
- non-functional levels,
- a single demo level,
- "TODO" placeholders,
- pseudo-code instead of implementation.

The game must be genuinely playable.

---

# 72. IMPORTANT: DO NOT USE FAKE LEVEL DATA

Every level must be actual puzzle data.

Do not create 50 copies of the same board with different numbers.

Levels must visibly and strategically differ.

---

# 73. IMPORTANT: DO NOT RANDOMIZE THE PLAYER EXPERIENCE

The shipped 50 levels should be deterministic.

If procedural generation is used during development, save validated results.

The same level ID should always load the same puzzle.

---

# 74. PERFORMANCE TARGET

Aim for:

- smooth 60 FPS animations on normal devices,
- no ANRs,
- no unnecessary allocations during drawing,
- no memory leaks,
- no giant bitmaps,
- minimal recomposition.

The board should render efficiently even at Expert difficulty.

---

# 75. TESTING CHECKLIST

Before considering the task complete, test:

## App startup

- [ ] App launches.
- [ ] Main menu loads.
- [ ] No crash.

## Level 1

- [ ] Level loads.
- [ ] Arrows render correctly.
- [ ] Tap works.
- [ ] Free arrow escapes.
- [ ] Blocked arrow is detected.

## Lives

- [ ] Invalid tap removes a life.
- [ ] Three hearts update correctly.
- [ ] Zero lives triggers game over.

## Hint

- [ ] Hint finds a genuinely free arrow.
- [ ] Highlight appears.
- [ ] Hint count decreases.

## Completion

- [ ] Last arrow clears.
- [ ] Completion animation runs.
- [ ] Level is marked complete.
- [ ] Next level unlocks.

## Persistence

- [ ] Close/reopen app.
- [ ] Completed level remains complete.
- [ ] Next level remains unlocked.

## Level select

- [ ] Locked levels cannot be opened.
- [ ] Unlocked levels open correctly.

## Settings

- [ ] Grid toggle works.
- [ ] Haptic toggle works.
- [ ] Sound toggle works if implemented.
- [ ] Reset progress works with confirmation.

## Rotation/lifecycle

- [ ] No crash.
- [ ] State remains sensible.

## All levels

- [ ] 1–50 load.
- [ ] 1–50 pass solver validation.

---

# 76. AUTOMATED LEVEL VALIDATION

Create a test that iterates through all 50 levels:

```kotlin
for (level in allLevels) {
    val result = solver.solve(level)
    check(result.solvable) {
        "Level ${level.id} is not solvable"
    }
}
```

Also verify:

```kotlin
check(result.moveSequence.size == level.arrows.size)
```

or otherwise verify that the sequence removes every arrow.

Do not assume a solver result is correct without simulating the returned sequence.

---

# 77. DEVELOPMENT WORKFLOW

Follow this exact workflow:

### Phase 1 — Inspect

Inspect:

- existing project files,
- Gradle configuration,
- AndroidManifest,
- source structure,
- Compose setup.

### Phase 2 — Architecture

Create:

- models,
- game engine,
- collision engine,
- solver,
- level repository,
- progress repository.

### Phase 3 — Core gameplay

Implement:

- board renderer,
- arrows,
- hit testing,
- free/blocked detection,
- removal animation,
- lives.

### Phase 4 — UI

Implement:

- main menu,
- level select,
- game header,
- hint,
- grid,
- settings,
- completion screen.

### Phase 5 — Levels

Create and validate 50 levels.

### Phase 6 — Persistence

Add DataStore.

### Phase 7 — Polish

Improve:

- animations,
- spacing,
- colors,
- typography,
- haptics,
- responsiveness.

### Phase 8 — Testing

Build APK and run tests.

### Phase 9 — Fix

Fix every compiler error, crash, warning that affects correctness, and obvious visual/gameplay issue.

---

# 78. BUILD AND TEST COMMANDS

After implementation, use the appropriate Gradle commands, for example:

```bash
./gradlew assembleDebug
```

and:

```bash
./gradlew test
```

If the environment uses Windows:

```powershell
gradlew.bat assembleDebug
gradlew.bat test
```

If an emulator/device is available, install and test the debug APK.

---

# 79. WHEN YOU ENCOUNTER ERRORS

Do not stop and ask me to fix ordinary implementation errors.

If Gradle fails:

1. Read the complete error.
2. Identify the root cause.
3. Fix the source/configuration.
4. Run the build again.

If Kotlin compilation fails:

1. Read the exact error.
2. Fix the code.
3. Rebuild.

If the UI crashes:

1. Inspect Logcat/stack trace.
2. Fix the root cause.
3. Re-test.

Only ask me for help if a required external credential, unavailable service, or genuinely ambiguous product decision blocks progress.

---

# 80. FINAL DELIVERABLE

When finished, report:

1. What was implemented.
2. Important files created/modified.
3. How the puzzle engine works.
4. How the 50 levels are stored.
5. How solvability is validated.
6. What tests were run.
7. Whether the project builds successfully.
8. Where the debug APK was generated.

Keep the final report concise.

---

# 81. CRITICAL QUALITY RULE

Before declaring completion, personally verify that this is a **real game**:

> I should be able to open the app, start Level 1, tap an arrow, see it leave the board, make an incorrect blocked tap and lose a life, use a hint, solve the level, see the completion screen, press Next Level, and continue through the progression toward Level 50.

If any of those actions are not actually functional, the task is not complete.

---

# 82. START NOW

Start by inspecting the current Android Studio project and the supplied reference screenshots/screen recording.

Then implement the game.

Do not return a long explanation before doing the work.

**Build the actual Android application now.**
