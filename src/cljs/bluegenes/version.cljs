(ns bluegenes.version)

;;;; Version numbers you *wouldn't* want to change (collected for reference).

;; this is not crazy to hardcode. The consequences of a mine that is lower than
;; the minimum version using bluegenes could potentially result in corrupt lists
;; so it *should* be hard to change.
;;https://github.com/intermine/intermine/issues/1482
(def minimum-intermine 27)

;; Prior to this InterMine version, multiple TagManager's on the backend
;; would cause updating and retrieving of tags set on lists to be buggy.
(def organize-support "4.1.3")

;;;; Version numbers you *might* want to change.

;; This is the current Tool API version. Increment this when you're forced to
;; perform a breaking change to the Tool API. All tools (as well as native viz
;; in `bluegenes.components.viz`) will then need to be updated to support the
;; new API and change their config version to the same number.
(def tool-api 2)
