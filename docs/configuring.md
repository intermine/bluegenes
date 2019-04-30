# Configuration

This applies primarily to standalone BlueGenes.

## Adding a new mine
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


## Google Analytics

If you wish to track pages hits, set up [Google analytics](https://analytics.google.com/analytics/web/#embed/report-home/a76615855w155408876p157084577/) for your domain, then add your google analytics id to your config.edn files (mentioned above) or environment variables. This is completely optional.

```clojure
  {:google-analytics "UA-12345678-9"}
```


# Example configuration

All keys can be overridden with environment variables, such as `DATABASE_URL=http://...`
These properties are read in by [yogthos](https://github.com/yogthos/), which can read in variables in a variety of ways
See [Yogthos's config docs](https://github.com/yogthos/config#yogthosconfig) for more details.

Copy and paste the code below and save it as `config.edn`, in the same `config` subfolder you inted to use. Don't forget to tweak it to match your preferred properties!

```clojure
{
 :server-port 5000
 :logging-level :info
 ; Keep this secret! It's used to digitally sign JSON Web Tokens
 :jwt-secret "If two witches would watch two watches, which witch would watch which watch?"

 ;Pass a Google Analytics tracking ID here (or not)
 :googleAnalytics "yourTrackingIdHere"

 ;if BlueGenes is being initialised by InterMine, the IM instance will
 ;need to tell BlueGenes which URL to default to. Set below,
 ;or on the command line set "bluegenes_default_service_root"
 ;NOTE: This only applies if you are running InterMine 2.0 or later.
 :bluegenes-default-service-root "http://beta.flymine.org/beta"

;Optional - set a mine name so it's not blank in the few seconds before the
 ;properties are loaded from intermine's web-properties endpoint.
 :bluegenes-default-mine-name "FlyMine Beta"
 }
 ```
