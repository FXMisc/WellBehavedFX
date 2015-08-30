package org.fxmisc.wellbehaved.event.experimental.template;

import javafx.event.Event;

import org.fxmisc.wellbehaved.event.experimental.InputHandler.Result;

@FunctionalInterface
public interface InputHandlerTemplate<S, E extends Event> {

    Result process(S state, E event);

}