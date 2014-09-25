package org.fxmisc.wellbehaved.skin;

import javafx.beans.property.ObjectProperty;
import javafx.event.EventHandler;
import javafx.scene.control.Control;
import javafx.scene.input.InputEvent;

import org.fxmisc.wellbehaved.input.EventSourceHelper;

abstract class VisualBase<C extends Control> implements Visual<C> {
    private final EventSourceHelper<C, InputEvent> helper;

    VisualBase(C control) {
        this.helper = EventSourceHelper.forNode(control, InputEvent.ANY);
    }

    @Override
    public final ObjectProperty<EventHandler<? super InputEvent>> onInputProperty() {
        return helper.onEventProperty();
    }

    @Override
    public final C getControl() {
        return helper.getEventSource();
    }

    @Override
    public void dispose() {
        helper.dispose();
    }
}
