package org.fxmisc.wellbehaved.input;

import static javafx.scene.input.KeyCode.*;
import static javafx.scene.input.KeyCombination.*;
import static javafx.scene.input.KeyEvent.*;
import static org.fxmisc.wellbehaved.input.EventPattern.*;
import static org.junit.Assert.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.embed.swing.JFXPanel;
import javafx.event.EventHandler;
import javafx.scene.input.InputEvent;
import javafx.scene.input.KeyEvent;

import org.junit.BeforeClass;
import org.junit.Test;

public class EventMatchingTest {

    @BeforeClass
    public static void setUpBeforeClass() {
        // initialize JavaFX
        new JFXPanel();
    }

    @Test
    public void test() {
        StringProperty res = new SimpleStringProperty();
        EventHandler<InputEvent> handler = EventHandlerHelper
                .<InputEvent, KeyEvent>
                 on(keyPressed(A, SHIFT_DOWN)).act(e -> res.set("Shift+A"))
                .on(keyPressed("a", ALT_DOWN)).act(e -> res.set("Alt+A"))
                .on(KEY_RELEASED).where(e -> e.getCode() == A
                         && e.isControlDown()
                         && !e.isAltDown()
                         && !e.isShiftDown()
                         && !e.isMetaDown()).act(e -> res.set("Ctrl+A"))
                .on(keyTyped("a", META_DOWN)).act(e -> res.set("Meta+A"))
                .on(KEY_TYPED).where(e -> !e.isControlDown() && !e.isAltDown() && ! e.isMetaDown())
                        .act(e -> res.set(e.getCharacter()))
                .create();

        KeyEvent shiftA = new KeyEvent(KEY_PRESSED, "", "", A, true, false, false, false);
        KeyEvent altA   = new KeyEvent(KEY_PRESSED, "", "", A, false, false, true, false);
        KeyEvent ctrlA  = new KeyEvent(KEY_RELEASED, "", "", A, false, true, false, false);
        KeyEvent metaA  = new KeyEvent(KEY_TYPED, "a", "", UNDEFINED, false, false, false, true);
        KeyEvent Q = new KeyEvent(KEY_TYPED, "Q", "", UNDEFINED, true, false, false, false);
        KeyEvent q = new KeyEvent(KEY_TYPED, "q", "", UNDEFINED, false, false, false, false);

        KeyEvent noMatch1 = new KeyEvent(KEY_PRESSED, "", "", A, true, false, false, true); // both Shift and Meta down
        KeyEvent noMatch2 = new KeyEvent(KEY_RELEASED, "", "", A, true, false, false, false); // wrong event type
        KeyEvent noMatch3 = new KeyEvent(KEY_PRESSED, "", "", B, true, false, false, false); // wrong key code
        KeyEvent noMatch4 = new KeyEvent(KEY_TYPED, "A", "", UNDEFINED, false, false, false, true); // wrong capitalization
        KeyEvent noMatch5 = new KeyEvent(KEY_TYPED, "q", "", UNDEFINED, false, true, false, false); // control down

        handler.handle(shiftA);
        assertEquals("Shift+A", res.get());

        handler.handle(altA);
        assertEquals("Alt+A", res.get());

        handler.handle(ctrlA);
        assertEquals("Ctrl+A", res.get());

        handler.handle(metaA);
        assertEquals("Meta+A", res.get());

        handler.handle(Q);
        assertEquals("Q", res.get());

        handler.handle(q);
        assertEquals("q", res.get());

        res.set(null);

        handler.handle(noMatch1);
        assertEquals(null, res.get());

        handler.handle(noMatch2);
        assertEquals(null, res.get());

        handler.handle(noMatch3);
        assertEquals(null, res.get());

        handler.handle(noMatch4);
        assertEquals(null, res.get());

        handler.handle(noMatch5);
        assertEquals(null, res.get());
    }

}
