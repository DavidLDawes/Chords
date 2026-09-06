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

**Phase 4 — Fretboard rendering** ✅ done
13. Build `FretboardDiagramView` as a Compose `Canvas`: draw nut, 5 fret lines, 6 string lines, fret markers, then overlay the current `ChordVoicing` (dots on fret/string intersections, "X"/"O" above nut for muted/open strings).
   - Shipped. Windowing/marker logic lives in a pure `FretboardLayout` object (no Compose deps) so it's JVM-unit-testable; the Composable just draws from it.
14. Auto-scroll/shift the shown fret window when a voicing sits above fret 5 (so barre chords higher up the neck still render legibly).
   - Shipped as `FretboardLayout.baseFret`: shows the nut when everything fits in frets 1-5, otherwise shifts the window so the highest fret is the last row and draws a "Nfr" label instead of the nut. Verified live on-device for both an open shape (C) and a shifted one (D#m, "4fr").
15. Compose preview + Compose UI tests for a few known voicings.
   - Shipped Compose previews (open + barre shapes) and a `FretboardLayoutTest` unit-test suite (8 tests: windowing + marker logic). Skipped instrumented Compose UI tests for now in favor of the on-device manual verification already done — revisit if regressions show up.
   - `ChordSelectionViewModel` now depends on a `GuitarVoicingLookup` interface (not the Android-asset-backed `GuitarVoicingRepository` directly) so it stays testable as a plain JVM unit; `ChordSelectorScreen` shows the fretboard when a voicing is curated, otherwise a "No guitar shape curated yet" message. Also fixed: the screen's Column needed `verticalScroll` — checkboxes + fretboard together don't fit on one screen and the content was being clipped uncroll-ably before this.

**Phase 5 — Audio** ✅ done
16. Write a small in-memory Standard MIDI File (SMF) builder: Program Change to a General MIDI instrument number, simultaneous Note-On for every pitch in the voicing (resolved from fret + open-string tuning), Note-Off ~1.5s later, End of Track.
   - Shipped as `MidiSequenceBuilder` (format 0, single track), fully covered by tests including an exact byte-for-byte comparison against a hand-computed expected sequence and a generic round-trip decode check for a multi-note chord.
17. Implement `MidiChordPlayer`: hand the generated bytes to `MediaPlayer` via a `MediaDataSource` (no temp file), `prepare()`/`start()` on tap. Map `Instrument` to a GM program number (e.g. nylon guitar = 24, steel guitar = 25, piano = 0).
   - Shipped. `Instrument` enum currently has just `GUITAR(24)` per the v1 non-goal of skipping a real instrument picker; adding more is a one-line addition. `StandardGuitarTuning` resolves fret positions to MIDI note numbers (verified against known open-chord shapes' pitch classes).
18. Wire a tap gesture on the fretboard canvas + an explicit "play" button to `ChordAudioSource.play(...)`.
   - Shipped: `FretboardDiagramView` takes an `onTap` callback, and `ChordSelectorScreen` also shows an explicit "Play {chord}" button; both call `ChordSelectionViewModel.playCurrentChord()`. Verified live on-device (tapping both the fretboard and the button engages the audio device with no exceptions and no crash — full audible confirmation isn't checkable from this environment, but the MIDI bytes themselves are verified correct by the exact-byte unit test).
   - Found and fixed a real bug during this work: `playCurrentChord()` originally read the cached `uiState.value`, which is a `WhileSubscribed` `StateFlow` — its cached value only updates while something is actively collecting it. Fixed by resolving directly from the source `root`/`qualities` state instead.

**Phase 6 — Integration & polish** ✅ done
19. Connect selection → voicing lookup → fretboard render → audio, all reactive off one ViewModel state.
   - Already satisfied by how Phases 3-5 were built — `ChordSelectionViewModel.uiState` is the single reactive source the whole screen renders from. No new code needed; confirmed still true.
20. Add instrument picker (guitar functional; ukulele stub); persist last selection via DataStore.
   - Persistence shipped: `ChordSelectionStore` interface + `DataStoreChordSelectionStore` (Jetpack DataStore Preferences). The ViewModel loads the last root/qualities on `init` and saves on every change. Verified live on-device across a real `am force-stop` + relaunch, not just a warm reinstall.
   - Instrument picker **deferred, on purpose** — building a picker with a single option (Guitar) has no value; it now belongs in the new Ukulele phase below, where there's actually a second option to pick.
21. Empty/error states: invalid combo, no voicing found, audio load failure.
   - Invalid combo: still structurally impossible (checkboxes are disabled before an invalid state can be reached), so there's nothing to display.
   - No voicing found: shipped in Phase 4 ("No guitar shape curated yet for {chord}").
   - Audio load failure: added `MediaPlayer.setOnErrorListener` to `MidiChordPlayer` so a decode/playback error releases cleanly instead of leaking or crashing.
22. Accessibility pass: content descriptions for dropdown/checkboxes, TalkBack labels for the fretboard ("A minor seventh, open position").
   - Quality checkboxes: each row is now one `Modifier.toggleable` target (checkbox + label announced together as a single checkbox control), the standard Compose accessibility pattern, rather than two separate elements.
   - Fretboard: added a generated content description (`FretboardLayout.describeVoicing`), e.g. *"Fretboard diagram, strings low to high: fret 3, fret 2, open, open, open, fret 3. Tap to play."* — confirmed present via `android layout`'s semantics dump on-device.
   - Root dropdown and Play button already accessible via Material3's built-in `TextField`/`DropdownMenuItem`/`Button` semantics.
   - Found and fixed a real visual bug while testing this: the outermost strings' dots were half-clipped by the canvas edge (the string sat exactly at the margin with no room for the dot's radius to overflow into). Fixed with a dedicated `EDGE_PADDING` and a capped dot radius.

**Phase 6.5 — Ukulele support** ✅ done

Ukulele reuses the music-theory layer completely unchanged (`Note`/`ChordQuality`/`ChordSymbol` don't know or care what instrument plays them) — this phase was entirely about the guitar-shaped assumptions baked into the voicing/rendering/audio layers.

23. Generalize the voicing/fretboard model off a hardcoded 6-string guitar. `GuitarVoicing.STRING_COUNT` and `FretboardLayout`/`FretboardDiagramView`'s string-count assumptions need to come from the voicing's own `frets.size` instead of a constant, so the same rendering code draws a 4-string ukulele diagram without a parallel copy of the drawing logic.
   - Shipped: `GuitarVoicing` renamed to instrument-neutral `ChordVoicing` (just `frets: List<Int?>`, `require(frets.isNotEmpty())` in place of the old `== 6` check); `GuitarVoicingParser` → `ChordVoicingParser`; `GuitarVoicingLookup` → `VoicingLookup`. `FretboardDiagramView` now computes `stringCount = voicing.frets.size` locally instead of a top-level constant. `GuitarVoicingRepository` stays guitar-specific (parses `guitar_voicings.json`); new `UkuleleVoicingRepository` is its 4-string twin, both implementing `VoicingLookup`.
24. Curate ukulele voicing data: 4 strings, **standard reentrant tuning (G4, C4, E4, A4)** — note this is *not* monotonically ascending in pitch (the G string is tuned higher than the C string next to it), unlike guitar's tuning. Order string data by physical position (as strung on the instrument, matching how a uke chord chart is conventionally drawn), not by pitch, to avoid modeling this incorrectly. New `assets/chords/ukulele_voicings.json` (same canonical-chord-name keys as the guitar file) + a shared parser, covering the same chord set already curated for guitar. Verify with the same note-by-note pitch-class self-check pattern `GuitarVoicingDataTest` already established.
   - Shipped: 36 chords (all 12 major/minor triads + all 12 dominant 7ths — sus chords deferred for ukulele same as guitar deferred 6th/9th/aug/dim). Derived via two movable "one shape covers all 12 roots" formulas proven correct by construction (verified structurally, not just spot-checked), with well-known easier open shapes substituted where they exist (C, D, F, G, A majors; Am, Cm, Dm, Em, Gm minors; C7, G7, A7, B7). `UkuleleVoicingDataTest` mirrors `GuitarVoicingDataTest`'s rigor and **caught a real arithmetic bug** (A#7 used semitone offset 11 instead of 10, from miscounting the distance from C) before it could ship a wrong chord.
25. Add `Instrument.UKULELE`. General MIDI has no dedicated ukulele program — General MIDI's Banjo (program 105) is the closest commonly-used substitute timbre and is what we'll use, documented as a deliberate approximation rather than an oversight. Add a ukulele tuning table (MIDI note per string, respecting the reentrant order above) alongside `StandardGuitarTuning`.
   - Shipped as `StandardUkuleleTuning`; `MidiChordPlayer` picks the tuning table by a `when (instrument)` (exhaustive, so adding a 3rd instrument without updating this won't compile).
26. Add the instrument picker (Guitar / Ukulele) — this is where it actually earns its place, since there are finally two real options. Switches which voicing repository feeds the fretboard and which GM program `MidiChordPlayer` uses; persist the choice via the same `ChordSelectionStore`.
   - Shipped as a Material3 `SingleChoiceSegmentedButtonRow`. `SavedSelection` gained an `instrument` field; `ChordSelectionViewModel` now takes `Map<Instrument, VoicingLookup>` instead of a single lookup. Verified live on-device: switching to Ukulele renders a correct 4-string G major diagram (0,2,3,2), plays without error, and the choice survives a real `am force-stop` + relaunch.
27. Tests: ukulele voicing-data self-verification (same rigor as `GuitarVoicingDataTest`), `FretboardLayout`/`FretboardDiagramView` with a 4-string voicing, and instrument-picker ViewModel coverage.
   - Shipped: `UkuleleVoicingDataTest`, `StandardUkuleleTuningTest`, a 4-string case in `FretboardLayoutTest`, and `ChordSelectionViewModelTest` coverage for instrument switching/persistence. 76 unit tests total across the project (up from 61).

**Phase 7 — Testing & hardening** ✅ done
28. Full unit test pass on theory + voicing modules; Compose UI tests for selector→render flow; manual pass on a real device via `android-cli`-driven install/run.
   - Unit tests: 76 tests already in place from earlier phases (theory, voicing data self-verification for both instruments, ViewModel, MIDI byte format), all still green.
   - Shipped: `ChordSelectorScreenTest`, 8 instrumented Compose UI tests driving the stateless `ChordSelectorScreen(state, ...)` overload directly (controlled state in, recorded callbacks out) — root selection, quality toggling (including a disabled checkbox correctly *not* firing), instrument switching, tapping the fretboard, the Play button, and the "no voicing curated" fallback message. All 8 pass on-device via `./gradlew connectedDebugAndroidTest`.
   - Deliberately **not** added to CI: emulator-based instrumented testing in GitHub Actions needs real machine emulation (KVM) and is a meaningfully heavier, flakier CI setup than the current unit-test-only workflow. Documented here as a conscious scope decision, not an oversight — revisit if regressions in this area start slipping through.
   - Manual device pass: re-confirmed the "no voicing curated" fallback switches correctly per-instrument (e.g. `C9` shows "No guitar shape curated yet" on Guitar and "No ukulele shape curated yet" on Ukulele) with no crash. Also noted for the record: running `connectedDebugAndroidTest` clears the app's DataStore as part of Android's normal test-isolation behavior — expected, not a persistence bug.
29. Basic crash reporting (Play Console's built-in Android Vitals is enough for v1 — skip a third-party SDK to avoid the privacy-policy overhead it adds).
   - Confirmed: `AndroidManifest.xml` has nothing that would interfere with Vitals' automatic OS-level crash/ANR collection. No code changes needed — this activates automatically once the app is distributed through Play Console.

**Phase 7.5 — Monetization** (decision: proceed with this, per direction to plan for it)

Model: **freemium** — free download with a single banner ad, plus a one-time ~$4.99 in-app purchase ("Remove Ads") that turns it off permanently. This directly reopens a decision already on record in CLAUDE.md's non-goals ("no analytics SDK... avoids Play Data-Safety/privacy-policy overhead") — an ads SDK makes network calls and collects an advertising ID, which is exactly that overhead. That's a reasonable trade for monetization, but it's a real trade, not a free lunch, so CLAUDE.md gets updated alongside this phase rather than left contradicting it.

Alternatives considered, for the record:
- **Stay fully free, no monetization** — zero added complexity/privacy surface, keeps the app's current clean non-goals intact. The right call if this stays a passion project rather than something meant to earn money.
- **Paid-only, no free tier** — simpler than freemium (no ad SDK, no dual UI state) but removes the free on-ramp that tends to drive first-app downloads and reviews.
- **Ads with no removal option** — simplest ads-only approach, but the ask here specifically wants a paid ad-free path, so this isn't the target.

30. Add Google AdMob (`com.google.android.gms:play-services-ads`) for a single banner ad, anchored below the Play button. A banner (not interstitial/rewarded) fits this app's usage pattern — quick chord lookups have no natural break point for a full-screen ad.
31. Add Google Play Billing (`com.android.billingclient:billing`) for a one-time non-consumable "Remove Ads" product. Wrap it behind a `PurchaseLookup`-style interface, matching the existing DI pattern (`VoicingLookup`, `ChordAudioSource`, `ChordSelectionStore`), so ad-gating logic is unit-testable without touching real Billing APIs. Always re-verify entitlement against Play Billing on launch rather than trusting only cached local state.
   - Note the resulting property is actually nicer than our own persistence: Play Billing purchases are tied to the Google account, not local storage, so "Remove Ads" correctly survives a reinstall — unlike the DataStore-based chord selection, which doesn't.
32. Update Play Console + policy docs to match: declare the app contains ads, add the in-app product (~$4.99, whatever Play's nearest price tier is), link the AdMob account, and update the Data Safety form (advertising ID + usage data shared for ad personalization) and the public privacy policy from Phase 8 below.
33. Test both paths on a real device: ad shows for a non-purchaser, the purchase flow completes via Play Console license testing, the ad disappears immediately after purchase, and entitlement is correctly restored after a fresh install.

**Phase 8 — Store readiness**
34. App icon, feature graphic, phone screenshots (Play Console now requires specific sizes), short/long description, privacy policy page — now needs an ads/analytics-ID section per Phase 7.5, not just the no-account-app boilerplate.
35. Set `versionCode`/`versionName`, enable Play App Signing, generate/upload signed `.aab` via Android Studio's "Generate Signed Bundle" or `./gradlew bundleRelease`.
36. Fill out Play Console's Data Safety form (now includes the advertising-ID disclosure from Phase 7.5, not "no data collected"), content rating questionnaire, target audience — including the ads-specific declarations (e.g. whether the app is child-directed, since AdMob has separate compliance requirements for that).

**Phase 9 — Release**
37. Upload to an **internal testing** track first; install on your own device via the internal-testing link, verify.
38. Promote to **closed testing** (a few real users) for a short soak, watching Android Vitals for crashes/ANRs.
39. Promote to **production**, staged rollout (e.g. start at 20%) then ramp to 100%.

**Phase 10 — Future extensibility (post-v1, enabled by the Phase 3 interfaces)**
40. `HandPhotoView`: bundle/curate photos per common voicing, swap in via the existing `ChordVisualization` interface.
41. `KeyboardDiagramView` + piano `ChordVoicing` data (frets model doesn't apply — model as pressed-key MIDI numbers instead) for the PIANO instrument.
42. `HandOnKeyboardPhotoView` analogous to the guitar hand-photo view.
43. Consider a `SoundFontChordPlayer` if adding many more instruments makes per-note sample libraries unwieldy.

## 5. Key risks to watch

- **Voicing data quality** is the single biggest effort sink — real hand-playable chord shapes for every root×quality combo is a lot of curation. Start with the most common ~40 chords fully correct rather than thin coverage of all combos.
- **Checkbox combination explosion** — decide early which combos are actually valid music (e.g. can you have min+Aug? no) so the UI never produces a nonsense symbol.
- **Audio licensing** — if sourcing samples rather than recording your own, confirm license terms allow redistribution in a published app.
- **Ads/monetization compliance** (Phase 7.5) — an ads SDK changes the Data Safety and privacy-policy answers, and AdMob has its own child-directed-treatment rules independent of Play's general families policy. Get the Play Console ads/target-audience declarations right the first time; changing them after publishing is more friction than getting them right up front.
