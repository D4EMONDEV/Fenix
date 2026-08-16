#!/usr/bin/env bash
# Synthesises the demo's two sounds. Ogg Vorbis, mono, 44.1 kHz — what the game
# reads. Anything else is a sound that registers, resolves and does not play.
#
# Commas separate a filter's options in ffmpeg, so every comma inside an
# expression is escaped; unescaped, "mod(t,0.5)" makes ffmpeg look for an
# option named "0.5)".
set -euo pipefail

OUT="examples/example-mod/src/main/resources/assets/example-mod/sounds"
mkdir -p "$OUT"

# A struck chime: a fundamental at C6 with two partials above it, decaying.
# The partials are what make it read as a struck object rather than a beep.
CHIME='aevalsrc=0.55*exp(-4.5*t)*(sin(2*PI*1046.5*t)+0.45*sin(2*PI*2349.3*t)+0.2*sin(2*PI*3520*t)):d=1.4:s=44100'

# The disc: a three-note figure repeating on the same bell voice, each note
# plucked and decaying. Four seconds, because the jukebox song declares four —
# a disc that declares a length it does not have leaves the jukebox on silence.
STEP='floor(mod(t/0.5\,3))'
WALTZ="aevalsrc=0.45*exp(-3.2*mod(t\\,0.5))*(sin(2*PI*523.25*(1+0.25*${STEP})*t)+0.3*sin(2*PI*1046.5*(1+0.25*${STEP})*t)):d=4:s=44100"

ffmpeg -hide_banner -loglevel error -y -f lavfi -i "$CHIME" \
  -c:a libvorbis -q:a 4 -ac 1 "$OUT/ruby_chime.ogg"

ffmpeg -hide_banner -loglevel error -y -f lavfi -i "$WALTZ" \
  -c:a libvorbis -q:a 4 -ac 1 "$OUT/ruby_waltz.ogg"

for f in "$OUT"/*.ogg; do
  printf '%s  %s bytes  ' "$(basename "$f")" "$(stat -c %s "$f")"
  ffprobe -hide_banner -loglevel error \
    -show_entries stream=codec_name,sample_rate,channels \
    -show_entries format=duration -of default=nw=1:nk=1 "$f" | tr '\n' ' '
  echo
done
