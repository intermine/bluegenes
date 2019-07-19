# Configuration

This applies primarily to standalone BlueGenes.


## Note: Ways to Configure

The preferred way to configure BlueGenes is via `.edn` configuration files, and it is the one being explained here. All keys can be overridden with environment variables. Please refer to  [Yogthos's config docs](https://github.com/yogthos/config#yogthosconfig) for more details regarding this method.


## Configuration via `.edn` files

Copy and paste the code from the `.template` files in `config/dev/` and `config/prod/`, tweak it to match your preferences, and save it as `config.edn` in the same folder.


### Analytics (optional)

For now, the only supported statistics service is [Google analytics](https://analytics.google.com/). If you wish to use this service, configure your domain and then add your Google Analytics ID.

```clojure
  {:google-analytics "UA-12345678-9"}
```
