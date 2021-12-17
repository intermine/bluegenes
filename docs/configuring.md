# Configuring BlueGenes

## Environment variables

| Envvar | Description | Default |
| ------ | ----------- | ------- |
| SERVER_PORT | Port to be used by BlueGenes web server | 5000 |
| LOGGING_LEVEL | Minimum level for logging | info |
| GOOGLE_ANALYTICS | Google Analytics tracking ID | nil |
| BLUEGENES_TOOL_PATH | Directory on server where BlueGenes tools are installed | ./tools |
| BLUEGENES_DEPLOY_PATH | Custom URL path to host BlueGenes. Must start with `/` and **not** end with `/`, e.g. `/bluegenes`. If you wish to host at root, set to `nil`. | nil |
| BLUEGENES_BACKEND_SERVICE_ROOT | Override BLUEGENES_DEFAULT_SERVICE_ROOT for backend API requests (usually an internal address) | nil |
| BLUEGENES_DEFAULT_SERVICE_ROOT | Default InterMine service to run API requests against | https://www.flymine.org/flymine |
| BLUEGENES_DEFAULT_MINE_NAME | Mine name to display for default mine | FlyMine |
| BLUEGENES_DEFAULT_NAMESPACE | Namespace of the default mine | flymine |
| BLUEGENES_ADDITIONAL_MINES | Additional mines managed by this BlueGenes instance | [] |
| HIDE_REGISTRY_MINES | Disable acquiring and displaying mines from the public InterMine registry | false |

## How to configure

BlueGenes supports the many methods of specifying configuration keys provided by [Yogthos' config](https://github.com/yogthos/config#yogthosconfig). The configuration keys listed above can be specified in uppercase or lowercase, and with dashes, underscores or periods as separators.

### Configuration via `config.edn` files

Copy and paste `config/defaults/config.edn` to `config/dev/config.edn` or `config/prod/config.edn` and tweak it to match your preferences. The correct config file will be used depending on if you start BlueGenes intended for development or for production. Note that `config/defaults/config.edn` will be used when you build an uberjar or docker image, unless you use `lein with-profile prod uberjar` to bundle `config/prod/config.edn` instead.

### Analytics (optional)

For now, the only supported statistics service is [Google analytics](https://analytics.google.com/). If you wish to use this service, configure your domain and then add your Google Analytics ID using the `GOOGLE_ANALYTICS` key listed above.
