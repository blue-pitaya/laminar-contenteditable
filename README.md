# Laminar contenteditable

Simple library for simulating extended textarea.

## XSS warning

Component is using innerHTML assign to update content. Although user input is escaped by default (so user can't inject some html/js code), your function is not escaped.

## Running tests

Tests are based on selenium. To run properly you must manually install firefox and chromium drivers. On linux based OS you should look for package: `geckodriver`, `chromiumdriver`. 

### Running test in background (or on server)

[source](https://github.com/scala-js/scala-js-env-selenium#xvfb)

A common approach on Linux and Mac OSX, is to use `xvfb`, "X Virtual FrameBuffer".
It starts an X server headlessly, without the need for a graphics driver.

Once you have `xvfb` installed, usage here with SBT is as simple as:
```sh
Xvfb :1 &
DISPLAY=:1 sbt
```

The `:1` indicates the X display-number, which is a means to uniquely identify an
X server on a host. The `1` is completely arbitrary—you can choose any number so
long as there isn't another X server running that's already associated with it.
