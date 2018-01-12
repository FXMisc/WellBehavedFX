package org.fxmisc.wellbehaved.event;

import java.util.function.Predicate;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;

/**
 * A helper class for pattern-matching a KeyEvent's {@link KeyCombination.Modifier modifiers} along with
 * the key event itself.
 */
class KeyCodePatternCombination extends KeyCombination {

    private final Predicate<KeyCode> keyTest;

    KeyCodePatternCombination(Predicate<KeyCode> keyTest, KeyCombination.Modifier... modifiers) {
        super(modifiers);
        this.keyTest = keyTest;
    }

    @Override
    public boolean match(KeyEvent event) {
        return super.match(event) // matches the modifiers
                && keyTest.test(event.getCode());
    }
}
