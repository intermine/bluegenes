# Development Notes

### General workflow

* User enters a string of identifiers into the textbox, or selects files to upload.
* On continue, the client sends a POST multipart request to <bluegenes-server>/api/ids/parse with the following params:
  * caseSensitive [true/false]
  * text [a plaintext string of identifiers to parse]
  * a-file-name [a file to parse]
* The server parses the plaintext string and any files and then, sends the results back to the client.
  * Duplicates are automatically removed, taking into account whether caseSensitive is true or false.
* The client posts an ID resolution to the InterMine server and polls for the results.
* Results are then managed by the user and a list can be saved.

### Notes

* The List Type dropdown only shows "qualified" InterMine classes, meaning that they have class keys. This rules out classes like Homologue.
* The Organism dropdown should be disabled and the value cleared if the selected List Type does not have a reference to an organism.
