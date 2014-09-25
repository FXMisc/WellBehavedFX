package org.fxmisc.wellbehaved.input;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;

@FunctionalInterface
public interface EventHandlerTemplate<T, E extends Event> {
    BiConsumer<? super T, ? super E> getHandler();

    default EventHandler<E> bind(T target) {
        BiConsumer<? super T, ? super E> handler = getHandler();
        return event -> handler.accept(target, event);
    }

    default <U extends T, F extends E> EventHandlerTemplate<U, F> orElse(EventHandlerTemplate<U, F> that) {
        return () -> {
            BiConsumer<? super T, ? super E> thisHandler = EventHandlerTemplate.this.getHandler();
            BiConsumer<? super U, ? super F> thatHandler = that.getHandler();
            return (u, e) -> {
                thisHandler.accept(u, e);
                if(!e.isConsumed()) {
                    thatHandler.accept(u, e);
                }
            };
        };
    }

    default <U extends T> EventHandlerTemplate<U, E> onlyWhen(Predicate<? super U> condition) {
        return () -> {
            BiConsumer<? super T, ? super E> handler = getHandler();
            return (u, e) -> {
                if(condition.test(u)) {
                    handler.accept(u, e);
                }
            };
        };
    }

    default <U extends T, F extends E> EventHandlerTemplate<U, F> ifConsumed(BiConsumer<? super U, ? super F> postConsumption) {
        return () -> {
            BiConsumer<? super T, ? super E> handler = getHandler();
            return (u, f) -> {
                handler.accept(u, f);
                if(f.isConsumed()) {
                    postConsumption.accept(u, f);
                }
            };
        };
    }


    /* ********************************************************************** *
     * Builder classes and methods
     * ********************************************************************** */

    abstract class Builder<T, E extends Event> {

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

        public EventHandlerTemplate<T, E> create() {
            // Collect handlers eagerly, so that the handler below does not
            // have to close over this builder (so this builder can be GC-ed).
            List<BiConsumer<? super T, ? super E>> handlers = getHandlers();

            // Since the resulting handler is stateless, we can pre-make a
            // single handler instance that will later be returned from each
            // call to getHandler().
            BiConsumer<? super T, ? super E> handler = (target, event) -> {
                for(BiConsumer<? super T, ? super E> h: handlers) {
                    h.accept(target, event);
                    if(event.isConsumed()) {
                        break;
                    }
                }
            };

            return () -> handler;
        }

        List<BiConsumer<? super T, ? super E>> getHandlers() {
            return getHandlers(0);
        }

        abstract <U extends T, F extends E> List<BiConsumer<? super U, ? super F>> getHandlers(int additionalCapacity);
    }

    class On<T, E extends Event, F extends E> {
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

    class When<T, E extends Event, F extends E> {
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

    static <T, E extends Event, F extends E> On<T, E, F> on(
            EventPattern<? super E, ? extends F> eventMatcher) {
        return Builder.<T, E>empty().on(eventMatcher);
    }

    static <T, E extends Event> On<T, Event, E> on(
            EventType<? extends E> eventType) {
        return Builder.<T, Event>empty().on(eventType);
    }

    static <T, E extends Event> Builder<T, E>
    startWith(BiConsumer<? super T, ? super E> handler) {
        return Builder.empty().addHandler(handler);
    }
}
