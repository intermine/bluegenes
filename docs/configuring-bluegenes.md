
# Configuration

## Adding a new mine
Open [src/cljc/bluegenes/mines.cljc](https://github.com/intermine/bluegenes/blob/dev/src/cljc/bluegenes/mines.cljc#L7-L51) and copy the value of the `sample-mine` variable into the `mines` hashmap. Change the key to something unique (`:yourmine`) and edit the default values appropriately. Be sure to edit the {:id ...} value to reflect the key you used for `:yourmine`

```clj
(def mines {:humanmine {...}
            :yourmine  {:id :yourmine
	                ...})
```



## Changing the default mine
Open `src/cljs/bluegenes/db.cljs` and edit the `:current-mine` hashmap value to the keyword of a mine in `mines.cljc` (see above).

```clj
(def default-db
  {...
   :mine-name :yourmine
   ...}
```
Please note that you will have to recompile the application for the changes to take effect (see below). Also, may need to clear your local storage for the `:default-mine` to take effect. You can do this by visiting the web application, clicking the cog on the top right, selecting Debug, and then clicking the button to delete local storage.


## Google Analytics
If you wish to track pages hits, set up [Google analytics](https://analytics.google.com/analytics/web/#embed/report-home/a76615855w155408876p157084577/) for your domain, then add your google analytics id to your config.edn files (mentioned above) or environment variables. This is completely optional.

```clojure
  {:google-analytics "UA-12345678-9"}
``
