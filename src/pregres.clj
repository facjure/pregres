(ns pregres
  (:require
   [clojure.tools.logging :as log]
   [pregres.db :as db]
   [pregres.types]))

;; Note: importing pregres.types above registers sql/time coercions

