package org.fxmisc.wellbehaved.event.template;

import static javafx.scene.input.KeyCode.*;
import static javafx.scene.input.KeyCombination.*;
import static javafx.scene.input.KeyEvent.*;
import static org.fxmisc.wellbehaved.event.EventPattern.*;
import static org.fxmisc.wellbehaved.event.template.InputMapTemplate.*;
import static org.junit.Assert.*;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Node;
import javafx.scene.control.TextArea;
import javafx.scene.input.InputEvent;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;

import org.fxmisc.wellbehaved.event.InputHandler;
import org.fxmisc.wellbehaved.event.InputMap;
import org.fxmisc.wellbehaved.event.InputMapTest;
import org.fxmisc.wellbehaved.event.Nodes;
import org.junit.BeforeClass;
import org.junit.Test;

public class InputMapTemplateTest {

    @BeforeClass
    public static void setUpBeforeClass() {
        new JFXPanel(); // initialize JavaFX
    }

    @Test
    public void test() {
        StringProperty res = new SimpleStringProperty();

        InputMapTemplate<Node, KeyEvent> imt1 = consume(keyPressed(A), (s, e) -> res.set("A"));
        InputMapTemplate<Node, KeyEvent> imt2 = consume(keyPressed(B), (s, e) -> res.set("B"));
        InputMapTemplate<Node, KeyEvent> imt = imt1.orElse(imt2);
        InputMap<KeyEvent> ignA = InputMap.ignore(keyPressed(A));

        Node node1 = new Region();
        Node node2 = new Region();

        Nodes.addInputMap(node1, ignA);
        InputMapTemplate.installFallback(imt, node1);
        InputMapTemplate.installFallback(imt, node2);

        KeyEvent aPressed = new KeyEvent(KEY_PRESSED, "", "", A, false, false, false, false);
        KeyEvent bPressed = new KeyEvent(KEY_PRESSED, "", "", B, false, false, false, false);

        InputMapTest.dispatch(aPressed, node1);
        assertNull(res.get());
        assertFalse(aPressed.isConsumed());

        InputMapTest.dispatch(aPressed, node2);
        assertEquals("A", res.get());
        assertTrue(aPressed.isConsumed());

        InputMapTest.dispatch(bPressed, node1);
        assertEquals("B", res.get());
        assertTrue(bPressed.isConsumed());
    }

    private static final InputMapTemplate<TextArea, InputEvent> INPUT_MAP_TEMPLATE =
        unless(TextArea::isDisabled, sequence(
            consume(keyPressed(A, SHORTCUT_DOWN), (area, evt) -> area.selectAll()),
            consume(keyPressed(C, SHORTCUT_DOWN), (area, evt) -> area.copy())
            /* ... */
        ));

    @Test
    public void textAreaExample() {
        TextArea area1 = new TextArea();
        TextArea area2 = new TextArea();

        InputMapTemplate.installFallback(INPUT_MAP_TEMPLATE, area1);
        InputMapTemplate.installFallback(INPUT_MAP_TEMPLATE, area2);
    }

    @Test
    public void testIfConsumed() {
        IntegerProperty counter = new SimpleIntegerProperty(0);

        InputMapTemplate<Node, KeyEvent> baseIMT = InputMapTemplate.sequence(
                consume(keyPressed(UP)),
                consume(keyPressed(DOWN)),
                consume(keyPressed(LEFT)),
                consume(keyPressed(RIGHT))
        );
        InputMapTemplate<Node, KeyEvent> imtPP = baseIMT.ifConsumed((n, e) -> counter.set(counter.get() + 1));

        Node node = new Region();
        InputMapTemplate.installFallback(imtPP, node);

        KeyEvent a = new KeyEvent(KEY_PRESSED, "", "", A, false, false, false, false);
        KeyEvent up = new KeyEvent(KEY_PRESSED, "", "", UP, false, false, false, false);
        KeyEvent down = new KeyEvent(KEY_PRESSED, "", "", DOWN, false, false, false, false);
        KeyEvent b = new KeyEvent(KEY_PRESSED, "", "", B, false, false, false, false);
        KeyEvent left = new KeyEvent(KEY_PRESSED, "", "", LEFT, false, false, false, false);
        KeyEvent right = new KeyEvent(KEY_PRESSED, "", "", RIGHT, false, false, false, false);

        InputMapTest.dispatch(a, node);
        assertEquals(0, counter.get());
        assertFalse(a.isConsumed());

        InputMapTest.dispatch(up, node);
        assertEquals(1, counter.get());
        assertTrue(up.isConsumed());

        InputMapTest.dispatch(down, node);
        assertEquals(2, counter.get());
        assertTrue(down.isConsumed());

        InputMapTest.dispatch(b, node);
        assertEquals(2, counter.get());
        assertFalse(b.isConsumed());

        InputMapTest.dispatch(left, node);
        assertEquals(3, counter.get());
        assertTrue(left.isConsumed());

        InputMapTest.dispatch(right, node);
        assertEquals(4, counter.get());
        assertTrue(right.isConsumed());
    }

    @Test
    public void testIfIgnored() {
        IntegerProperty counter = new SimpleIntegerProperty(0);

        InputMapTemplate<Node, KeyEvent> baseIMT = InputMapTemplate.sequence(
                ignore(keyPressed(UP)),
                ignore(keyPressed(DOWN)),
                ignore(keyPressed(LEFT)),
                ignore(keyPressed(RIGHT))
        );
        InputMapTemplate<Node, KeyEvent> imtPP = baseIMT.ifIgnored((n, e) -> counter.set(counter.get() + 1));

        Node node = new Region();
        InputMapTemplate.installFallback(imtPP, node);

        KeyEvent a = new KeyEvent(KEY_PRESSED, "", "", A, false, false, false, false);
        KeyEvent up = new KeyEvent(KEY_PRESSED, "", "", UP, false, false, false, false);
        KeyEvent down = new KeyEvent(KEY_PRESSED, "", "", DOWN, false, false, false, false);
        KeyEvent b = new KeyEvent(KEY_PRESSED, "", "", B, false, false, false, false);
        KeyEvent left = new KeyEvent(KEY_PRESSED, "", "", LEFT, false, false, false, false);
        KeyEvent right = new KeyEvent(KEY_PRESSED, "", "", RIGHT, false, false, false, false);

        InputMapTest.dispatch(a, node);
        assertEquals(0, counter.get());
        assertFalse(a.isConsumed());

        InputMapTest.dispatch(up, node);
        assertEquals(1, counter.get());
        assertFalse(up.isConsumed());

        InputMapTest.dispatch(down, node);
        assertEquals(2, counter.get());
        assertFalse(down.isConsumed());

        InputMapTest.dispatch(b, node);
        assertEquals(2, counter.get());
        assertFalse(b.isConsumed());

        InputMapTest.dispatch(left, node);
        assertEquals(3, counter.get());
        assertFalse(left.isConsumed());

        InputMapTest.dispatch(right, node);
        assertEquals(4, counter.get());
        assertFalse(right.isConsumed());
    }

    @Test
    public void testIfProceeded() {
        IntegerProperty counter = new SimpleIntegerProperty(0);

        InputHandler.Result returnVal = InputHandler.Result.PROCEED;

        InputMapTemplate<Node, KeyEvent> baseIMT = InputMapTemplate.sequence(
                consume(keyPressed(A)),
                consume(keyPressed(B)),

                process(keyPressed(UP),    (n, e) -> returnVal),
                process(keyPressed(DOWN),  (n, e) -> returnVal),
                process(keyPressed(LEFT),  (n, e) -> returnVal),
                process(keyPressed(RIGHT), (n, e) -> returnVal)
        );
        InputMapTemplate<Node, KeyEvent> imtPP = baseIMT.ifProcessed((n, e) -> counter.set(counter.get() + 1));

        Node node = new Region();
        InputMapTemplate.installFallback(imtPP, node);

        KeyEvent a = new KeyEvent(KEY_PRESSED, "", "", A, false, false, false, false);
        KeyEvent up = new KeyEvent(KEY_PRESSED, "", "", UP, false, false, false, false);
        KeyEvent down = new KeyEvent(KEY_PRESSED, "", "", DOWN, false, false, false, false);
        KeyEvent b = new KeyEvent(KEY_PRESSED, "", "", B, false, false, false, false);
        KeyEvent left = new KeyEvent(KEY_PRESSED, "", "", LEFT, false, false, false, false);
        KeyEvent right = new KeyEvent(KEY_PRESSED, "", "", RIGHT, false, false, false, false);

        InputMapTest.dispatch(a, node);
        assertEquals(0, counter.get());
        assertTrue(a.isConsumed());

        InputMapTest.dispatch(up, node);
        assertEquals(1, counter.get());
        assertFalse(up.isConsumed());

        InputMapTest.dispatch(down, node);
        assertEquals(2, counter.get());
        assertFalse(down.isConsumed());

        InputMapTest.dispatch(b, node);
        assertEquals(2, counter.get());
        assertTrue(b.isConsumed());

        InputMapTest.dispatch(left, node);
        assertEquals(3, counter.get());
        assertFalse(left.isConsumed());

        InputMapTest.dispatch(right, node);
        assertEquals(4, counter.get());
        assertFalse(right.isConsumed());
    }
}
