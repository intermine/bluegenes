# Configuration

This applies primarily to standalone BlueGenes.


## Note: Ways to Configure

The preferred way to configure BlueGenes is via `.edn` configuration files, and it is the one being explained here. All keys can be overridden with environment variables. Please refer to  [Yogthos's config docs](https://github.com/yogthos/config#yogthosconfig) for more details regarding this method.


## Configuration via `.edn` files

Copy and paste the code from the `.template` files in `config/dev/` and `config/prod/`, tweak it to match your preferences, and save it as `config.edn` in the same folder.


### Adding a new mine
Open [src/cljc/bluegenes/mines.cljc](https://github.com/intermine/bluegenes/blob/dev/src/cljc/bluegenes/mines.cljc#L7-L51) and copy the value of the `sample-mine` variable into the `mines` hashmap. Change the key to something unique (`:yourmine`) and edit the default values appropriately. Be sure to edit the {:id ...} value to reflect the key you used for `:yourmine`

```clojure

(def mines {:humanmine {...}
            :yourmine  {:id :yourmine
	                ...})
```

This configuration step will go away once we make BlueGenes start using the registry.


### Changing the default mine

Open `src/cljs/bluegenes/db.cljs` and edit the `:current-mine` hashmap value to the keyword of a mine in `mines.cljc` (see above).

``` clojure
(def default-db
  {...
   :mine-name :yourmine
   ...}
```
Please note that you will have to recompile the application for the changes to take effect (see below). Also, may need to clear your local storage for the `:default-mine` to take effect. You can do this by visiting the web application, clicking the cog on the top right, selecting Debug, and then clicking the button to delete local storage.


### Analytics (optional)

For now, the only supported statistics service is [Google analytics](https://analytics.google.com/). If you wish to use this service, configure your domain and then add your Google Analytics ID.

```clojure
  {:google-analytics "UA-12345678-9"}
```
