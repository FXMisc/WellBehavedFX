package org.fxmisc.wellbehaved.event;

import static javafx.scene.input.KeyEvent.*;

import java.util.Optional;
import java.util.function.Predicate;

import javafx.event.Event;
import javafx.event.EventType;
import javafx.scene.input.KeyCharacterCombination;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;

@FunctionalInterface
public interface EventPattern<T extends Event, U extends T> {
    Optional<U> match(T event);

    default <V extends U> EventPattern<T, V> andThen(EventPattern<? super U, V> next) {
        return t -> match(t).flatMap(next::match);
    }

    default EventPattern<T, U> and(Predicate<? super U> condition) {
        return t -> match(t).map(u -> condition.test(u) ? u : null);
    }

    static <T extends Event> EventPattern<Event, T> eventTypePattern(EventType<? extends T> eventType) {
        return event -> {
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
        };
    }

    static EventPattern<Event, KeyEvent> keyPressed() {
        return eventTypePattern(KeyEvent.KEY_PRESSED);
    }

    static EventPattern<Event, KeyEvent> keyPressed(KeyCombination combination) {
        return keyPressed().and(combination::match);
    }

    static EventPattern<Event, KeyEvent> keyPressed(KeyCode code, KeyCombination.Modifier... modifiers) {
        return keyPressed(new KeyCodeCombination(code, modifiers));
    }

    static EventPattern<Event, KeyEvent> keyPressed(String character, KeyCombination.Modifier... modifiers) {
        return keyPressed(new KeyCharacterCombination(character, modifiers));
    }

    static EventPattern<Event, KeyEvent> keyReleased() {
        return eventTypePattern(KEY_RELEASED);
    }

    static EventPattern<Event, KeyEvent> keyReleased(KeyCombination combination) {
        return keyReleased().and(combination::match);
    }

    static EventPattern<Event, KeyEvent> keyReleased(KeyCode code, KeyCombination.Modifier... modifiers) {
        return keyReleased(new KeyCodeCombination(code, modifiers));
    }

    static EventPattern<Event, KeyEvent> keyReleased(String character, KeyCombination.Modifier... modifiers) {
        return keyReleased(new KeyCharacterCombination(character, modifiers));
    }

    static EventPattern<Event, KeyEvent> keyTyped() {
        return eventTypePattern(KEY_TYPED);
    }

    static EventPattern<Event, KeyEvent> keyTyped(String character, KeyCombination.Modifier... modifiers) {
        KeyTypedCombination combination = new KeyTypedCombination(character, modifiers);
        return keyTyped().and(combination::match);
    }
}