package org.fxmisc.wellbehaved.event.template;

import javafx.event.Event;

import org.fxmisc.wellbehaved.event.InputHandler.Result;

/**
 * Template version of {@link org.fxmisc.wellbehaved.event.InputHandler}.
 *
 * @param <S> the type of the object that will be passed into the {@link InputHandlerTemplate}'s block of code.
 * @param <E> the event type for which this InputMap's {@link org.fxmisc.wellbehaved.event.EventPattern} matches
 */
@FunctionalInterface
public interface InputHandlerTemplate<S, E extends Event> {

    Result process(S state, E event);

}