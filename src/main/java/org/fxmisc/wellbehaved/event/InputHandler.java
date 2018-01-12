package org.fxmisc.wellbehaved.event;

import javafx.event.Event;
import javafx.event.EventHandler;

/**
 * Runs a block of code when its corresponding {@link EventPattern} matches a given event type (e.g. the block of code
 * to run in a more powerful switch statement)
 * @param <T> the type of the event that will be passed into {@link #process(Event)}
 */
@FunctionalInterface
public interface InputHandler<T extends Event> extends EventHandler<T> {

    /**
     * Signifies what to do after handling some input:
     * <ul>
     *     <li>
     *         continue trying to match the event type with the next given {@link EventPattern} ({@link #PROCEED})
     *     </li>
     *     <li>
     *         stop trying to match the event type and consume the event ({@link #CONSUME})
     *     </li>
     *     <li>
     *         stop trying to match the event type and do not consume it ({@link #IGNORE})
     *     </li>
     * </ul>
     */
    public enum Result {
        /**
         * Try to continue to match the event type with the next given {@link EventPattern}. This can be
         * used to run some code before an {@link InputHandler} that consumes the event.
         */
        PROCEED,
        /** Stop trying to match the event type with the next given {@link EventPattern} and consume the event */
        CONSUME,
        /**
         * Stop trying to match the event type with the next given {@link EventPattern} and do not consume it
         */
        IGNORE
    }

    /**
     * When the corresponding {@link EventPattern} matches an event type, this method is called. The implementation
     * does not need to consume the event.
     */
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
