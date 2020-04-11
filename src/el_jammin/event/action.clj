(ns el-jammin.event.action
  (:import [javafx.scene.input KeyCode KeyEvent]
           [javafx.scene.control DialogEvent Dialog]
		   [javafx.stage FileChooser]
           [javafx.scene Node])
  (:require [el-jammin.state.state :refer :all]
            [el-jammin.rhythm.looper :refer :all]
            [el-jammin.rhythm.section :refer :all]
            [el-jammin.visualization.visualizer :refer :all]
            [cljfx.api :as fx])
  (:use [overtone.core]
	[overtone.inst.drum]
	[overtone.inst.piano]
	[overtone.inst.synth]))

(defn ucitaj-file
  [event]
  (let [window (.getWindow (.getScene ^Node (.getTarget event)))
        chooser (doto (FileChooser.)
                  (.setTitle "Open File"))]
    (when-let [file @(fx/on-fx-thread (.showOpenDialog chooser window))]
      (swap! *state assoc :file file)
      (swap! *state assoc :samples (cons (sample (str file)) (@*state :samples)))
      (swap! *state assoc :samples-title (cons (:name (sample (str file))) (@*state :samples-title))))))

(defn play-from-keyboard
  [event instr]
  (let [cd (.getCode ^KeyEvent (:fx/event event))
        octave (@*state :current-octave)
        option (@*state :option)
        chord-name (keyword (clojure.string/lower-case (@*state :chord-name)))]
    (when (= cd KeyCode/L) (swap! *state assoc :l (not (@*state :l))))
    (when (= cd KeyCode/A) (if (= option "Note") (instr (note (str "C"octave))) (play-chord instr (note (str "C"octave)) chord-name)))
    (when (= cd KeyCode/W) (if (= option "Note") (instr (note (str "C#"octave))) (play-chord instr (note (str "C#"octave)) chord-name)))
    (when (= cd KeyCode/S) (if (= option "Note") (instr (note (str "D"octave))) (play-chord instr (note (str "D"octave)) chord-name)))
    (when (= cd KeyCode/E) (if (= option "Note") (instr (note (str "D#"octave))) (play-chord instr (note (str "D#"octave)) chord-name)))
    (when (= cd KeyCode/D) (if (= option "Note") (instr (note (str "E"octave))) (play-chord instr (note (str "E"octave)) chord-name)))
    (when (= cd KeyCode/F) (if (= option "Note") (instr (note (str "F"octave))) (play-chord instr (note (str "F"octave)) chord-name)))
    (when (= cd KeyCode/R) (if (= option "Note") (instr (note (str "F#"octave))) (play-chord instr (note (str "F#"octave)) chord-name)))
    (when (= cd KeyCode/G) (if (= option "Note") (instr (note (str "G"octave))) (play-chord instr (note (str "G"octave)) chord-name)))
    (when (= cd KeyCode/T) (if (= option "Note") (instr (note (str "G#"octave))) (play-chord instr (note (str "G#"octave)) chord-name)))
    (when (= cd KeyCode/H) (if (= option "Note") (instr (note (str "A"octave))) (play-chord instr (note (str "A"octave)) chord-name)))
    (when (= cd KeyCode/Y) (if (= option "Note") (instr (note (str "A#"octave))) (play-chord instr (note (str "A#"octave)) chord-name)))
    (when (= cd KeyCode/J) (if (= option "Note") (instr (note (str "B"octave))) (play-chord instr (note (str "B"octave)) chord-name)))))

(defn safety-start-scope
  [event]
  (when (= false (:playing @scope))
    (start-scope (:fx/event event))))

(defn event-note-play
  [event]
  (case (@*state :loop-instrument)
    "Piano" (play-note event piano)
    "Guitar" (play-note event string)
    "Synthesizer" (play-note event overpad)))

(defn event-clean-and-play
  []
  (case (@*state :loop-instrument)
    "Piano" (clean-and-play piano)
    "Guitar" (clean-and-play string)
    "Synthesizer" (clean-and-play overpad)))

(defn event-play-from-keyboard
  [event]
  (case (@*state :instrument)
    "Piano" (play-from-keyboard event piano)
    "Guitar" (play-from-keyboard event string)
    "Synthesizer" (play-from-keyboard event overpad)))

(defn play-selected-item
  []
  (let [selected-item (@*state :selected-item)]
    (case selected-item
      "Kick & Clap 1" (kick-clap1 (m))
      "Kick" (my-kick (m))
      "Kick & Clap 2" (kick-clap2 (m))
      "Kick & Clap 3" (kick-clap3 (m))
      "Kick & Snare" (kick-snare (m))
      "Hi-hat 1" (my-hihat1 (m))
      "Hi-hat 2" (my-hihat2 (m))
      "Hi-hat 3" (my-hihat3 (m))
      "Hi-hat 4" (my-hihat4 (m))
      "Hi-hat 5" (my-hihat5 (m))
      nil (swap! *state assoc :poruka "You must select instrument.")
      (play-sample selected-item 0))))

(defn on-play
  [event]
  (do (play-selected-item)
      (if (not= (@*state :selected-item) nil)
        (swap! *state assoc :poruka ""))
      (safety-start-scope event)))

(defn on-stop
  [event]
  (do (stop)
      (swap! *state assoc :anim-status :stopped)
      (swap! *state assoc :set-speed-disabled false)
      (swap! *state assoc :looper-live false)
      (swap! *state assoc :add-new-loop false)
      (stop-timer (:fx/event event))))

(defn on-rec-stop
  []
  (do (recording-stop)
      (swap! *state assoc :stop-rec-disabled true)
      (swap! *state assoc :rec-disabled false)
      (swap! *state assoc :poruka "You stopped recording.")))

(defn on-press
  [event]
  (do
    (event-play-from-keyboard event)
    (safety-start-scope event)))

(defn loop-sample
  [selected-item event]
  (case (re-find #".wav" selected-item)
    nil (swap! *state assoc :poruka "You must select sample.")
    (do (play-sample selected-item 1)
        (swap! *state assoc :poruka "")
        (safety-start-scope event))))

(defn on-loop
  [event]
  (let [selected-item (@*state :selected-item)]
    (if (= selected-item nil)
      (swap! *state assoc :poruka "You must select sample.")
      (loop-sample selected-item event))))

(defn on-play-note
  [event]
  (do
    (event-note-play event)
    (safety-start-scope event)))

(defn on-looper-if-true
  [event]
  (do
    (swap! *state assoc :looper-live true)
    (swap! *state assoc :add-new-loop true)
    (swap! *state assoc :poruka "")
    (swap! *state assoc :set-speed-disabled true)
    (event-clean-and-play)
    (safety-start-scope event)))

(defn on-looper
  [event]
  (if (= 0.0 (@*state :loop-end))
    (swap! *state assoc :poruka "You must select end of the loop.")
    (on-looper-if-true event)))

(defn on-new-looper-true
  []
  (swap! *state assoc :add-new-loop false)
  (swap! *state assoc :loop-line '())
  (swap! *state assoc :loop-end 0.0)
  (swap! *state assoc :loop-line-id (inc (@*state :loop-line-id))))

(defn on-new-looper-capacity-true
  []
  (if (= true (@*state :add-new-loop))
    (on-new-looper-true)
    (swap! *state assoc :poruka "You already have line which is not looped.")))

(defn on-new-looper
  []
  (if (>= (count (@*state :loop-lines)) 3)
    (swap! *state assoc :poruka "Maximum number of loops.")
    (on-new-looper-capacity-true)))

(defn restart-all-components
  [event]
  (when (= true (@*state :looper-live))
    (on-stop event)))

(defn check-if-restart-message
  []
  (if (= 0 (count (@*state :loop-lines)))
    (swap! *state assoc :poruka "Your looper is already empty.")
    (swap! *state assoc :poruka "Your looper is now empty.")))

(defn check-if-restart
  []
  (when (> (count (@*state :loop-lines)) 0)
    (do
      (swap! *state assoc :loop-lines [])
      (swap! *state assoc :loop-lines-end [])
      (swap! *state assoc :loop-line-id 1)
      (swap! *state assoc :loop-line '())
      (swap! *state assoc :loop-end 0.0)
      (swap! *state assoc :add-new-loop false))))

(defn on-restart-looper
  [event]
  (check-if-restart-message)
  (check-if-restart)
  (restart-all-components event))

(defn enable-chord-name
  [event]
  (if (= (:option event) "Chord")
    (swap! *state assoc :chord-name-disabled false)
    (swap! *state assoc :chord-name-disabled true)))

(defn on-set-option
  [event]
  (do
    (swap! *state assoc :option (:option event))
    (enable-chord-name event)))

(defn on-set-instrument
  [event]
  (if (= (@*state :l) true)
    (swap! *state assoc :instrument (:option event))
    (swap! *state assoc :loop-instrument (:option event))))

(defn on-exit
  []
  (do
    (when (= true (:playing @scope))
      (.stop (:timer @scope)))
    (stop)))
