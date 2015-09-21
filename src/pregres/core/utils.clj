(ns pregres.core.utils
  (:require [clojure.string :as str])
  (:import
   java.io.ByteArrayOutputStream
   java.io.ByteArrayInputStream
   java.io.BufferedReader
   java.io.InputStreamReader
   java.util.zip.GZIPOutputStream
   java.util.zip.GZIPInputStream
   (java.sql Timestamp)
   (java.text SimpleDateFormat)
   (java.util Calendar Date GregorianCalendar TimeZone)))

;; ----------------------------------------------------------------------------
;; Keyword Functions

(defn underscores->hyphens [k]
  (when k
    (-> k name (.replace "_" "-") keyword)))

(defn underscores->hyphens [k]
  (when k
    (-> k name (.replace "_" "-") keyword)))

(defn hyphens->underscores [k]
  (when k
    (-> k name (.replace "-" "_") keyword)))

(defn keyword->hyphenated-string [k]
  (-> k name (.replace "_" "-")))

(defn keyword->hyphenated-keyword [k]
  (-> k keyword->hyphenated-string keyword))

(defn keyword->underscored-string [k]
  (-> k name (.replace "-" "_")))

(defn keyword->underscored-keyword [k]
  (-> k keyword->underscored-string keyword))

;; ----------------------------------------------------------------------------
;; Seqs

(defn only
  "Gives the sole element of a sequence"
  [coll]
  (if (seq (rest coll))
    (throw (RuntimeException. "should have precisely one item, but had at least 2"))
    (if (seq coll)
      (first coll)
      (throw (RuntimeException. "should have precisely one item, but had 0")))))

;; ----------------------------------------------------------------------------
;; Time

(def ^:const yyyy-mm-dd "yyyy-MM-dd")

(defn yyyy-mm-dd? [x]
  (and (string? x)
       (boolean (re-matches #"\d{4}-\d{2}-\d{2}" x))))

(def ^:const yyyy-mm-dd-hh-mm "yyyy-MM-dd HH:mm")

(defn yyyy-mm-dd-hh-mm? [x]
  (and (string? x)
       (boolean (re-matches #"\d{4}-\d{2}-\d{2} \d{2}:\d{2}" x))))

(def ^:const yyyy-mm-dd-hh-mm-ss "yyyy-MM-dd HH:mm:ss")

(def ^:const yyyy-mm-dd-hh-mm-ss-SSS "yyyy-MM-dd HH:mm:ss.SSS")

(defn yyyy-mm-dd-hh-mm-ss? [x]
  (and (string? x)
       (boolean (re-matches #"\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}" x))))

(def ^:const american-date-format "MM/dd/yyyy")

(def ^TimeZone utc-tz (TimeZone/getTimeZone "UTC"))

(defn ^SimpleDateFormat simple-date-format
  ([format-string]
     (simple-date-format format-string utc-tz))
  ([format-string tz-or-tz-str]
     (let [^TimeZone tz (if (instance? TimeZone tz-or-tz-str)
                          tz-or-tz-str
                          (TimeZone/getTimeZone tz-or-tz-str))]
       (doto (SimpleDateFormat. format-string)
         (.setTimeZone tz)))))

(defn ^Long ->timestamp
  ([s format-string tz-or-tz-str]
     (-> (simple-date-format format-string tz-or-tz-str)
         (.parse s)
         .getTime))
  ([s format-string]
     (->timestamp s format-string utc-tz))
  ([x]
     (cond
      (integer? x)
      (long x)

      (yyyy-mm-dd? x)
      (->timestamp x yyyy-mm-dd)

      (yyyy-mm-dd-hh-mm? x)
      (->timestamp x yyyy-mm-dd-hh-mm)

      (yyyy-mm-dd-hh-mm-ss? x)
      (->timestamp x yyyy-mm-dd-hh-mm-ss)

      (and (string? x) (re-matches #"\d+" x))
      (Long/parseLong x)

      :else
      (throw (IllegalArgumentException. (str "Don't know how to parse " (pr-str x)))))))

(defn ^:dynamic now []
  (System/currentTimeMillis))

(defn ->str
  ([x]
     (->str x yyyy-mm-dd-hh-mm-ss))
  ([x date-format]
     (->str x date-format utc-tz))
  ([x date-format tz-or-tz-str]
     (.format
      (simple-date-format date-format tz-or-tz-str)
      (Date. (long (->timestamp x))))))

(defn ->date-str [x]
  (.format
   (simple-date-format yyyy-mm-dd)
   (Date. (long (->timestamp x)))))

(defn ->sql-time [timestamp]
  (->str timestamp yyyy-mm-dd-hh-mm-ss-SSS))

(defn ->Timestamp
  "Creating a UTC string and parsing it adjusts the timezone
   properly. (Timestamp. millis) alters the millis into the local timezone"
  [ts]
  (Timestamp/valueOf (->sql-time ts)))

;; ----------------------------------------------------------------------------
;; GZIP

(defn compress [^String in]
  (let [^ByteArrayOutputStream byte-stream (ByteArrayOutputStream.)]
    (with-open [^GZIPOutputStream gzip-stream (GZIPOutputStream. byte-stream)]
      (.write gzip-stream (.getBytes in "UTF-8")))
    (.toByteArray byte-stream)))

(defn stream-contents [byte-stream & [^String encoding]]
  (let [buf (StringBuffer.)]
    (with-open [^GZIPInputStream gzip-stream (GZIPInputStream. byte-stream)]
      (let [reader (BufferedReader.
                    (InputStreamReader. gzip-stream (or encoding "UTF-8")))]
        (loop [l (.readLine reader)]
          (if (nil? l)
            (.toString buf)
            (do
              (.append buf l)
              (recur (.readLine reader)))))))))

(defn decompress [byte-array & [encoding]]
  (stream-contents (ByteArrayInputStream. byte-array) encoding))
