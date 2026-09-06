# CLAUDE.md

Guidance for working in this repo. Full phased build plan is in [PLAN.md](PLAN.md) — read it for sequencing; this file is the standing decisions that should stay stable across sessions.

## What this app is

Android app: pick a chord (root note + quality/extension checkboxes), see a
hand-playable fretboard diagram, tap to hear it. v1 = guitar only. Architecture
must leave piano/ukulele, hand-position photos, and keyboard diagrams as
additive work, not rework.

## Stack decisions (don't relitigate without a reason)

- **Language**: Kotlin.
- **UI**: Jetpack Compose, Material 3. No XML layouts, no legacy View system except where Compose has no equivalent (none expected for this app).
- **Architecture**: MVVM. One `ChordSelectionViewModel` per screen exposing `StateFlow`; UI is a pure function of that state. Don't scatter chord-selection state across composables.
- **Min/target SDK**: minSdk 26, target/compile = latest stable at time of work.
- **DI**: manual (constructor injection / simple factories). Don't add Hilt/Koin unless the object graph actually gets painful — it won't at this app's size.
- **Persistence**: Jetpack DataStore (Preferences) for last-used selection/instrument and favorites. No Room unless voicing data outgrows bundled JSON (unlikely).
- **Music theory**: hand-written Kotlin (`Note`, `ChordQuality`, `ChordSymbol`). Do not pull in an external music-theory library for 12-TET interval math — it's ~200 lines and we want full control over the checkbox-combination validity rules.
- **Chord voicing data**: bundled JSON under `app/src/main/assets/chords/`, keyed by canonical chord symbol (e.g. `"Am7"`). Treat this as curated data, not generated at runtime — voicing quality (real, low-fret, hand-playable shapes) is the main product-quality risk in this app, so prefer hand-verifying entries over generating them algorithmically.
- **Audio**: `SoundPool` playing one short sample per semitone per instrument, layered to produce a chord (not one recording per chord — that doesn't scale across root × quality × instrument). Pitch-shift via `setRate` to fill gaps between recorded notes if the sample set is sparse.
- **Testing**: JUnit for the theory/voicing-loader modules (highest-value tests in this codebase), Compose UI tests for selector → render flow.
- **CI**: GitHub Actions running unit tests + `assembleDebug` on push.

## Architectural seams to preserve

- `ChordVisualization` interface — `FretboardDiagramView` is the only v1 implementation. Future: `HandPhotoView`, `KeyboardDiagramView`, `HandOnKeyboardPhotoView`.
- `ChordAudioSource` interface — `SampledNoteChordPlayer` is the only v1 implementation. A future `SoundFontChordPlayer` may replace/supplement it if instrument coverage grows.
- `Instrument` enum drives both which visualization and which audio source get used — keep selection logic instrument-agnostic; don't hardcode "guitar" assumptions (6 strings, fret model) outside the guitar-specific voicing/rendering code.

## Chord-quality checkbox rules

Checkboxes (min, 7, maj7, 9, sus2, sus4, 6, Aug, Dim, ...) must be validated as a compatibility matrix before being turned into a `ChordSymbol` — not every combination is real music (e.g. Aug + Dim, or min + sus4, are mutually exclusive). This matrix lives with the theory module and should be unit-tested, not left as implicit UI behavior.

## Non-goals for v1

- No user accounts, no backend/network calls, no analytics SDK (avoids Play Data-Safety/privacy-policy overhead beyond the baseline).
- No barre-chord-only voicings when a real open/low-fret shape exists — prefer the hand-playable option.
- No piano/ukulele UI beyond a stubbed instrument picker entry until the interfaces above are proven out on guitar.
