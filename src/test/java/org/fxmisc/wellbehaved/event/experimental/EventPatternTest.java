package org.fxmisc.wellbehaved.event.experimental;

import static javafx.scene.input.KeyCode.*;
import static javafx.scene.input.KeyCombination.*;
import static javafx.scene.input.KeyEvent.*;
import static org.fxmisc.wellbehaved.event.experimental.EventPattern.*;
import static org.junit.Assert.*;
import javafx.event.Event;
import javafx.scene.input.KeyEvent;

import org.fxmisc.wellbehaved.event.experimental.EventPattern;
import org.junit.Test;

public class EventPatternTest {

    @Test
    public void simpleKeyMatchTest() {
        EventPattern<Event, KeyEvent> pAPressed = keyPressed(A);
        EventPattern<Event, KeyEvent> pShiftAPressed = keyPressed(A, SHIFT_DOWN);
        EventPattern<Event, KeyEvent> pAnyShiftAPressed = keyPressed(A, SHIFT_ANY);


        KeyEvent eAPressed = new KeyEvent(KEY_PRESSED, "", "", A, false, false, false, false);
        KeyEvent eShiftAPressed = new KeyEvent(KEY_PRESSED, "", "", A, true, false, false, false);

        assertTrue(pAPressed.match(eAPressed).isPresent());
        assertFalse(pAPressed.match(eShiftAPressed).isPresent());
        assertFalse(pShiftAPressed.match(eAPressed).isPresent());
        assertTrue(pShiftAPressed.match(eShiftAPressed).isPresent());
        assertTrue(pAnyShiftAPressed.match(eAPressed).isPresent());
        assertTrue(pAnyShiftAPressed.match(eShiftAPressed).isPresent());
    }

}
