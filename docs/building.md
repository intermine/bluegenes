# Local build

## System Requirements

* OpenJDK, version 8 (only until we make our software compatible with OpenJDK 11)
* Latest [Leiningen](https://leiningen.org/)
* Latest supported [nodejs](https://nodejs.org/).  You can check your version using `node -v`). We recommend installing node using [nvm](https://github.com/creationix/nvm)
* Latest supported [npm](https://www.npmjs.com/)
* InterMine version 1.8+ (version 2.0 recommended)

## Download NPM dependencies

```
npm install
```

## Compile the CSS

We use [less](http://lesscss.org/) to write our styles.

If you only want the CSS to be uploaded once, run:

    lein less once

Note: in this case you will have to manually recompile the CSS files after each change.

If you will be changing the CSS files continuously, you can automatically recompile the CSS files after each change using:

    lein less auto

Note: even that you will not see a prompt telling you when it's complete, the browser page will automatically refresh.


## Make Leiningen reload code changes in the browser

    lein figwheel dev


## Start the web server

If you have OpenJDK 8:

    lein with-profile +dev run

If you have OpenJDK 9:

    lein with-profile +java9 figwheel


By default, the web server will be started on http://localhost:5000/. To change this value, edit the corresponding `config.edn`.


## Running tests


    lein clean
    lein doo phantom test once

The above command assumes you will use [phantomjs](https://www.npmjs.com/package/phantomjs) (already declared as a Node.js development dependency). However, please note that [doo](https://github.com/bensu/doo) can be configured to run cljs.test in many other JS environments (chrome, ie, safari, opera, slimer, node, rhino, or nashorn).



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
