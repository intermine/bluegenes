# BlueGenes Tool API

The BlueGenes Tool API is designed to make it easy to run javascript-based tools in the BlueGenes non-js codebase. Anything that compiles to javascript is valid.

## Installing tools

Since BlueGenes tools are always JavaScript, we use npm (a popular JavaScript package manager) to manage the packages. You must have a recent version of npm and node installed. See our [requirements](https://github.com/intermine/bluegenes/blob/dev/docs/getting-started.md#system-requirements) for more info.

### Published tools
Tools that conform to the [tool API specifications](tool-api.md) may be published in [npm](https://www.npmjs.com/) under the tag [bluegenes-intermine-tool](https://www.npmjs.com/search?q=keywords:bluegenes-intermine-tool). To install a package that is already published, run:

```bash
# make a tools directory somewhere on your computer
mkdir tools
cd tools
npm init -y # This creates package.json, where your tool list is stored.
npm install --save some-tool-name-here
```

This will install the tool into the `tools/node_modules/@intermine` directory, and save the tool to your package.json file.

#### When launching BlueGenes via a JAR
The tool folder will automatically be picked up and displayed in any relevant pages, so long as you [configure InterMine so it knows where to look for the tools folder](https://intermine.readthedocs.io/en/latest/webapp/blue-genes/).

#### When working locally with a lein-compiled BlueGenes
The default location for tools is the following path (you can modify it to elsewhere if you wish): https://github.com/yochannah/bluegenes/blob/tool-api-2018/config/defaults/config.edn#L3

#### Dokku-based deployments

If you're launching BlueGenes via Dokku, we recommend mounting the tools in a folder outside the container, using [dokku:storage](https://github.com/dokku/dokku/blob/master/docs/advanced-usage/persistent-storage.md). This allows you to install and update tools without having to re-start the container, and ensures that the tools persist even if the container is restarted or redeployed. 

#### Updating published tools

If a tool's npm package is updated, all you need to do in order to pull the updates is run `npm update` from your tools folder, and all your packages will be updated.

### Development tools

If your tool is under development, you'll need to mimic it living in the `tools/@intermine` directory. Your tool should comply with the [tool API guidelines](tool-api.md). 

To add a tool, visit the `tools` directory and add a new entry with the path to your tool. This can be realtive - e.g. you might add a new line that says `"../../path/to/my/tool" "1.0.0"` to add a recently developed new tool. Any changes you make to the tool in its own directory will automatically apply to BlueGenes (although you'll probably have to refresh your browser to load the new JS bundle).

## Creating new tools

You can create your own tools in JavaScript or any language that compiles to JavaScript (e.g. TypeScript, CoffeeScript, ClojureScript). We also have a [yeoman generator](https://github.com/intermine/generator-bluegenes-tool) that sets up the basics for you. See the [Tool API Guidelines](tool-api.md) for more details.

## Roadmap

Some of the planned work:
 - Support for global variables and events (i.e. for javascript libraries that aren't modern enough to export a single module). 
