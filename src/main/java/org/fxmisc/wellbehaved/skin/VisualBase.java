package org.fxmisc.wellbehaved.skin;

import javafx.beans.property.ObjectProperty;
import javafx.event.EventHandler;
import javafx.scene.control.Control;
import javafx.scene.input.InputEvent;

import org.fxmisc.wellbehaved.input.InputReceiverHelper;

abstract class VisualBase<C extends Control> implements Visual<C> {
    private final InputReceiverHelper<C> helper;

    VisualBase(C control) {
        this.helper = new InputReceiverHelper<>(control);
    }

    @Override
    public final ObjectProperty<EventHandler<? super InputEvent>> onInputProperty() {
        return helper.onInputProperty();
    }

    @Override
    public final C getControl() {
        return helper.getTarget();
    }

    @Override
    public void dispose() {
        helper.dispose();
    }
}
