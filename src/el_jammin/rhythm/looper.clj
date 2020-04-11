(ns el-jammin.rhythm.looper
  (:require [el-jammin.state.state :refer :all])
  (:use [overtone.core]))

(defn has-value [key1 value1 key2 value2]
  (fn [m]
    (and (= value1 (m key1)) (= value2 (m key2)))))

(definst sample-inst
  [note 59 level 1 rate 1 loop? 0
   attack 0 decay 1 sustain 1 release 0.1 curve -4 gate 1 buf (buffer 2048)]
  (let [env (env-gen (adsr attack decay sustain release level curve)
                     :gate gate
                     :action FREE)]
    (* env (scaled-play-buf 1 buf :level level :loop loop? :action FREE))))

(defn play-chord [instrument root chord-name]
  (doseq [note (chord root chord-name)]
    (instrument note)))

(defn check-note-option
  [option instr octave j chord-name]
  (if (= option "Note")
    (instr (note (str (nth notes j)octave)))
    (play-chord instr (note (str (nth notes j)octave)) chord-name)))
	
(defn play-sample
  [smpl loop]
  (sample-inst :loop? loop :buf (first (filter (fn [x] (= (:name x) smpl)) (@*state :samples)))))

(defn play-note
  [event instr]
  (let [j (:j event)
        i (:i event)
        octave (@*state :current-octave)
        option (@*state :option)
        chord-name (keyword (clojure.string/lower-case (@*state :chord-name)))]
    (swap! *state assoc :loop-line (cons (assoc {} :i i :j j :beat (* i 0.25) :note (str (nth notes j)octave) :option option :chord-name chord-name) (@*state :loop-line)))
    (check-note-option option instr octave j chord-name)))

(defn play-looper
  [beat x instr]
    (let [loop-line (x (@*state :loop-lines))
          next-beat (+ (x (@*state :loop-lines-end)) beat)
          chord-name (keyword (clojure.string/lower-case (@*state :chord-name)))]
      (dotimes [i (count loop-line)]
        (at (m (+ (:beat (nth loop-line i)) beat))
            (if (= (:option (nth loop-line i)) "Note")
              (instr (note (:note (nth loop-line i))))
              (play-chord instr (note (:note (nth loop-line i))) (:chord-name (nth loop-line i))))))
      (apply-by (m next-beat) #'play-looper [next-beat x instr])))

(defn if-duplicate
  [i j cleaned-line]
  (if (= 0 (mod (count (filter (has-value :i i :j j) cleaned-line)) 2))
    (remove #(= (first (filter (has-value :i i :j j) cleaned-line)) %) cleaned-line)
    cleaned-line))

(defn remove-from-loop-line
  []
  (loop [loop-line (@*state :loop-line)
         cleaned-line (@*state :loop-line)]
    (if (empty? loop-line)
      cleaned-line
      (let [i (:i (first loop-line))
            j (:j (first loop-line))]
        (recur (rest loop-line) (if-duplicate i j cleaned-line))))))

(defn if-exist
  [i j cleaned-line loop-line]
  (if (= (count (filter (has-value :i i :j j) cleaned-line)) 0)
    (cons (first (filter (has-value :i i :j j) loop-line)) cleaned-line)
    cleaned-line))

(defn cleaned-loop-line
  []
  (loop [loop-line (remove-from-loop-line)
         cleaned-line '()]
    (if (empty? loop-line)
      cleaned-line
      (let [i (:i (first loop-line))
            j (:j (first loop-line))]
        (recur (rest loop-line) (if-exist i j cleaned-line loop-line))))))

(defn clean-loop-line
  []
  (when (< (count (@*state :loop-lines)) (@*state :loop-line-id))
    (let [cleaned-line-loop (cleaned-loop-line)]
      (swap! *state assoc :loop-line cleaned-line-loop)
      (swap! *state assoc :loop-lines (conj (@*state :loop-lines) cleaned-line-loop))
      (swap! *state assoc :loop-lines-end (conj (@*state :loop-lines-end) (@*state :loop-end))))))

(defn start-animation
  []
  (do
    (swap! *state assoc :anim-duration (* (last (@*state :loop-lines-end)) (double (/ 60 (:bpm m)))))
    (swap! *state assoc :to-x (* 200 (last (@*state :loop-lines-end))))
    (swap! *state assoc :anim-status :running)))

(defn clean-and-play
  [instr]
  (clean-loop-line)
  (start-animation)
  (case (count (@*state :loop-lines))
    1 (play-looper (m) first instr)
    2 (play-looper (m) second instr)
    3 (play-looper (m) last instr)))
