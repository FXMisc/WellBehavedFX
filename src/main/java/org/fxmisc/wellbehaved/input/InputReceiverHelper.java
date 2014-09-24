package org.fxmisc.wellbehaved.input;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.input.InputEvent;

public final class InputReceiverHelper<T extends Node> {

    private final ObjectProperty<EventHandler<? super InputEvent>> onInput
            = new SimpleObjectProperty<>(EventHandlerHelper.empty());

    private final T target;

    public InputReceiverHelper(T target) {
        this.target = target;
        onInput.addListener((obs, oldHandler, newHandler) -> {
            if(oldHandler != EventHandlerHelper.empty()) {
                target.removeEventHandler(InputEvent.ANY, oldHandler);
            }

            if(newHandler != EventHandlerHelper.empty()) {
                target.addEventHandler(InputEvent.ANY, newHandler);
            }
        });
    }

    public ObjectProperty<EventHandler<? super InputEvent>> onInputProperty() {
        return onInput;
    }

    public T getTarget() {
        return target;
    }

    public void dispose() {
        onInput.set(EventHandlerHelper.empty());
    }
}
