## Writing ClojureScript unit tests

### re-frame.test macros

There are some things you need to pay extra attention to when writing ClojureScript unit tests for re-frame. Firstly, you'd want to wrap the body of a `deftest` with one of:

- `run-test-sync` Use this when you wish to `dispatch` events sequentially, and do not need to wait for a chain of events to finish.
- `run-test-async` This is so you can use `wait-for` to block until specific event handlers are run. Make sure you're using `dispatch-sync` instead of the regular async dispatch.

The two macros above also make sure to revert any effects, coeffects, event handlers, etc. that are registered inside their body, to keep re-frame clean for the next unit test to run.

### Redefs and stubs

The `(with-redefs <bindings> ...)` form is invaluable for stubbing arbitrary functions. Most of the time you'll be using this for `imcljs.fetch`. Note that this only applies to variables inside the calling scope of `with-redefs` (ie. it won't apply to an event handler dispatched by an event handler you dispatched; it only applies to the first in the chain).

If you need to stub a variable that isn't in your calling scope, you'll need to resort to `set!`.

    (let [orig-fn fetch/session]
      (set! fetch/session (stub-fetch-fn "anon-token"))
      (swap! stubbed-variables conj #(set! fetch/session orig-fn)))

This should do the trick but could cause problems depending on the asynchronicity of your code. (I'd love to wrap this inside a utility function or macro but haven't been successful due to how `set!` works in JavaScript.)

### Documenting assertions

To improve documentation, put your `is` assertions (which optionally take a message as their second argument) inside the body of `(testing "message" ...)` forms. Calls to `testing` may be nested, and all of the strings will be joined together with spaces in the final report.

### Resources

- https://github.com/Day8/re-frame-test
- https://juxt.pro/blog/posts/cljs-apps.html
- https://clojurescript.org/tools/testing
- https://clojure.github.io/clojure/clojure.test-api.html
