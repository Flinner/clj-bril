(defproject bril-clj "0.1.0-SNAPSHOT"
  :description "bril (Big Red IR) compiler, following through Cornell's CS6120 course"
  :url "https://github.com/Flinner/clj-bril"
  :license {:name "AGPL-3.0"
            :url "https://www.gnu.org/licenses/agpl-3.0.html"}
  :dependencies [[org.clojure/clojure "1.10.3"]
                 [org.clojure/data.json "2.4.0"]]
                 
  :main ^:skip-aot bril-clj.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
