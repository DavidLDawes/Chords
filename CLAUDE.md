# CLAUDE.md

Guidance for working in this repo. Full phased build plan is in [PLAN.md](PLAN.md) — read it for sequencing; this file is the standing decisions that should stay stable across sessions.

## What this app is

Android app: pick a chord (root note + quality/extension checkboxes), see a
hand-playable fretboard diagram, tap to hear it. Guitar shipped first;
ukulele is next (Phase 6.5 in PLAN.md) and reuses the theory layer entirely —
it only touches the voicing/rendering/audio layers. Architecture must leave
piano, hand-position photos, and keyboard diagrams as additive work too, not
rework.

## Stack decisions (don't relitigate without a reason)

- **Language**: Kotlin.
- **UI**: Jetpack Compose, Material 3. No XML layouts, no legacy View system except where Compose has no equivalent (none expected for this app).
- **Architecture**: MVVM. One `ChordSelectionViewModel` per screen exposing `StateFlow`; UI is a pure function of that state. Don't scatter chord-selection state across composables.
- **Min/target SDK**: minSdk 26, target/compile = latest stable at time of work.
- **DI**: manual (constructor injection / simple factories). Don't add Hilt/Koin unless the object graph actually gets painful — it won't at this app's size.
- **Persistence**: Jetpack DataStore (Preferences) for last-used selection/instrument and favorites. No Room unless voicing data outgrows bundled JSON (unlikely).
- **Music theory**: hand-written Kotlin (`Note`, `ChordQuality`, `ChordSymbol`). Do not pull in an external music-theory library for 12-TET interval math — it's ~200 lines and we want full control over the checkbox-combination validity rules.
- **Chord voicing data**: bundled JSON under `app/src/main/assets/chords/`, keyed by canonical chord symbol (e.g. `"Am7"`). Treat this as curated data, not generated at runtime — voicing quality (real, low-fret, hand-playable shapes) is the main product-quality risk in this app, so prefer hand-verifying entries over generating them algorithmically.
- **Audio**: MIDI, not recorded samples. Build a short in-memory MIDI sequence per chord (Program Change to the target General MIDI instrument, e.g. 24 = nylon guitar, then simultaneous Note-On for every pitch in the voicing, then Note-Off) and play it via `MediaPlayer` + a `MediaDataSource`, using Android's built-in Sonivox synth. Zero audio assets to source/license, and adding an instrument is a program-number change, not a new sample set. Tradeoff: the built-in synth is a modest-quality wavetable synth, not a realistic recorded instrument — acceptable for v1; a future `SoundFontChordPlayer` can replace it behind the same interface if quality needs to improve. General MIDI has no dedicated ukulele program — Banjo (105) is the deliberate substitute timbre, not an oversight.
- Playback failures are handled at the `MediaPlayer` boundary (`setOnErrorListener` releases cleanly) since that's a real system boundary (codec/OS variability); our own generated MIDI bytes are fully within our control and don't need defensive handling — they're verified correct by unit test instead.
- **Testing**: JUnit for the theory/voicing-loader modules (highest-value tests in this codebase), Compose UI tests for selector → render flow.
- **CI**: GitHub Actions running unit tests + `assembleDebug` on push.

## Architectural seams to preserve

- `ChordVisualization` interface — `FretboardDiagramView` is the only v1 implementation. Future: `HandPhotoView`, `KeyboardDiagramView`, `HandOnKeyboardPhotoView`.
- `ChordAudioSource` interface — `MidiChordPlayer` is the only v1 implementation. A future `SoundFontChordPlayer` may replace/supplement it if audio quality needs to improve.
- `Instrument` enum drives both which visualization and which audio source get used — keep selection logic instrument-agnostic; don't hardcode "guitar" assumptions (6 strings, fret model) outside the guitar-specific voicing/rendering code. Concretely: `GuitarVoicing.STRING_COUNT` and `FretboardLayout`/`FretboardDiagramView`'s string-count math need to come from the voicing's own `frets.size`, not a hardcoded 6 — this is exactly what Phase 6.5 (ukulele, 4 strings) needs and is the seam to generalize rather than fork.

## Chord-quality checkbox rules

Checkboxes (min, 7, maj7, 9, sus2, sus4, 6, Aug, Dim, ...) must be validated as a compatibility matrix before being turned into a `ChordSymbol` — not every combination is real music (e.g. Aug + Dim, or min + sus4, are mutually exclusive). This matrix lives with the theory module and should be unit-tested, not left as implicit UI behavior.

## Non-goals for v1

- No user accounts, no backend/network calls, no analytics SDK (avoids Play Data-Safety/privacy-policy overhead beyond the baseline).
- No barre-chord-only voicings when a real open/low-fret shape exists — prefer the hand-playable option.
- No piano UI or data — deferred past ukulele (Phase 10). Ukulele itself is no longer a non-goal; it's Phase 6.5.
- No instrument picker UI while there's only one real instrument — build it alongside ukulele (Phase 6.5), where there are finally two options worth picking between.
