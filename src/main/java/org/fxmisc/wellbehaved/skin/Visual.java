package org.fxmisc.wellbehaved.skin;

import java.util.Collections;
import java.util.List;

import javafx.beans.property.ObjectProperty;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.event.EventHandler;
import javafx.scene.control.Control;
import javafx.scene.input.InputEvent;

/**
 * Represents the view aspect of a JavaFX control. It defines how the control
 * is rendered visually on the screen. The implementations should either
 * implement {@link SimpleVisualBase}, or extend from {@link ComplexVisualBase}.
 *
 * @param <C> type of the control this Visual is used for.
 */
public interface Visual<C extends Control> {
    /**
     * Returns the control this Visual is used for.
     */
    C getControl();

    /**
     * Called to release resources associated with this Visual when it is no
     * longer being used, in particular to stop observing the control, i.e.
     * remove any listeners, etc.
     */
    void dispose();

    /**
     * Defines a function to be called when this visual's control receives
     * input.
     */
    ObjectProperty<EventHandler<? super InputEvent>> onInputProperty();
    default EventHandler<? super InputEvent> getOnInput() {
        return onInputProperty().get();
    }
    default void setOnInput(EventHandler<? super InputEvent> handler) {
        onInputProperty().set(handler);
    }

    /**
     * Returns information about the extra styleable properties availabe on the
     * skin in addition to those available on the control itself. The default
     * implementation returns an empty list.
     */
    default List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return Collections.emptyList();
    }
}
