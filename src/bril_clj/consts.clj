(ns bril-clj.consts)

(def test-files-dir (clojure.java.io/file "./test/bril_files/"))

(def test-bril-files 
  ; Source: https://clojuredocs.org/clojure.core/file-seq#example-59f3948ee4b0a08026c48c79
 (let [grammar-matcher (.getPathMatcher 
                           (java.nio.file.FileSystems/getDefault)
                           "glob:*.{bril}")]
   (->> "./test/bril_files"
        clojure.java.io/file
        file-seq
        (filter #(.isFile %))
        (filter #(.matches grammar-matcher (.getFileName (.toPath %))))
        ;; FIXME: bril2json can't handle imports!
        ;; see: https://github.com/sampsyo/bril/issues/423
        (remove #(re-find #"from" (slurp %))))))
 

