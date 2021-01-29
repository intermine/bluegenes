(ns bluegenes.version)

;; This makes the `release` version passed using closure-defines (see project.clj)
;; available like any def-ed thing, with dev as the fallback value.
(goog-define release "dev")

;;;; Version numbers you *wouldn't* want to change (collected for reference).

;; Minimum InterMine API version Bluegenes supports.
;; this is not crazy to hardcode. The consequences of a mine that is lower than
;; the minimum version using bluegenes could potentially result in corrupt lists
;; so it *should* be hard to change.
;;https://github.com/intermine/intermine/issues/1482
(def minimum-intermine 27)

;; Signing in through a third-party OAuth 2.0 provider was added in this version.
(def oauth-support "5.0.0")

;; Prior to this InterMine version, multiple TagManager's on the backend
;; would cause updating and retrieving of tags set on lists to be buggy.
(def list-tags-support "4.1.3")

;; From this InterMine version, the `/bluegenes-properties` webservice was
;; added to act as a generic key-value store for BlueGenes. Many new features
;; depend on this and will show a useful warning with fallback behaviour if not
;; available.
(def bg-properties-support "5.0.0")

;; Prior to this InterMine API version, BlueGenes "logins" to the mine by using
;; basic-auth with username and password to the generate API access key
;; webservice. This is problematic as only one API token can exist a time,
;; meaning it will both invalidate any existing API key, and get invalidated
;; when a new API key is generated.
(def proper-login-support 31)

;;;; Version numbers you *might* want to change.

;; This is the current Tool API version. Increment this when you're forced to
;; perform a breaking change to the Tool API. All tools (as well as native viz
;; in `bluegenes.components.viz`) will then need to be updated to support the
;; new API and change their config version to the same number.
(def tool-api 2)
