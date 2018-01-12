package org.fxmisc.wellbehaved.event;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import javafx.event.Event;
import javafx.event.EventType;

import org.fxmisc.wellbehaved.event.InputHandler.Result;

/**
 * Pattern matching for {@link Event}s.
 *
 * <h2>General Concept as an Analogy</h2>
 *
 * Think of an {@code InputMap} as a powerful switch statement:
 * <pre><code>
 * switch (eventType) {
 *     case KEY_PRESSED:    // the basic EventPattern
 *          doCodeWithEvent(e); // the basic InputHandler
 *          break;
 *
 *     // an EventPattern with a condition
 *     // it only matches KeyTyped events whose text is a space character
 *     // other KeyTyped events don't run the space insertion code beow
 *     case KEY_TYPED e if e.getText().equals(" "):
 *          addSpaceToText();
 *          break;
 *
 *     // an EventPattern with another condition
 *     case MOUSE_MOVED &amp;&amp; otherNode.isVisible():
 *          otherNode.hide();
 *          thirdNode.show();
 *          break;
 *
 *     // runs the same behavior for multiple event types
 *     case MOUSE_PRESSED || MOUSE_RELEASED || MOUSE_DRAGGED || MOUSE_ENTERED_TARGET:
 *          showPopupThatSays("You did something with the mouse!")
 *
 *     // sometimes there is no "default" behavior and the event bubbles back up
 *     default:
 *          break;
 * }
 * </code></pre>
 *
 * <h2>Types of InputMaps</h2>
 * <p>
 *     There are a few types of {@link InputMap}s:
 * </p>
 * <ul>
 *     <li>
 *         The base ones: {@link #ignore(EventType)}, {@link #consume(EventType)}, and
 *         {@link #process(EventType, Function)} (and all their variations).
 *     </li>
 *     <li>
 *         The composable ones: {@link #sequence(InputMap[])}/{@link #orElse(InputMap)},
 *         {@link #upCast(InputMap)}, and {@link #without(InputMap)}.
 *     </li>
 *     <li>
 *         The condition-adding ones: {@link #when(BooleanSupplier, InputMap)} and
 *         {@link #unless(BooleanSupplier, InputMap)}
 *     </li>
 *     <li>
 *         (convenience) Consume with conditions:
 *         {@link #consumeWhen(EventType, BooleanSupplier, Consumer)}
 *         and {@link #consumeUnless(EventType, BooleanSupplier, Consumer)}
 *     </li>
 *     <li>
 *         The post-conditions: {@link #ifIgnored(Consumer)}, {@link #ifProcessed(Consumer)},
 *         {@link #ifConsumed(Consumer)}.
 *     </li>
 * </ul>
 *
 * <h2>Examples</h2>
 * <pre><code>
 * InputMap&lt;KeyEvent&gt; typeTheLetter = consume(
 *      // event pattern (e.g. the "case" line in a switch statement)
 *      keyTyped(),
 *
 *      // input handler (e.g. the "block of code" for a case in a switch statement
 *      e -&gt; textField.addLetter(e.getText)
 * );
 *
 * InputMap&lt;KeyEvent&gt; copy = consume(
 *      anyOf(
 *          keyPressed(KeyCode.C, KeyCode.SHORTCUT),
 *          keyPressed(KeyCode.COPY)
 *      ),
 *      e -&gt; textField.copyToClipboard()
 * );
 *
 * InputMap&lt;KeyEvent&gt; pasteThenClearTextField = consume(
 *      // event pattern
 *      anyOf(
 *          keyPressed(KeyCode.V, KeyCode.SHORTCUT),
 *          keyPressed(KeyCode.PASTE)
 *      ),
 *      e -&gt; textField.copyToClipboard()
 * ).ifConsumed(ignoreMe -&gt; textField.clearText();
 *
 * InputMap&lt;CustomEvent&gt; someCustomEventInputMap = // imagine I put something here...
 *
 * // notice the very generic "Event" event type
 * InputMap&lt;? super Event&gt; allBehaviorForCustomTextField = sequence(
 *      // when an event occurs, check whether the below input map's EventPattern matches the given event
 *      typeTheLetter,
 *      // only pattern match against the below input map if the previous one didn't match
 *      copy,
 *      // check this one only if the above two didn't match
 *      pasteThenClearTextField,
 *
 *      someCustomEventInputMap
 *
 *      // If reach this part, none of them matched, so do nothing
 * );
 *
 * // at this point, "allBehaviorForCustomTextField" could be installed into a Node
 * Nodes.addFallbackInputMap(node, allBehaviorForCustomTextField);
 *
 * // when user types a letter, the text field adds that letter
 * // when user moves the mouse over the text field, nothing happens
 * </code></pre>
 *
 * <h2>Last Words</h2>
 *
 * <p>When creating a subclass of a node that will be instantiated multiple times, use
 * {@link org.fxmisc.wellbehaved.event.template.InputMapTemplate} to create an {@link InputMap} for each
 * created {@link javafx.scene.Node} as this saves some memory. See the template class' javadoc for more details.</p>
 *
 * @param <E> type of events that this {@linkplain InputMap} <em>may</em> handle.
 * That is, {@code InputMap<E>} certainly does not handle any events that
 * are not of type {@code E}; it does <em>not</em> mean it handles <em>any</em>
 * event of type {@code E}.
 */
@FunctionalInterface
public interface InputMap<E extends Event> {

    static final InputMap<?> EMPTY = handlerConsumer -> {};
    static <E extends Event> InputMap<E> empty() { return (InputMap<E>) EMPTY; }

    @FunctionalInterface
    static interface HandlerConsumer<E extends Event> {
        <F extends E> void accept(EventType<? extends F> t, InputHandler<? super F> h);
    }

    /**
     * For each {@link EventPattern} that matches the given event, run the corresponding {@link InputHandler}
     */
    void forEachEventType(HandlerConsumer<? super E> f);

    /**
     * Shorthand for {@link #sequence(InputMap[]) sequence(this, that)}
     */
    default InputMap<E> orElse(InputMap<? extends E> that) {
        return sequence(this, that);
    }

    /**
     * Returns an InputMap that does nothing when {@code this.equals(that)}; otherwise, returns this input map.
     */
    default InputMap<E> without(InputMap<?> that) {
        return this.equals(that) ? empty() : this;
    }

    /**
     * Executes some additional handler if the event was consumed (e.g. {@link InputHandler#process(Event)} returns
     * {@link Result#CONSUME}).
     */
    default InputMap<E> ifConsumed(Consumer<? super E> postConsumption) {
        return postResult(this, Result.CONSUME, postConsumption);
    }

    /**
     * Executes some additional handler if the event was ignored (e.g. {@link InputHandler#process(Event)} returns
     * {@link Result#IGNORE}).
     */
    default InputMap<E> ifIgnored(Consumer<? super E> postIgnore) {
        return postResult(this, Result.IGNORE, postIgnore);
    }

    /**
     * Executes some additional handler if the event was matched but not consumed
     * (e.g. {@link InputHandler#process(Event)} returns {@link Result#PROCEED}).
     */
    default InputMap<E> ifProcessed(Consumer<? super E> postProceed) {
        return postResult(this, Result.PROCEED, postProceed);
    }

    static <E extends Event> InputMap<E> postResult(InputMap<? extends E> map, Result checkedResult, Consumer<? super E> postDesiredResult) {
        return handlerConsumer -> map.forEachEventType(new HandlerConsumer<E>() {

            @Override
            public <T extends E> void accept(EventType<? extends T> t, InputHandler<? super T> h) {
                InputHandler<T> h2 = e -> {
                    Result res = h.process(e);
                    if(res == checkedResult) {
                        postDesiredResult.accept(e);
                    }
                    return res;
                };
                handlerConsumer.accept(t, h2);
            }

        });
    }

    static <E extends Event> InputMap<E> upCast(InputMap<? extends E> inputMap) {
        // Unsafe cast is justified by this type-safe equivalent expression:
        // InputMap<E> res = f -> inputMap.forEachEventType(f);
        @SuppressWarnings("unchecked")
        InputMap<E> res = (InputMap<E>) inputMap;
        return res;
    }

    /**
     * Creates a single InputMap that pattern matches a given event type against all the given input maps. This
     * is often the InputMap installed on a given node since it contains all the other InputMaps.
     */
    @SafeVarargs
    static <E extends Event> InputMap<E> sequence(InputMap<? extends E>... inputMaps) {
        return new InputMapChain<>(inputMaps);
    }

    /**
     * If the given {@link EventPattern} matches the given event type, runs the given action, and then attempts
     * to pattern match the event type with the next {@code InputMap} (if one exists).
     */
    public static <T extends Event, U extends T> InputMap<U> process(
            EventPattern<? super T, ? extends U> eventPattern,
            Function<? super U, InputHandler.Result> action) {
        return new PatternActionMap<>(eventPattern, action);
    }

    /**
     * When the given event type occurs, runs the given action, and then attempts
     * to pattern match the event type with the next {@code InputMap} (if one exists).
     */
    public static <T extends Event> InputMap<T> process(
            EventType<? extends T> eventType,
            Function<? super T, InputHandler.Result> action) {
        return process(EventPattern.eventType(eventType), action);
    }

    /**
     * If the given {@link EventPattern} matches the given event type, runs the given action, consumes the event,
     * and does not attempt to match additional {@code InputMap}s (if they exist).
     */
    public static <T extends Event, U extends T> InputMap<U> consume(
            EventPattern<? super T, ? extends U> eventPattern,
            Consumer<? super U> action) {
        return process(eventPattern, u -> {
            action.accept(u);
            return Result.CONSUME;
        });
    }

    /**
     * When the given event type occurs, runs the given action, consumes the event,
     * and does not attempt to match additional {@code InputMap}s (if they exist).
     */
    public static <T extends Event> InputMap<T> consume(
            EventType<? extends T> eventType,
            Consumer<? super T> action) {
        return consume(EventPattern.eventType(eventType), action);
    }

    /**
     * If the given {@link EventPattern} matches the given event type,
     * consumes the event and does not attempt to match additional
     * {@code InputMap}s (if they exist).
     */
    public static <T extends Event, U extends T> InputMap<U> consume(
            EventPattern<? super T, ? extends U> eventPattern) {
        return process(eventPattern, u -> Result.CONSUME);
    }

    /**
     * When the given event type occurs, consumes the event and does not attempt
     * to match additional {@code InputMap}s (if they exist).
     */
    public static <T extends Event> InputMap<T> consume(
            EventType<? extends T> eventType) {
        return consume(EventPattern.eventType(eventType));
    }

    /**
     * If the given {@link EventPattern} matches the given event type and {@code condition} is true,
     * consumes the event and does not attempt to match additional
     * {@code InputMap}s (if they exist).
     */
    public static <T extends Event, U extends T> InputMap<U> consumeWhen(
            EventPattern<? super T, ? extends U> eventPattern,
            BooleanSupplier condition,
            Consumer<? super U> action) {
        return process(eventPattern, u -> {
            if(condition.getAsBoolean()) {
                action.accept(u);
                return Result.CONSUME;
            } else {
                return Result.PROCEED;
            }
        });
    }

    /**
     * When the given event type occurs and {@code condition} is true,
     * consumes the event and does not attempt to match additional
     * {@code InputMap}s (if they exist).
     */
    public static <T extends Event> InputMap<T> consumeWhen(
            EventType<? extends T> eventType,
            BooleanSupplier condition,
            Consumer<? super T> action) {
        return consumeWhen(EventPattern.eventType(eventType), condition, action);
    }

    /**
     * If the given {@link EventPattern} matches the given event type and {@code condition} is false,
     * consumes the event and does not attempt to match additional
     * {@code InputMap}s (if they exist). If {@code condition} is true, continues to try to pattern match
     * the event type with the next {@code InputMap} (if one exists).
     */
    public static <T extends Event, U extends T> InputMap<U> consumeUnless(
            EventPattern<? super T, ? extends U> eventPattern,
            BooleanSupplier condition,
            Consumer<? super U> action) {
        return consumeWhen(eventPattern, () -> !condition.getAsBoolean(), action);
    }

    /**
     * When the given event type occurs and {@code condition} is false,
     * consumes the event and does not attempt to match additional
     * {@code InputMap}s (if they exist). If {@code condition} is true, continues to try to pattern match
     * the event type with the next {@code InputMap} (if one exists).
     */
    public static <T extends Event> InputMap<T> consumeUnless(
            EventType<? extends T> eventType,
            BooleanSupplier condition,
            Consumer<? super T> action) {
        return consumeUnless(EventPattern.eventType(eventType), condition, action);
    }

    /**
     * If the given {@link EventPattern} matches the given event type, does nothing and does not attempt
     * to match additional {@code InputMap}s (if they exist).
     */
    public static <T extends Event, U extends T> InputMap<U> ignore(
            EventPattern<? super T, ? extends U> eventPattern) {
        return new PatternActionMap<>(eventPattern, PatternActionMap.CONST_IGNORE);
    }

    /**
     * When the given event type occurs, does nothing and does not attempt to match additional
     * {@code InputMap}s (if they exist).
     */
    public static <T extends Event> InputMap<T> ignore(
            EventType<? extends T> eventType) {
        return ignore(EventPattern.eventType(eventType));
    }

    /**
     * When the given {@code condition} is true, pattern matches the event with the given {@link InputMap} or
     * proceeds to the next {@code InputMap} (if it exists).
     */
    public static <T extends Event> InputMap<T> when(
            BooleanSupplier condition, InputMap<T> im) {

        return new InputMap<T>() {

            @Override
            public void forEachEventType(HandlerConsumer<? super T> f) {
                HandlerConsumer<T> g = new HandlerConsumer<T>() {

                    @Override
                    public <F extends T> void accept(
                            EventType<? extends F> t, InputHandler<? super F> h) {
                        f.accept(t, evt -> condition.getAsBoolean() ? h.process(evt) : Result.PROCEED);
                    }

                };

                im.forEachEventType(g);
            }
        };
    }

    /**
     * When the given {@code condition} is false, pattern matches the event with the given {@link InputMap} or
     * proceeds to the next {@code InputMap} (if it exists).
     */
    public static <T extends Event> InputMap<T> unless(
            BooleanSupplier condition, InputMap<T> im) {
        return when(() -> !condition.getAsBoolean(), im);
    }
}

class PatternActionMap<T extends Event, U extends T> implements InputMap<U> {
    static final Function<Object, Result> CONST_IGNORE = x -> Result.IGNORE;

    private final EventPattern<T, ? extends U> pattern;
    private final Function<? super U, InputHandler.Result> action;

    PatternActionMap(EventPattern<T, ? extends U> pattern, Function<? super U, InputHandler.Result> action) {
        this.pattern = pattern;
        this.action  = action;
    }

    @Override
    public void forEachEventType(HandlerConsumer<? super U> f) {
        InputHandler<T> h = t -> pattern.match(t).map(action::apply).orElse(Result.PROCEED);
        pattern.getEventTypes().forEach(et -> f.accept(et, h));
    }

    @Override
    public boolean equals(Object other) {
        if(other instanceof PatternActionMap) {
            PatternActionMap<?, ?> that = (PatternActionMap<?, ?>) other;
            return Objects.equals(this.pattern, that.pattern)
                && Objects.equals(this.action,  that.action);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(pattern, action);
    }
}

class InputMapChain<E extends Event> implements InputMap<E> {
    private final InputMap<? extends E>[] inputMaps;

    @SafeVarargs
    InputMapChain(InputMap<? extends E>... inputMaps) {
        this.inputMaps = inputMaps;
    }

    @Override
    public void forEachEventType(HandlerConsumer<? super E> f) {
        InputHandlerMap<E> ihm = new InputHandlerMap<E>();
        for(InputMap<? extends E> im: inputMaps) {
            im.forEachEventType(ihm::insertAfter);
        }
        ihm.forEach(f);
    }

    @Override
    public InputMap<E> without(InputMap<?> that) {
        if(this.equals(that)) {
            return InputMap.empty();
        } else {
            @SuppressWarnings("unchecked")
            InputMap<? extends E>[] ims = (InputMap<? extends E>[]) Stream.of(inputMaps)
                    .map(im -> im.without(that))
                    .filter(im -> im != EMPTY)
                    .toArray(n -> new InputMap<?>[n]);
            switch(ims.length) {
                case 0: return InputMap.empty();
                case 1: return InputMap.upCast(ims[0]);
                default: return new InputMapChain<>(ims);
            }
        }
    }

    @Override
    public boolean equals(Object other) {
        if(other instanceof InputMapChain) {
            InputMapChain<?> that = (InputMapChain<?>) other;
            return Arrays.equals(this.inputMaps, that.inputMaps);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(inputMaps);
    }
}