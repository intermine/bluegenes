# Local build

Start by installing the dependencies in [System Requirements](/docs/building.md#system-requirements), then [Download NPM dependencies](/docs/building.md#download-npm-dependencies) and finally, run the command in [Running a dev environment](/docs/building.md#running-a-dev-environment). For more information, refer to the rest of this document or the [FAQ](/docs/useful-faqs.md).

## System Requirements

* Java 8-11 (we recommend [OpenJDK](https://adoptopenjdk.net/))
* Latest [Leiningen](https://leiningen.org/)
* Latest supported [nodejs](https://nodejs.org/). You can check your version using `node -v`. We recommend installing node using [nvm](https://github.com/creationix/nvm)
* Latest supported [npm](https://www.npmjs.com/)

## Download NPM dependencies

    npm install

## Quickstart

These commands are explained in more depth below, but if you know what you want, here's a quick reference of the most useful ones.

    lein dev         # start dev server with hot-reloading
    lein repl        # start dev server with hot-reloading and nrepl (no clean or css)
    lein prod        # start prod server
    lein deploy      # build prod release and deploy to clojars

    lein format      # run cljfmt to fix code indentation
    lein kaocha      # run unit tests
    npx cypress run  # run cypress ui tests

## Running a dev environment

You can start a complete developer environment with automatic compilation of Less CSS and hot-reloading of code changes by running:

    lein dev

However, all the output will be thrown into one terminal. If you wish to keep the processes separate, you can start them individually by following the instructions below.

### Compile the CSS

We use [less](http://lesscss.org/) to write our styles.

You can compile the Less sources to CSS with the command below:

    lein less

It will run in watch mode, recompiling the CSS after every change until you exit it with *Ctrl+C*. The browser page will automatically reload the updated CSS.

### Make Leiningen reload code changes in the browser

    lein figwheel dev

Note: if you use `lein run` or any alias calling it like `dev` or `repl`, Figwheel will be started automatically.

### Start the web server

If you have OpenJDK 8, run this command:

    lein with-profile +dev run

If you have OpenJDK 9, run this command:

    lein with-profile +java9 figwheel

By default, the web server will be started on http://localhost:5000/. To change this value, edit the corresponding `config.edn`.


## Running tests

### Unit tests

You can run the ClojureScript unit tests by invoking the test runner kaocha.

    lein kaocha

Kaocha also has a watch mode that's useful when editing the tests.

    lein kaocha --watch

If you need something faster, evaluating `(cljs.test/run-tests)` from a connected editor would be even better. Although with async tests, you'll need some way of [having your editor report the results](https://clojurescript.org/tools/testing#detecting-test-completion-success).

### Cypress integration tests

Checklist:
- You might have to install [additional cypress dependencies](https://docs.cypress.io/guides/guides/continuous-integration.html#Dependencies) to be able to run Cypress.
- The integration tests are designed to be run against [Biotestmine](https://github.com/intermine/biotestmine). The easiest way to achieve this is by using [intermine_boot](https://github.com/intermine/intermine_boot) to start one locally.

Start BlueGenes connected to your local Biotestmine by using the command below. If your local Biotestmine runs on a different address than http://localhost:9999/biotestmine, you can specify it with the `BLUEGENES_DEFAULT_SERVICE_ROOT` envvar.

    lein biotestmine

Run all the Cypress tests:

    npx cypress run

Cypress also has a really useful interface for debugging failing tests:

    DEBUG=cypress:* npx cypress open

# Production Builds

## Testing a minified instance before deploying:

Most of the time, we develop with uncompressed files since it's faster for hot reloads. But for production, we want things to be extra fast on load and we don't hot-reload changes, so it's better to present minified files that have had all un-necessary code stripped. Clojure uses Google Closure tools (yes, Clojure uses Closure) to minify things.

Sometimes the Closure compiler is overzealous and removes something we actually want to keep. To check what your work looks like in a minified build, run this in the terminal (I'd recommend closing any existing lein run / lein figwheel sessions first).

    lein with-profile prod cljsbuild once min
    lein with-profile prod run

There is also a shortcut that in addition, cleans and compiles CSS.

    lein prod


## Deploying your build

### docker

BlueGenes has a Dockerfile which you can build with a fresh uberjar.

    lein uberjar
    docker build -t bluegenes .

Run it and the web server should default to port 5000.

    docker run -p 5000:5000 -it --rm bluegenes

You can specify environment variables by using the `-e` or `--env-file` arguments when calling docker. See [Configuring](/docs/configuring.md) for a list of all available environment variables.

There is also a [prebuilt docker image available on Docker Hub](https://hub.docker.com/r/intermine/bluegenes/tags).

    docker pull intermine/bluegenes:latest

For an example of running BlueGenes in docker for a production environment, start by creating a `bluegenes.env` file.

    BLUEGENES_DEFAULT_SERVICE_ROOT=https://mymine.org/mymine
    BLUEGENES_DEFAULT_MINE_NAME=MyMine
    BLUEGENES_DEFAULT_NAMESPACE=mymine

Once you have added all your environment variables, start the docker container.

    docker run -p 5000:5000 --env-file bluegenes.env -v "$(pwd)"/tools:/tools -d --restart unless-stopped bluegenes

This will create a `tools` folder in the current directory mounted as a docker bind mount. This is so your tools can be persisted when changing BlueGenes versions and to facilitate manual modification of tools.

### Dokku

[Dokku](http://dokku.viewdocs.io/dokku/) allows you to push a Git branch and have it automatically build and serve it using the appropriate docker container. (You can also use BlueGenes with [heroku](https://www.heroku.com/).)

Once dokku is configured on your remote host, you'll need to add your public key, create a remote for your host and push to it:

    # On your dokku host
    sudo dokku ssh-keys:add your-user /path/to/your/public/key
    # On your dev computer
    git remote add dokku dokku@your-host:bluegenes
    git push dokku master

If you want to deploy a different branch, you can use `git push dokku dev:master` (replace *dev* with the branch you wish to deploy from) instead.

#### Deploying docker images to dokku instance

You can also deploy docker images that you build locally, to the dokku instance. This is useful since it's much quicker and avoids the extra load on the dokku server from building.
Make sure `youruser` on `dokku-server` is part of the `docker` group. This allows you to stream the file instead of manually transferring it through ssh.

```
lein uberjar
docker build -t dokku/appname:v1 .
docker save dokku/appname:v1 | bzip2 | ssh youruser@dokku-server "bunzip2 | docker load"
ssh youruser@dokku-server
sudo dokku tags:deploy appname v1
```

Note that the tag `v1` needs to be unique for each deployment. It is common to use a version string and increment it for each deployment (might not necessarily correspond with version releases). See the [dokku documentation](http://dokku.viewdocs.io/dokku/deployment/methods/images/#deploying-an-image-from-ci) for more information.

### Uberjar

It's also possible to compile BlueGenes to a jar that will automatically launch a server when executed.

To compile and package BlueGenes into an executable jar, run the following command in the project folder:

    lein uberjar

Then, to start the application, execute the jar and pass in a [`config.edn` file](/docs/configuring.md) like so:

    java -jar -Dconfig="config/prod/config.edn" target/bluegenes.jar

For security reasons, the `config.edn` file used to execute the jar can be located anywhere, including your home directory.

You can alternatively bundle `config/prod/config.edn` into the uberjar by including the prod profile:

    lein with-profile prod uberjar


### Launching your uberjar with InterMine

InterMine 2.0 includes a [Gradle target to launch a BlueGenes instance](http://intermine.org/im-docs/docs/system-requirements/software/gradle/index#trying-out-bluegenes).

By default, it launches the latest BlueGenes release from Clojars. If you want to update the version of the JAR being launched, you'll need to create an uberjar (see above).


### Deploying your uberjar to Clojars

Official BlueGenes releases can be deployed to [Clojars](https://clojars.org/), under the [org.intermine Clojars organisation](https://clojars.org/groups/org.intermine).

When deploying BlueGenes to Clojars, the JAR file should include all compiled assets. This includes JavaScript, less, and vendor libraries. This allows other projects to include BlueGenes as a dependency and deploy the client and server without needing to compile BlueGenes.

To deploy a compiled JAR to Clojars, simply use the `deploy` alias which automatically includes the `uberjar` profile and targets Clojars.

    $ lein deploy

### Releasing a new version

1. Update the version number in **project.clj**.
2. Don't forget to add the new version with notes to **CHANGELOG.md**.
3. Commit this change and tag it using `git tag -a v1.0.0 -m "Release v1.0.0"`, replacing *1.0.0* with your version number.
4. Push your commit and tag it using `git push origin` followed by `git push origin v1.0.0` (again, replace *1.0.0* with your version number). Make sure that you push to the intermine repository, not just your fork!
5. Deploy a new uberjar to Clojars with `lein deploy`.
6. Push a new docker image to dockerhub.
    1. `lein uberjar`
    2. `docker build -t bluegenes .`
    3. `docker tag bluegenes intermine/bluegenes:1.0.0` (remember to use your correct version number)
    4. `docker tag bluegenes intermine/bluegenes:latest`
    5. `docker push intermine/bluegenes:1.0.0`
    6. `docker push intermine/bluegenes:latest`
7. Deploy the latest release to dokku with `git push dokku dev:master`.

# Troubleshooting

1. When things get weird, you might consider clearing both your browser's and BlueGene's cache data. To clear BlueGenes', click on the cog at the top right, then "developer". There should be a big blue button that clears local storage.
2. Verify what branch you have checked out. `dev` is our main development branch, whereas `master` is our production branch.
3. Verify that the InterMine web services ("InterMines") you are using are running the latest InterMine release. You will find a list of InterMines and their current version under the key `intermine_version` in the [InterMine registry](http://registry.intermine.org/service/instances). The changelog for InterMine release versions is [available on GitHub](https://github.com/intermine/intermine/releases).
4. Remember that you can always change which InterMine you're using in BlueGenes by using the cog (top right).

If none of these tips help you, create an [issue](https://github.com/intermine/bluegenes/issues) or [contact us (via chat, email, mailing list, etc.)](http://intermine.org/im-docs/docs/about/contact-us).
