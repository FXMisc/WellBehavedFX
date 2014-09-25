package org.fxmisc.wellbehaved.skin;

import javafx.scene.control.Control;

abstract class VisualBase<C extends Control> implements Visual<C> {
    private final C control;

    VisualBase(C control) {
        this.control = control;
    }

    @Override
    public final C getControl() {
        return control;
    }
}
