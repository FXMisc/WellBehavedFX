package org.fxmisc.wellbehaved.event;

import static javafx.scene.input.KeyEvent.*;
import static javafx.scene.input.MouseEvent.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import javafx.event.Event;
import javafx.event.EventType;
import javafx.scene.input.KeyCharacterCombination;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

public interface EventPattern<T extends Event, U extends T> {

    Optional<? extends U> match(T event);
    Set<EventType<? extends U>> getEventTypes();

    default <V extends U> EventPattern<T, V> andThen(EventPattern<? super U, V> next) {
        return new EventPattern<T, V>() {

            @Override
            public Optional<? extends V> match(T event) {
                return EventPattern.this.match(event).flatMap(next::match);
            }

            @Override
            public Set<EventType<? extends V>> getEventTypes() {
                return next.getEventTypes();
            }
        };
    }

    default EventPattern<T, U> onlyIf(Predicate<? super U> condition) {
        return new EventPattern<T, U>() {

            @Override
            public Optional<U> match(T event) {
                return EventPattern.this.match(event).map(u -> condition.test(u) ? u : null);
            }

            @Override
            public Set<EventType<? extends U>> getEventTypes() {
                return EventPattern.this.getEventTypes();
            }

        };
    }

    default EventPattern<T, U> unless(Predicate<? super U> condition) {
        return onlyIf(condition.negate());
    }

    @SafeVarargs
    static <T extends Event, U extends T> EventPattern<T, U> anyOf(EventPattern<T, ? extends U>... events) {
        return new EventPattern<T, U>() {

            @Override
            public Optional<? extends U> match(T event) {
                for (EventPattern<T, ? extends U> evt : events) {
                    Optional<? extends U> match = evt.match(event);
                    if(match.isPresent()) {
                        return match;
                    }
                }
                return Optional.empty();
            }

            @Override
            public Set<EventType<? extends U>> getEventTypes() {
                HashSet<EventType<? extends U>> ret = new HashSet<>();
                for (EventPattern<T, ? extends U> evt : events) {
                    ret.addAll(evt.getEventTypes());
                }
                return ret;
            }

        };
    }

    static <T extends Event> EventPattern<Event, T> eventType(EventType<? extends T> eventType) {
        return new EventPattern<Event, T>() {

            @Override
            public Optional<T> match(Event event) {
                EventType<? extends Event> actualType = event.getEventType();
                do {
                    if(actualType.equals(eventType)) {
                        @SuppressWarnings("unchecked")
                        T res = (T) event;
                        return Optional.of(res);
                    }
                    actualType = actualType.getSuperType();
                } while(actualType != null);
                return Optional.empty();
            }

            @Override
            public Set<EventType<? extends T>> getEventTypes() {
                return Collections.singleton(eventType);
            }

        };
    }

    static EventPattern<Event, KeyEvent> keyPressed() {
        return eventType(KeyEvent.KEY_PRESSED);
    }

    static EventPattern<Event, KeyEvent> keyPressed(KeyCombination combination) {
        return keyPressed().onlyIf(combination::match);
    }

    static EventPattern<Event, KeyEvent> keyPressed(KeyCode code, KeyCombination.Modifier... modifiers) {
        return keyPressed(new KeyCodeCombination(code, modifiers));
    }

    static EventPattern<Event, KeyEvent> keyPressed(Predicate<KeyCode> keyTest, KeyCombination.Modifier... modifiers) {
        return keyPressed(new KeyCodePatternCombination(keyTest, modifiers));
    }

    static EventPattern<Event, KeyEvent> keyPressed(String character, KeyCombination.Modifier... modifiers) {
        return keyPressed(new KeyCharacterCombination(character, modifiers));
    }

    static EventPattern<Event, KeyEvent> keyReleased() {
        return eventType(KEY_RELEASED);
    }

    static EventPattern<Event, KeyEvent> keyReleased(KeyCombination combination) {
        return keyReleased().onlyIf(combination::match);
    }

    static EventPattern<Event, KeyEvent> keyReleased(KeyCode code, KeyCombination.Modifier... modifiers) {
        return keyReleased(new KeyCodeCombination(code, modifiers));
    }

    static EventPattern<Event, KeyEvent> keyReleased(Predicate<KeyCode> keyTest, KeyCombination.Modifier... modifiers) {
        return keyReleased(new KeyCodePatternCombination(keyTest, modifiers));
    }

    static EventPattern<Event, KeyEvent> keyReleased(String character, KeyCombination.Modifier... modifiers) {
        return keyReleased(new KeyCharacterCombination(character, modifiers));
    }

    static EventPattern<Event, KeyEvent> keyTyped() {
        return eventType(KEY_TYPED);
    }

    static EventPattern<Event, KeyEvent> keyTyped(Predicate<String> charTest, KeyCombination.Modifier... modifiers) {
        KeyTypedCombination combination = new KeyTypedCombination(charTest, modifiers);
        return keyTyped().onlyIf(combination::match);
    }

    static EventPattern<Event, KeyEvent> keyTyped(String character, KeyCombination.Modifier... modifiers) {
        return keyTyped(character::equals, modifiers);
    }

    static EventPattern<Event, MouseEvent> mouseClicked() {
        return eventType(MOUSE_CLICKED);
    }

    static EventPattern<Event, MouseEvent> mouseClicked(MouseButton button) {
        return mouseClicked().onlyIf(e -> e.getButton() == button);
    }

    static EventPattern<Event, MouseEvent> mousePressed() {
        return eventType(MOUSE_PRESSED);
    }

    static EventPattern<Event, MouseEvent> mousePressed(MouseButton button) {
        return mousePressed().onlyIf(e -> e.getButton() == button);
    }

    static EventPattern<Event, MouseEvent> mouseReleased() {
        return eventType(MOUSE_RELEASED);
    }

    static EventPattern<Event, MouseEvent> mouseReleased(MouseButton button) {
        return mouseReleased().onlyIf(e -> e.getButton() == button);
    }

    static EventPattern<Event, MouseEvent> mouseMoved() {
        return eventType(MOUSE_MOVED);
    }

    static EventPattern<Event, MouseEvent> mouseDragged() {
        return eventType(MOUSE_DRAGGED);
    }

    static EventPattern<Event, MouseEvent> dragDetected() {
        return eventType(DRAG_DETECTED);
    }

    static EventPattern<Event, MouseEvent> mouseEntered() {
        return eventType(MOUSE_ENTERED);
    }

    static EventPattern<Event, MouseEvent> mouseEnteredTarget() {
        return eventType(MOUSE_ENTERED_TARGET);
    }

    static EventPattern<Event, MouseEvent> mouseExited() {
        return eventType(MOUSE_EXITED);
    }

    static EventPattern<Event, MouseEvent> mouseExitedTarget() {
        return eventType(MOUSE_EXITED_TARGET);
    }
}