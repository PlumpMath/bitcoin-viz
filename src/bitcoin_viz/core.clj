(ns bitcoin-viz.core
  (:require [quil.core :as q]
            [quil.middleware :as m]
            [clojure.core.async :refer [sliding-buffer go-loop >!]]
            [midje.sweet :refer [fact]]
           [clojure.data.json :as json]))

(def price-chan (sliding-buffer 50))

(def coinbase-api "https://coinbase.com")

(def price-endpoint (str coinbase-api "/api/v1/prices/spot_rate"))

(defn get-bitcoin-price []
   (json/read-str (slurp price-endpoint) :key-fn keyword))

(defn start-grabber [c]
  (go-loop []
    (>! c (:amount get-bitcoin-price))
    ))

(get-bitcoin-price)

(def max-price 800)
(def min-price 400)

(fact "scale-factor should be the number that you need
      to multiply a field by so all number in it fit in a range"
      (scaler 5 15 0 5) => [(/ 1 2) -5]
      (scaler 10 20 10 5) => [(/ 1 2) 0]
      (scaler 0 10 0 5)=> [(/ 1 2) 0])

(defn scaler [min-value max-value min-range max-range]
  (let [offset (- 0 (- min-value min-range))
        scaler (/ max-range (- (+ max-value offset) (+ min-value offset)))]
   [scaler offset]))

(defn scale [[factor offset] value]
  (+ offset  (* factor value)))

(defn update [state]
  (update-in state [:x] inc))

(defn draw [state]
  (q/background 255)
  (q/ellipse (:x state) (:y state) (:r state) (:r state)))

 
