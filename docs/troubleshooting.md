# Troubleshootings BlueGenes Issues

1. When stuff is being weird, one option is to delete your cache (do this manually using the browser) for bluegenes and in particular localstorage. In bluegenes, click on the cog in the top right, then "developer". There should be a big blue button that clears local storage.
2. Check what branch you have checked out. Usually `dev` is our main developer branch, and should always be in a working state.
3. Check which InterMine's web services you are using. Different InterMines may or may not be running the most recent version of InterMine. You can view a list of InterMines and their current version under the key `intermine_version` in the [InterMine registry](http://registry.intermine.org/service/instances). The changelog for InterMine release versions is [available on GitHub](https://github.com/intermine/intermine/releases). To change which InterMine you're using in BlueGenes, use the cog (top right) to select it.

If none of these help, run through the actions causing your error and screenshot your javascript console, then share in an [issue](https://github.com/intermine/bluegenes/issues) or [contact us (via chat, email, mailing list, etc.)](http://intermine.readthedocs.io/en/latest/about/contact-us/)
