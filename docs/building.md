# Local build

You can run bluegenes locally for development purposes. Here's what the local setup should look like.

## System Requirements
* Java 6+
* [Leiningen 2.5+](https://leiningen.org/)
* [node 7+][nodejs]  (you can check your version using `node -v`). We recommend installing node using [nvm](https://github.com/creationix/nvm)
* **Required:** The InterMine you point BlueGenes at *must* be running InterMine 1.8 or later; ideally 2.0.

### Download dependencies.

```
npm install
```

### Compile the CSS

We use [less](http://lesscss.org/) to write our styles. In order to run BlueGenes you'll need to compile the less, using


```
lein less once
```

_Or_, if you'll be making lots of style edits and don't want to type `lein less once` every time you make a change, you can automatically recompile the css file whenever it changes, using:

```
lein less auto
```

Note that you won't see a prompt telling you when it's complete if you use `lein less auto` - but the browser page will automatically refresh so you'll know when it's done.

###


### Start the process to reload code changes in the browser:

```
lein figwheel dev
```

### Start the web server:

In another terminal, run the following

If you have **Java 8** or lower:  

```
lein with-profile +dev run
```

**Java 9+**:  

```bash
lein with-profile +java9 figwheel
```

Then visit http://localhost:5000/ (or whichever port you specific in config.edn)

### Run tests:

```
lein clean
lein doo phantom test once
```

The above command assumes you will use [phantomjs](https://www.npmjs.com/package/phantomjs) (already declared as a Node.js development dependency). However, please note that [doo](https://github.com/bensu/doo) can be configured to run cljs.test in many other JS environments (chrome, ie, safari, opera, slimer, node, rhino, or nashorn).

## What next?

Once you're happy with any edits you've made, you probably want to check that it all works the same in a minified prod build. See [production builds](production-builds.md) for info on deploying and testing a minified build.


[lein]: https://github.com/technomancy/leiningen
[npm]: https://www.npmjs.com/
[nodejs]: https://nodejs.org/



# Building BlueGenes for production

The steps details in [getting-started](getting-started.md) are designed to get you up and running for developing BlueGenes.
But now that you've completed your snazzy updates and you want to deploy the BlueGenes to production, how do you do it?
There are actually a few ways, depending in your needs.

## Testing a minified instance before deploying:

Most of the time, we develop with uncompressed files - it's faster for hot reloads. But for production, we want things to be extra fast on load and we don't hot-reload changes, so it's better to present minified files that have had all un-necessary code stripped. Clojure uses Google Closure tools (yup, Clojure uses Closure) to minify things.

Sometimes the Closure compiler is overzealous and removes something we actually wanted to keep. To check what your work looks like in a minified build, run this in the terminal (I'd recommend closing any existing lein run / lein figwheel sessions first).

```bash
lein cljsbuild once min + lein run
```

OR there is also a shortcut - you could just say this for the same results

```
lein prod
```

## Standalone BlueGenes

One of the easiest ways to deploy the prod minified version is to set up [Dokku](http://dokku.viewdocs.io/dokku/) on your intended server. You can also use BlueGenes with [heroku](https://www.heroku.com/).

### Minified deployment using dokku
Once dokku is configured on your remote host, all you need to do to deploy a minified build is add the server as a remote and push to it:

```bash
	git remote add my-awesome-server bluegenes@my-awesome-server.git
        git push my-awesome-server master
```

### Uberjar

It's also possible to compile BlueGenes to a jar that will automatically launch a server when executed.

To compile and package BlueGenes into an executable jar, run the following command in the project folder:
```bash
$ lein uberjar
```
Then, to start the application, execute the jar and pass in a [`config.edn` file](../config/dev/README.md):

```bash
$ java -jar -Dconfig="config/prod/config.edn" target/bluegenes.jar
```

(When executing the jar the `config.edn` file can be located anywhere, including your home directory for security.)

## BlueGenes as a Jar on Clojars

InterMine 2.0 includes [a Gradle target to launch BlueGenes](https://intermine.readthedocs.io/en/latest/system-requirements/software/gradle/index.html#deploy-blue-genes). If you want to update the version of the JAR being launched, you'll need to create an uberjar as abov, OR if it's an official InterMine release, then it can be deployed to clojars, an online artifact repository. Here's how, assuming you have access to deploy to the [org.intermine organisation on clojars](https://clojars.org/groups/org.intermine). Speak to Yo for access if you need it.

### Deploying to Clojars

When deploying BlueGenes to Clojars, the JAR file should include all compiled assets include javascript, less, and the vendor libraries. This allows other projects to include BlueGenes as a dependency and deploy the client and server without needing to compile BlueGenes. To deploy a compiled JAR to clojars, include the `uberjar` profile when running the `lein deploy clojars` command:
```bash
$ lein with-profile +uberjar deploy clojars
```


# Troubleshootings BlueGenes Issues

1. When stuff is being weird, one option is to delete your cache (do this manually using the browser) for bluegenes and in particular localstorage. In bluegenes, click on the cog in the top right, then "developer". There should be a big blue button that clears local storage.
2. Check what branch you have checked out. Usually `dev` is our main developer branch, and should always be in a working state.
3. Check which InterMine's web services you are using. Different InterMines may or may not be running the most recent version of InterMine. You can view a list of InterMines and their current version under the key `intermine_version` in the [InterMine registry](http://registry.intermine.org/service/instances). The changelog for InterMine release versions is [available on GitHub](https://github.com/intermine/intermine/releases). To change which InterMine you're using in BlueGenes, use the cog (top right) to select it.

If none of these help, run through the actions causing your error and screenshot your javascript console, then share in an [issue](https://github.com/intermine/bluegenes/issues) or [contact us (via chat, email, mailing list, etc.)](http://intermine.readthedocs.io/en/latest/about/contact-us/)
