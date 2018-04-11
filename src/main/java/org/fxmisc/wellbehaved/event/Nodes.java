package org.fxmisc.wellbehaved.event;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;

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
 *     <li>
 *         See also {@link #pushInputMap(Node, InputMap)} and {@link #popInputMap(Node)} for temporary behavior
 *         modification.
 *     </li>
 * </ul>
 */
public class Nodes {

    private static final String P_INPUTMAP = "org.fxmisc.wellbehaved.event.inputmap";
    private static final String P_HANDLERS = "org.fxmisc.wellbehaved.event.handlers";
    private static final String P_STACK    = "org.fxmisc.wellbehaved.event.stack";

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

    /**
     * Gets the {@link InputMap} for the given node or {@link InputMap#empty()} if there is none.
     */
    public static InputMap<?> getInputMap(Node node) {
        init(node);
        return getInputMapUnsafe(node);
    }

    /**
     * Removes the currently installed {@link InputMap} (InputMap1) on the given node and installs the {@code im}
     * (InputMap2) in its place. When finished, InputMap2 can be uninstalled and InputMap1 reinstalled via
     * {@link #popInputMap(Node)}. Multiple InputMaps can be installed so that InputMap(n) will be installed over
     * InputMap(n-1)
     */
    public static void pushInputMap(Node node, InputMap<?> im) {
        // store currently installed im; getInputMap calls init
        InputMap<?> previousInputMap = getInputMap(node);
        getStack(node).push(previousInputMap);

        // completely override the previous one with the given one
        setInputMapUnsafe(node, im);
    }

    /**
     * If the internal stack has an {@link InputMap}, removes the current {@link InputMap} that was installed
     * on the give node via {@link #pushInputMap(Node, InputMap)}, reinstalls the previous {@code InputMap},
     * and then returns true. If the stack is empty, returns false.
     */
    public static boolean popInputMap(Node node) {
        Stack<InputMap<?>> stackedInputMaps = getStack(node);
        if (!stackedInputMaps.isEmpty()) {
            // If stack is not empty, node has already been initialized, so can use unsafe methods.
            // Now, completely override current input map with previous one on stack
            setInputMapUnsafe(node, stackedInputMaps.pop());
            return true;
        } else {
            return false;
        }
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

    private static Stack<InputMap<?>> getStack(Node node) {
        ObservableMap<Object, Object> nodeProperties = getProperties(node);
        if (nodeProperties.get(P_STACK) == null) {
            Stack<InputMap<?>> stackedInputMaps = new Stack<>();
            nodeProperties.put(P_STACK, stackedInputMaps);
            return stackedInputMaps;
        }

        return (Stack<InputMap<?>>) nodeProperties.get(P_STACK);
    }

    private static ObservableMap<Object, Object> getProperties(Node node) {
        return node.getProperties();
    }
}
