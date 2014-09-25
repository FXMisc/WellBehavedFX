package org.fxmisc.wellbehaved.event;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javafx.beans.property.ObjectProperty;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;

/**
 * Methods of this class could be added directly to the {@link EventHandler}
 * interface. The interface could further be extended with default methods
 * <pre>
 * {@code
 * EventHandler<? super T> orElse(EventHandler<? super T> other);
 * EventHandler<? super T> without(EventHandler<?> other);
 * }
 * </pre>
 * The latter may replace the {@link #exclude(EventHandler, EventHandler)}
 * static method.
 * @param <T>
 */
public final class EventHandlerHelper<T extends Event> {

    public static abstract class Builder<T extends Event> {

        private static <T extends Event> Builder<T> empty() {
            return new Builder<T>() {
                @Override
                <U extends T> List<EventHandler<? super U>> getHandlers(int additionalCapacity) {
                    return new ArrayList<>(additionalCapacity);
                }
            };
        }

        // private constructor to prevent subclassing by the user
        private Builder() {}

        public <U extends T> On<T, U> on(EventPattern<? super T, ? extends U> eventMatcher) {
            return new On<>(this, eventMatcher);
        }

        public <U extends T> On<T, U> on(EventType<? extends U> eventType) {
            return on(EventPattern.eventTypePattern(eventType));
        }

        public <U extends T> Builder<U> addHandler(EventHandler<? super U> handler) {
            return new CompositeBuilder<>(this, handler);
        }

        public final EventHandler<T> create() {
            List<EventHandler<? super T>> handlers = getHandlers();
            return new CompositeEventHandler<>(handlers);
        }

        List<EventHandler<? super T>> getHandlers() {
            return getHandlers(0);
        }

        abstract <U extends T> List<EventHandler<? super U>> getHandlers(int additionalCapacity);
    }

    private static class CompositeBuilder<T extends Event> extends Builder<T> {
        private final Builder<? super T> previousBuilder;
        private final EventHandler<? super T> handler;

        private CompositeBuilder(
                Builder<? super T> previousBuilder,
                EventHandler<? super T> handler) {
            this.previousBuilder = previousBuilder;
            this.handler = handler;
        }

        @Override
        <U extends T> List<EventHandler<? super U>> getHandlers(int additionalCapacity) {
            List<EventHandler<? super U>> handlers = previousBuilder.getHandlers(additionalCapacity + 1);
            handlers.add(handler);
            return handlers;
        }
    }

    public static class On<T extends Event, U extends T> {
        private final Builder<? super T> previousBuilder;
        private final EventPattern<? super T, ? extends U> eventMatcher;

        private On(
                Builder<? super T> previous,
                EventPattern<? super T, ? extends U> eventMatcher) {
            this.previousBuilder = previous;
            this.eventMatcher = eventMatcher;
        }

        public On<T, U> where(Predicate<? super U> condition) {
            return new On<>(previousBuilder, eventMatcher.and(condition));
        }

        public Builder<T> act(Consumer<? super U> action) {
            return previousBuilder.addHandler(t -> {
                eventMatcher.match(t).ifPresent(u -> {
                    action.accept(u);
                    t.consume();
                });
            });
        }
    }

    public static <T extends Event, U extends T> On<T, U> on(
            EventPattern<? super T, ? extends U> eventMatcher) {
        return Builder.<T>empty().on(eventMatcher);
    }

    public static <T extends Event> On<Event, T> on(
            EventType<? extends T> eventType) {
        return Builder.empty().on(eventType);
    }

    public static <T extends Event> Builder<T>
    startWith(EventHandler<? super T> handler) {
        return Builder.empty().addHandler(handler);
    }

    static <T extends Event> EventHandler<T> empty() {
        return EmptyEventHandler.instance();
    }

    @SafeVarargs
    public static <T extends Event> EventHandler<? super T> chain(EventHandler<? super T>... handlers) {
        ArrayList<EventHandler<? super T>> nonEmptyHandlers = new ArrayList<>(handlers.length);
        for(EventHandler<? super T> handler: handlers) {
            if(handler != empty()) {
                nonEmptyHandlers.add(handler);
            }
        }
        if(nonEmptyHandlers.isEmpty()) {
            return empty();
        } else if(nonEmptyHandlers.size() == 1) {
            return nonEmptyHandlers.get(0);
        } else {
            nonEmptyHandlers.trimToSize();
            return new CompositeEventHandler<>(nonEmptyHandlers);
        }
    }

    public static <T extends Event> EventHandler<? super T> exclude(EventHandler<T> handler, EventHandler<?> subHandler) {
        if(handler instanceof CompositeEventHandler) {
            return ((CompositeEventHandler<T>) handler).without(subHandler);
        } else if(handler.equals(subHandler)) {
            return empty();
        } else {
            return handler;
        }
    }

    public static <T extends Event> void install(
            ObjectProperty<EventHandler<? super T>> handlerProperty,
            EventHandler<? super T> handler) {
        EventHandler<? super T> oldHandler = handlerProperty.get();
        if(oldHandler != null) {
            handlerProperty.set(EventHandlerHelper.chain(handler, oldHandler));
        } else {
            handlerProperty.set(handler);
        }
    }

    public static <T extends Event> void remove(
            ObjectProperty<EventHandler<? super T>> handlerProperty,
            EventHandler<? super T> handler) {
        EventHandler<? super T> oldHandler = handlerProperty.get();
        if(oldHandler != null) {
            handlerProperty.set(EventHandlerHelper.exclude(oldHandler, handler));
        }
    }

    // prevent instantiation
    private EventHandlerHelper() {}
}

final class EmptyEventHandler<T extends Event> implements EventHandler<T> {
    private static EmptyEventHandler<?> INSTANCE = new EmptyEventHandler<>();

    @SuppressWarnings("unchecked")
    static <T extends Event> EmptyEventHandler<T> instance() {
        return (EmptyEventHandler<T>) INSTANCE;
    }

    private EmptyEventHandler() {}

    @Override
    public void handle(T event) {
        // do nothing
    }

}


class CompositeEventHandler<T extends Event> implements EventHandler<T> {
    private final List<EventHandler<? super T>> handlers;

    CompositeEventHandler(List<EventHandler<? super T>> handlers) {
        this.handlers = handlers;
    }

    @Override
    public void handle(T event) {
        for(EventHandler<? super T> handler: handlers) {
            handler.handle(event);
            if(event.isConsumed()) {
                break;
            }
        }
    }

    public EventHandler<? super T> without(EventHandler<?> other) {
        if(this.equals(other)) {
            return EmptyEventHandler.instance();
        } else {
            boolean changed = false;
            List<EventHandler<? super T>> newHandlers = new ArrayList<>(handlers.size());
            for(EventHandler<? super T> handler: handlers) {
                EventHandler<? super T> h = EventHandlerHelper.exclude(handler, other);
                if(h != handler) {
                    changed = true;
                }
                if(h != EmptyEventHandler.instance()) {
                    newHandlers.add(h);
                }
            }

            if(!changed) {
                return this;
            } else if(newHandlers.isEmpty()) {
                return EmptyEventHandler.instance();
            } else if(newHandlers.size() == 1) {
                return newHandlers.get(0);
            } else {
                return new CompositeEventHandler<>(newHandlers);
            }
        }
    }
}