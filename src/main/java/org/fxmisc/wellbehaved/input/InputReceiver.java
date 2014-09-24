package org.fxmisc.wellbehaved.input;

import javafx.beans.property.ObjectProperty;
import javafx.event.EventHandler;
import javafx.scene.input.InputEvent;

public interface InputReceiver {
    ObjectProperty<EventHandler<? super InputEvent>> onInputProperty();

    default EventHandler<? super InputEvent> getOnInput() {
        return onInputProperty().get();
    }

    default void setOnInput(EventHandler<? super InputEvent> handler) {
        onInputProperty().set(handler);
    }
}
