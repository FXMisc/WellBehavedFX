package org.fxmisc.wellbehaved.event;

import java.util.function.Predicate;

import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;

/**
 * The purpose of this class is to reuse the modifier-matching implementation
 * from KeyCombination.
 */
class KeyTypedCombination extends KeyCombination {
    private final Predicate<String> charTest;

    KeyTypedCombination(Predicate<String> charTest, Modifier... modifiers) {
        super(modifiers);
        this.charTest = charTest;
    }

    KeyTypedCombination(String character, Modifier... modifiers) {
        this(character::equals);
    }

    @Override
    public boolean match(KeyEvent event) {
        return super.match(event) // matches the modifiers
                && event.getEventType() == KeyEvent.KEY_TYPED
                && charTest.test(event.getCharacter());
    }
}
