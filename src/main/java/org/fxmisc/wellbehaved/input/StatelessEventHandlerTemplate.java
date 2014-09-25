package org.fxmisc.wellbehaved.input;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import javafx.event.Event;
import javafx.event.EventType;

/**
 *
 * @param <T> type of the control the input handler applies to.
 */
public abstract class StatelessEventHandlerTemplate<T, E extends Event>
implements EventHandlerTemplate<T, E>, BiConsumer<T, E> {

    public static abstract class Builder<T, E extends Event> {
        private static <T, E extends Event> Builder<T, E> empty() {
            return new Builder<T, E>() {
                @Override
                <U extends T, F extends E> List<BiConsumer<? super U, ? super F>> getHandlers(int additionalCapacity) {
                    return new ArrayList<>(additionalCapacity);
                }
            };
        }

        // private constructor to prevent subclassing by the user
        private Builder() {}

        public <F extends E> On<T, E, F> on(EventPattern<? super E, ? extends F> eventMatcher) {
            return new On<>(this, eventMatcher);
        }

        public <F extends E> On<T, E, F> on(EventType<? extends F> eventType) {
            return on(EventPattern.eventTypePattern(eventType));
        }

        public <U extends T, F extends E> Builder<U, F> addHandler(BiConsumer<? super U, ? super F> handler) {
            return new CompositeBuilder<>(this, handler);
        }

        public StatelessEventHandlerTemplate<T, E> create() {
            List<BiConsumer<? super T, ? super E>> handlers = getHandlers();

            return new StatelessEventHandlerTemplate<T, E>() {
                @Override
                public void accept(T target, E event) {
                    for(BiConsumer<? super T, ? super E> handler: handlers) {
                        handler.accept(target, event);
                        if(event.isConsumed()) {
                            break;
                        }
                    }
                }
            };
        }

        List<BiConsumer<? super T, ? super E>> getHandlers() {
            return getHandlers(0);
        }

        abstract <U extends T, F extends E> List<BiConsumer<? super U, ? super F>> getHandlers(int additionalCapacity);
    }

    private static class CompositeBuilder<T, E extends Event> extends Builder<T, E> {
        private final Builder<? super T, ? super E> previousBuilder;
        private final BiConsumer<? super T, ? super E> handler;

        private CompositeBuilder(
                Builder<? super T, ? super E> previousBuilder,
                BiConsumer<? super T, ? super E> handler) {
            this.previousBuilder = previousBuilder;
            this.handler = handler;
        }

        @Override
        <U extends T, F extends E> List<BiConsumer<? super U, ? super F>> getHandlers(int additionalCapacity) {
            List<BiConsumer<? super U, ? super F>> handlers = previousBuilder.getHandlers(additionalCapacity + 1);
            handlers.add(handler);
            return handlers;
        }
    }

    public static class On<T, E extends Event, F extends E> {
        private final Builder<? super T, ? super E> previousBuilder;
        private final EventPattern<? super E, ? extends F> eventMatcher;

        private On(
                Builder<? super T, ? super E> previous,
                EventPattern<? super E, ? extends F> eventMatcher) {
            this.previousBuilder = previous;
            this.eventMatcher = eventMatcher;
        }

        public On<T, E, F> where(Predicate<? super F> condition) {
            return new On<>(previousBuilder, eventMatcher.and(condition));
        }

        public <U extends T> When<U, E, F> when(Predicate<? super U> condition) {
            return new When<>(previousBuilder, eventMatcher, condition);
        }

        public <U extends T> Builder<U, E> act(BiConsumer<? super U, ? super F> action) {
            return previousBuilder.addHandler((t, e) -> {
                eventMatcher.match(e).ifPresent(f -> {
                    action.accept(t, f);
                    f.consume();
                });
            });
        }
    }

    public static class When<T, E extends Event, F extends E> {
        private final Builder<? super T, ? super E> previousBuilder;
        private final EventPattern<? super E, ? extends F> eventMatcher;
        private final Predicate<? super T> condition;

        private When(
                Builder<? super T, ? super E> previousBuilder,
                EventPattern<? super E, ? extends F> eventMatcher,
                Predicate<? super T> condition) {
            this.previousBuilder = previousBuilder;
            this.eventMatcher = eventMatcher;
            this.condition = condition;
        }

        public <U extends T> Builder<U, E> act(BiConsumer<? super U, ? super F> action) {
            return previousBuilder.addHandler((u, e) -> {
                eventMatcher.match(e).ifPresent(f -> {
                    if(condition.test(u)) {
                        action.accept(u, f);
                        f.consume();
                    }
                });
            });
        }
    }

    public static <T, E extends Event, F extends E> On<T, E, F> on(
            EventPattern<? super E, ? extends F> eventMatcher) {
        return Builder.<T, E>empty().on(eventMatcher);
    }

    public static <T, E extends Event> On<T, Event, E> on(
            EventType<? extends E> eventType) {
        return Builder.<T, Event>empty().on(eventType);
    }

    public static <T, E extends Event> Builder<T, E>
    startWith(BiConsumer<? super T, ? super E> handler) {
        return Builder.empty().addHandler(handler);
    }


    // private constructor to prevent subclassing by the user
    private StatelessEventHandlerTemplate() {}

    @Override
    public final BiConsumer<? super T, ? super E> getHandler() {
        return this;
    }

    public final <U extends T> StatelessEventHandlerTemplate<U, E> onlyWhen(Predicate<? super U> condition) {
        return new StatelessEventHandlerTemplate<U, E>() {

            @Override
            public void accept(U target, E event) {
                if(condition.test(target)) {
                    StatelessEventHandlerTemplate.this.accept(target, event);
                }
            }
        };
    }
}