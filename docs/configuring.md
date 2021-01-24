# Configuring BlueGenes

## Environment variables

| Envvar | Description | Default |
| ------ | ----------- | ------- |
| SERVER_PORT | Port used by web server | 5000 |
| LOGGING_LEVEL | Minimum level for logging | :info |
| GOOGLE_ANALYTICS | Google Analytics tracking ID | nil |
| JWT_SECRET | Secret for signing JSON Web Tokens | nil |
| BLUEGENES_DEFAULT_SERVICE_ROOT | InterMine service that is default | https://alpha.flymine.org/alpha/ |
| BLUEGENES_DEFAULT_MINE_NAME | Optional default mine name to be displayed until it gets fetched | FlyMine |
| BLUEGENES_TOOL_PATH | Server directory where BlueGenes tools are installed | ./tools |

## How to configure

BlueGenes supports the many methods of specifying configuration keys provided by [Yogthos' config](https://github.com/yogthos/config#yogthosconfig). The configuration keys listed above can be specified in uppercase or lowercase, and with dashes, underscores or periods as separators.

### Configuration via `config.edn` files

Copy and paste the code from the `.template` files in `config/dev/` and `config/prod/`, tweak it to match your preferences, and save it as `config.edn` in the same folder.

### Analytics (optional)

For now, the only supported statistics service is [Google analytics](https://analytics.google.com/). If you wish to use this service, configure your domain and then add your Google Analytics ID using the `GOOGLE_ANALYTICS` key listed above.
