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

    void forEachEventType(HandlerConsumer<? super E> f);

    default InputMap<E> orElse(InputMap<? extends E> that) {
        return sequence(this, that);
    }

    default InputMap<E> without(InputMap<?> that) {
        return this.equals(that) ? empty() : this;
    }

    /**
     * Executes some additional handler if the event was consumed
     */
    default InputMap<E> ifConsumed(Consumer<? super E> postConsumption) {
        return handlerConsumer -> InputMap.this.forEachEventType(new HandlerConsumer<E>() {

            @Override
            public <T extends E> void accept(EventType<? extends T> t, InputHandler<? super T> h) {
                InputHandler<T> h2 = e -> {
                    Result res = h.process(e);
                    if(res == Result.CONSUME) {
                        postConsumption.accept(e);
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

    @SafeVarargs
    static <E extends Event> InputMap<E> sequence(InputMap<? extends E>... inputMaps) {
        return new InputMapChain<>(inputMaps);
    }

    public static <T extends Event, U extends T> InputMap<U> process(
            EventPattern<? super T, ? extends U> eventPattern,
            Function<? super U, InputHandler.Result> action) {
        return new PatternActionMap<>(eventPattern, action);
    }

    public static <T extends Event> InputMap<T> process(
            EventType<? extends T> eventType,
            Function<? super T, InputHandler.Result> action) {
        return process(EventPattern.eventType(eventType), action);
    }

    public static <T extends Event, U extends T> InputMap<U> consume(
            EventPattern<? super T, ? extends U> eventPattern,
            Consumer<? super U> action) {
        return process(eventPattern, u -> {
            action.accept(u);
            return Result.CONSUME;
        });
    }

    public static <T extends Event> InputMap<T> consume(
            EventType<? extends T> eventType,
            Consumer<? super T> action) {
        return consume(EventPattern.eventType(eventType), action);
    }

    public static <T extends Event, U extends T> InputMap<U> consume(
            EventPattern<? super T, ? extends U> eventPattern) {
        return process(eventPattern, u -> Result.CONSUME);
    }

    public static <T extends Event> InputMap<T> consume(
            EventType<? extends T> eventType) {
        return consume(EventPattern.eventType(eventType));
    }

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

    public static <T extends Event> InputMap<T> consumeWhen(
            EventType<? extends T> eventType,
            BooleanSupplier condition,
            Consumer<? super T> action) {
        return consumeWhen(EventPattern.eventType(eventType), condition, action);
    }

    public static <T extends Event, U extends T> InputMap<U> consumeUnless(
            EventPattern<? super T, ? extends U> eventPattern,
            BooleanSupplier condition,
            Consumer<? super U> action) {
        return consumeWhen(eventPattern, () -> !condition.getAsBoolean(), action);
    }

    public static <T extends Event> InputMap<T> consumeUnless(
            EventType<? extends T> eventType,
            BooleanSupplier condition,
            Consumer<? super T> action) {
        return consumeUnless(EventPattern.eventType(eventType), condition, action);
    }

    public static <T extends Event, U extends T> InputMap<U> ignore(
            EventPattern<? super T, ? extends U> eventPattern) {
        return new PatternActionMap<>(eventPattern, PatternActionMap.CONST_IGNORE);
    }

    public static <T extends Event> InputMap<T> ignore(
            EventType<? extends T> eventType) {
        return ignore(EventPattern.eventType(eventType));
    }

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