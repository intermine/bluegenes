# Installing tools on Dokku

To install a tool on a dokku instance (such the server that bluegenes.apps.intermine.org lives on), we need to connect to the server in question, enter the container, and npm install the desired tools. 

Assuming the server is called ernie:

1. `ssh ernie` (enter password if needed)
2. `sudo docker enter bluegenes` to enter the container running bluegenes
3. `cd /intermine/tools` (or whatever tools directory you have configured)
4. Now you should be able to install tools via npm as normal, e.g. `npm install some-tool-name-here --save` or `npm install` if you want to install all tools that are saved in package.json. 
