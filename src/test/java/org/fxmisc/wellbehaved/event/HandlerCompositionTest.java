package org.fxmisc.wellbehaved.event;

import static javafx.scene.input.KeyCode.*;
import static javafx.scene.input.KeyEvent.*;
import static org.junit.Assert.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.embed.swing.JFXPanel;
import javafx.event.EventHandler;
import javafx.scene.input.KeyEvent;

import org.junit.BeforeClass;
import org.junit.Test;

public class HandlerCompositionTest {

    @BeforeClass
    public static void setUpBeforeClass() {
        // initialize JavaFX
        new JFXPanel();
    }

    @Test
    public void test() {
        StringProperty res = new SimpleStringProperty();

        EventHandler<KeyEvent> a = e -> { res.set("A"); };
        EventHandler<KeyEvent> b = e -> { res.set("B"); e.consume(); };
        EventHandler<KeyEvent> c = e -> { res.set("C"); e.consume(); };
        EventHandler<KeyEvent> d = e -> { res.set("D"); };

        KeyEvent event = new KeyEvent(KEY_PRESSED, "", "", A, false, false, false, false);

        EventHandler<? super KeyEvent> handler = EventHandlerHelper.chain(a, b, c, d);
        handler.handle(event);
        assertEquals("B", res.get());

        handler = EventHandlerHelper.exclude(handler, b);
        event = event.copyFor(null, null); // obtain unconsumed event
        handler.handle(event);
        assertEquals("C", res.get());

        handler = EventHandlerHelper.exclude(handler, c);
        event = event.copyFor(null, null); // obtain unconsumed event
        handler.handle(event);
        assertEquals("D", res.get());
    }

}
