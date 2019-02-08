# API Docs

API docs are built using [codox](https://github.com/weavejester/codox) and deployed automatically to GitHub pages, using [TravisCI's GitHub pages integration](https://docs.travis-ci.com/user/deployment/pages/). See [.travis.yml](https://github.com/intermine/bluegenes/blob/dev/.travis.yml) for details.

## Replicating locally

If you would like to deploy API docs locally, run `lein codox`. Docs will be in the `target/doc` directory.

## Setting up on Travis

For this to run successfully on your own Travis branch, you'll need to add a GitHub API token.

1. Visit your Github [Personal access tokens](https://github.com/settings/tokens) and create a token with the `public_repo` access only.
2. In TravisCI, visit the settings for your repo and add an environment variable named `GITHUB_TOKEN`. Paste your new token into the value field.
3. Once the build has run successfully, you may need to enable GitHub pages deploying from the gh-pages branch in your GitHub repository settings. 


## General note: We need to improve docstrings

Many functions are missing docstrings. Always aim to leave the API docs in a better state than you found them - if you spend a minute or two figuring out what a function does, please add or update its docstring.
