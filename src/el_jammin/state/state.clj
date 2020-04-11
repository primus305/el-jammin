(ns el-jammin.state.state
    (:use [overtone.core]))

(def m (metronome 128))
(def kick-side-bar '("Kick & Clap 1" "Kick" "Kick & Clap 2" "Kick & Clap 3" "Kick & Snare"))
(def kontra-side-bar '("Hi-hat 1" "Hi-hat 2" "Hi-hat 3" "Hi-hat 4" "Hi-hat 5"))
(def notes '("C" "C#" "D" "D#" "E" "F" "F#" "G" "G#" "A" "A#" "B"))


(def scope
  (atom {:first-time true
         :playing false}))

(def *state
  (atom {:selected-item nil
         :poruka ""
         :prikazi false
         :stop-rec-disabled true
         :rec-disabled false
         :set-speed-disabled false
         :file nil
         :samples '()
         :samples-title '()
         :loop-line '()
         :loop-end 0.0
         :loop-line-id 1
         :loop-lines []
         :loop-lines-end []
         :anim-status :stopped
         :anim-duration 0.0
         :to-x 0
         :current-octave 1
         :option "Note"
         :chord-name "Major"
         :chord-name-disabled true
         :instrument "Piano"
         :loop-instrument "Piano"
         :l true
         :looper-live false
         :add-loop-line false}))
