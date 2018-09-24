# Bluegenes Tool API Tutorial

You don't need to know Clojure/Clojurescript in order to create a data vis / analysis tool for BlueGenes. This document will walk you through converting your favourite javascript tool into a BlueGenes compatible tool. You may wish to keep the [Tool API documentation](tool-api) open in another tab.

## Getting ready:

What you'll need to build the tool:

- npm and node 7+, preferably installed via [nvm](https://github.com/creationix/nvm)
- A JavaScript tool that you would like to implement in a BlueGenes report page. See our [Tool Board](https://github.com/intermine/bluegenes/projects/2#column-943519) for some options.
- [yeoman](http://yeoman.io/) (You can install via `npm install -g yo`).
- A text editor and modern browser.

Optionally, to test your tool:

- [A local BlueGenes install](https://github.com/intermine/bluegenes/blob/dev/docs/getting-started.md)

## Generate your tool scaffold

If you look at the [Tool API documentation](tool-api), you many note that BlueGenes tools have a specific folder structure. The good news is that you don't have to create all these files yourself! We have a yeoman generator which makes things easier for you.

### Install the Yeoman Generator

Assuming you've installed yeoman already, run the following command to install the BlueGenes tool generator:

```bash
 npm install -g generator-bluegenes-tool
```

Brilliant! You'll only need to install the generator once.

### Generate a new project: The wizard

In the parent folder where you'd like your project to live, make a new folder with your preferred name (perhaps something like `~/projects/myBluegenesTool`), and then run:

```
yo bluegenes-tool
```

This will walk you through a few questions in a step-by-step wizard. We'll walk through what each one means now:

1. **? What shall we name your project? This is a computer name with no spaces or special characters.** a name for your tool. We'd recommend prefixing every tool with the word bluegenes, e.g. `bluegenesProtVista` or `bluegenesCytoscape`. If you're not sure what you want to call your project yet, you could name it `bluegenesTest`
2. **Thanks! Now, give me a human name for the project - e.g. "Protein Feature Viewer"** - a nice friendly name for humans to read - spaces are allowed. Examples might be "BlueGenes ProtVista" or "BlueGenes Cytoscape Interaction Viewer".
3. **Fabulous. Which report pages do you expect this tool to work for, e.g. "Gene" or "Protein"? Separate with commas and put * for all.** This needs to be an InterMine class or classes - for the [Cytoscape interaction viewer](https://github.com/intermine/bluegenes/projects/2#column-943519), I entered `Protein, Gene` since we can show Protein and Gene interactions in this tool.
4. **Awesome. What type of InterMine data can you work with?** - Right now, you should only select `id` - the tool API as of version 1 only supports the report page. Selecting id means that the tool will be passed the id of a single InterMine entity - e.g. a protein might be represented by the ID 4815162342.
5. **What's your name?** Hopefully you know the answer to this one! ;) This is important for package.json, which we will automatically generate you.
6. **Your email** As above - it's useful for package.json.
7. **Your website**  As above - it's useful for package.json.
8. **Which license do you want to use?** we're pre-provided a few licences to choose from. Whichever you choose, remember that InterMine is LGPL 2.1, meaning it can be taken into private repositories. This is _not_ compatible with viral licences like GPL. For your tool, LGPL, MIT, or Apache might be good choices that are compatible with LGPL.

Once you select a licence, the yeoman installer should set up your repository and all the files within based on the responses you gave to the wizard. This may take a minute or two.

### Setting up your newly scaffolded tool

//TODO:

- explain signature, importing other packages, imtables dependencies
- config json & how to use it to convert to an im-tables friendly request
- namespacing css and importing external css
- bundling
- releasing on npm.
- testing on bluegenes
