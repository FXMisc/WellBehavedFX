WellBehavedFX
=============

This project tries to help with two (related) aspects of JavaFX development:

 * defining and overriding event handlers (e.g. keyboard shortcuts); and
 * implementing skins for JavaFX controls according to the MVC pattern.
 
If you like the approach taken here and want something like this in JavaFX, [RT-21598](https://javafx-jira.kenai.com/browse/RT-21598) is the place to discuss.


Builder Pattern for Event Handlers
-----------------------------------

WellBehavedFX provides builder-style API to define `EventHandler`s.

```java
import static javafx.scene.input.KeyCode.*;
import static javafx.scene.input.KeyCombination.*;
import static org.fxmisc.wellbehaved.event.EventPattern.*;

EventHandler<? super KeyEvent> keyPressedHandler = EventHandlerHelper
        .on(keyPressed(O, CONTROL_DOWN))            .act(event -> open())
        .on(keyPressed(S, CONTROL_DOWN))            .act(event -> save())
        .on(keyPressed(S, CONTROL_DOWN, SHIFT_DOWN)).act(event -> saveAll())
        .create();

textArea.setOnKeyPressed(keyPressedHandler);
```


(De)composable Event Handlers
-----------------------------

In the example above we have overridden `textArea`'s original `onKeyPressed` handler (if any). We may instead want to combine the new handler and the original handler, so that if the new handler does not handle (that is, does not _consume_) the event, the original handler is invoked. This is what we should have done instead:

```java
EventHandlerHelper.install(textArea.onKeyPressedProperty(), keyPressedHandler);
```

We can add another one that will take precedence over both previously installed handlers:

```java
EventHandler<? super KeyEvent> anotherHandler = ...;
EventHandlerHelper.install(textArea.onKeyPressedProperty(), anotherHandler);
```

We can as well exclude a sub-handler from a composite handler:

```java
EventHandlerHelper.remove(textArea.onKeyPressedProperty(), keyPressedHandler);
```

Now the handler that remains installed on `textArea` first invokes `anotherHandler`, and if it does not consume the event, `textArea`s original handler (if any) is invoked.


Event Handler Templates
-----------------------

TBD


Skin Scaffolding
----------------

TBD  
Have a look at [this blog post](http://tomasmikula.github.io/blog/2014/06/11/separation-of-view-and-controller-in-javafx-controls.html) for the general idea.


Download
--------

Maven artifacts are deployed to Maven Central repository with the following Maven coordinates:

| Group ID               | Artifact ID    | Version |
| :--------------------: | :------------: | :-----: |
| org.fxmisc.wellbehaved | wellbehavedfx  | 0.1     |

### Gradle example

```groovy
dependencies {
    compile group: 'org.fxmisc.wellbehaved', name: 'wellbehavedfx', version: '0.1'
}
```

### Sbt example

```scala
libraryDependencies += "org.fxmisc.wellbehaved" % "wellbehavedfx" % "0.1"
```

### Manual download

[Download](https://oss.sonatype.org/content/groups/public/org/fxmisc/wellbehaved/wellbehavedfx/0.1/) the JAR file and place it on your classpath.


Links
-------

License: [BSD 2-Clause License](http://opensource.org/licenses/BSD-2-Clause)  
API documentation: [Javadoc](http://www.fxmisc.org/wellbehaved/javadoc/overview-summary.html)  
