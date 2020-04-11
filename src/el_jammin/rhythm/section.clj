(ns el-jammin.rhythm.section
	(:require [el-jammin.state.state :refer :all])
    (:use [overtone.core]
			[overtone.inst.drum]))

(defn my-sequencer [my-metronome beat pattern scale idx]
  (doseq [[sound ptn] @pattern]
    (when (= 1 (nth ptn (mod idx (count ptn))))
      (at (my-metronome beat) (sound))))
  (let [next-beat (+ scale beat)]
    (apply-by (my-metronome next-beat) my-sequencer [my-metronome next-beat pattern scale (inc idx)])))

(defn kick-clap1
  [beat]
  (let [next-beat (inc beat)]
    (at (m beat)
        (quick-kick :amp 0.5)
        (if (zero? (mod beat 2))
          (clap :amp 0.1)))

   (apply-by (m next-beat) #'kick-clap1 [next-beat])))

(defn my-hihat1
  [beat]
  (let [next-beat (inc beat)]
    (at (m beat)
        (closed-hat2 :amp 0.3))
    (at (m (+ 0.5 beat))
        (closed-hat2 :amp 0.3))

    (apply-by (m next-beat) #'my-hihat1 [next-beat])))

(defn my-hihat2
  [beat]
  (let [next-beat (inc beat)]
    (at (m beat)
        (soft-hat :decay 0.03 :amp 0.5))
    (at (m (+ 0.25 beat))
        (soft-hat :decay 0.03 :amp 0.5))
    (at (m (+ 0.5 beat))
        (soft-hat :decay 0.03 :amp 0.5))
    (at (m (+ 0.75 beat))
        (soft-hat :decay 0.03 :amp 0.5))

    (apply-by (m next-beat) #'my-hihat2 [next-beat])))

(defn my-hihat3
  [beat]
  (let [next-beat (inc beat)]
    (at (m beat)
        (closed-hat2 :amp 0.3))
    (at (m (+ 0.25 beat))
        (closed-hat2 :amp 0.3))
    (at (m (+ 0.5 beat))
        (closed-hat2 :amp 0.3))
    (at (m (+ 0.75 beat))
        (closed-hat2 :amp 0.3))

    (apply-by (m next-beat) #'my-hihat3 [next-beat])))

(defn my-hihat4
  [beat]
  (let [next-beat (inc beat)]
    (at (m beat)
        (closed-hat2 :amp 0.3))

    (apply-by (m next-beat) #'my-hihat4 [next-beat])))

(defn my-hihat5
  [beat]
  (let [next-beat (inc beat)]
    (at (m (+ 0.5 beat))
        (closed-hat2 :amp 0.3))

    (apply-by (m next-beat) #'my-hihat5 [next-beat])))

(defn my-kick
  [beat]
  (let [next-beat (inc beat)]
    (at (m beat)
        (quick-kick :amp 0.5))

    (apply-by (m next-beat) #'my-kick [next-beat])))

(def ritam3
  {quick-kick    [1 0 0 0 0 0 0 1 0 0 1 0 0 0 0 0 ]
   clap    [0 0 0 0 1 0 0 0 0 0 0 0 1 0 0 0 ]})

(def *ritam3 (atom ritam3))

(defn kick-clap2
  [beat]
  (my-sequencer m beat *ritam3 1/4 0))

(def ritam4
  {quick-kick    [1 0 0 0 0 0 0 0 1 0 1 0 0 0 0 0 ]
   clap    [0 0 0 0 1 0 0 0 0 0 0 0 1 0 0 0 ]})

(def *ritam4 (atom ritam4))

(defn kick-clap3
  [beat]
  (my-sequencer m beat *ritam4 1/4 0))

(def ritam5
  {quick-kick    [1 0 0 1 0 0 1 0 0 0 0 0 0 0 0 0 ]
   noise-snare   [0 0 0 0 0 0 0 0 1 0 0 0 0 0 0 0 ]})

(def *ritam5 (atom ritam5))

(defn kick-snare
  [beat]
  (my-sequencer m beat *ritam5 1/4 0))

(definst string [note 60 amp 1.0 dur 0.5 decay 30 coef 0.3 gate 1]
  (let [freq (midicps note)
        noize (* 0.8 (white-noise))
        dly   (/ 1.0 freq)
        plk   (pluck noize gate dly dly decay coef)
        dist  (distort plk)
        filt  (rlpf dist (* 12 freq) 0.6)
        clp   (clip2 filt 0.8)
        reverb (free-verb clp 0.4 0.8 0.2)]
    (* amp (env-gen (perc 0.0001 dur) :action 0) reverb)))