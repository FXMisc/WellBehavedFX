package org.fxmisc.wellbehaved.event;

import static javafx.scene.input.KeyCode.*;
import static javafx.scene.input.KeyEvent.*;
import static org.fxmisc.wellbehaved.event.EventPattern.*;
import static org.fxmisc.wellbehaved.event.InputHandler.Result.*;
import static org.fxmisc.wellbehaved.event.InputMap.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.Event;
import javafx.event.EventType;
import javafx.scene.Node;
import javafx.scene.input.InputEvent;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;

import org.fxmisc.wellbehaved.event.InputMap.HandlerConsumer;
import org.junit.Test;

public class InputMapTest {

    @SuppressWarnings("serial")
    private static class FooEvent extends Event {
        public static final EventType<FooEvent> FOO = new EventType<>("FOO");

        private final boolean secret;
        private final String value;

        public FooEvent(boolean secret, String value) {
            super(FOO);
            this.secret = secret;
            this.value = value;
        }

        public boolean isSecret() {
            return secret;
        }

        public String getValue() {
            return value;
        }
    }


    public static void dispatch(Event event, InputMap<?> inputMap) {
        IntegerProperty matchingHandlers = new SimpleIntegerProperty(0);

        inputMap.forEachEventType(new HandlerConsumer<Event>() {

            @Override
            public <E extends Event> void accept(
                    EventType<? extends E> t, InputHandler<? super E> h) {
                eventType(t).match(event).ifPresent(evt -> {
                    h.handle(evt);
                    matchingHandlers.set(matchingHandlers.get() + 1);
                });
            }

        });

        assertThat(matchingHandlers.get(), lessThanOrEqualTo(1));
    }

    public static void dispatch(Event event, Node node) {
        dispatch(event, Nodes.getInputMap(node));
    }


    @Test
    public void overridePreviouslyAddedHandler() {
        StringProperty res = new SimpleStringProperty();

        InputMap<KeyEvent> im1 = consume(keyPressed(), e -> res.set("handler 1"));
        InputMap<KeyEvent> im2 = consume(keyPressed(A), e -> res.set("handler 2"));

        KeyEvent aPressed = new KeyEvent(KEY_PRESSED, "", "", A, false, false, false, false);
        KeyEvent bPressed = new KeyEvent(KEY_PRESSED, "", "", B, false, false, false, false);

        InputMap<KeyEvent> im = im1.orElse(im2);
        dispatch(aPressed, im);
        assertEquals("handler 1", res.get());
        dispatch(bPressed, im);
        assertEquals("handler 1", res.get());

        im = im2.orElse(im1);
        dispatch(aPressed, im);
        assertEquals("handler 2", res.get());
        dispatch(bPressed, im);
        assertEquals("handler 1", res.get());
    }

    @Test
    public void fallbackHandlerTest() {
        StringProperty res = new SimpleStringProperty();

        InputMap<KeyEvent> fallback = consume(keyPressed(), e -> res.set("fallback"));
        InputMap<KeyEvent> custom   = consume(keyPressed(A), e -> res.set("custom"));

        KeyEvent aPressed = new KeyEvent(KEY_PRESSED, "", "", A, false, false, false, false);
        KeyEvent bPressed = new KeyEvent(KEY_PRESSED, "", "", B, false, false, false, false);

        Node node = new Region();

        // install custom handler first, then fallback
        Nodes.addInputMap(node, custom);
        Nodes.addFallbackInputMap(node, fallback);

        // check that custom handler is not overridden by fallback handler
        dispatch(aPressed, node);
        assertEquals("custom", res.get());

        // check that fallback handler is in effect
        dispatch(bPressed, node);
        assertEquals("fallback", res.get());
    }


    @Test
    public void ignoreTest() {
        StringProperty res = new SimpleStringProperty();

        InputMap<KeyEvent> fallback = consume(keyPressed(), e -> res.set("consumed"));
        InputMap<KeyEvent> ignore   = ignore (keyPressed(A));

        KeyEvent aPressed = new KeyEvent(KEY_PRESSED, "", "", A, false, false, false, false);
        KeyEvent bPressed = new KeyEvent(KEY_PRESSED, "", "", B, false, false, false, false);

        Node node = new Region();

        // install ignore handler first, then fallback
        Nodes.addInputMap(node, ignore);
        Nodes.addFallbackInputMap(node, fallback);

        // check that ignore works
        dispatch(aPressed, node);
        assertNull(res.get());
        assertFalse(aPressed.isConsumed());

        // check that other events are not ignored
        dispatch(bPressed, node);
        assertEquals("consumed", res.get());
        assertTrue(bPressed.isConsumed());
    }

    @Test
    public void withoutTest() {
        StringProperty res = new SimpleStringProperty();

        InputMap<KeyEvent> im1 = consume(keyPressed(B), e -> { res.set("1"); });
        InputMap<KeyEvent> im2 = consume(keyPressed(A), e -> { res.set("2"); });
        InputMap<KeyEvent> im3 = consume(keyPressed(A), e -> { res.set("3"); });
        InputMap<KeyEvent> im4 = process(keyPressed(A), e -> { res.set("4"); return PROCEED; });

        KeyEvent event = new KeyEvent(KEY_PRESSED, "", "", A, false, false, false, false);

        InputMap<? super KeyEvent> im = sequence(im1, im2, im3, im4);
        dispatch(event, im);
        assertEquals("2", res.get());

        im = im.without(im2);
        event = event.copyFor(null, null); // obtain unconsumed event
        dispatch(event, im);
        assertEquals("3", res.get());

        im = im.without(im3);
        event = event.copyFor(null, null); // obtain unconsumed event
        dispatch(event, im);
        assertEquals("4", res.get());
        assertFalse(event.isConsumed());
    }

    @Test
    public void whenTest() {
        BooleanProperty condition = new SimpleBooleanProperty(false);

        InputMap<KeyEvent> im = when(condition::get, consume(keyPressed()));

        KeyEvent event = new KeyEvent(KEY_PRESSED, "", "", A, false, false, false, false);

        dispatch(event, im);
        assertFalse(event.isConsumed());

        condition.set(true);
        dispatch(event, im);
        assertTrue(event.isConsumed());
    }

    @Test
    public void removePreviousHandlerTest() {
        StringProperty res = new SimpleStringProperty();

        InputMap<KeyEvent> fallback = consume(keyPressed(), e -> res.set("fallback"));
        InputMap<KeyEvent> custom   = consume(keyPressed(A), e -> res.set("custom"));

        KeyEvent aPressed = new KeyEvent(KEY_PRESSED, "", "", A, false, false, false, false);
        KeyEvent bPressed = new KeyEvent(KEY_PRESSED, "", "", B, false, false, false, false);

        Node node = new Region();

        // install custom handler first, then fallback
        Nodes.addInputMap(node, custom);
        Nodes.addFallbackInputMap(node, fallback);

        // check that fallback handler works
        dispatch(bPressed, node);
        assertEquals("fallback", res.get());

        // remove fallback handler
        Nodes.removeInputMap(node, fallback);
        res.set(null);

        // check that fallback handler was removed
        dispatch(bPressed, node);
        assertNull(res.get());

        // check that custom handler still works
        dispatch(aPressed, node);
        assertEquals("custom", res.get());
    }

    private void moveUp() {}
    private void moveDown() {}
    private void moveLeft() {}
    private void moveRight() {}
    private void move(double x, double y) {}
    public void justKidding() {
        Node node = new Region();

        InputMap<InputEvent> im = sequence(
                consume(keyPressed(UP),    e -> moveUp()),
                consume(keyPressed(DOWN),  e -> moveDown()),
                consume(keyPressed(LEFT),  e -> moveLeft()),
                consume(keyPressed(RIGHT), e -> moveRight()),
                consume(
                        mouseMoved().onlyIf(MouseEvent::isPrimaryButtonDown),
                        e -> move(e.getX(), e.getY())));

        Nodes.addFallbackInputMap(node, im);

        Nodes.removeInputMap(node, im);
    }

    @Test
    public void customEventTest() {
        StringProperty res = new SimpleStringProperty();

        // if event is not secret, assign its value to res.
        // Otherwise, don't consume the event.
        InputMap<FooEvent> im = consume(
                eventType(FooEvent.FOO).unless(FooEvent::isSecret),
                e -> res.set(e.getValue()));

        FooEvent secret = new FooEvent(true, "Secret");
        FooEvent open   = new FooEvent(false, "Open");

        Node node = new Region();
        Nodes.addInputMap(node, im);

        // check that secret event is not processed or consumed
        dispatch(secret, node);
        assertNull(res.get());
        assertFalse(secret.isConsumed());

        // check that open event is processed and consumed
        dispatch(open, node);
        assertEquals("Open", res.get());
        assertTrue(open.isConsumed());
    }

    @Test
    public void ifConsumedTest() {
        StringProperty res = new SimpleStringProperty();
        IntegerProperty counter = new SimpleIntegerProperty(0);

        InputMap<KeyEvent> im = InputMap.sequence(
                consume(keyPressed(UP),    e -> res.set("Up")),
                consume(keyPressed(DOWN),  e -> res.set("Down")),
                consume(keyPressed(LEFT),  e -> res.set("Left")),
                consume(keyPressed(RIGHT), e -> res.set("Right")))
                .ifConsumed(e -> counter.set(counter.get() + 1));

        KeyEvent a = new KeyEvent(KEY_PRESSED, "", "", A, false, false, false, false);
        KeyEvent up = new KeyEvent(KEY_PRESSED, "", "", UP, false, false, false, false);
        KeyEvent down = new KeyEvent(KEY_PRESSED, "", "", DOWN, false, false, false, false);
        KeyEvent left = new KeyEvent(KEY_PRESSED, "", "", LEFT, false, false, false, false);
        KeyEvent right = new KeyEvent(KEY_PRESSED, "", "", RIGHT, false, false, false, false);

        dispatch(a, im);
        assertNull(res.get());
        assertEquals(0, counter.get());
        assertFalse(a.isConsumed());

        dispatch(up, im);
        assertEquals("Up", res.get());
        assertEquals(1, counter.get());
        assertTrue(up.isConsumed());

        dispatch(down, im);
        assertEquals("Down", res.get());
        assertEquals(2, counter.get());
        assertTrue(down.isConsumed());

        dispatch(left, im);
        assertEquals("Left", res.get());
        assertEquals(3, counter.get());
        assertTrue(left.isConsumed());

        dispatch(right, im);
        assertEquals("Right", res.get());
        assertEquals(4, counter.get());
        assertTrue(right.isConsumed());
    }
}
