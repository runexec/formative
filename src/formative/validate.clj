(ns formative.validate
  (:require [formative.data :as data]
            [clojure.string :as string]
            [jkkramer.verily :as v]))

(def ^:private us-states (set (map first data/us-states)))

(defn us-state [keys & [msg]]
  (v/make-validator keys #(and (not= :jkkramer.verily/absent %)
                               (not (string/blank? %))
                               (not (us-states %)))
                    (or msg "must be a valid US state")))

(def ^:private ca-states (set (map first data/ca-states)))

(defn ca-state [keys & [msg]]
  (v/make-validator keys #(and (not= :jkkramer.verily/absent %)
                               (not (string/blank? %))
                               (not (ca-states %)))
                    (or msg "must be a valid Canadian state")))

(def ^:private alpha2-countries (set (map :alpha2 data/countries)))

(defn country [keys & [msg]]
  (v/make-validator keys #(and (not= :jkkramer.verily/absent %)
                               (not (string/blank? %))
                               (not (alpha2-countries %)))
                    (or msg "must be a valid country")))

(defmethod v/validation->fn :us-state [vspec]
  (apply us-state (rest vspec)))

(defmethod v/validation->fn :ca-state [vspec]
  (apply us-state (rest vspec)))

(defmethod v/validation->fn :country [vspec]
  (apply us-state (rest vspec)))

(def type-validators
  {:us-zip v/us-zip
   :us-state us-state
   :ca-state ca-state
   :country country
   :email v/email
   :url v/url
   :web-url v/web-url
   :str v/string
   :strs v/strings
   :clob v/string
   :clobs v/strings
   :boolean v/bool
   :booleans v/bools
   :int v/integer
   :long v/integer
   :bigint v/integer
   :ints v/integers
   :longs v/integers
   :bigints v/integers
   :float v/floating-point
   :double v/floating-point
   :floats v/floating-points
   :doubles v/floating-points
   :decimal v/decimal
   :decimals v/decimals
   :date v/date
   :dates v/dates
   :currency v/decimal})

(defn- keywordize [x]
  (if (keyword? x) x (keyword (name x))))

(defn validate-types [fields values]
  (let [groups (group-by #(:datatype % (:type %))
                         fields)
        validators (for [[type tfields] groups
                         :let [validator (type-validators type)]
                         :when validator]
                     (validator (map (comp keywordize :name) tfields)))]
    ((apply v/combine validators) values)))

(defn validate [form values]
  (let [type-validator (partial validate-types (:fields form))
        validator (if-let [custom-validator (:validator form)]
                    (v/combine type-validator custom-validator)
                    type-validator)
        validator (if-let [validations (:validations form)]
                    (v/combine validator (v/validations->fn validations))
                    validator)]
    (validator values)))