# Chords

Android app for looking up chords across instruments: pick a root note and a
combination of qualities/extensions (minor, 7, 9, sus2/sus4, 6, Aug, Dim, ...),
see a hand-playable fretboard diagram for the resulting chord, and tap to hear
it played.

v1 targets guitar (fretboard diagram + audio). The visualization and audio
layers are built behind interfaces so ukulele/piano, hand-position photos, and
keyboard diagrams can be added later without reworking the selection logic.

See [PLAN.md](PLAN.md) for the full build plan and [CLAUDE.md](CLAUDE.md) for
the tooling/architecture decisions guiding development.
