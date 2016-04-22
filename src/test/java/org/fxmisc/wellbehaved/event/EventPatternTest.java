package org.fxmisc.wellbehaved.event;

import static javafx.scene.input.KeyCode.*;
import static javafx.scene.input.KeyCombination.*;
import static javafx.scene.input.KeyEvent.*;
import static org.fxmisc.wellbehaved.event.EventPattern.*;
import static org.junit.Assert.*;

import javafx.embed.swing.JFXPanel;
import javafx.event.Event;
import javafx.scene.input.KeyEvent;

import org.junit.BeforeClass;
import org.junit.Test;

import com.sun.javafx.util.Utils;

public class EventPatternTest {

    @BeforeClass
    public static void setUpBeforeClass() {
        new JFXPanel(); // initialize JavaFX
    }

    @Test
    public void simpleKeyMatchTest() {
        EventPattern<Event, KeyEvent> pAPressed = keyPressed(A);
        EventPattern<Event, KeyEvent> pShiftAPressed = keyPressed(A, SHIFT_DOWN);
        EventPattern<Event, KeyEvent> pAnyShiftAPressed = keyPressed(A, SHIFT_ANY);
        EventPattern<Event, KeyEvent> pCtrlAReleased = keyReleased(A, CONTROL_DOWN);
        EventPattern<Event, KeyEvent> pMetaATyped = keyTyped("a", META_DOWN);
        EventPattern<Event, KeyEvent> pAltAPressed = keyPressed("a", ALT_DOWN);
        EventPattern<Event, KeyEvent> pNoControlsTyped = keyTyped().onlyIf(e -> !e.isControlDown() && !e.isAltDown() && ! e.isMetaDown());

        KeyEvent eAPressed = new KeyEvent(KEY_PRESSED, "", "", A, false, false, false, false);
        KeyEvent eShiftAPressed = new KeyEvent(KEY_PRESSED, "", "", A, true, false, false, false);
        KeyEvent eShiftAReleased = new KeyEvent(KEY_RELEASED, "", "", A, true, false, false, false);
        KeyEvent eShiftMetaAPressed = new KeyEvent(KEY_PRESSED, "", "", A, true, false, false, true);
        KeyEvent eCtrlAReleased = new KeyEvent(KEY_RELEASED, "", "", A, false, true, false, false);
        KeyEvent eMetaaTyped = new KeyEvent(KEY_TYPED, "a", "", UNDEFINED, false, false, false, true);
        KeyEvent eMetaATyped = new KeyEvent(KEY_TYPED, "A", "", UNDEFINED, false, false, false, true);
        KeyEvent eShiftQTyped = new KeyEvent(KEY_TYPED, "Q", "", UNDEFINED, true, false, false, false);
        KeyEvent eQTyped = new KeyEvent(KEY_TYPED, "q", "", UNDEFINED, false, false, false, false);
        KeyEvent eCtrlQTyped = new KeyEvent(KEY_TYPED, "q", "", UNDEFINED, false, true, false, false);
        KeyEvent eAltAPressed = new KeyEvent(KEY_PRESSED, "", "", A, false, false, true, false);

        assertTrue(pAPressed.match(eAPressed).isPresent());
        assertFalse(pAPressed.match(eShiftAPressed).isPresent()); // should not match when Shift pressed
        assertFalse(pShiftAPressed.match(eAPressed).isPresent()); // should not match when Shift not pressed
        assertFalse(pShiftAPressed.match(eShiftMetaAPressed).isPresent()); // should not match when Meta pressed
        assertTrue(pShiftAPressed.match(eShiftAPressed).isPresent());
        assertFalse(pShiftAPressed.match(eShiftAReleased).isPresent()); // released instead of pressed
        assertFalse(pCtrlAReleased.match(eShiftAReleased).isPresent()); // Shift instead of Control
        assertTrue(pAnyShiftAPressed.match(eAPressed).isPresent());
        assertTrue(pAnyShiftAPressed.match(eShiftAPressed).isPresent());
        assertTrue(pCtrlAReleased.match(eCtrlAReleased).isPresent());
        assertTrue(pMetaATyped.match(eMetaaTyped).isPresent());
        assertFalse(pMetaATyped.match(eMetaATyped).isPresent()); // wrong capitalization
        assertTrue(pNoControlsTyped.match(eShiftQTyped).isPresent());
        assertTrue(pNoControlsTyped.match(eQTyped).isPresent());
        assertFalse(pNoControlsTyped.match(eCtrlQTyped).isPresent()); // should not match when Control pressed
        if(!Utils.isMac()) { // https://bugs.openjdk.java.net/browse/JDK-8134723
            assertTrue(pAltAPressed.match(eAltAPressed).isPresent());
        }
    }

}
