package org.fxmisc.wellbehaved.event.template;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import javafx.event.Event;
import javafx.event.EventType;
import javafx.scene.Node;

import org.fxmisc.wellbehaved.event.EventPattern;
import org.fxmisc.wellbehaved.event.InputHandler;
import org.fxmisc.wellbehaved.event.InputHandler.Result;
import org.fxmisc.wellbehaved.event.InputMap;
import org.fxmisc.wellbehaved.event.Nodes;

/**
 * See {@link InputMap} for an explanation. This simply turns that concept into a template that can be used
 * to add the same {@link InputMap} to multiple instances of the same class.
 *
 * <h2>Adding a template to a class</h2>
 *
 * <p>
 *     Given a class, the InputMapTemplate code should be created in a {@code static} block and then instantiate
 *     itself in the node's constructor:
 * </p>
 * <pre><code>
 * public class CustomTextField extends TextField {
 *
 *     private final static InputMapTemplate&lt;? super Event&gt; BEHAVIOR;
 *
 *     static {
 *
 *          // creating InputMapTemplates by
 *          InputMapTemplate&lt;CustomTextField, ? extends KeyEvent&gt; keyEventBehavior = sequence(
 *              consume(keyTyped(), (field, event) -&gt; field.setText(event.getText()),
 *              consume(
 *                  anyOf(
 *                      mousePressed(),
 *                      mouseMoved(),
 *                      mouseReleased()
 *                  ),
 *                  (field, event) -&gt; field.setText("Mouse event detected! Event: " + event)
 *              )
 *          );
 *
 *          // Other InputMapTemplates (though these don't need to be broken up by
 *          // KeyEvent, MouseEvent, etc. They could be interwoven depending on your desired behavior)
 *          InputMapTemplate&lt;CustomTextField, ? extends MouseEvent&gt; mouseEventBehavior = sequence(
 *              // other InputMapTemplates go here...
 *          );
 *
 *          InputMapTemplate&lt;CustomTextField, ? extends Event&gt; otherCustomEventBehavior = sequence(
 *              // other InputMapTemplates go here...
 *          );
 *
 *          // Tying all of them together into one final InputMapTemplate
 *          BEHAVIOR = sequence(
 *              keyEventBehavior,
 *              mouseEventBehavior,
 *              otherCustomEventBehavior
 *          );
 *     }
 *
 *     public CustomTextField(Object[] args) {
 *         super(args);
 *         // other constructor stuff here
 *
 *         // Install InputMapTemplate onto this node here via one of the two "install" methods
 *         // described after this code block
 *     }
 *
 *     // rest of the class
 * }
 * </code></pre>
 * <p>
 *     The InputMapTemplate can be instantiated as a default behavior ({@link #installFallback(InputMapTemplate, Node)}
 *     or as something that overrides prior default behavior ({@link #installOverride(InputMapTemplate, Node)} (or
 *     their variants). Likewise, it can be removed via {@link #uninstall(InputMapTemplate, Node)}.
 * </p>
 * @param <S> the type of the object that will be passed into the {@link InputHandlerTemplate}'s block of code.
 * @param <E> the event type for which this InputMap's {@link EventPattern} matches
 */
public abstract class InputMapTemplate<S, E extends Event> {

    @FunctionalInterface
    public static interface HandlerTemplateConsumer<S, E extends Event> {

        <F extends E> void accept(
                EventType<? extends F> t,
                InputHandlerTemplate<S, ? super F> h);


        static <S, E extends Event> HandlerTemplateConsumer<S, E> from(
                InputMap.HandlerConsumer<E> hc, S target) {
            return new HandlerTemplateConsumer<S, E>() {
                @Override
                public <F extends E> void accept(
                        EventType<? extends F> t, InputHandlerTemplate<S, ? super F> h) {
                    hc.accept(t, evt -> h.process(target, evt));

                }
            };
        }
    }

    private InputHandlerTemplateMap<S, E> inputHandlerTemplates = null;

    public final void forEachEventType(HandlerTemplateConsumer<S, ? super E> f) {
        if(inputHandlerTemplates == null) {
            inputHandlerTemplates = getInputHandlerTemplateMap();
        }
        inputHandlerTemplates.forEach(f);
    }

    /**
     * Shorthand for {@link #sequence(InputMapTemplate[])}  sequence(this, that)}
     */
    public final InputMapTemplate<S, E> orElse(InputMapTemplate<S, ? extends E> that) {
        return sequence(this, that);
    }

    /**
     * Converts this InputMapTemplate into an {@link InputMap} for the given {@code target}
     */
    public final InputMap<E> instantiate(S target) {
        return new InputMapTemplateInstance<>(this, target);
    }

    protected abstract InputHandlerTemplateMap<S, E> getInputHandlerTemplateMap();


    static <S, E extends Event> InputMapTemplate<S, E> upCast(InputMapTemplate<S, ? extends E> imt) {
        @SuppressWarnings("unchecked")
        InputMapTemplate<S, E> res = (InputMapTemplate<S, E>) imt;
        return res;
    }

    /**
     * Creates a single InputMapTemplate that pattern matches a given event type against all the given
     * InputMapTemplates. This is often the InputMapTemplate installed on a given node since it contains all
     * the other InputMapTemplates.
     */
    @SafeVarargs
    public static <S, E extends Event> InputMapTemplate<S, E> sequence(InputMapTemplate<S, ? extends E>... templates) {
        return new TemplateChain<>(templates);
    }

    /**
     * If the given {@link EventPattern} matches the given event type, runs the given action, and then attempts
     * to pattern match the event type with the next {@code InputMap} (if one exists).
     */
    public static <S, T extends Event, U extends T> InputMapTemplate<S, U> process(
            EventPattern<? super T, ? extends U> eventPattern,
            BiFunction<? super S, ? super U, InputHandler.Result> action) {
        return new PatternActionTemplate<>(eventPattern, action);
    }

    /**
     * When the given event type occurs, runs the given action, and then attempts
     * to pattern match the event type with the next {@code InputMap} (if one exists).
     */
    public static <S, T extends Event> InputMapTemplate<S, T> process(
            EventType<? extends T> eventType,
            BiFunction<? super S, ? super T, InputHandler.Result> action) {
        return process(EventPattern.eventType(eventType), action);
    }

    /**
     * Executes some additional handler if the event was consumed (e.g. {@link InputHandler#process(Event)} returns
     * {@link Result#CONSUME}).
     */
    public InputMapTemplate<S, E> ifConsumed(BiConsumer<? super S, ? super E> postConsumption) {
        return postResult(Result.CONSUME, postConsumption);
    }

    /**
     * Executes some additional handler if the event was ignored
     * (e.g. {@link InputHandlerTemplate#process(Object, Event)} returns {@link Result#IGNORE}).
     */
    public InputMapTemplate<S, E> ifIgnored(BiConsumer<? super S, ? super E> postIgnore) {
        return postResult(Result.IGNORE, postIgnore);
    }

    /**
     * Executes some additional handler if the event was consumed (e.g. {@link InputHandler#process(Event)} returns
     * {@link Result#CONSUME}).
     */
    public InputMapTemplate<S, E> ifProcessed(BiConsumer<? super S, ? super E> postProceed) {
        return postResult(Result.PROCEED, postProceed);
    }

    private InputMapTemplate<S, E> postResult(Result checkedResult, BiConsumer<? super S, ? super E> postDesiredResult) {
        return new InputMapTemplate<S, E>() {
            @Override
            protected InputHandlerTemplateMap<S, E> getInputHandlerTemplateMap() {
                return InputMapTemplate.this.getInputHandlerTemplateMap().map(iht -> {
                    return (s, evt) -> {
                        Result res = iht.process(s, evt);
                        if (res == checkedResult) {
                            postDesiredResult.accept(s, evt);
                        }
                        return res;
                    };
                });
            }
        };
    }

    /**
     * If the given {@link EventPattern} matches the given event type, runs the given action, consumes the event,
     * and does not attempt to match additional {@code InputMap}s (if they exist).
     */
    public static <S, T extends Event, U extends T> InputMapTemplate<S, U> consume(
            EventPattern<? super T, ? extends U> eventPattern,
            BiConsumer<? super S, ? super U> action) {
        return process(eventPattern, (s, u) -> {
            action.accept(s, u);
            return Result.CONSUME;
        });
    }

    /**
     * When the given event type occurs, runs the given action, consumes the event,
     * and does not attempt to match additional {@code InputMap}s (if they exist).
     */
    public static <S, T extends Event> InputMapTemplate<S, T> consume(
            EventType<? extends T> eventType,
            BiConsumer<? super S, ? super T> action) {
        return consume(EventPattern.eventType(eventType), action);
    }

    /**
     * If the given {@link EventPattern} matches the given event type,
     * consumes the event and does not attempt to match additional
     * {@code InputMap}s (if they exist).
     */
    public static <S, T extends Event, U extends T> InputMapTemplate<S, U> consume(
            EventPattern<? super T, ? extends U> eventPattern) {
        return process(eventPattern, (s, u) -> Result.CONSUME);
    }

    /**
     * When the given event type occurs, consumes the event and does not attempt
     * to match additional {@code InputMap}s (if they exist).
     */
    public static <S, T extends Event> InputMapTemplate<S, T> consume(
            EventType<? extends T> eventType) {
        return consume(EventPattern.eventType(eventType));
    }

    /**
     * If the given {@link EventPattern} matches the given event type and {@code condition} is true,
     * consumes the event and does not attempt to match additional
     * {@code InputMap}s (if they exist).
     */
    public static <S, T extends Event, U extends T> InputMapTemplate<S, U> consumeWhen(
            EventPattern<? super T, ? extends U> eventPattern,
            Predicate<? super S> condition,
            BiConsumer<? super S, ? super U> action) {
        return process(eventPattern, (s, u) -> {
            if(condition.test(s)) {
                action.accept(s, u);
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
    public static <S, T extends Event> InputMapTemplate<S, T> consumeWhen(
            EventType<? extends T> eventType,
            Predicate<? super S> condition,
            BiConsumer<? super S, ? super T> action) {
        return consumeWhen(EventPattern.eventType(eventType), condition, action);
    }

    /**
     * If the given {@link EventPattern} matches the given event type and {@code condition} is false,
     * consumes the event and does not attempt to match additional
     * {@code InputMap}s (if they exist). If {@code condition} is true, continues to try to pattern match
     * the event type with the next {@code InputMap} (if one exists).
     */
    public static <S, T extends Event, U extends T> InputMapTemplate<S, U> consumeUnless(
            EventPattern<? super T, ? extends U> eventPattern,
            Predicate<? super S> condition,
            BiConsumer<? super S, ? super U> action) {
        return consumeWhen(eventPattern, condition.negate(), action);
    }

    /**
     * When the given event type occurs and {@code condition} is false,
     * consumes the event and does not attempt to match additional
     * {@code InputMap}s (if they exist). If {@code condition} is true, continues to try to pattern match
     * the event type with the next {@code InputMap} (if one exists).
     */
    public static <S, T extends Event> InputMapTemplate<S, T> consumeUnless(
            EventType<? extends T> eventType,
            Predicate<? super S> condition,
            BiConsumer<? super S, ? super T> action) {
        return consumeUnless(EventPattern.eventType(eventType), condition, action);
    }

    /**
     * If the given {@link EventPattern} matches the given event type, does nothing and does not attempt
     * to match additional {@code InputMap}s (if they exist).
     */
    public static <S, T extends Event, U extends T> InputMapTemplate<S, U> ignore(
            EventPattern<? super T, ? extends U> eventPattern) {
        return new PatternActionTemplate<>(eventPattern, PatternActionTemplate.CONST_IGNORE);
    }

    /**
     * When the given event type occurs, does nothing and does not attempt to match additional
     * {@code InputMap}s (if they exist).
     */
    public static <S, T extends Event> InputMapTemplate<S, T> ignore(
            EventType<? extends T> eventType) {
        return ignore(EventPattern.eventType(eventType));
    }

    /**
     * When the given {@code condition} is true, pattern matches the event with the given {@link InputMap} or
     * proceeds to the next {@code InputMap} (if it exists).
     */
    public static <S, T extends Event> InputMapTemplate<S, T> when(
            Predicate<? super S> condition, InputMapTemplate<S, T> imt) {

        return new InputMapTemplate<S, T>() {
            @Override
            protected InputHandlerTemplateMap<S, T> getInputHandlerTemplateMap() {
                return imt.getInputHandlerTemplateMap().map(
                        h -> (s, evt) -> condition.test(s) ? h.process(s, evt) : Result.PROCEED);
            }
        };
    }

    /**
     * When the given {@code condition} is false, pattern matches the event with the given {@link InputMap} or
     * proceeds to the next {@code InputMap} (if it exists).
     */
    public static <S, T extends Event> InputMapTemplate<S, T> unless(
            Predicate<? super S> condition, InputMapTemplate<S, T> imt) {
        return when(condition.negate(), imt);
    }

    public static <S, T, E extends Event> InputMapTemplate<S, E> lift(
            InputMapTemplate<T, E> imt,
            Function<? super S, ? extends T> f) {

        return new InputMapTemplate<S, E>() {
            @Override
            protected InputHandlerTemplateMap<S, E> getInputHandlerTemplateMap() {
                return imt.getInputHandlerTemplateMap().map(
                        h -> (s, evt) -> h.process(f.apply(s), evt));
            }
        };
    }

    /**
     * Instantiates the input map and installs it into the node via {@link Nodes#addInputMap(Node, InputMap)}
     */
    public static <S extends Node, E extends Event> void installOverride(InputMapTemplate<S, E> imt, S node) {
        Nodes.addInputMap(node, imt.instantiate(node));
    }

    /**
     * Instantiates the input map and installs it into the node via {@link Nodes#addInputMap(Node, InputMap)}
     */
    public static <S, N extends Node, E extends Event> void installOverride(InputMapTemplate<S, E> imt, S target, Function<? super S, ? extends N> getNode) {
        Nodes.addInputMap(getNode.apply(target), imt.instantiate(target));
    }

    /**
     * Instantiates the input map and installs it into the node via {@link Nodes#addFallbackInputMap(Node, InputMap)}
     */
    public static <S extends Node, E extends Event> void installFallback(InputMapTemplate<S, E> imt, S node) {
        Nodes.addFallbackInputMap(node, imt.instantiate(node));
    }

    /**
     * Instantiates the input map and installs it into the node via {@link Nodes#addFallbackInputMap(Node, InputMap)}
     */
    public static <S, N extends Node, E extends Event> void installFallback(InputMapTemplate<S, E> imt, S target, Function<? super S, ? extends N> getNode) {
        Nodes.addFallbackInputMap(getNode.apply(target), imt.instantiate(target));
    }

    /**
     * Removes the input map template's instance from the given node.
     */
    public static <S extends Node, E extends Event> void uninstall(InputMapTemplate<S, E> imt, S node) {
        Nodes.removeInputMap(node, imt.instantiate(node));
    }

    /**
     * Removes the input map template's instance from the given node.
     */
    public static <S, N extends Node, E extends Event> void uninstall(InputMapTemplate<S, E> imt, S target, Function<? super S, ? extends N> getNode) {
        Nodes.removeInputMap(getNode.apply(target), imt.instantiate(target));
    }
}

class PatternActionTemplate<S, T extends Event, U extends T> extends InputMapTemplate<S, U> {
    static final BiFunction<Object, Object, Result> CONST_IGNORE = (x, y) -> Result.IGNORE;

    private final EventPattern<T, ? extends U> pattern;
    private final BiFunction<? super S, ? super U, InputHandler.Result> action;

    PatternActionTemplate(EventPattern<T, ? extends U> pattern, BiFunction<? super S, ? super U, InputHandler.Result> action) {
        this.pattern = pattern;
        this.action  = action;
    }

    @Override
    protected InputHandlerTemplateMap<S, U> getInputHandlerTemplateMap() {
        InputHandlerTemplateMap<S, U> ihtm = new InputHandlerTemplateMap<>();
        InputHandlerTemplate<S, T> iht = (s, t) -> pattern.match(t).map(u -> action.apply(s, u)).orElse(Result.PROCEED);
        pattern.getEventTypes().forEach(et -> ihtm.insertAfter(et, iht));
        return ihtm;
    }

    @Override
    public boolean equals(Object other) {
        if(other instanceof PatternActionTemplate) {
            PatternActionTemplate<?, ?, ?> that = (PatternActionTemplate<?, ?, ?>) other;
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

class TemplateChain<S, E extends Event> extends InputMapTemplate<S, E> {
    private final InputMapTemplate<S, ? extends E>[] templates;

    @SafeVarargs
    TemplateChain(InputMapTemplate<S, ? extends E>... templates) {
        this.templates = templates;
    }

    @Override
    protected InputHandlerTemplateMap<S, E> getInputHandlerTemplateMap() {
        InputHandlerTemplateMap<S, E> ihtm = new InputHandlerTemplateMap<>();
        for(InputMapTemplate<S, ? extends E> imt: templates) {
            imt.getInputHandlerTemplateMap().forEach(ihtm::insertAfter);
        }
        return ihtm;
    }

    @Override
    public boolean equals(Object other) {
        if(other instanceof TemplateChain) {
            TemplateChain<?, ?> that = (TemplateChain<?, ?>) other;
            return Arrays.equals(this.templates, that.templates);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(templates);
    }
}

class InputMapTemplateInstance<S, E extends Event> implements InputMap<E> {
    private final InputMapTemplate<S, E> template;
    private final S target;

    InputMapTemplateInstance(InputMapTemplate<S, E> template, S target) {
        this.template = template;
        this.target = target;
    }

    @Override
    public void forEachEventType(HandlerConsumer<? super E> hc) {
        template.forEachEventType(
                InputMapTemplate.HandlerTemplateConsumer.from(hc, target));
    }

    @Override
    public boolean equals(Object other) {
        if(other instanceof InputMapTemplateInstance) {
            InputMapTemplateInstance<?, ?> that = (InputMapTemplateInstance<?, ?>) other;
            return Objects.equals(this.template, that.template)
                && Objects.equals(this.target, that.target);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(template, target);
    }
}