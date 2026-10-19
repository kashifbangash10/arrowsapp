# Tap Away — Soft-3D Redesign Plan

**Project root:** `D:\arrowsapp` · **Package:** `com.amowpuzzle.aitricker` · **Written:** 2026-09-05
**Status:** plan only. No source file is changed by this document.

---

## 1. What is being asked for

The engine is finished and correct — fifty verified levels, exact corridor collision, a
provably complete solver, a full JVM test suite. What is missing is the *game*. The current
build presents that engine as a white page with a flat navy line drawing on it, and the
first screenshot makes the gap obvious next to the reference: same mechanic, none of the
appetite.

Eleven things were asked for, and every one of them is carried by a phase below. A bent
arrow currently escapes as a rigid shape and must instead straighten itself out as it
leaves. The top and bottom of the play screen need real cards, in their own colour, themed,
so the screen feels like something you play rather than something you read. Every card in
the app, small or large, needs that same feel. The bottom controls — the bulb and its
neighbours — need proper ready-made icons in the Google Material style, not doodles that
read as emoji. The first screen, the second screen and the main screen need to follow the
conventions that level-based games actually use, researched rather than guessed. The arrows
are flat black in the competitor; ours go multi-coloured and must read as three-dimensional.
Play must be completely smooth, with a light fade on opening and on going back that still
has some character to it. The research findings get folded into the plan alongside the
explicit asks. And the whole thing has to end up addictive, which is a separate problem from
being correct — it is what phase 8 exists for.

---

## 2. Ground rules that do not change

Everything in the original 82-section specification still binds. Kotlin and Jetpack Compose,
the board drawn on Compose Canvas, `minSdk` 24, `targetSdk` / `compileSdk` 36, portrait only,
edge-to-edge, offline with no internet permission, minimal third-party libraries, no ads and
no in-app purchases. Branding stays original: no name, logo, artwork, sound, screenshot or
store text taken from the reference game — the screenshots are reference for *feel* only.

The mechanic is untouched. Arrows point up, down, left or right; an arrow may leave only when
the straight corridor from its head to the board edge is completely clear; tapping a free
arrow flies it off, tapping a blocked one costs a life. Collision stays exact geometry on the
occupancy grid, never a same-row / same-column shortcut. Three hearts, two hints by default,
the grid overlay toggle, DataStore persistence, deterministic seeding from the level id.

Nothing in this plan is allowed to change what a level *is*. The generator, the solver, the
collision engine and `LevelCatalog` are load-bearing and verified; the redesign sits entirely
above them. Where a phase does touch shared code — `FeedbackTiming`, `GamePreferences` — the
affected test is named in that phase so the change is never silent.

---

## 3. Research findings

### 3.1 The bent-arrow bug — root cause, and the fix

The cause is a single call. `ArrowBoard.drawEscaping` translates the entire pre-built polyline
along the exit direction:

```kotlin
withTransform({ translate(arrow.exit.dCol * distance, arrow.exit.dRow * distance) }) {
    drawArrow(arrow, geometry, stroke, AppColors.Navy, alpha)
}
```

A straight arrow looks fine under a rigid translate. An L-shaped one slides sideways with its
bend intact, which is exactly what was reported.

The fix is to stop moving the shape and start moving the arrow *along its own route*. Treat
the arrow as a ribbon of fixed length sliding down a guide curve. The guide — call it the
travel path — is the arrow's own tail-to-head polyline, extended straight along the exit
direction far enough for the whole body to clear the board. Measure arc length from the tail:
the head sits at `bodyLength`, the far end of the run at `bodyLength + exitRun`.

At progress `p` the head has advanced to `dHead = bodyLength + p * exitRun` and the tail
follows one body-length behind at `dTail = dHead - bodyLength = p * exitRun`. The visible
arrow is the slice between them, which Compose gives directly:

```kotlin
measure.getSegment(dTail, dHead, ribbon, /* startWithMoveTo = */ true)
```

That one substitution produces the requested behaviour for free. Because `dHead` never drops
below `bodyLength`, the head is always inside the straight exit run — so the arrowhead only
ever translates along `exit` and never needs rotating. Meanwhile `dTail` walks forward through
the body's corners, and each corner it passes is a corner the arrow no longer has. The arrow
unwinds. Once `dTail` reaches `bodyLength` the arrow is perfectly straight and simply slides
out. No per-frame maths beyond two additions, and no special case for turn count.

Two details matter for smoothness. `getSegment` writes into a destination `Path`, so allocate
one per departing arrow and call `rewind()` each frame rather than constructing a new one —
otherwise this is one allocation per arrow per frame, the same trap `BoardGeometry` already
avoids for bodies and heads. And the duration has to grow with the amount of unwinding: at the
current flat 300 ms a three-turn arrow straightens too fast to be seen. Scaling as
`300 + 45 * turnCount`, capped near 480 ms, keeps a straight arrow feeling as snappy as it does
today while giving a bent one time to read.

One consequence to schedule rather than discover: the board `Canvas` is not clipped by
default, so an arrow flying past the edge would currently draw over the header and dock cards.
`Modifier.clipToBounds()` on the board goes in with this change, and the tail's fade is moved
to start only once the tail has crossed the board edge rather than at a fixed 72 % of the
flight, so the arrow reads as leaving rather than as dissolving.

### 3.2 Icons — Material Symbols shipped as vector drawables

The complaint is fair. `AppIcons.kt` hand-draws every icon on a 24×24 canvas, and while the
geometry is honest, the bulb is an arc with four line segments stuck under it and it looks
approximate at 26 dp — which is precisely the "emoji" impression.

The obvious reach is `material-icons-extended`, and that is a dead end: it has been deprecated
and unpublished, and removed from recent Compose BOMs. Google's own guidance is now to pick
the symbols you need from Material Symbols and ship them as resources. That happens to suit
this project better anyway — it adds no dependency, nothing to resolve offline, and only the
two dozen icons actually used land in the APK instead of several thousand.

So: author `res/drawable/ic_*.xml` vector drawables for lightbulb, grid, undo, refresh,
settings, back, close, heart, star, star outline, lock, play, home, shop, calendar, bolt,
trophy, chevron, check circle, help, volume, music and vibration. `AppIcons.kt` collapses to a
single `AppIcon(id, tint, size)` wrapper over `painterResource`. `BrandArrowIcon` stays
hand-drawn, because it is the logo and it should match the board's geometry exactly.

### 3.3 Screen transitions

`navigation-compose` takes the four transitions per destination — `enterTransition`,
`exitTransition`, `popEnterTransition`, `popExitTransition` — so the "light fade with some
character" is set where each route is declared, not globally. The current graph passes none of
them, which is why movement between screens feels like a cut.

The combination that reads as interesting without being showy is a fade paired with a small
scale: entering at 0.96 → 1, leaving at 1 → 1.02, and on the way back the incoming screen
comes from 1.04 → 1 so returning feels like pulling back rather than pushing forward. Around
220 ms in and 180 ms out. The board screen is the exception — it earns a slightly longer 260 ms
enter, because arriving at a puzzle should feel like sitting down at it.

### 3.4 What level-based games actually do

Three conventions turned up consistently and all three are worth taking. First, the map is
hierarchical: worlds hold episodes, episodes hold a uniform run of levels, and the player is
never shown fifty undifferentiated cells. Fifty levels map cleanly onto five chapters of ten,
which also lines up with the difficulty bands the catalog test already enforces.

Second, the trail is vertical and climbs. Progress reading upward is doing real work: it makes
the map a record of what you have done rather than a menu of what you have not. The current
node is always found for you, never hunted for.

Third — and this is the part that separates a puzzle from a game people keep opening — the home
screen resumes, it does not ask. One large button that continues where you were, with the level
number and a progress ring on it, and everything else demoted to satellites.

For the Compose implementation there is a specific catch. A single `Canvas` drawn behind a
`LazyColumn` cannot draw the trail, because it has no idea where off-screen nodes are. Each row
must draw its own segment of the path in `drawBehind` — from its node's anchor to the next
node's anchor, computed from the row index, not from measured positions. That keeps the trail
correct under lazy recycling and costs nothing.

### 3.5 Fake three-dimensionality on a flat canvas

The arrows have to look extruded without a renderer. The recipe that holds up is layering the
same path five times: three stacked contact shadows at widths ×1.35, ×1.18 and ×1.06 with
alphas near 4 %, 7 % and 11 %, offset downward — three cheap strokes read as one soft shadow
without any blur; then an extrusion base in a darkened shade of the arrow's colour, offset down
about 0.13 × stroke width, which becomes the visible side wall; then the top face in the base
colour; then a thin white specular highlight at about 0.34 × stroke width, offset *up*, which is
what turns a flat band into a glossy tube.

The same logic drives the cards. Claymorphism is two shadows, not one — a wide soft ambient
shadow that seats the card on the page and a tight directional one that gives it thickness —
plus a light inner edge along the top. And a 3D button should be a top face resting on a
coloured riser: pressing translates the face down onto the riser and the riser compresses, so
the press is felt rather than merely tinted.

The cost is the thing to watch. Five stroked paths per arrow on a seventy-arrow expert board is
350 draw calls a frame. Two mitigations, both planned rather than reactive: draw the static
arrows into a cached layer with `drawWithCache` and keep only the hinted, blocked and departing
ones in a live overlay, and drop from five layers to three above roughly thirty arrows.

### 3.6 Splash

`core-splashscreen` is already a dependency. The system splash shows the brand mark;
`setKeepOnScreenCondition` holds it until DataStore has produced its first snapshot, which
removes the flash of an empty menu on a cold start; `setOnExitAnimationListener` hands off with
a fade and a slight zoom instead of a cut. The animated wordmark — arrows flying in to form
"TAP AWAY" — is a Compose screen *after* that hand-off, not an attempt to animate the system
window, and it is skippable on tap. Holding the system splash artificially for effect is the one
thing to avoid here; it blocks the UI thread and users notice.

---

## 4. The design language: Soft-3D "Clay"

Cream-white page, around `#FFFBF6`, warmer than the current pure white — pure white makes
shadows look like dirt, and this whole language is built out of shadows. On top of it sit
pastel-tinted clay cards, each chapter carrying its own tint so the game changes colour as you
progress without ever changing its identity.

The arrows get an eight-entry palette and each entry is a triple: a base colour, a shade for the
extrusion, and the specular white. Assignment is deterministic from the level seed so an arrow
is the same colour every time you open the level, with an adjacency pass so two touching arrows
never share a colour. One rule is absolute: **colour must carry no information about
solvability.** If the free arrows were tinted differently the puzzle would solve itself. Colour
is decoration and identity only; direction and shape stay the only signal.

Radii run 12 / 18 / 24 / 28 / 34 and full. The floating header and dock take 28. Spacing runs on
a 4 dp grid. Counters use tabular figures so a heart count or a coin total never makes the
layout twitch as it changes.

## 5. Screen flow

```
system splash  ──▶  animated wordmark  ──▶  HOME
   (icon,              (~1.2 s,              (PLAY continues · ring "Level N/50"
    held until          skippable)            coins · streak · Levels · Daily · Settings)
    prefs load)                                    │
                                                   ├──▶ JOURNEY MAP ──▶ level preview sheet ──┐
                                                   │      (5 chapters × 10, climbing trail)    │
                                                   ├──▶ DAILY CHALLENGE ───────────────────────┤
                                                   ├──▶ SETTINGS                               │
                                                   └──▶ TUTORIAL (first run only)              │
                                                                                               ▼
                                                                                          GAME BOARD
                                                                             (header card · clay table · dock)
                                                                                     │            │
                                                                              LEVEL COMPLETE   GAME OVER
                                                                            (stars, coins,    (retry sheet)
                                                                             confetti, next)
```

Two new destinations join the graph — the animated wordmark and the daily challenge — and the
existing `main`, `levels`, `settings`, `tutorial`, `game/{levelId}` and
`complete/{levelId}/{moves}/{lives}` routes keep their shapes. The back stack discipline already
in `AppNavigation` stays: finishing a level replaces the game destination rather than stacking on
it, so Back never lands on a board that has already been cleared.

---

## 6. The phases

Twelve phases, ordered so that nothing is built twice. The design system comes before anything
that uses it, the icons before the screens that place them, and the two board phases before the
performance pass that has to measure them. Each phase names what it touches and how it is known
to be done.

### Phase 0 — Foundation, cleanup, design tokens

Clear the ground first. Four leftovers from the previous project sit in the source tree with
extensions that keep them out of the build — `data/sound/SoundSynthesizer.kt.2`,
`domain/model/PuzzleLevel.kt.2`, `domain/model/SnakeLevelRepository.kt.2` and
`presentation/game/SnakeGameViewModel.kt.bak` — and they are deleted. The outstanding
compile-correctness self-review is finished in the same pass, since no compiler is available in
this environment and the six screens plus `MainActivity` and `AppNavigation` were written without
a build behind them; every import and signature is re-read by hand.

Then `ui/theme/DesignTokens.kt` is added: one spacing scale, one radius scale, one elevation
scale, and the clay shadow constants. Every dp value in the redesign is measured from this file,
because the alternative — ad-hoc numbers per card — is how "every card should feel the same" quietly
fails.

*Touches:* four deletions, `ui/theme/DesignTokens.kt` (new), a read-through of `MainActivity.kt`,
`AppNavigation.kt` and the six screens.
*Done when:* the tree contains no non-`.kt` source leftovers, the token file exists, and the
manual review has produced a written list of anything suspicious.

### Phase 1 — Soft-3D clay design system

`ui/theme/Palette.kt` is rewritten. Out goes the single flat `Navy` for arrows; in come the cream
page, five chapter tints, the eight arrow triples, star gold, coin gold and a locked grey. The
existing semantic names that screens already reference are kept where they still mean something so
this phase does not turn into a rename of the whole app.

Then the component set that every later phase draws from: `ClayCard` with its two shadows and its
light top edge, `ClayButton` with the riser and the physical press, `ClayCircleButton` replacing
the flat white `CircleIconButton`, `ClayPill` replacing `Pill`, and the tinted top and bottom
bands. `AppText` gets a heavier rounded display face and tabular figures on the counters.

*Touches:* `ui/theme/Palette.kt`, `ui/theme/Type.kt`, `ui/components/GameChrome.kt`, plus a
new `ui/components/Clay.kt`.
*Done when:* every card and button in the app is one of these components, and no screen sets a
shadow or a radius directly.

### Phase 2 — Material Symbols instead of hand-drawn icons

The twenty-three drawables from §3.2 are authored into `res/drawable/`, monochrome and tintable so
one file serves every state. `AppIcons.kt` loses its per-icon composables and keeps a single
`AppIcon` wrapper plus `BrandArrowIcon`. Every call site across `GameChrome`, the dock, the settings
rows, the level nodes and the complete screen is updated.

The bulb is the one the user pointed at, so it is worth being specific: the replacement is the
Material `lightbulb` symbol, and the dock's grid, undo and restart controls come from the same
family so the four buttons finally look like a set.

*Touches:* `res/drawable/ic_*.xml` (23 new), `ui/components/AppIcons.kt`, every screen that draws an
icon.
*Done when:* no icon in the app is hand-drawn except the brand mark, and each drawable has been
checked at 24 dp for weight and alignment against its neighbours.

### Phase 3 — 3D multi-colour arrows

`ArrowSkin` is introduced and assigned per arrow as described in §4. `BoardGeometry` gains the shade
and specular derivations alongside the paths it already prebuilds. The five-layer draw from §3.5
replaces the two `drawPath` calls in `drawArrow`, using `withTransform { translate(...) }` for the
offsets rather than duplicate `Path` objects, so the layering costs draw calls but no allocation.

The board is then split in two. Static arrows go into a cached layer via `drawWithCache`, and only
the hinted, blocked and departing arrows are drawn live on an overlay. This is what makes the extra
layers affordable, and it is cheap to do now and expensive to retrofit later. Above roughly thirty
arrows the skin drops to three layers.

*Touches:* `ui/components/BoardGeometry.kt`, `ui/components/ArrowBoard.kt`, `ui/theme/Palette.kt`, a
new `ui/components/ArrowSkin.kt`.
*Done when:* arrows read as extruded tubes, two touching arrows never share a colour, the colouring
is identical across app restarts, and nothing about the colouring hints at which arrows are free.

### Phase 4 — The bent-arrow escape, fixed

The ribbon model from §3.1 replaces `drawEscaping`'s rigid translate. `BoardGeometry` builds a travel
path and a `PathMeasure` per arrow, plus `bodyLength` and `exitRun`; `escapeDistance` is redefined so
that `exitRun` is head-to-edge plus a full body length plus a margin, which is what guarantees the
tail actually clears. A single reusable destination `Path` per departing arrow is rewound each frame.
The arrowhead translates along `exit` only. Duration scales with turn count, easing starts gently and
accelerates out, the fade waits until the tail crosses the edge, and `clipToBounds()` goes on the
board.

The arithmetic — given a turn count, a body length and a progress, which slice is visible — is
extracted into a pure helper so it can be unit-tested on the JVM without a device.

Raising `ESCAPE_MS` invalidates `assertEquals(300, FeedbackTiming.ESCAPE_MS)` in `GameStateTest`, and
it has a second consequence that is easy to miss: `GameViewModel` waits `ESCAPE_MS + 140L` before
declaring victory. Once the duration becomes per-arrow, a three-turn arrow can outlive that wait and the
victory screen would appear over an arrow still in flight. The fix is to have the view model wait on the
departure callbacks it already receives rather than on a fixed delay, or failing that to pace against the
longest possible escape rather than the base one. Either way it is decided in this phase, not debugged
later.

*Touches:* `ui/components/ArrowBoard.kt`, `ui/components/BoardGeometry.kt`,
`presentation/game/GameUiState.kt` (which is where `FeedbackTiming` lives),
`presentation/game/GameViewModel.kt` (the tap debounce and the `ESCAPE_MS + 140L` victory wait),
`GameStateTest.kt`.
*Done when:* an L-shaped and an S-shaped arrow both visibly straighten on their way out, a straight
arrow feels no slower than it does today, and no allocation happens inside the draw pass.

### Phase 5 — First screen: animated splash into Home

`MainActivity` wires `installSplashScreen()` before `super.onCreate()`, holds the system splash on the
first preferences snapshot, and hands off with a fade and a slight zoom. A new `SplashScreen`
composable then runs about 1.2 seconds: six 3D arrows fly in from off-screen along their own exit
directions and lock into the "TAP AWAY" wordmark, hold briefly, and fade into Home. A tap skips it.
It is shown once per cold start, not on every navigation.

`MainMenuScreen` becomes Home: a chapter-tinted sky that drifts slowly behind everything, the
wordmark, a progress ring reading "Level N / 50", one large clay PLAY button that continues at the
resume level, satellites for Levels, Daily and Settings, a coin pill and a streak flame in the top
bar, and a small decorative board that quietly solves itself on a loop to advertise the mechanic
before the player has played anything.

The resume level comes from `GamePreferences.Snapshot.resumeLevel`, which already returns the first
gap in the completed set rather than the highest unlocked — the behaviour is pinned by an existing
test and PLAY inherits it.

*Touches:* `MainActivity.kt`, `presentation/splash/SplashScreen.kt` (new),
`presentation/menu/MainMenuScreen.kt`, `presentation/navigation/AppNavigation.kt`.
*Done when:* a cold start never shows an empty menu, the wordmark animation is skippable, and PLAY
lands on the same level the old menu would have resumed.

### Phase 6 — Second screen: the journey map

`LevelSelectScreen` is rebuilt as the trail from §3.4. A `LazyColumn` with `reverseLayout` so level 1
sits at the bottom and progress climbs; each row places its node on a sine-wave horizontal offset and
draws its own trail segment in `drawBehind`, solid where the stretch is cleared and dotted where it is
locked. A chapter banner sits between each band of ten, carrying the chapter name and its tint.

Nodes are clay discs in four states: locked in grey with the lock symbol, unlocked in the chapter
colour with its number, completed with an arc of up to three stars above it, and current with a pulsing
ring and a bobbing pin. On first composition the list scrolls to the current level and settles with a
small bounce. Tapping a node raises a preview sheet — mini board, best stars, best move count, PLAY —
which is where `MiniBoard` finally earns its place.

*Touches:* `presentation/levels/LevelSelectScreen.kt`, `ui/components/MiniBoard.kt`, new
`presentation/levels/LevelNode.kt` and `presentation/levels/ChapterBanner.kt`.
*Done when:* the trail is continuous across scroll with no gaps or doubled segments, the current level
is on screen without scrolling, and a locked node cannot be entered.

### Phase 7 — The game screen: header card and dock

This is the phase that answers "top and bottom need cards in their own colour, themed, so it feels
like a game". `HeaderBar` is currently a plain `Row` of 56 dp with a chevron, a centred title and a
gear — no card at all. It becomes a floating clay header at radius 28, tinted by the current chapter,
carrying back, the level number, the difficulty pill and the hearts row.

Below the board sits a floating dock card holding four clay circular buttons — hint with its count
badge, grid toggle, undo, restart — in the new Material Symbols. The board itself is seated in an inset
clay table with a soft inner shadow, so it reads as sitting *inside* a frame rather than floating on the
page; this is most of what separates the reference's feel from the current build's.

Then every remaining card is brought onto the same recipe, because the request was explicitly that
small cards feel as considered as large ones: the level preview sheet, the settings rows, the pause
sheet, the complete sheet and the game-over sheet.

*Touches:* `presentation/game/GameScreen.kt`, `ui/components/GameChrome.kt`,
`presentation/settings/SettingsScreen.kt`, `presentation/complete/LevelCompleteScreen.kt`,
`presentation/tutorial/TutorialScreen.kt`.
*Done when:* no screen in the app still shows a bare row or an untinted white panel, and the header,
dock and table tints all move together when the chapter changes.

### Phase 8 — Meta systems: stars, coins, daily, undo, combo

This is the addictiveness phase, and it is deliberately last among the feature phases because it needs
the board and the map to already exist.

Stars are awarded three for a clean clear with no life lost and no hint used, two for one life or one
hint, one otherwise, and persisted per level. Coins are earned by stars — 10, 15, 25 — and by daily
play, and spent on a hint at 50, an undo at 25 or a level skip at 150; the counter counts up rather
than snapping. The daily challenge generates its level from `LocalDate.toEpochDay()` as the seed, so
every device gets the same puzzle on the same day with no server involved, and a streak counter drives
a seven-day reward strip. Undo pushes a command stack in the engine that restores the last departed
arrow, one free per level and coin-priced after that. Combos come from consecutive correct taps inside
a two-second window, raising a multiplier with a floating "×3 NICE!" and a coin bonus.

All of it persists through the existing DataStore `GamePreferences`, with the per-level star map stored
as a delimited string. `Snapshot` stays Context-free so it remains JVM-testable — that property is what
makes the existing progress tests possible and it is not given up.

*Touches:* `data/prefs/GamePreferences.kt`, `presentation/game/GameViewModel.kt`,
`presentation/game/GameUiState.kt`, `domain/engine/` for the undo stack, new
`presentation/daily/DailyChallengeScreen.kt` and `presentation/shop/HintShopSheet.kt`.
*Done when:* stars and coins survive a process death, the same date yields the same daily puzzle twice
running, coins can never go negative, and undo restores the board exactly.

### Phase 9 — Motion: transitions, celebration, haptics, sound

The four transitions from §3.3 are attached to each destination in `AppNavigation`. Level complete gets
its stars stamping in one at a time with an overshoot, confetti firing from the existing nine-colour
list, and the coin total counting up. Game over desaturates the board, shatters the hearts and raises a
retry sheet. Haptics are mapped deliberately — a light tick on a good tap, a double thud on a blocked
one, a success pattern on a clear — and the synthesized audio is extended with a whoosh, a thunk, a star
chime, a coin and a quiet two-bar ambient loop, all generated rather than shipped, which keeps the APK
clean and the game fully offline.

*Touches:* `presentation/navigation/AppNavigation.kt`, `presentation/complete/LevelCompleteScreen.kt`,
`data/sound/`, the haptics wrapper in `AppContainer`.
*Done when:* every route has its own four transitions, no transition exceeds 260 ms, and both sound and
haptics respect their settings toggles.

### Phase 10 — Performance, accessibility, settings

The board became roughly five times more expensive to draw, so the 60 FPS requirement is re-verified on
the densest expert level rather than assumed: confirm the static cache is being hit, confirm every
per-frame value is still read inside the draw lambda so nothing recomposes at frame rate, and confirm
the three-layer fallback triggers where intended.

Accessibility is not an afterthought here because colour was just made load-bearing for identity. The
eight-colour palette is checked for colour-blind separation, a "classic arrows" setting restores the flat
navy for anyone who prefers it, and a reduce-motion setting shortens or removes the decorative animation
while also honouring the system animator duration scale. Every icon button keeps a content description,
touch targets stay at 48 dp or more, and the new colours are contrast-checked on the cream page.

*Touches:* `ui/components/ArrowBoard.kt`, `presentation/settings/SettingsScreen.kt`,
`data/prefs/GamePreferences.kt`.
*Done when:* the densest level holds frame on a low-end device, and both new settings work from a cold
start.

### Phase 11 — Tests, regression pass, release verification

New JVM tests cover the pure logic added above: the ribbon arithmetic, arrow skin stability and adjacency,
the star thresholds, the coin ledger's floor at zero, and the daily seed being stable per date and
distinct across dates. `GameStateTest`'s timing assertions are updated for the new escape duration, and
the fifty-level catalog validation is re-run in case chapter banding shifted the difficulty curve it
asserts. Then a manual pass screen by screen, and a release build under R8 — vector drawables need no
keep rules, but the build is confirmed rather than presumed.

*Touches:* `app/src/test/java/.../` throughout.
*Done when:* the whole suite passes, the release variant builds and installs, and every phase's "done
when" above has actually been checked off.

---

## 7. Order and dependencies

```
0 ─▶ 1 ─┬─▶ 2 ─┬─▶ 5 ─▶ 6 ─┐
        │      │           ├─▶ 9 ─┐
        │      └─▶ 7 ─▶ 8 ─┘      ├─▶ 11
        └─▶ 3 ─▶ 4 ───────────────┴─▶ 10 ─┘
```

Phase 1 gates everything visual. Phases 2 and 3 are independent of each other and can run in parallel.
Phase 4 must follow 3 because both rewrite the same draw code and doing them in the other order means
writing the escape path twice. Phase 10 must follow 4 and 9 because it measures what they cost. Phase 11
closes.

## 8. Risks, and what is done about them

The one that could actually derail the redesign is the draw-call budget. Five layers per arrow on a
seventy-arrow board is a real cost, and the answer is designed in from the start rather than bolted on:
the static/dynamic split in phase 3, the three-layer fallback, and a measured verification in phase 10
instead of a hopeful one.

Second, `PathMeasure.getPosition` and `getTangent` are newer than `getSegment`. The plan does not depend
on them — the travel path is orthogonal, so a tip position and direction can be computed by walking the
polyline directly, which is both cheaper and free of float wobble at the rounded corners. If they turn
out to be unavailable on the pinned Compose version, nothing changes.

Third, colour becoming meaningful is an accessibility hazard, answered by the palette check and the
classic-arrows setting in phase 10, and by the absolute rule that colour never encodes solvability.

Fourth, `GamePreferences` grows a lot in phase 8 — stars, coins, streak, daily state. `Snapshot` must stay
Context-free or the existing progress tests stop being possible, so that constraint is stated in the phase
rather than discovered when the suite breaks.

Fifth, and worth saying plainly: none of this can be compiled or run here. The Linux workspace is
unavailable and there is no Gradle, no `kotlinc` and no way to execute the test suite, so every phase is
written to be verified by hand-reading APIs before use and then confirmed in Android Studio. That is a real
limitation of this environment, not a claim that the code is verified.

---

## 9. Verification — every request traced to a phase

| # | What was asked | Phase | Where it lands |
|---|---|---|---|
| 1 | Current build is far too simple; make it like the reference screenshots | 1, 3, 7 | clay design system, 3D arrows, header + dock + board table |
| 2 | **Bug:** a bent arrow escapes rigid, must straighten smoothly | 4 | ribbon-along-travel-path via `PathMeasure.getSegment` |
| 3 | Top and bottom cards in a different colour, themed | 7 | chapter-tinted clay header and dock |
| 4 | Every card, small or large, must feel game-like | 1, 7 | `ClayCard` recipe applied to all sheets, rows and pills |
| 5 | Fix the bottom icons — ready-made Google icons, no emoji | 2 | 23 Material Symbols vector drawables |
| 6 | First, second and main screens like level-based games, researched | 5, 6 | splash → Home; journey-map trail; §3.4 findings |
| 7 | Arrows multi-coloured, 3D style with a 3D feel | 3 | `ArrowSkin` triples, five-layer extrusion draw |
| 8 | Completely smooth play | 3, 10 | static/dynamic split, layer fallback, measured 60 FPS |
| 9 | Light fade on open and back, but with an interesting feel | 9 | per-destination fade + scale, 220/180 ms, pop from 1.04 |
| 10 | Deep research, and the findings go in the task list too | §3 | six findings, each cited into the phase that uses it |
| 11 | Addictive and interesting, skip nothing | 8 | stars, coins and shop, daily and streak, undo, combo |

Each of the twelve phases also exists as a tracked task, with the dependency edges from §7 recorded on
them, so the order cannot be lost between sessions.

### What this plan does *not* do

It does not touch the generator, the solver, the collision engine or `LevelCatalog`. It does not add a
third-party UI or animation library. It does not add the deprecated `material-icons-extended`. It does not
introduce network access, ads or purchases. And it does not reuse anything from the reference game beyond
the mechanic itself, which was already original in this build.

### Known consequences to accept before starting

Raising `ESCAPE_MS` past 300 ms breaks a current assertion in `GameStateTest` by design, and it also
lengthens `GameViewModel`'s `delay(FeedbackTiming.ESCAPE_MS + 140L)` victory wait, which a per-arrow
duration can still outrun; phase 4 owns both. The fifty-level difficulty curve assertions in
`LevelCatalogTest` are re-run in phase 11 because chapter banding sits on top of the same bands. And
`GamePreferences` gains four new persisted fields in phase 8, which is a schema change to an existing
DataStore — defaults must be chosen so an existing install reads as "no stars, no coins, no streak"
rather than crashing or resetting progress.

---

## 10. This plan was checked, not just written

The technical claims above were verified against the code and the APIs rather than assumed:

`androidx.compose.ui.graphics.PathMeasure.getSegment(startDistance, stopDistance, destination,
startWithMoveTo)` exists in Compose UI and returns the intervening segment into the destination path —
confirmed against the Compose reference, so the fix in §3.1 rests on a real API. The plan deliberately
does not depend on the newer `getPosition` / `getTangent`, because the travel path is orthogonal and the
tip can be derived by walking the polyline.

`FeedbackTiming` was located in `presentation/game/GameUiState.kt` with `ESCAPE_MS = 300` and
`TAP_DEBOUNCE_MS = 110L`, and its two other consumers were found by search: `ArrowBoard.kt` line 101 and
`GameViewModel.kt` lines 95 and 192. The victory-wait interaction in §Phase 4 came out of that search, not
out of guesswork.

`drawEscaping`'s rigid `translate` and its fixed 72 % fade were read directly in `ArrowBoard.kt`.
`HeaderBar` was confirmed to be a bare 56 dp `Row` with no card, and `CircleIconButton` a flat white circle
on `AppColors.Background`. `AppNavigation`'s `composable(...)` calls were confirmed to pass no transitions
at all. The theme package was confirmed to contain `Palette.kt`, `Type.kt` and `Theme.kt` — the file names
in the phase bodies match what is actually on disk. `Snapshot.resumeLevel` returning the first gap rather
than the highest unlocked is pinned by an existing test, which is why phase 5's PLAY button inherits it
instead of reimplementing it.

Every one of the eleven requests in §1 maps to a phase in the table in §9, and every phase exists as a
tracked task carrying the dependency edges from §7.











