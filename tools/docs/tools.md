# BlueGenes Tool API

The BlueGenes Tool API is designed to make it easy to run javascript-based tools in the BlueGenes non-js codebase. Anything that compiles to javascript is valid.

## Installing tools

Since BlueGenes tools are always JavaScript, we use npm (a pipular JavaScript package manager) to manage the packages. You must have a recent version of npm and node installed. See our [requirements](https://github.com/intermine/bluegenes/blob/dev/README) for more info.

### Published tools
Tools that conform to the [tool API spec](tool-api.md) may be published in [npm](https://www.npmjs.com/) under the tag [bluegenes-intermine-tool](https://www.npmjs.com/search?q=keywords:bluegenes-intermine-tool). To install a package that is already published:

```bash
# assuming you are in the root bluegenes directory
cd tools
npm install --save some-tool-name-here
```

This will install the tool into the `tools/node_modules/@intermine` directory, and save the tool to your package.json file. It will automatically be picked up and displayed in any relevant pages.

### Development tools

If your tool is under development, you'll need to mimic it living in the `tools/node_modules/@intermine` directory. Your tool should comply with the [tool API guidelines](tool-api.md).

1. In the folder where **your tool** is being developed, type `npm link`. This tells npm on your system that you have a local node package at this location.
2. In `bluegenes/tools` type `npm link your-package-name` where your-package-name is the name defined for your package in package.json.

This creates a symbolic link to your package using [npm link](https://docs.npmjs.com/cli/link), so any updates you make in your package will automatically be mirrored in BlueGenes.

## Creating new tools

You can create your own tools in JavaScript or any language that compiles to JavaScript (e.g. TypeScript, CoffeeScript, ClojureScript). We also have a [yeoman generator](https://github.com/intermine/generator-bluegenes-tool) that sets up the basics for you. See the [Tool API Guidelines](tool-api.md) for more details.

## Roadmap

Currently tools are only supported in report pages. Longer term we would like to add support for tools in list results pages.
