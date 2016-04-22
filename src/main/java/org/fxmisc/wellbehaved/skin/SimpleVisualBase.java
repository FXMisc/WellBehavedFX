package org.fxmisc.wellbehaved.skin;

import javafx.scene.Node;
import javafx.scene.control.Control;

/**
 * A Visual that is represented by a single node.
 *
 * @deprecated Since 0.3. We have come to believe that skins, as designed in
 * JavaFX, are not very useful and not worth the trouble. Package
 * {@link org.fxmisc.wellbehaved.skin} will be removed in a future version.
 */
@Deprecated
public abstract class SimpleVisualBase<C extends Control> extends VisualBase<C> {

    public SimpleVisualBase(C control) {
        super(control);
    }

    /**
     * Returns the node representing the visual rendering of the control. This
     * node will be attached to the control as its child on skin creation and
     * removed from the control on skin disposal.
     */
    public abstract Node getNode();
}
