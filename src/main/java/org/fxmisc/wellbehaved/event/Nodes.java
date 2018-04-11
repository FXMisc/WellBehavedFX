package org.fxmisc.wellbehaved.event;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Node;

import org.fxmisc.wellbehaved.event.InputMap.HandlerConsumer;

/**
 * Helper class for "installing/uninstalling" an {@link InputMap} into a {@link Node}.
 *
 * <h3>Method Summary</h3>
 * <ul>
 *     <li>
 *         To add an {@link InputMap} as a default behavior that can be overridden later,
 *         use {@link #addFallbackInputMap(Node, InputMap)}.
 *     </li>
 *     <li>
 *         To add an {@code InputMap} that might override default behaviors, use {@link #addInputMap(Node, InputMap)}.
 *     </li>
 *     <li>
 *         To remove an {@code InputMap}, use {@link #removeInputMap(Node, InputMap)}.
 *     </li>
 * </ul>
 */
public class Nodes {

    private static final String P_INPUTMAP = "org.fxmisc.wellbehaved.event.inputmap";
    private static final String P_HANDLERS = "org.fxmisc.wellbehaved.event.handlers";

    /**
     * Adds the given input map to the start of the node's list of input maps, so that an event will be pattern-matched
     * against the given input map before being pattern-matched against any other input maps currently
     * "installed" in the node.
     */
    public static void addInputMap(Node node, InputMap<?> im) {
        // getInputMap calls init, so can use unsafe setter
        setInputMapUnsafe(node, InputMap.sequence(im, getInputMap(node)));
    }

    /**
     * Adds the given input map to the end of the node's list of input maps, so that an event will be pattern-matched
     * against all other input maps currently "installed" in the node before being pattern-matched against the given
     * input map.
     */
    public static void addFallbackInputMap(Node node, InputMap<?> im) {
        // getInputMap calls init, so can use unsafe setter
        setInputMapUnsafe(node, InputMap.sequence(getInputMap(node), im));
    }

    /**
     * Removes (or uninstalls) the given input map from the node.
     */
    public static void removeInputMap(Node node, InputMap<?> im) {
        // getInputMap calls init, so can use unsafe setter
        setInputMapUnsafe(node, getInputMap(node).without(im));
    }

    static InputMap<?> getInputMap(Node node) {
        init(node);
        return getInputMapUnsafe(node);
    }

    /**
     *
     * @param node
     */
    private static void init(Node node) {
        ObservableMap<Object, Object> nodeProperties = getProperties(node);
        if(nodeProperties.get(P_INPUTMAP) == null) {

            nodeProperties.put(P_INPUTMAP, InputMap.empty());
            nodeProperties.put(P_HANDLERS, new ArrayList<Map.Entry<?, ?>>());

            MapChangeListener<Object, Object> listener = ch -> {
                if(!P_INPUTMAP.equals(ch.getKey())) {
                    return;
                }

                getHandlers(node).forEach(entry -> {
                    node.removeEventHandler(entry.getKey(), (EventHandler<Event>) entry.getValue());
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
            nodeProperties.addListener(listener);
        }
    }

    /** Expects a {@link #init(Node)} call with the given node before this one is called */
    private static void setInputMapUnsafe(Node node, InputMap<?> im) {
        getProperties(node).put(P_INPUTMAP, im);
    }

    /** Expects a {@link #init(Node)} call with the given node before this one is called */
    private static InputMap<?> getInputMapUnsafe(Node node) {
        return (InputMap<?>) getProperties(node).get(P_INPUTMAP);
    }

    private static List<Map.Entry<EventType<?>, EventHandler<?>>> getHandlers(Node node) {
        return (List<Entry<EventType<?>, EventHandler<?>>>) getProperties(node).get(P_HANDLERS);
    }

    private static ObservableMap<Object, Object> getProperties(Node node) {
        return node.getProperties();
    }
}
