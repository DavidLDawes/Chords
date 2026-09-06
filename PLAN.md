# Chords — Android App Build Plan

## 1. What we're building (v1 scope)

- Root note dropdown: A, A#/Bb, B, C, C#/Db, D, D#/Eb, E, F, F#/Gb, G, G#/Ab
- Quality/extension checkboxes: min, 7, maj7, 9, sus2, sus4, 6, Aug, Dim (+ dim7, min7b5 later)
- Combining checkboxes builds a canonical chord symbol (e.g. root=A, min+7 → "Am7"), with invalid combos (e.g. Aug + Dim together) disabled/blocked
- Fretboard view: 6 strings, nut, ~5 frets, drawn to reflect a real, hand-playable voicing (not just "a" shape — the *lowest reasonable* fingering)
- Tap the fretboard image (or a dedicated button) to hear the chord played on the current instrument
- Instrument picker (guitar first; ukulele/piano as stretch) feeding both the fretboard/keyboard diagram and the audio engine
- Architecture leaves room for future visualization types (hand-position photos, keyboard diagrams, hand-on-keyboard photos) without reworking the selection/data layer

## 2. Tools, libraries, environment

| Area | Choice | Why |
|---|---|---|
| Language | Kotlin | standard modern Android |
| UI toolkit | Jetpack Compose (Material 3) | dropdowns/checkboxes are trivial in Compose; fretboard is a `Canvas` draw, no custom View needed |
| IDE | Android Studio (latest stable) | official tooling, Compose preview, device manager |
| Min/target SDK | minSdk 26 (Android 8), target/compile = latest stable | Compose-friendly floor, avoids ancient-device edge cases |
| Architecture | MVVM: `ViewModel` + `StateFlow`, single `ChordSelection` UI state | keeps selector state, fretboard render, and audio trigger all reacting to one source of truth |
| Chord/music theory | Hand-written Kotlin module (no external lib) | interval math for 12-TET is ~200 lines; avoids pulling in a heavyweight music-theory dependency for something this small |
| Chord voicing data | Bundled JSON in `assets/chords/*.json`, one entry per (root, quality) → list of playable fret positions | Curating real, hand-playable shapes (open + barre) is the hard part — treat it as **data**, not code, so it can be corrected/extended without a rebuild of logic. Seed it from a permissively-licensed chord-chart dataset (e.g. the public `chords-db`/`guitar-chords-db` JSON corpora) and hand-verify/trim to hand-playable low-fret shapes. |
| Audio engine | MIDI via `MediaPlayer` + Android's built-in Sonivox synth (General MIDI instruments) | No samples to source, record, or license at all. Build a short in-memory MIDI sequence (Program Change to the target GM instrument, e.g. 24 = nylon guitar, then simultaneous Note-On for every pitch in the voicing), hand it to `MediaPlayer` via a `MediaDataSource`, play. Adding an instrument is a program-number change. Tradeoff: built-in synth is modest-quality wavetable, not a recorded instrument — a future `SoundFontChordPlayer` can upgrade this later behind the same `ChordAudioSource` interface. |
| Local persistence | DataStore (Preferences) | remember last-used instrument/selection, favorites (v1.1) |
| DI | Manual/Koin (skip Hilt for a project this size unless it grows) | keep build simple |
| Testing | JUnit5/JUnit4 for the theory module, Compose UI test + Espresso for screens | chord-symbol logic is the part most worth unit testing |
| CI | GitHub Actions: `./gradlew testDebugUnitTest assembleDebug` on push | catches regressions before Play Store upload |
| Distribution | Play Console, Android App Bundle (`.aab`), Play App Signing | required for Play Store; App Bundle is now mandatory for new apps |
| Version control | existing git repo (this one) | already initialized |
| Skill | `android-cli` (this environment) | use it once we're actually building/running — installs/manages the `android` CLI for building, running on emulator/device, screenshots, UI inspection |

## 3. Data model (core abstraction — build this before any UI)

```
Note        = enum C..B (12 values, with sharp/flat display names)
ChordQuality = enum { MAJOR, MINOR, DOM7, MAJ7, MIN7, NINE, SUS2, SUS4, SIX, AUG, DIM, DIM7, ... }
ChordSymbol = data class(root: Note, qualities: Set<ChordQuality>) 
              -> canonicalName: String  (e.g. "Am7", "Csus4")
              -> intervals: List<Int>   (semitone offsets from root, derived from qualities)

Instrument  = enum { GUITAR, UKULELE, PIANO, ... }

ChordVoicing = data class(
    symbol: ChordSymbol,
    instrument: Instrument,
    frets: List<Int?>,      // per string, null = muted, 0 = open — guitar/uke case
    fingers: List<Int?>,    // optional, for future finger-number labels
)

ChordVisualization = interface { fun render(voicing, modifier) }
  -> FretboardDiagramView   (v1)
  -> HandPhotoView          (future)
  -> KeyboardDiagramView    (future, for PIANO)
  -> HandOnKeyboardPhotoView(future)

ChordAudioSource = interface { fun play(symbol: ChordSymbol, instrument: Instrument) }
  -> MidiChordPlayer       (v1: build an in-memory MIDI sequence, play via MediaPlayer + the built-in Sonivox GM synth)
  -> SoundFontChordPlayer  (future stretch: bundle a soundfont + a real synth for better tone if the built-in synth's quality isn't enough)
```

Keeping `ChordVisualization` and `ChordAudioSource` as interfaces from day one is what makes "add hand photos later" and "add piano later" additive instead of a rewrite.

## 4. Build sequence

**Phase 0 — Environment** ✅ done
1. Scaffolded via `android create empty-activity` (Compose template), package `com.virtualsoundnw.chords`, minSdk 26.
2. Verified `assembleDebug` builds clean.
3. ✅ `.github/workflows/android-ci.yml` runs unit tests + `assembleDebug` on push to `main` and on pull requests.

**Phase 1 — Music theory core (no UI yet)** ✅ done
4. Implemented `Note`, `ChordQuality`, and `ChordSymbol` (canonical-name + pitch-class derivation) in `com.virtualsoundnw.chords.theory`.
5. Compatibility matrix implemented as a mutually-exclusive triad-quality group (Minor/Sus2/Sus4/Aug/Dim) plus a 6th-vs-7th conflict — see `ChordSymbol.findConflict`.
6. 27 unit tests across `NoteTest`/`ChordSymbolTest` cover valid combinations (pitch classes + names) and rejected ones.

**Phase 2 — Chord voicing data** ✅ done
7. Source/curate an initial JSON dataset of guitar voicings for all 12 roots × the v1 quality set, favoring open/low-fret hand-playable shapes over barre chords where possible.
   - Shipped: all 12 major and minor triads, all 12 dominant 7ths, and the common open-position sus2/sus4 chords (43 entries total) in `app/src/main/assets/chords/guitar_voicings.json`. 6th/9th/Aug/Dim voicings deliberately deferred rather than shipping guessed shapes — real risk here is wrong data, not missing data.
8. Write a loader that indexes voicings by `ChordSymbol.canonicalName`; handle "no known low-fret voicing" gracefully (fall back to a generated barre shape or show "voicing not yet available").
   - Shipped: `GuitarVoicingParser` (pure JSON→data parsing) + `GuitarVoicingRepository` (Android asset-backed lookup). The "no voicing yet" fallback UI is still TODO in Phase 6.
9. Unit test the loader against a handful of known chords (E, Am, G7, Cmaj7).
   - Shipped, and taken further: `GuitarVoicingDataTest` validates every fretted note in the *actual bundled file* is a real chord tone (transposed through standard tuning) and that every voicing sounds its root — not just a synthetic sample.

**Phase 3 — Selector UI** ✅ done
10. Build the root-note dropdown (`ExposedDropdownMenuBox`) with sharps/flats shown together (e.g. "A# / Bb").
11. Build the quality checkbox group, greying out/disabling combinations that fail the Phase 1 compatibility matrix.
   - Shipped: `ChordSelectorScreen` (root dropdown via `ExposedDropdownMenuBox` + a `ChordQuality` checkbox group). Disabling reuses `ChordSymbol.findConflict` directly (`ChordSelectionUiState.isQualityEnabled`) instead of a second copy of the compatibility rules.
12. Wire both into a `ChordSelectionViewModel` exposing a `StateFlow<ChordSymbol?>`.
   - Shipped as `StateFlow<ChordSelectionUiState>` (root + qualities + the always-valid resolved `ChordSymbol>`, since invalid combinations are prevented rather than represented). Replaces the template's placeholder `MainScreen`/`MainScreenViewModel`/`DataRepository`, which were deleted. 6 unit tests in `ChordSelectionViewModelTest`.
   - Still shows only the resolved chord name as text — the fretboard diagram (Phase 4) is next.

**Phase 4 — Fretboard rendering**
13. Build `FretboardDiagramView` as a Compose `Canvas`: draw nut, 5 fret lines, 6 string lines, fret markers, then overlay the current `ChordVoicing` (dots on fret/string intersections, "X"/"O" above nut for muted/open strings).
14. Auto-scroll/shift the shown fret window when a voicing sits above fret 5 (so barre chords higher up the neck still render legibly).
15. Compose preview + Compose UI tests for a few known voicings.

**Phase 5 — Audio**
16. Write a small in-memory Standard MIDI File (SMF) builder: Program Change to a General MIDI instrument number, simultaneous Note-On for every pitch in the voicing (resolved from fret + open-string tuning), Note-Off ~1.5s later, End of Track.
17. Implement `MidiChordPlayer`: hand the generated bytes to `MediaPlayer` via a `MediaDataSource` (no temp file), `prepare()`/`start()` on tap. Map `Instrument` to a GM program number (e.g. nylon guitar = 24, steel guitar = 25, piano = 0).
18. Wire a tap gesture on the fretboard canvas + an explicit "play" button to `ChordAudioSource.play(...)`.

**Phase 6 — Integration & polish**
19. Connect selection → voicing lookup → fretboard render → audio, all reactive off one ViewModel state.
20. Add instrument picker (guitar functional; ukulele stub); persist last selection via DataStore.
21. Empty/error states: invalid combo, no voicing found, audio load failure.
22. Accessibility pass: content descriptions for dropdown/checkboxes, TalkBack labels for the fretboard ("A minor seventh, open position").

**Phase 7 — Testing & hardening**
23. Full unit test pass on theory + voicing modules; Compose UI tests for selector→render flow; manual pass on a real device via `android-cli`-driven install/run.
24. Basic crash reporting (Play Console's built-in Android Vitals is enough for v1 — skip a third-party SDK to avoid the privacy-policy overhead it adds).

**Phase 8 — Store readiness**
25. App icon, feature graphic, phone screenshots (Play Console now requires specific sizes), short/long description, privacy policy page (needed even for a no-account app if you request any permissions — MIDI/MediaPlayer audio playback needs no special permissions, but a policy is still required for Play listing).
26. Set `versionCode`/`versionName`, enable Play App Signing, generate/upload signed `.aab` via Android Studio's "Generate Signed Bundle" or `./gradlew bundleRelease`.
27. Fill out Play Console's Data Safety form (likely "no data collected" for v1), content rating questionnaire, target audience.

**Phase 9 — Release**
28. Upload to an **internal testing** track first; install on your own device via the internal-testing link, verify.
29. Promote to **closed testing** (a few real users) for a short soak, watching Android Vitals for crashes/ANRs.
30. Promote to **production**, staged rollout (e.g. start at 20%) then ramp to 100%.

**Phase 10 — Future extensibility (post-v1, enabled by the Phase 3 interfaces)**
31. `HandPhotoView`: bundle/curate photos per common voicing, swap in via the existing `ChordVisualization` interface.
32. `KeyboardDiagramView` + piano `ChordVoicing` data (frets model doesn't apply — model as pressed-key MIDI numbers instead) for the PIANO instrument.
33. `HandOnKeyboardPhotoView` analogous to the guitar hand-photo view.
34. Consider a `SoundFontChordPlayer` if adding many more instruments makes per-note sample libraries unwieldy.

## 5. Key risks to watch

- **Voicing data quality** is the single biggest effort sink — real hand-playable chord shapes for every root×quality combo is a lot of curation. Start with the most common ~40 chords fully correct rather than thin coverage of all combos.
- **Checkbox combination explosion** — decide early which combos are actually valid music (e.g. can you have min+Aug? no) so the UI never produces a nonsense symbol.
- **Audio licensing** — if sourcing samples rather than recording your own, confirm license terms allow redistribution in a published app.
