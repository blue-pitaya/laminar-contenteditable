# Laminar contenteditable

![Preview of pickers.](/preview.png)

Simple library for simulating extended textarea in [Laminar](https://laminar.dev/).

See [demo](https://blue-pitaya.github.io/laminar-contenteditable/).

## Instalation

For Laminar 16:

```scala
libraryDependencies += "dev.bluepitaya" %%% "laminar-contenteditable" % "0.2"
```

## Usage

Library exposed single component `Editor.component(options: Options = Editor.Options())`.

```scala
case class Options(
    parseText: String => String = identity,
    textSignal: Signal[String] = Signal.fromValue(""),
    onTextChanged: Observer[String] = Observer[String](_ => ()),
    autoIndent: Signal[Boolean] = Val(false),
    autoIndentChar: Char = '\t'
)
```

This component is div with contenteditable prop that behave like textarea. The most important part of options is `parseText`. Parse function can change text to add some HTML tags to existing text.

Component is not adding any additional styles to mimic textarea. You have to provide one on your own (or copy one from example).

### Example

```scala
def component() = {
  val text =
    Var("""Some hex colors (try write one yourself): #FF0000 #00aa00 #0000ff
  |	Auto indent included. (hit enter on this line)
  |
  |Text in quotes is 'big'!!!""".stripMargin)

  val editorOptions = Editor.Options(
    parseText = (text: String) => {
      // special HTML characters (like single quote) are escaped
      val quoteRegex = """&#39;(.+?)&#39;""".r
      val halfParsedText = quoteRegex.replaceAllIn(
        text,
        m => s"""<span style="font-size: 20px;">'${m.group(1)}'</span>"""
      )
      val colorRegex = """#([0-9]|[a-f]|[A-F]){6}""".r
      val parsedText = colorRegex.replaceAllIn(
        halfParsedText,
        m => s"""<span style="color: $m;">$m</span>"""
      )

      parsedText
    },
    autoIndent = Val(true),
    textSignal = text.signal,
    onTextChanged = text.writer
  )

  val editorStyles = Seq(
    padding("10px"),
    border("1px solid black"),
    width("600px"),
    height("300px"),
    overflowY.auto
  )

  div(
    display.flex,
    flexDirection.row,
    styleProp("gap")("30px"),
    div(
      h2("Extended textarea"),
      Editor.component(editorOptions).amend(editorStyles)
    ),
    div(h2("Raw text"), pre(child.text <-- text.signal))
  )
}
```

### XSS warning

Component is using innerHTML assign to update content. Although user input is escaped by default (so user can't inject some html/js code), your function is not escaped.

## Todos

Raw text and parsed text are depended on each other at this moment. For example, if you provide parse function that changes "red" to "blue", then after typing "red" original text will change to "blue". This can be problematic in some use cases. In future, dependency of text and parsed text will be optional.

## Development

### Running example

To run example page you need to:

1. Run `sbt` -> `project example` -> `~fastLinkJS`
2. Execute `yarn` (only once to install JS deps) -> `yarn dev` in `example/ui` dir.

### Running tests

Tests are based on selenium. To run properly you must manually install firefox and chromium drivers. On linux based OS you should look for package: `geckodriver`, `chromiumdriver`. Also you need to uncomment one line in build.sbt.

#### Running test in background (or on server)

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
