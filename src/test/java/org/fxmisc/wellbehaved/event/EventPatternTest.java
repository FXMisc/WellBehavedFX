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
        // "p" prefix = EventPattern
        // "e" prefix = KeyEvent

        EventPattern<Event, KeyEvent> pAPressed = keyPressed(A);
        EventPattern<Event, KeyEvent> pShiftAPressed = keyPressed(A, SHIFT_DOWN);
        EventPattern<Event, KeyEvent> pAnyShiftAPressed = keyPressed(A, SHIFT_ANY);
        EventPattern<Event, KeyEvent> pCtrlAReleased = keyReleased(A, CONTROL_DOWN);
        EventPattern<Event, KeyEvent> pMeta_a_Typed = keyTyped("a", META_DOWN);
        EventPattern<Event, KeyEvent> pAltAPressed = keyPressed("a", ALT_DOWN);
        EventPattern<Event, KeyEvent> pNoControlsTyped = keyTyped().onlyIf(e -> !e.isControlDown() && !e.isAltDown() && ! e.isMetaDown());
        EventPattern<Event, KeyEvent> p_a_Typed = keyTyped("a");

        KeyEvent eAPressed          = new KeyEvent(KEY_PRESSED, "", "", A, false, false, false, false);
        KeyEvent eShiftAPressed     = new KeyEvent(KEY_PRESSED, "", "", A, true, false, false, false);
        KeyEvent eShiftAReleased    = new KeyEvent(KEY_RELEASED, "", "", A, true, false, false, false);
        KeyEvent eShiftMetaAPressed = new KeyEvent(KEY_PRESSED, "", "", A, true, false, false, true);
        KeyEvent eCtrlAReleased     = new KeyEvent(KEY_RELEASED, "", "", A, false, true, false, false);
        KeyEvent eMeta_a_Typed      = new KeyEvent(KEY_TYPED, "a", "", UNDEFINED, false, false, false, true);
        KeyEvent eMeta_A_Typed      = new KeyEvent(KEY_TYPED, "A", "", UNDEFINED, false, false, false, true);
        KeyEvent eShiftQTyped       = new KeyEvent(KEY_TYPED, "Q", "", UNDEFINED, true, false, false, false);
        KeyEvent eQTyped            = new KeyEvent(KEY_TYPED, "q", "", UNDEFINED, false, false, false, false);
        KeyEvent eCtrlQTyped        = new KeyEvent(KEY_TYPED, "q", "", UNDEFINED, false, true, false, false);
        KeyEvent eAltAPressed       = new KeyEvent(KEY_PRESSED, "", "", A, false, false, true, false);

        KeyEvent e_a_Typed          = new KeyEvent(KEY_TYPED, "a", "", UNDEFINED, false, false, false, false);
        KeyEvent eShift_a_Typed          = new KeyEvent(KEY_TYPED, "a", "", UNDEFINED, true, false, false, false);

        assertMatchSuccess(pAPressed, eAPressed);
        assertMatchSuccess(pAPressed, eShiftAPressed); // should match even when Shift pressed
        assertMatchSuccess(pAPressed, eShiftMetaAPressed); // or when any other combo of modifiers pressed

        assertMatchFailure(pShiftAPressed, eAPressed); // should not match when Shift not pressed
        assertMatchSuccess(pShiftAPressed, eShiftAPressed);
        assertMatchFailure(pShiftAPressed, eShiftMetaAPressed); // should not match when Meta pressed
        assertMatchFailure(pShiftAPressed, eShiftAReleased); // released instead of pressed
        assertMatchFailure(pCtrlAReleased, eShiftAReleased); // Shift instead of Control

        assertMatchSuccess(pAnyShiftAPressed, eAPressed);
        assertMatchSuccess(pAnyShiftAPressed, eShiftAPressed);

        assertMatchSuccess(pCtrlAReleased, eCtrlAReleased);

        assertMatchSuccess(pMeta_a_Typed, eMeta_a_Typed);
        assertMatchFailure(pMeta_a_Typed, eMeta_A_Typed); // wrong capitalization

        assertMatchSuccess(pNoControlsTyped, eShiftQTyped);
        assertMatchSuccess(pNoControlsTyped, eQTyped);
        assertMatchFailure(pNoControlsTyped, eCtrlQTyped); // should not match when Control pressed

        if(!Utils.isMac()) { // https://bugs.openjdk.java.net/browse/JDK-8134723
            assertMatchSuccess(pAltAPressed, eAltAPressed);
        }

        assertMatchSuccess(p_a_Typed, e_a_Typed);
        assertMatchSuccess(p_a_Typed, eShift_a_Typed);
        assertMatchSuccess(p_a_Typed, eMeta_a_Typed);
        assertMatchFailure(p_a_Typed, eMeta_A_Typed); // wrong capitalization
    }

    private void assertMatchSuccess(EventPattern<Event, KeyEvent> pattern, KeyEvent event) {
        assertTrue(pattern.match(event).isPresent());
    }

    private void assertMatchFailure(EventPattern<Event, KeyEvent> pattern, KeyEvent event) {
        assertFalse(pattern.match(event).isPresent());
    }

}
