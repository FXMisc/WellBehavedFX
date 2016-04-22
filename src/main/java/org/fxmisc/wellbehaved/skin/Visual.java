package org.fxmisc.wellbehaved.skin;

import java.util.Collections;
import java.util.List;

import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.scene.control.Control;

/**
 * Represents the view aspect of a JavaFX control. It defines how the control
 * is rendered visually on the screen. The implementations should either
 * implement {@link SimpleVisualBase}, or extend from {@link ComplexVisualBase}.
 *
 * @param <C> type of the control this Visual is used for.
 *
 * @deprecated Since 0.3. We have come to believe that skins, as designed in
 * JavaFX, are not very useful and not worth the trouble. Package
 * {@link org.fxmisc.wellbehaved.skin} will be removed in a future version.
 */
@Deprecated
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
     * Returns information about the extra styleable properties availabe on the
     * skin in addition to those available on the control itself. The default
     * implementation returns an empty list.
     */
    default List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return Collections.emptyList();
    }
}
