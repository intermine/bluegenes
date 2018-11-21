# BlueGenes Tool API

The BlueGenes Tool API is designed to make it easy to run javascript-based tools in the BlueGenes non-js codebase. Anything that compiles to javascript is valid.

## Installing tools

Since BlueGenes tools are always JavaScript, we use npm (a popular JavaScript package manager) to manage the packages. You must have a recent version of npm and node installed. See our [requirements](https://github.com/intermine/bluegenes/blob/dev/README) for more info.

### Published tools
Tools that conform to the [tool API spec](tool-api.md) may be published in [npm](https://www.npmjs.com/) under the tag [bluegenes-intermine-tool](https://www.npmjs.com/search?q=keywords:bluegenes-intermine-tool). To install a package that is already published:

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
The default location for tools is the following path - you can modify it to elsewhere if you wish: https://github.com/yochannah/bluegenes/blob/tool-api-2018/config/defaults/config.edn#L3

#### Updating published tools

If a tool's npm package is updated, all you need to do in order to pull the updates is run `npm update` from your tools folder, and all your packages will be updated.

### Development tools

If your tool is under development, you'll need to mimic it living in the `tools/node_modules/@intermine` directory. Your tool should comply with the [tool API guidelines](tool-api.md).

1. In the folder where **your tool** is being developed, type `npm link`. This tells npm on your system that you have a local node package at this location.
2. In the bluegenes tool folder (which might be `bluegenes/tools`) type `npm link your-package-name` where your-package-name is the name defined for your package in its package.json.

This creates a symbolic link to your package using [npm link](https://docs.npmjs.com/cli/link), so any updates you make in your package will automatically be mirrored in BlueGenes.

## Creating new tools

You can create your own tools in JavaScript or any language that compiles to JavaScript (e.g. TypeScript, CoffeeScript, ClojureScript). We also have a [yeoman generator](https://github.com/intermine/generator-bluegenes-tool) that sets up the basics for you. See the [Tool API Guidelines](tool-api.md) for more details.

## Roadmap

Currently tools are only supported in report pages. Longer term we would like to add support for tools in list results pages.
