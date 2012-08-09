(ns formative.core
  (:require [formative.render-form :refer [render-form*]]
            [formative.render-form.table]
            [formative.render-field :refer [render-field]]
            [clojure.walk :refer [stringify-keys]]
            [clojure.string :as string]))

(def ^:dynamic *form-style* :table)

(defn normalize-field [field]
  (assoc field
    :name (name (:name field))
    :type (if (:type field)
            (keyword (name (:type field)))
            :text)))

(defn- ucfirst [^String s]
  (str (Character/toUpperCase (.charAt s 0)) (subs s 1)))

(defn field-name->label [fname]
  (-> fname
      (string/replace #"[_-]" " ")
      ucfirst))

(defmulti prep-field (fn [field values]
                       (:type field)))

(defmethod prep-field :default [field values]
  (assoc field
    :value (get values (:name field))
    :label (:label field (field-name->label (:name field)))))

(defmethod prep-field :checkbox [field values]
  (let [field (if (and (not (contains? field :value))
                        (not (contains? field :unchecked-value)))
                 (assoc field :value 1 :unchecked-value 0)
                 field)
        val (get values (:name field))]
    (assoc field
      :value (:value field 1)
      :checked (= (str val) (str (:value field 1)))
      :label (:label field (field-name->label (:name field))))))

(defmethod prep-field :submit [field values]
  (assoc field
    :value (:value field "")))

(defmethod prep-field :html [field values]
  field)

(defn prep-fields [names+fields values]
  (for [[fname field] (partition 2 names+fields)
        :let [field (assoc field :name fname)]]
    (-> field
        (normalize-field)
        (prep-field values))))

(defn prep-form [params names+fields]
  (let [form-attrs (select-keys
                    params [:action :method :enctype :accept :name :id :class
                            :onsubmit :onreset :accept-charset :autofill])
        form-attrs (assoc form-attrs
                     :form-style (:form-style params *form-style*))
        values (stringify-keys (:values params))
        fields (prep-fields names+fields values)
        fields (if (:cancel-href params)
                 (for [field fields]
                   (if (= :submit (:type field))
                     (assoc field :cancel-href (:cancel-href params))
                     field))
                 fields)
        fields (if (some #(= :submit (:type %)) fields)
                 fields
                 (concat fields
                         [{:type :submit
                           :name "submit"
                           :cancel-href (:cancel-href params)
                           :value (:submit-label params "Submit")}]))]
    [form-attrs fields]))

(defn render-form [params & names+fields]
  (apply render-form* (prep-form params names+fields)))

(defmacro with-form-style [style & body]
  `(binding [*form-style* ~style]
     (do ~@body)))