# Chords

Android app for looking up chords across instruments: pick a root note and a
combination of qualities/extensions (minor, 7, 9, sus2/sus4, 6, Aug, Dim, ...),
see a hand-playable fretboard diagram for the resulting chord, and tap to hear
it played.

## What's built so far

- **Instrument picker**: switch between **Guitar** and **Ukulele**. Each has
  its own curated fretboard shapes and its own tuning; switching instantly
  re-renders the diagram and changes what plays when you tap "Play."
- **Chord picker**: a root-note dropdown (all 12 notes, sharps/flats shown
  together) plus checkboxes for the qualities above. Invalid combinations
  (e.g. Aug + Dim) are never selectable in the first place — checkboxes that
  would conflict with the current selection are disabled rather than allowing
  a bad chord to be built.
- **Fretboard diagram**: a real, hand-playable voicing for the selected chord
  and instrument, drawn as a standard chord-chart diagram (nut or a shifted
  fret window for higher shapes, X/O markers, fingering dots) — the same
  renderer draws both guitar's 6 strings and ukulele's 4. 43 guitar chords and
  36 ukulele chords are curated so far (all major/minor triads and all
  dominant 7ths on both, plus guitar's common open sus2/sus4 shapes); an
  uncurated chord shows a "not curated yet" message instead of a wrong
  diagram.
- **Audio**: tap the fretboard or a "Play" button to hear the chord, rendered
  via a tiny generated MIDI sequence played through Android's built-in
  General-MIDI synth — no recorded audio samples involved. Guitar sounds like
  a nylon-string guitar; ukulele uses Banjo, the closest timbre General MIDI
  has (it has no dedicated ukulele instrument).
- **Persistence**: your last chord selection — root, qualities, and
  instrument — is remembered across app restarts.
- **Accessibility**: the checkbox rows and fretboard diagram carry proper
  screen-reader semantics (each checkbox row announces as one control; the
  fretboard describes its own shape, e.g. "fret 3, fret 2, open, open, open,
  fret 3").

Guitar and ukulele are both built behind the same visualization/audio
interfaces, so **piano is next** (see PLAN.md), followed by hand-position
photos and keyboard diagrams — all additive work rather than a rework of the
selection logic.

See [PLAN.md](PLAN.md) for the full build plan and progress, and
[CLAUDE.md](CLAUDE.md) for the tooling/architecture decisions guiding
development.
