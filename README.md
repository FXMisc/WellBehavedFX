**This project is no longer being maintained. See [this issue](https://github.com/FXMisc/WellBehavedFX/issues/22) for more details.**

WellBehavedFX
=============

This project provides a better mechanism for defining and overriding event handlers (e.g. keyboard shortcuts) for JavaFX. Such mechanism, also known as InputMap API, was considered as part of [JEP 253](http://openjdk.java.net/jeps/253) (see also [JDK-8076423](https://bugs.openjdk.java.net/browse/JDK-8076423)), but was dropped. (I guess I was the most vocal opponent of the proposal ([link to discussion thread](http://mail.openjdk.java.net/pipermail/openjfx-dev/2015-August/017667.html)).)


Use Cases
---------

### Event Matching ###

Use cases in this section focus on expressivity of event matching, i.e. expressing what events should be handled.

#### Key Combinations ####
The task is to add handlers for the following key combinations to `Node node`:

| Key Combination | Comment | Handler |
| --------------- | ------- | ------- |
| <kbd>Enter</kbd> | no modifier keys pressed | `enterPressed()` |
| [<kbd>Shift</kbd>+]<kbd>A</kbd> | optional <kbd>Shift</kbd>, no other modifiers | `aPressed()` |
| <kbd>Shortcut</kbd>+<kbd>Shift</kbd>+<kbd>S</kbd> |  | `saveAll()` |

```java
Nodes.addInputMap(node, sequence(
    consume(keyPressed(ENTER),                        e -> enterPressed()),
    consume(keyPressed(A, SHIFT_ANY),                 e -> aPressed()),
    consume(keyPressed(S, SHORTCUT_DOWN, SHIFT_DOWN), e -> saveAll())
));
```

#### Same action for different events ####
In some situations it is desirable to bind multiple different events to the same action. Task: Invoke `action()` when a `Button` is either left-clicked or <kbd>Space</kbd>-pressed. If these are the only two events handled by the button, one handler for each of `MOUSE_CLICKED` and `KEY_PRESSED` event types will be installed on the button (as opposed to, for example, installing a common handler for the nearest common supertype, which in this case would be `InputEvent.ANY`).

```java
Nodes.addInputMap(button, consume(
        anyOf(mouseClicked(PRIMARY), keyPressed(SPACE)),
        e -> action()));
```

#### Text Input ####
Handle text input, i.e. `KEY_TYPED` events, _except_ for the new line character, which should be left unconsumed. In this example, echo the input to standard output.

```java
Nodes.addInputMap(button, consume(
        keyTyped(c -> !c.equals("\n")),
        e -> System.out.print(e.getCharacter())));
```

#### Custom Events ####
Assume the following custom event declaration:

```java
class FooEvent extends Event {
    public static final EventType<FooEvent> FOO;

    public boolean isSecret();
    public String getValue();
}
```

The task is to print out the value of and consume non-secret `Foo` events of `node`. Secret `Foo` events should be left unconsumed, i.e. let to bubble up.

```java
Nodes.addInputMap(node, consume(
        eventType(FooEvent.FOO).unless(FooEvent::isSecret),
        e -> System.out.print(e.getValue())));
```


### Manipulating Input Mappings ###

Use cases in this section focus on manipulating input mappings of a control, such as overriding mappings, adding default mappings, intercepting mappings, removing a previously added mapping, etc.

#### Override a previously defined mapping ####
First install a handler on `node` that invokes `charTyped(String character)` for each typed character. Later override the <kbd>Tab</kbd> character with `tabTyped()`. All other characters should still be handled by `charTyped(character)`.

```java
Nodes.addInputMap(node, consume(keyTyped(), e -> charTyped(e.getCharacter())));

// later override the Tab character
Nodes.addInputMap(node, consume(keyTyped("\t"), e -> tabTyped()));
```

#### Override even a more specific previous mapping ####
The `Control` might have installed a <kbd>Tab</kbd>-pressed handler for <kbd>Tab</kbd> navigation, but you want to consume all letter, digit and whitespace keys (maybe because you are handling their corresponding key-typed events). The point here is that the previously installed <kbd>Tab</kbd> handler is overridden even if it is more specific than the letter/digit/whitespace handler.

```java
Nodes.addInputMap(node, consume(keyPressed(TAB), e -> tabNavigation()));

// later consume all letters, digits and whitespace
Nodes.addInputMap(node, consume(keyPressed(kc -> kc.isLetterKey() || kc.isDigitKey() || kc.isWhitespaceKey())));
```

#### Add default mappings ####
It has to be possible to add default (or fallback) mappings, i.e. mappings that do not override any previously defined mappings, but take effect if the event is not handled by any previously installed mapping. That is the case for mappings added by skins, since skin is only installed after the user has instantiated the control and customized the mappings.

The task is to _first_ install the (custom) <kbd>Tab</kbd> handler (`tabTyped()`) and _then_ the (default) key typed handler (`charTyped(c)`), but the custom handler should not be overridden by the default handler.

```java
// user-specified Tab handler
Nodes.addInputMap(node, consume(keyTyped("\t"), e -> tabTyped()));

// later in skin
Nodes.addFallbackInputMap(node, consume(keyTyped(), e -> charTyped(e.getCharacter())));
```

#### Ignore certain events ####
Suppose the skin defines a generic key-pressed handler, but the user needs <kbd>Tab</kbd>-pressed to be ignored by the control and bubble up the scene graph.

```java
// ignore Tab handler
Nodes.addInputMap(node, ignore(keyPressed(TAB)));

// later in skin
Nodes.addFallbackInputMap(node, consume(keyPressed(), e -> handleKeyPressed(e)));
```

#### Remove a previously added handler ####
When changing skins, the skin that is being disposed should remove any mappings it has added to the control. Any mappings added before or after the skin was instantiated should stay in effect. In this example, let's add handlers for each of the arrow keys and for mouse move with left button pressed. Later, remove all of them, but leaving any other mappings untouched.

```java
// on skin creation
InputMap<InputEvent> im = sequence(
        consume(keyPressed(UP),    e -> moveUp()),
        consume(keyPressed(DOWN),  e -> moveDown()),
        consume(keyPressed(LEFT),  e -> moveLeft()),
        consume(keyPressed(RIGHT), e -> moveRight()),
        consume(
                mouseMoved().onlyIf(MouseEvent::isPrimaryButtonDown),
                e -> move(e.getX(), e.getY())));
Nodes.addFallbackInputMap(node, im);

// on skin disposal
Nodes.removeInputMap(node, im);
```

#### Common post-consumption processing ####
Suppose we have a number of input mappings whose handlers share some common at the end. We would like to factor out this common code to avoid repetition. To give an example, suppose each `move*()` method from the previous example ends with `this.moveCount += 1`. Let's factor out this common code to a single place. (Notice the `ifConsumed`.)

```java
InputMap<InputEvent> im0 = sequence(
        consume(keyPressed(UP),    e -> moveUp()),
        consume(keyPressed(DOWN),  e -> moveDown()),
        consume(keyPressed(LEFT),  e -> moveLeft()),
        consume(keyPressed(RIGHT), e -> moveRight()),
        consume(
                mouseMoved().onlyIf(MouseEvent::isPrimaryButtonDown),
                e -> move(e.getX(), e.getY()))
        ).ifConsumed(e - { this.moveCount += 1; });

Nodes.addFallbackInputMap(node, im);
```

#### Temporary installation of an InputMap ####
Suppose one wants to use a given `InputMap` for a node's basic behavior, and upon a specific trigger (e.g. the user presses CTRL+Space), we want the node to have a different behavior temporarily. Once another trigger occurs in this "special behavior" context (e.g. the user presses ESC), we want to revert back to the basic behavior. How can this be done?

````java
// Basic idea
InputMap<?> anInputMap = // creation code
InputMap<?> aTempInputMap = // creation code

// install anInputMap
Nodes.addInputMap(node, anInputMap);
// uninstall anInputMap and install aTempInputMap
Nodes.pushInputMap(node, aTempInputMap);
// uninstall aTempInputMap and reinstall anInputMap
Nodes.popInputMap(node);
````

For example:

````java
// Special Behavior: refuse to show user a message
InputMap<Event> specialBehavior = sequence(
    // individual input maps here
    consume(
            keyPressed("a"),
            e -> System.out.println("We aren't showing you what the user pressed :-p"),

    // handler for reverting back to basic behavior
    consume(
        // trigger that will reinstall basic behavior
        keyPressed(ESC),

        // uninstalls this behavior from this node and reinstalls the basic behavior
        e -> {
            boolean basicBehaviorReinstalled = Nodes.popInputMap(this);
            if (!basicBehaviorReinstalled) {
                throw new IllegalStateException("Basic behavior was not reinstalled!");
            }
    })
);
// Basic Behavior: show user a message
InputMap<Event> basicBehavior = sequence(
    // individual input maps here
    consume(
        keyPressed("a"),
        e -> System.out.println("The user pressed: " + e.getText()),

    // handler for installing special behavior temporarily
    consume(
        // trigger that will install new behavior
        keyPressed(SPACE, CONTROL),

        e -> Nodes.pushInputMap(this, specialBehavior)
    )
);
Nodes.addInputMap(node, basicBehavior);

// user presses 'A'
// System outputs: "The user pressed: A"

// user presses CTRL + Space
// user presses 'A'
// System outputs: "We aren't showing you what the user pressed :-p"

// user presses 'ESC'
// user presses 'A'
// System outputs: "The user pressed: A"
````

These temporary `InputMap`s can be stacked multiple times, so that one can have multiple contexts:
- basic context
  - Up Trigger: when user presses `CTRL+SPACE`, uninstalls this context's behavior and installs `temp context 1`
- temp context 1
  - Down Trigger: when user presses `ESC`, uninstalls this context's behavior and reinstalls `basic context`
  - Up Trigger: when user presses `CTRL+SPACE`, uninstalls this context's behavior and installs `temp context 2`
- temp context 2
  - Down Trigger: when user presses `ESC`, uninstalls this context's behavior and reinstalls `temp context 1`

### Structural sharing between input maps ###
Consider a control that defines _m_ input mappings and that there are _n_ instances of this control in the scene. The space complexity of all input mappings of all these controls combined is then _O(n*m)_. The goal is to reduce this complexity to _O(m+n)_ by having a shared structure of complexity _O(m)_ of the _m_ input mappings, and each of the _n_ controls to have an input map that is a constant overhead (_O(1)_) on top of this shared structure.

This is supported by package `org.fxmisc.wellbehaved.event.template`.
The shared structure is an instance of `InputMapTemplate`.
The API for constructing `InputMapTemplate`s very much copies the API for constructing `InputMap`s that you have seen throughout this document, except the handlers take an additional argument&mdash;typically the control or the "behavior". A template can then be _instantiated_ to an `InputMap`, which is a constant overhead wrapper around the template, by providing the control/behavior object.

**Example:**

```java
static final InputMapTemplate<TextArea, InputEvent> INPUT_MAP_TEMPLATE =
    unless(TextArea::isDisabled, sequence(
        consume(keyPressed(A, SHORTCUT_DOWN), (area, evt) -> area.selectAll()),
        consume(keyPressed(C, SHORTCUT_DOWN), (area, evt) -> area.copy())
        /* ... */
    ));

TextArea area1 = new TextArea();
TextArea area2 = new TextArea();

InputMapTemplate.installFallback(INPUT_MAP_TEMPLATE, area1);
InputMapTemplate.installFallback(INPUT_MAP_TEMPLATE, area2);
```

Notice that `INPUT_MAP_TEMPLATE` is `static` and then added to two `TextArea`s.

Download
--------

Maven artifacts are deployed to Maven Central repository with the following Maven coordinates:

| Group ID               | Artifact ID    | Version |
| :--------------------: | :------------: | :-----: |
| org.fxmisc.wellbehaved | wellbehavedfx  | 0.3.3     |

### Gradle example

```groovy
dependencies {
    compile group: 'org.fxmisc.wellbehaved', name: 'wellbehavedfx', version: '0.3.3'
}
```

### Sbt example

```scala
libraryDependencies += "org.fxmisc.wellbehaved" % "wellbehavedfx" % "0.3.3"
```

### Manual download

[Download](https://oss.sonatype.org/content/groups/public/org/fxmisc/wellbehaved/wellbehavedfx/0.3.3/) the JAR file and place it on your classpath.


Links
-------

License: [BSD 2-Clause License](http://opensource.org/licenses/BSD-2-Clause)
API documentation: [Javadoc](http://fxmisc.github.io/wellbehaved/javadoc/0.3.3/overview-summary.html)
