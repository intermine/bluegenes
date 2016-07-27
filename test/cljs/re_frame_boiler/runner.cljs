(ns re-frame-boiler.runner
    (:require [doo.runner :refer-macros [doo-tests]]
              [re-frame-boiler.core-test]))

(doo-tests 're-frame-boiler.core-test)
