# CLAUDE.md

Guidance for working in this repo. Full phased build plan is in [PLAN.md](PLAN.md) — read it for sequencing; this file is the standing decisions that should stay stable across sessions.

## What this app is

Android app: pick a chord (root note + quality/extension checkboxes), an
instrument (Guitar or Ukulele), see a hand-playable fretboard diagram, tap to
hear it. Guitar shipped first; ukulele (Phase 6.5) reused the theory layer
entirely unchanged — it only touched the voicing/rendering/audio layers, as
intended. Architecture must leave piano, hand-position photos, and keyboard
diagrams as additive work too, not rework.

## Stack decisions (don't relitigate without a reason)

- **Language**: Kotlin.
- **UI**: Jetpack Compose, Material 3. No XML layouts, no legacy View system except where Compose has no equivalent (none expected for this app).
- **Architecture**: MVVM. One `ChordSelectionViewModel` per screen exposing `StateFlow`; UI is a pure function of that state. Don't scatter chord-selection state across composables.
- **Min/target SDK**: minSdk 26, target/compile = latest stable at time of work.
- **DI**: manual (constructor injection / simple factories). Don't add Hilt/Koin unless the object graph actually gets painful — it won't at this app's size.
- **Persistence**: Jetpack DataStore (Preferences) for last-used selection/instrument and favorites. No Room unless voicing data outgrows bundled JSON (unlikely).
- **Music theory**: hand-written Kotlin (`Note`, `ChordQuality`, `ChordSymbol`). Do not pull in an external music-theory library for 12-TET interval math — it's ~200 lines and we want full control over the checkbox-combination validity rules.
- **Chord voicing data**: bundled JSON under `app/src/main/assets/chords/` (one file per instrument: `guitar_voicings.json`, `ukulele_voicings.json`), keyed by canonical chord symbol (e.g. `"Am7"`). Treat this as curated data, not generated at runtime — voicing quality (real, low-fret, hand-playable shapes) is the main product-quality risk in this app, so prefer hand-verifying entries over generating them algorithmically. The shared value type is `ChordVoicing` (`frets: List<Int?>`, string count read from `frets.size`) — instrument-neutral on purpose; don't reintroduce a fixed string-count assumption.
- **Audio**: MIDI, not recorded samples. Build a short in-memory MIDI sequence per chord (Program Change to the target General MIDI instrument, e.g. 24 = nylon guitar, then simultaneous Note-On for every pitch in the voicing, then Note-Off) and play it via `MediaPlayer` + a `MediaDataSource`, using Android's built-in Sonivox synth. Zero audio assets to source/license, and adding an instrument is a program-number change, not a new sample set. Tradeoff: the built-in synth is a modest-quality wavetable synth, not a realistic recorded instrument — acceptable for v1; a future `SoundFontChordPlayer` can replace it behind the same interface if quality needs to improve. General MIDI has no dedicated ukulele program — Banjo (105) is the deliberate substitute timbre, not an oversight.
- Playback failures are handled at the `MediaPlayer` boundary (`setOnErrorListener` releases cleanly) since that's a real system boundary (codec/OS variability); our own generated MIDI bytes are fully within our control and don't need defensive handling — they're verified correct by unit test instead.
- **Testing**: JUnit for the theory/voicing-loader modules (highest-value tests in this codebase, plain-JVM and fast); instrumented Compose UI tests (`app/src/androidTest`) for the selector→render flow, driving `ChordSelectorScreen`'s stateless overload directly rather than the real ViewModel (that's already covered by the JVM ViewModel tests) — run via `./gradlew connectedDebugAndroidTest` against a device/emulator.
- **CI**: GitHub Actions running unit tests + `assembleDebug` on push/PR. Instrumented tests are deliberately *not* in CI — emulator-based CI (KVM, boot time, flakiness) is a much heavier lift than unit-test-only CI; run them locally via `android-cli` before a release instead. Revisit if UI regressions start slipping past review.
- **Monetization** (Phase 7.5): freemium — free with a single AdMob banner ad, one-time ~$4.99 Play Billing purchase to remove it. This is a deliberate, later reversal of the "no analytics SDK" non-goal below, made for monetization — not an accident. When implementing it, wrap Billing behind a `PurchaseLookup`-style interface (matching `VoicingLookup`/`ChordAudioSource`/`ChordSelectionStore`) so ad-gating stays unit-testable.

## Architectural seams to preserve

- `ChordVisualization` interface — `FretboardDiagramView` is the only v1 implementation. Future: `HandPhotoView`, `KeyboardDiagramView`, `HandOnKeyboardPhotoView`.
- `ChordAudioSource` interface — `MidiChordPlayer` is the only v1 implementation. A future `SoundFontChordPlayer` may replace/supplement it if audio quality needs to improve.
- `Instrument` enum (`GUITAR`, `UKULELE`) drives both which visualization and which audio source get used. `ChordSelectionViewModel` takes `Map<Instrument, VoicingLookup>`, not a single lookup — adding a 3rd instrument means adding a map entry and a repository, not touching the ViewModel's shape. `FretboardLayout`/`FretboardDiagramView` derive string count from `voicing.frets.size`; never reintroduce a hardcoded string count. `MidiChordPlayer` picks the tuning table via an exhaustive `when (instrument)` — the compiler will force updating it when a new instrument is added.

## Chord-quality checkbox rules

Checkboxes (min, 7, maj7, 9, sus2, sus4, 6, Aug, Dim, ...) must be validated as a compatibility matrix before being turned into a `ChordSymbol` — not every combination is real music (e.g. Aug + Dim, or min + sus4, are mutually exclusive). This matrix lives with the theory module and should be unit-tested, not left as implicit UI behavior.

## Non-goals for v1

- No user accounts, no backend of our own. This used to also say "no network calls, no analytics SDK" — Phase 7.5 deliberately reopens that: AdMob and Play Billing both make network calls to Google's infrastructure. Still true: no backend *we* run, no user accounts, no data leaving the device other than what those two Google SDKs handle.
- No barre-chord-only voicings when a real open/low-fret shape exists — prefer the hand-playable option.
- No piano UI or data — deferred (Phase 10).
- No sus2/sus4/6th/9th/Aug/Dim voicing data for ukulele yet, mirroring guitar's own deferral of 6th/9th/Aug/Dim — extend `ukulele_voicings.json` the same way (curate and self-verify, don't guess) rather than adding a shortcut.
