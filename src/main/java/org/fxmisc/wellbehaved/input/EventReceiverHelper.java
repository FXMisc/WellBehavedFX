package org.fxmisc.wellbehaved.input;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Node;

public final class EventReceiverHelper<T extends Node, E extends Event> {

    private final ObjectProperty<EventHandler<? super E>> onEvent
            = new SimpleObjectProperty<>(EventHandlerHelper.empty());

    private final T target;

    public EventReceiverHelper(T target, EventType<? extends E> eventType) {
        this.target = target;
        onEvent.addListener((obs, oldHandler, newHandler) -> {
            if(oldHandler != EventHandlerHelper.empty()) {
                target.removeEventHandler(eventType, oldHandler);
            }

            if(newHandler != EventHandlerHelper.empty()) {
                target.addEventHandler(eventType, newHandler);
            }
        });
    }

    public ObjectProperty<EventHandler<? super E>> onEventProperty() {
        return onEvent;
    }

    public T getTarget() {
        return target;
    }

    public void dispose() {
        onEvent.set(EventHandlerHelper.empty());
    }
}
