(ns vg-cli.core
  (:require [clojure.java.shell :as shell]
            [cheshire.core :as json]
             [taoensso.timbre :as log]
            )
  )

(def vega-version "5.9.0")
(def vega-lite-version "4.0.2")
(def vega-embed-version "6.0.0")


(defn- vega-cli-installed? [mode]
  (case mode
    :vega-lite (= 0 (:exit (shell/sh "vl2svg" "--help")))
    :vega      (= 0 (:exit (shell/sh "vg2svg" "--help")))))

(defn- tmp-filename
  [ext]
  (str (java.io.File/createTempFile (str (java.util.UUID/randomUUID)) (str "." (name ext)))))
  

(defn vg-cli
  "Takes either spec or the contents of spec-filename, and uses the vega/vega-lite cli tools to translate to the specified format.
  If both spec and spec-filename are present, writes spec to spec-filename for running cli tool (otherwise, a tmp file is used)."
  ([{:keys [spec scale seed format mode spec-filename output-filename return-output?]
     :or {format :svg mode :vega-lite return-output? true}}]
   {:pre [(#{:vega-lite :vega} mode)
          (#{:png :pdf :svg :vega} format)
          (or spec spec-filename)]}
   (if (vega-cli-installed? mode)
     (let [short-mode (case (keyword mode) :vega-lite "vl" :vega "vg")
           ext (name (if (= format :vega) :vg format))
           spec-filename (or spec-filename (tmp-filename (str short-mode ".json")))
           output-filename (or output-filename (tmp-filename ext))
           command (str short-mode 2 ext)
           ;; Write out the spec file, and run the vega(-lite) cli command
           _ (when spec
               (spit spec-filename (json/encode spec)))
           {:keys [out exit err]} (shell/sh command spec-filename output-filename)] 
       (log/info "input:" spec-filename)
       (log/info "output:" output-filename)
       (if (= exit 0)
         (when return-output?
           (slurp output-filename))
         (do
           (log/error "Problem creating output")
           (log/error err)
           err)))
     (log/error "Vega CLI not installed! Please run `npm install -g vega vega-lite vega-cli` and try again. (Note: You may have to run with `sudo` depending on your npm setup.)"))))
     ;; Todo; should be throwing
