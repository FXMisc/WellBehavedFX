package org.fxmisc.wellbehaved.input;

import java.util.function.BiConsumer;

import javafx.event.Event;
import javafx.event.EventHandler;

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

    default <U extends T, F extends E> EventHandlerTemplate<U, F> ifConsumed(BiConsumer<? super U, ? super F> postConsumption) {
        return () -> {
            BiConsumer<? super T, ? super E> thisHandler = EventHandlerTemplate.this.getHandler();
            return (u, f) -> {
                thisHandler.accept(u, f);
                if(f.isConsumed()) {
                    postConsumption.accept(u, f);
                }
            };
        };
    }
}
