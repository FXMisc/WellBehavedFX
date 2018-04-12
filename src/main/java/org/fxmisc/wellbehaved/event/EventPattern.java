package org.fxmisc.wellbehaved.event;

import static javafx.scene.input.KeyCombination.ALT_ANY;
import static javafx.scene.input.KeyCombination.META_ANY;
import static javafx.scene.input.KeyCombination.SHIFT_ANY;
import static javafx.scene.input.KeyCombination.SHORTCUT_ANY;
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

/**
 * Helper class for pattern-matching one or more {@link EventType}s (e.g. the "case" line in a powerful switch
 * statement). When {@link #match(Event)} returns a non-empty {@link Optional}, the corresponding
 * {@link InputHandler} will be called.
 *
 * <h2>Usages</h2>
 * <p>
 *     This class provides a number of static factory methods that provide the base pattern to match.
 * </p>
 * <ul>
 *     <li>
 *         <em>Most will use the ones for the common event types:</em> {@link #keyPressed()}, {@link #keyTyped()},
 *         {@link #mousePressed()}, {@link #mouseClicked()}, {@link #mouseDragged()}, etc.
 *     </li>
 *     <li>
 *         <em>For custom events or super event types (e.g. {@link javafx.scene.input.InputEvent#ANY}),</em> one
 *         will use the base pattern, {@link #eventType(EventType)}
 *     </li>
 * </ul>
 *
 * <p>
 *     Once a base pattern is created, one can further define the pattern for which to match for by
 *     adding what are known as "guards" in pattern matching: {@link #andThen(EventPattern)},
 *     {@link #onlyIf(Predicate)}, {@link #unless(Predicate)}, and {@link #anyOf(EventPattern[])}. See each
 *     method's javadoc for more info.
 * </p>
 *
 * <h2>Examples</h2>
 * <pre><code>
 * // a pattern that matches any key pressed event
 * keyPressed()
 *
 * // a pattern that matches only key pressed events where the user
 * // pressed a digit key
 * keyPressed().onlyIf(pressedKey -&gt; pressedKey.getCode().isDigitKey())
 * </code></pre>
 */
public interface EventPattern<T extends Event, U extends T> {

    static KeyCombination.Modifier[] ALL_MODIFIERS_AS_ANY = new KeyCombination.Modifier[] {
            SHORTCUT_ANY, SHIFT_ANY, ALT_ANY, META_ANY
    };

    /**
     * Returns a non-empty {@link Optional} when a match is found.
     */
    Optional<? extends U> match(T event);
    Set<EventType<? extends U>> getEventTypes();

    /**
     * Returns an EventPattern that matches the given event type only when this event pattern matches it
     * and the {@code next} EventPattern matches it.
     */
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

    /**
     * Returns an EventPattern that matches the given event type only if this event pattern matches it
     * and the event type passed the given {@code condition}
     */
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

    /**
     * Returns an EventPattern that matches the given event type only if this event pattern matches it
     * and the event type fails the given {@code condition}
     */
    default EventPattern<T, U> unless(Predicate<? super U> condition) {
        return onlyIf(condition.negate());
    }

    /**
     * Returns an EventPattern that matches the given event type when any of the given EventPatterns match the
     * given event type; useful when one wants to specify the same behavior for a variety of events (i.e. the
     * "copy" action when a user press "CTRL+C" on Windows or "COMMAND+C" on Mac).
     */
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
        return keyPressed(new GenericKeyCombination(e -> keyTest.test(e.getCode()), modifiers));
    }

    static EventPattern<Event, KeyEvent> keyPressed(String character, KeyCombination.Modifier... modifiers) {
        return keyPressed(new KeyCharacterCombination(character, modifiers));
    }

    /**
     * Matches the given key pressed event regardless of modifiers; this should only be used for the rare KeyEvents
     * which require a pressed modifier (e.g. Shift) to generate it (e.g. "{"). If passed in a regular character
     * (e.g. "a") and this appears before another EventPattern (e.g. keyPressed("a", SHORTCUT_DOWN)) in an
     * {@link InputMap#sequence(InputMap[])}, the second EventPattern will never run.
     */
    static EventPattern<Event, KeyEvent> keyPressedNoMod(String character) {
        KeyCharacterCombination combination = new KeyCharacterCombination(character, ALL_MODIFIERS_AS_ANY);
        return keyPressed().onlyIf(combination::match);
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
        return keyReleased(new GenericKeyCombination(e -> keyTest.test(e.getCode()), modifiers));
    }

    static EventPattern<Event, KeyEvent> keyReleased(String character, KeyCombination.Modifier... modifiers) {
        return keyReleased(new KeyCharacterCombination(character, modifiers));
    }

    /**
     * Matches the given key released event regardless of modifiers; this should only be used for the rare KeyEvents
     * which require a pressed modifier (e.g. Shift) to generate it (e.g. "{"). If passed in a regular character
     * (e.g. "a") and this appears before another EventPattern (e.g. keyReleased("a", SHORTCUT_DOWN)) in an
     * {@link InputMap#sequence(InputMap[])}, the second EventPattern will never run.
     */
    static EventPattern<Event, KeyEvent> keyReleasedNoMod(String character) {
        KeyCharacterCombination combination = new KeyCharacterCombination(character, ALL_MODIFIERS_AS_ANY);
        return keyReleased().onlyIf(combination::match);
    }

    static EventPattern<Event, KeyEvent> keyTyped() {
        return eventType(KEY_TYPED);
    }

    static EventPattern<Event, KeyEvent> keyTyped(Predicate<String> charTest, KeyCombination.Modifier... modifiers) {
        GenericKeyCombination combination = new GenericKeyCombination(e -> charTest.test(e.getCharacter()), modifiers);
        return keyTyped().onlyIf(combination::match);
    }

    static EventPattern<Event, KeyEvent> keyTyped(String character, KeyCombination.Modifier... modifiers) {
        return keyTyped(character::equals, modifiers);
    }

    /**
     * Matches the given key typed event regardless of modifiers; this should only be used for the rare KeyEvents
     * which require a pressed modifier (e.g. Shift) to generate it (e.g. "{"). If passed in a regular character
     * (e.g. "a") and this appears before another EventPattern (e.g. keyTyped("a", SHORTCUT_DOWN)) in an
     * {@link InputMap#sequence(InputMap[])}, the second EventPattern will never run.
     */
    static EventPattern<Event, KeyEvent> keyTypedNoMod(String character) {
        return keyTyped().onlyIf(e -> e.getCharacter().equals(character));
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