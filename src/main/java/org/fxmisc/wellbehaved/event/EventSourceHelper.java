package org.fxmisc.wellbehaved.event;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Node;
import javafx.scene.Scene;

public abstract class EventSourceHelper<T, E extends Event> {

    public static <T extends Node, E extends Event>
    EventSourceHelper<T, E> forNode(T node, EventType<? extends E> eventType) {
        return new NodeHelper<>(node, eventType);
    }

    public static <E extends Event>
    EventSourceHelper<Scene, E> forScene(Scene scene, EventType<? extends E> eventType) {
        return new SceneHelper<>(scene, eventType);
    }

    private final ObjectProperty<EventHandler<? super E>> onEvent
            = new SimpleObjectProperty<>(EventHandlerHelper.empty());

    private final T source;

    EventSourceHelper(T eventSource, EventType<? extends E> eventType) {
        this.source = eventSource;
        onEvent.addListener((obs, oldHandler, newHandler) -> {
            if(oldHandler != EventHandlerHelper.empty()) {
                removeEventHandler(source, eventType, oldHandler);
            }

            if(newHandler != EventHandlerHelper.empty()) {
                addEventHandler(source, eventType, newHandler);
            }
        });
    }

    abstract <F extends Event> void addEventHandler(T source, EventType<F> eventType, EventHandler<? super F> handler);
    abstract <F extends Event> void removeEventHandler(T source, EventType<F> eventType, EventHandler<? super F> handler);

    public final ObjectProperty<EventHandler<? super E>> onEventProperty() {
        return onEvent;
    }

    public final T getEventSource() {
        return source;
    }

    public final void dispose() {
        onEvent.set(EventHandlerHelper.empty());
    }
}

class NodeHelper<T extends Node, E extends Event> extends EventSourceHelper<T, E> {

    NodeHelper(T node, EventType<? extends E> eventType) {
        super(node, eventType);
    }

    @Override
    <F extends Event> void addEventHandler(T node, EventType<F> eventType,
            EventHandler<? super F> handler) {
        node.addEventHandler(eventType, handler);
    }

    @Override
    <F extends Event> void removeEventHandler(T node, EventType<F> eventType,
            EventHandler<? super F> handler) {
        node.removeEventHandler(eventType, handler);
    }
}

class SceneHelper<E extends Event> extends EventSourceHelper<Scene, E> {

    SceneHelper(Scene scene, EventType<? extends E> eventType) {
        super(scene, eventType);
    }

    @Override
    <F extends Event> void addEventHandler(Scene scene,
            EventType<F> eventType, EventHandler<? super F> handler) {
        scene.addEventHandler(eventType, handler);
    }

    @Override
    <F extends Event> void removeEventHandler(Scene scene,
            EventType<F> eventType, EventHandler<? super F> handler) {
        scene.removeEventHandler(eventType, handler);
    }
}