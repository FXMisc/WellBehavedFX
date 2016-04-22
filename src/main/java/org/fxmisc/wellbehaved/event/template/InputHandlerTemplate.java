package org.fxmisc.wellbehaved.event.template;

import javafx.event.Event;

import org.fxmisc.wellbehaved.event.InputHandler.Result;

@FunctionalInterface
public interface InputHandlerTemplate<S, E extends Event> {

    Result process(S state, E event);

}