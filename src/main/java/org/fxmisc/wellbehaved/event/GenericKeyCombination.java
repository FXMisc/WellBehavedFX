package org.fxmisc.wellbehaved.event;

import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;

import java.util.function.Predicate;

/**
 * A generic helper class for pattern-matching a KeyEvent's {@link KeyCombination.Modifier modifiers} along with
 * the key event itself.
 */
class GenericKeyCombination extends KeyCombination {

    private final Predicate<? super KeyEvent> keyTest;

    GenericKeyCombination(Predicate<? super KeyEvent> keyTest, KeyCombination.Modifier... modifiers) {
        super(modifiers);
        this.keyTest = keyTest;
    }

    @Override
    public boolean match(KeyEvent event) {
        return super.match(event) // matches the modifiers
                && keyTest.test(event);
    }
}
