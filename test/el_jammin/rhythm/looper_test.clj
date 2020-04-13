(ns el-jammin.rhythm.looper-test
  (:require [clojure.test :refer :all]
            [el-jammin.rhythm.looper :refer :all]
            [cljfx.api :as fx])
  (:use [overtone.core]
        [overtone.inst.piano]))

(defn setup
  []
  (play-note {:event/type ::play-note :j 6 :i 4} piano)
  (play-note {:event/type ::play-note :j 6 :i 4} piano)
  (play-note {:event/type ::play-note :j 1 :i 3} piano)
  (play-note {:event/type ::play-note :j 1 :i 3} piano)
  (play-note {:event/type ::play-note :j 2 :i 7} piano)
  (play-note {:event/type ::play-note :j 1 :i 3} piano))

(defn teardown
  []
  (empty-loop-line))

(defn looper-line-fixture
  [looper-test]
  (setup)
  (looper-test)
  (teardown))

(use-fixtures :once looper-line-fixture)

(deftest cleaned-loop-line-test
  (testing "Testing if function for cleaning loop line work expected."
    (let [loop-line (cleaned-loop-line)]
      (is (= 2 (count loop-line))))))

(deftest play-note-test-true
  (testing "Testing if function for playing note and pushing it into
loop line work expected."
    (play-note {:event/type ::play-note :j 5 :i 8} piano)
    (is (= 7 (count-loop-line)))))

(deftest play-note-test-false
  (testing "Testing if play note function throws exception when the
wrong argument is passed to it."
    (is (thrown? IndexOutOfBoundsException (play-note {:event/type ::play-note :j 15 :i 4} piano)))))



