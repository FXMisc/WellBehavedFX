package org.fxmisc.wellbehaved.event;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javafx.collections.MapChangeListener;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Node;

import org.fxmisc.wellbehaved.event.InputMap.HandlerConsumer;

public class Nodes {

    private static final String P_INPUTMAP = "org.fxmisc.wellbehaved.event.inputmap";
    private static final String P_HANDLERS = "org.fxmisc.wellbehaved.event.handlers";

    public static void addInputMap(Node node, InputMap<?> im) {
        init(node);
        setInputMap(node, InputMap.sequence(im, getInputMap(node)));
    }

    public static void addFallbackInputMap(Node node, InputMap<?> im) {
        init(node);
        setInputMap(node, InputMap.sequence(getInputMap(node), im));
    }

    public static void removeInputMap(Node node, InputMap<?> im) {
        setInputMap(node, getInputMap(node).without(im));
    }

    static InputMap<?> getInputMap(Node node) {
        init(node);
        return (InputMap<?>) node.getProperties().get(P_INPUTMAP);
    }

    private static void setInputMap(Node node, InputMap<?> im) {
        node.getProperties().put(P_INPUTMAP, im);
    }

    private static void init(Node node) {
        if(node.getProperties().get(P_INPUTMAP) == null) {

            node.getProperties().put(P_INPUTMAP, InputMap.empty());
            node.getProperties().put(P_HANDLERS, new ArrayList<Map.Entry<?, ?>>());

            MapChangeListener<Object, Object> listener = ch -> {
                if(!P_INPUTMAP.equals(ch.getKey())) {
                    return;
                }

                getHandlers(node).forEach(entry -> {
                    node.removeEventHandler((EventType<Event>) entry.getKey(), (EventHandler<Event>) entry.getValue());
                });

                getHandlers(node).clear();

                InputMap<?> inputMap = (InputMap<?>) ch.getValueAdded();
                inputMap.forEachEventType(new HandlerConsumer<Event>() {

                    @Override
                    public <E extends Event> void accept(
                            EventType<? extends E> t, InputHandler<? super E> h) {
                        node.addEventHandler(t, h);
                        getHandlers(node).add(new SimpleEntry<>(t, h));
                    }});
            };
            node.getProperties().addListener(listener);
        }
    }

    private static List<Map.Entry<EventType<?>, EventHandler<?>>> getHandlers(Node node) {
        return (List<Entry<EventType<?>, EventHandler<?>>>) node.getProperties().get(P_HANDLERS);
    }
}
