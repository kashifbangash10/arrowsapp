# Campaign levels - 19 October 2026

## Current scope

The campaign now contains **1500 playable levels**, including 500 completed Apex layouts for levels 1001-1500. Levels 501-750 are **Legend**, harder than Grandmaster; levels 751-1000 are **Extreme**, harder than Legend; Apex sits above Extreme. All levels 1-1500 are unlocked on both new and existing saves. Unlocking access does not mark levels completed, and earned completion records are retained.

The final compiled catalog, complete unit suite, independent geometry audit and actual APK resource verification have passed. The precise results and separate Android verification status are in [the verification record](artifacts/campaign/verification.md). The distributable debug APK is [arrows-1500-debug.apk](artifacts/campaign/arrows-1500-debug.apk). The earlier verified 1000-level checkpoint remains unchanged in `artifacts/campaign/baseline-1000/`; `baseline-500/` retains historical evidence.

## Layouts and difficulty

The first 1000 levels exactly preserve the geometry and independent-audit results of the verified 1000-level checkpoint. Legend, Extreme and Apex use the established 250 connected object silhouettes and grid dimensions with separately authored path layouts. They revisit those subjects with more independently removable arrows on average and deeper blocking chains; they are not additional unrelated silhouette subjects.

| Levels | Tier | Verified dependency rounds |
|---|---|---:|
| 1-5 | Easy | 1-4 |
| 6-25 | Hard | 11-20 |
| 26-100 | Expert | 24-49 |
| 101-250 | Master | 64-107 |
| 251-500 | Grandmaster | 115-184 |
| 501-750 | Legend | 191-245 |
| 751-1000 | Extreme | 275-399 |
| 1001-1500 | Apex | 425-739 |

A dependency round removes all currently free arrows together. It measures the depth of blocking chains, not predicted human solve time. Every Apex board exceeds the strongest Extreme board on this measure. The 500 Apex boards have 789-1253 arrows and 1-5 initial openings, with 1155.856 arrows and 486.148 dependency rounds on average. All 500 Apex layouts are unique and have complete legal removal certificates.

The original campaign, Master, Grandmaster, Legend and Extreme comprise 95 chapters. Apex adds fifty chapters of ten levels, for 145 total. Chapter and board names come from production metadata.

## Appearance and interaction

The level selector uses the supplied reference's navy background, gold accents, staggered cards and connected roadmap. Difficulty filters provide direct access to later chapters. All cards are playable without earning earlier wins.

Board arrows use a thicker 28%-of-cell white stroke with larger heads. A shared Read view uses Level 30 as a size reference, with at least a 7 dp cell and 1.96 dp stroke at its default reading scale. Dense boards open enlarged; pan and pinch explore the shape, Fit shows the complete silhouette, and Read restores the reading size. Puzzle coordinates and difficulty are preserved.

Level 1 retains its white onboarding canvas. Existing board theme preferences are preserved. The three lives, hint counter and tier-colored game panels remain available. See the verification record for which interactions have been observed on the current Android build.

## Fixed data and verification

Grandmaster resources live in `app/src/main/resources/levels/grandmaster/`; Legend and Extreme resources live in `app/src/main/resources/levels/advanced/`; Apex resources live in `app/src/main/resources/levels/apex/`. Each fixed file contains stable arrow IDs, orthogonal corner coordinates and a complete removal certificate. The phone loads fixed records; it does not generate campaign boards during play. The first 250 levels are defined in compiled application code.

All 147 tests across the complete 23 source test classes passed with no failures, errors or skips. Source test method names also exactly match all 147 JUnit method names. The production solver solved and replayed all 1500 levels. The independent geometry, hint and clearance oracle checked 523,089,590 choices. Every first-1000 geometry record and independent-audit row is unchanged. The APK includes all 1253 expected resource files, and all 1250 packaged fixed-level certificates were independently replayed with source/compiled coordinate agreement.

`tools/campaign/CampaignExport.java` exports actual compiled runtime levels. `tools/campaign/render_campaign.py` creates board diagrams and the complete manifest from that export. These diagrams are separate from Android screenshots and do not establish touch comfort or frame pacing.

Artifacts include:

- `artifacts/campaign/arrows-1500-debug.apk`: verified final debug APK; its SHA-256 is recorded in the verification report.
- `artifacts/campaign/catalog.json`, `campaign-validation.json`, `final-build-verification.json`, `apk-resource-validation.json`, and `unit-test-coverage.json`: current compiled, independent, package and complete test evidence.
- `artifacts/campaign/apex/authoring-audit.json`: all 500 Apex resource certificates, uniqueness and difficulty measurements.
- `artifacts/campaign/level-manifest.md`, `all-1500-levels.png`, `difficulty-boundaries.png`, and `boards/`: compiled board diagrams, metrics and solution sequences; generation status is recorded separately.
- `artifacts/campaign/device-1500/`: fresh Android observations for this expansion when executed. Older captures remain in `device/`.
