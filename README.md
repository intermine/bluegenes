

# BlueGenes
## About
BlueGenes is designed to make searching and analysing genomic data easy. It's powered by [InterMine](http://intermine.org/) web services, meaning that the data from over 30 InterMines worldwide can be accessed from the same familiar interface.

[Try BlueGenes now](http://bluegenes.apps.intermine.org/)


![BlueGene screenshots](http://i.imgur.com/zwp0uxM.jpg)

**Feedback:** Please create an issue in this repo or email `info - at - intermine - dot - org`

## Setting up your dev environment and running BlueGenes

**Development BlueGenes** If you want to run BlueGenes locally so that you can modify it, please see [docs/getting-started](docs/getting-started.md).
**BlueGenes as built into InterMine 2.0** If you'd like to deploy BlueGenes as part of InterMine 2.0, see the [instructions to launch a BlueGenes target](https://intermine.readthedocs.io/en/intermine-2.0/system-requirements/software/gradle/index.html#deploy-blue-genes). 
**Standalone BlueGenes** If you'd like to launch BlueGenes without it being associated directly with a single InterMine, or if you haven't upgraded to InterMine 2.0 yet (but you're above 1.8), please see [docs/production-builts](docs/production-builds.md).
`

## Configuration

#### Adding a new mine
Open [src/cljc/bluegenes/mines.cljc](https://github.com/intermine/bluegenes/blob/dev/src/cljc/bluegenes/mines.cljc#L7-L51) and copy the value of the `sample-mine` variable into the `mines` hashmap. Change the key to something unique (`:yourmine`) and edit the default values appropriately. Be sure to edit the {:id ...} value to reflect the key you used for `:yourmine`

```clj
(def mines {:humanmine {...}
            :yourmine  {:id :yourmine
	                ...})
```



#### Changing the default mine
Open `src/cljs/bluegenes/db.cljs` and edit the `:current-mine` hashmap value to the keyword of a mine in `mines.cljc` (see above).

```clj
(def default-db
  {...
   :mine-name :yourmine
   ...}
```
Please note that you will have to recompile the application for the changes to take effect (see below). Also, may need to clear your local storage for the `:default-mine` to take effect. You can do this by visiting the web application, clicking the cog on the top right, selecting Debug, and then clicking the button to delete local storage.


##### Google Analytics
If you wish to track pages hits, set up [Google analytics](https://analytics.google.com/analytics/web/#embed/report-home/a76615855w155408876p157084577/) for your domain, then add your google analytics id to your config.edn files (mentioned above) or environment variables. This is completely optional.

```clojure
  {:google-analytics "UA-12345678-9"}
``

### Deploying to Clojars

When deploying BlueGenes to Clojars, the JAR file should include all compiled assets include javascript, less, and the vendor libraries. This allows other projects to include BlueGenes as a dependency and deploy the client and server without needing to compile BlueGenes. To deploy a compiled JAR to clojars, include the `uberjar` profile when running the `lein deploy clojars` command:
```bash
$ lein with-profile +uberjar deploy clojars
```


### Further help needed?

If you think the issue is related to InterMine or its webservices, check out the [InterMine documentation](http://intermine.readthedocs.io/en/latest/about/contact-us/)
Documentation on BlueGenes is available in [the docs folder](https://github.com/intermine/bluegenes/blob/dev/docs/index.md)
