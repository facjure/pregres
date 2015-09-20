(ns pregres.csv
  "Wrapper around clojure-csv to build flexible k/v maps."
  (:require
   [clojure.data.csv :as csv]))

(set! *warn-on-reflection* true)

(def ^:dynamic *parse-opts*
  {:skip-header false
   :delimiter \,
   :end-of-line nil
   :quote-char \"
   :strict false})

(defn read-csv
  ([csv-rdr]
   (read-csv csv-rdr *parse-opts*))
  ([csv-rdr opts]
   (let [merged-opts (merge *parse-opts* opts)
         {:keys [skip-header delimiter end-of-line
                 quote-char strict]} merged-opts]
     (->> (csv/read-csv csv-rdr
                        :delimiter delimiter
                        :end-of-line end-of-line
                        :quote-char quote-char
                        :strict strict)
          (#(if skip-header (rest %) %))
          (filter #(not= [""] %))))))

(defn import-csv
  "Turn CSV into column-name, key value pair that can be configured via :key-fn,
  :val-fn and :reader opts for each field"
  [csv-row {:keys [key-fn val-fn pred-fn exclude-columns]
            :or {key-fn identity
                 val-fn identity
                 pred-fn (constantly true)}
            :as field-reader-opts}]
  (let [add-column (fn [m i]
                     (let [col (get csv-row i)
                           field (get field-reader-opts i)]
                       (if (and field
                                (not (contains? exclude-columns i)))
                         (assoc m (:label field) ((:reader field) col))
                         m)))
        csv-row-value (->> csv-row
                           count
                           range
                           (reduce add-column nil)
                           val-fn)
        csv-rows->coll (->> csv-row
                            (map #(csv-row->value % field-reader-opts))
                            (filter pred-fn))]
    (reduce #(assoc %1 (key-fn %2) %2)
            nil
            (csv-rows->coll csv-row field-reader-opts))))
