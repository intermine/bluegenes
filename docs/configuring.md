# Configuring BlueGenes

## Environment variables

| Envvar | Description | Default |
| ------ | ----------- | ------- |
| SERVER_PORT | Port to be used by BlueGenes web server | 5000 |
| LOGGING_LEVEL | Minimum level for logging | info |
| GOOGLE_ANALYTICS | Google Analytics tracking ID | nil |
| BLUEGENES_TOOL_PATH | Directory on server where BlueGenes tools are installed | ./tools |
| BLUEGENES_DEFAULT_SERVICE_ROOT | Default InterMine service to run API requests against | https://www.flymine.org/flymine |
| BLUEGENES_DEFAULT_MINE_NAME | Mine name to display for default mine | FlyMine |
| BLUEGENES_DEFAULT_NAMESPACE | Namespace of the default mine | flymine |
| BLUEGENES_ADDITIONAL_MINES | Additional mines managed by this BlueGenes instance | [] |
| HIDE_REGISTRY_MINES | Disable acquiring and displaying mines from the public InterMine registry | false |

## How to configure

BlueGenes supports the many methods of specifying configuration keys provided by [Yogthos' config](https://github.com/yogthos/config#yogthosconfig). The configuration keys listed above can be specified in uppercase or lowercase, and with dashes, underscores or periods as separators.

### Configuration via `config.edn` files

Copy and paste `config/defaults/config.edn` to `config/dev/config.edn` or `config/prod/config.edn` and tweak it to match your preferences. When building an uberjar or docker image, `config/prod/config.edn` will be bundled and its values used as defaults unless overridden by the other methods documented in the link above.

### Analytics (optional)

For now, the only supported statistics service is [Google analytics](https://analytics.google.com/). If you wish to use this service, configure your domain and then add your Google Analytics ID using the `GOOGLE_ANALYTICS` key listed above.
