onmessage = function(e) {
  console.log('Message received from main scriptttttttttttttttttttttttttt');

  var workerResult = 'Result: ' + (e.data[0] * e.data[1]);
  var str = e.data[0];
  var property = e.data[1];
  var books = e.data[2];
  var filtered = books.filter(function(book) {
    console.log("book title", book.title)
    console.log("MATCH", book.title.match(/e/));
    if (book.title.match(/x/i) == null) {
        return false;
    } else {
        return true;
    }

  });
  debugger;

  console.log('Posting message back to main script');
  postMessage(workerResult);
}