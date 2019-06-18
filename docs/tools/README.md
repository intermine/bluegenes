## Tools folder

This folder is only for tool-api compliant tools.

Any file/folder matching the pattern bluegenes** is automatically ignored by git.

To install a tool, run `npm install NameOfYourTool`. This assumes you have a
recent version of [npm and node](https://nodejs.org/en/download/) installed (you can check by running `node --version`). A full list of tools is on npm under the tag
[bluegenes-intermine-tool](https://www.npmjs.com/search?q=keywords:bluegenes-intermine-tool).

## For more details about writing tools, see:

- [tool admin/installation guide](tools.md)
- [tutoral: how to make a new tool](tool-api-tutorial.md)
- [tool api specs](tool-api-tutorial.md)
- [Installing tools in a dokku container](dokku-tool-installation.md)
