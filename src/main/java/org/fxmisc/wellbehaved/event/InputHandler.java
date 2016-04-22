package org.fxmisc.wellbehaved.event;

import javafx.event.Event;
import javafx.event.EventHandler;

@FunctionalInterface
public interface InputHandler<T extends Event> extends EventHandler<T> {

    public enum Result { PROCEED, CONSUME, IGNORE }

    Result process(T event);

    @Override
    default void handle(T event) {
        switch(process(event)) {
            case CONSUME: event.consume(); break;
            case PROCEED: /* do nothing */ break;
            case IGNORE:  /* do nothing */ break;
        }
    }
}
