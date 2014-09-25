package org.fxmisc.wellbehaved.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javafx.event.Event;
import javafx.event.EventType;

public final class StatefulEventHandlerTemplate<T, S, E extends Event> implements EventHandlerTemplate<T, E> {

    @FunctionalInterface
    public interface StateTransition<T, S, E extends Event> {
        S transition(T target, S state, E event);
    }

    /**
     * An instance of this interface is expected to <em>consume</em> the event
     * if it successfully handled the event. If the event was not handled by an
     * instance of this interface, the event should be left unconsumed and the
     * returned state should be unchanged.
     */
    @FunctionalInterface
    public interface StateTransitioningHandler<T, S, E extends Event> {
        S handle(T target, S state, E event);

        default <U extends T, F extends E> StateTransitioningHandler<U, S, F> orElse(
                StateTransitioningHandler<? super U, S, ? super F> nextHandler) {
            return (u, s, f) -> {
                S newState = StateTransitioningHandler.this.handle(u, s, f);
                if(f.isConsumed()) {
                    return newState;
                } else {
                    return nextHandler.handle(u, s, f);
                }
            };
        }

        default <U extends T> StateTransitioningHandler<U, S, E> onlyWhen(BiPredicate<? super U, ? super S> condition) {
            return (u, s, e) -> {
                return condition.test(u, s)
                        ? StateTransitioningHandler.this.handle(u, s, e)
                        : s;
            };
        }

        default StatefulEventHandlerTemplate<T, S, E> initialStateSupplier(Supplier<? extends S> initialStateSupplier) {
            return new StatefulEventHandlerTemplate<>(this, initialStateSupplier);
        }
    }

    public static abstract class Builder<T, S, E extends Event> {

        private static <T, S, E extends Event> Builder<T, S, E> empty() {
            return new Builder<T, S, E>() {
                @Override
                <U extends T, F extends E> List<StateTransitioningHandler<? super U, S, ? super F>> getHandlers(
                        int additionalCapacity) {
                    return new ArrayList<>(additionalCapacity);
                }
            };
        }

        // private constructor to prevent subclassing by the user
        private Builder() {}

        public <F extends E> On<T, S, E, F> on(EventPattern<? super E, ? extends F> eventMatcher) {
            return new On<>(this, eventMatcher);
        }

        public <F extends E> On<T, S, E, F> on(EventType<? extends F> eventType) {
            return on(EventPattern.eventTypePattern(eventType));
        }

        public <U extends T, F extends E> Builder<U, S, F> addHandler(
                StateTransitioningHandler<? super U, S, ? super F> handler) {
            return new CompositeBuilder<>(this, handler);
        }

        public StateTransitioningHandler<T, S, E> createHandler() {
            return (t, s, e) -> {
                S newState = s;
                for(StateTransitioningHandler<? super T, S, ? super E> handler: getHandlers()) {
                    newState = handler.handle(t, newState, e);
                    if(e.isConsumed()) {
                        break;
                    }
                }
                return newState;
            };
        }

        public StatefulEventHandlerTemplate<T, S, E> initialStateSupplier(Supplier<? extends S> initialStateSupplier) {
            return createHandler().initialStateSupplier(initialStateSupplier);
        }

        List<StateTransitioningHandler<? super T, S, ? super E>> getHandlers() {
            return getHandlers(0);
        }

        abstract <U extends T, F extends E> List<StateTransitioningHandler<? super U, S, ? super F>> getHandlers(int additionalCapacity);
    }

    private static class CompositeBuilder<T, S, E extends Event> extends Builder<T, S, E> {
        private final Builder<? super T, S, ? super E> previousBuilder;
        private final StateTransitioningHandler<? super T, S, ? super E> handler;

        private CompositeBuilder(
                Builder<? super T, S, ? super E> previousBuilder,
                StateTransitioningHandler<? super T, S, ? super E> handler) {
            this.previousBuilder = previousBuilder;
            this.handler = handler;
        }

        @Override
        <U extends T, F extends E> List<StateTransitioningHandler<? super U, S, ? super F>> getHandlers(
                int additionalCapacity) {
            List<StateTransitioningHandler<? super U, S, ? super F>> handlers = previousBuilder.getHandlers(additionalCapacity + 1);
            handlers.add(handler);
            return handlers;
        }
    }

    public static class On<T, S, E extends Event, F extends E> {
        private final Builder<? super T, S, ? super E> previousBuilder;
        private final EventPattern<? super E, ? extends F> eventMatcher;

        private On(
                Builder<? super T, S, ? super E> previousBuilder,
                EventPattern<? super E, ? extends F> eventMatcher) {
            this.previousBuilder = previousBuilder;
            this.eventMatcher = eventMatcher;
        }

        public On<T, S, E, F> where(Predicate<? super E> condition) {
            return new On<>(previousBuilder, eventMatcher.and(condition));
        }

        public <U extends T> When<U, S, E, F> when(Predicate<? super U> condition) {
            return when((u, s) -> condition.test(u));
        }

        public <U extends T> When<U, S, E, F> when(BiPredicate<? super U, ? super S> condition) {
            return new When<>(previousBuilder, eventMatcher, condition);
        }

        public <U extends T> Builder<U, S, E> act(BiConsumer<? super U, ? super E> action) {
            return act((u, s, e) -> { action.accept(u, e); return s; });
        }

        public <U extends T> Builder<U, S, E> act(StateTransition<? super U, S, ? super F> action) {
            return previousBuilder.addHandler((u, s, e) -> {
                Optional<? extends F> optF = eventMatcher.match(e);
                if(optF.isPresent()) {
                    F f = optF.get();
                    S newState = action.transition(u, s, f);
                    f.consume();
                    return newState;
                } else {
                    return s;
                }
            });
        }
    }

    public static class When<T, S, E extends Event, F extends E> {
        private final Builder<? super T, S, ? super E> previousBuilder;
        private final EventPattern<? super E, ? extends F> eventMatcher;
        private final BiPredicate<? super T, ? super S> condition;

        private When(
                Builder<? super T, S, ? super E> previousBuilder,
                EventPattern<? super E, ? extends F> eventMatcher,
                BiPredicate<? super T, ? super S> condition) {
            this.previousBuilder = previousBuilder;
            this.eventMatcher = eventMatcher;
            this.condition = condition;
        }

        public <U extends T> Builder<U, S, E> act(BiConsumer<? super U, ? super F> action) {
            return act((u, s, e) -> { action.accept(u, e); return s; });
        }

        public <U extends T> Builder<U, S, E> act(StateTransition<? super U, S, ? super F> action) {
            return previousBuilder.addHandler((u, s, e) -> {
                Optional<? extends F> optF = eventMatcher.match(e);
                if(optF.isPresent() && condition.test(u, s)) {
                    F f = optF.get();
                    S newState = action.transition(u, s, f);
                    f.consume();
                    return newState;
                } else {
                    return s;
                }
            });
        }
    }

    public static <T, S, E extends Event, F extends E> On<T, S, E, F> on(
            EventPattern<? super E, ? extends F> eventMatcher) {
        return Builder.<T, S, E>empty().on(eventMatcher);
    }

    public static <T, S, E extends Event, F extends E> On<T, S, E, F> on(
            EventType<? extends F> eventType) {
        return Builder.<T, S, E>empty().on(eventType);
    }

    public static <T, S, E extends Event> Builder<T, S, E>
    startWith(StateTransitioningHandler<? super T, S, ? super E> handler) {
        return Builder.<T, S, E>empty().addHandler(handler);
    }


    private final Supplier<? extends S> initialStateSupplier;
    private final StateTransitioningHandler<? super T, S, ? super E> handler;

    StatefulEventHandlerTemplate(
            StateTransitioningHandler<? super T, S, ? super E> handler,
            Supplier<? extends S> initialStateSupplier) {
        this.initialStateSupplier = initialStateSupplier;
        this.handler = handler;
    }

    @Override
    public BiConsumer<? super T, ? super E> getHandler() {
        return new BiConsumer<T, E>() {
            private S state = initialStateSupplier.get();

            @Override
            public void accept(T t, E e) {
                state = handler.handle(t, state, e);
            }
        };
    }

    public <U extends T> StatefulEventHandlerTemplate<U, S, ? super E> onlyWhen(
            BiPredicate<? super U, ? super S> condition) {
        return handler.<U>onlyWhen(condition).initialStateSupplier(initialStateSupplier);
    }

    public <U extends T, F extends E> StatefulEventHandlerTemplate<U, S, F> addHandler(
            StateTransitioningHandler<? super U, S, ? super F> nextHandler) {
        return handler.<U, F>orElse(nextHandler).initialStateSupplier(initialStateSupplier);
    }
}