package org.fxmisc.wellbehaved.event.template;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import javafx.event.Event;
import javafx.event.EventType;

import org.fxmisc.wellbehaved.event.InputHandler.Result;
import org.fxmisc.wellbehaved.event.internal.PrefixTree;
import org.fxmisc.wellbehaved.event.internal.PrefixTree.Ops;
import org.fxmisc.wellbehaved.event.template.InputMapTemplate.HandlerTemplateConsumer;

class InputHandlerTemplateMap<S, E extends Event> {

    private static <S, E extends Event> InputHandlerTemplate<S, E> sequence(
            InputHandlerTemplate<S, ? super E> h1,
            InputHandlerTemplate<S, ? super E> h2) {
        return (s, evt) -> {
            switch(h1.process(s, evt)) {
                case PROCEED: return h2.process(s, evt);
                case CONSUME: return Result.CONSUME;
                case IGNORE:  return Result.IGNORE;
                default: throw new AssertionError("unreachable code");
            }
        };
    }

    private static <S, E extends Event> Ops<EventType<? extends E>, InputHandlerTemplate<S, ? super E>> ops() {
        return new Ops<EventType<? extends E>, InputHandlerTemplate<S, ? super E>>() {
            @Override
            public boolean isPrefixOf(EventType<? extends E> t1, EventType<? extends E> t2) {
                EventType<?> t = t2;
                while(t != null) {
                    if(t.equals(t1)) {
                        return true;
                    } else {
                        t = t.getSuperType();
                    }
                }
                return false;
            }

            @Override
            public EventType<? extends E> commonPrefix(
                    EventType<? extends E> t1, EventType<? extends E> t2) {
                Iterator<EventType<?>> i1 = toList(t1).iterator();
                Iterator<EventType<?>> i2 = toList(t2).iterator();
                EventType<?> common = null;
                while(i1.hasNext() && i2.hasNext()) {
                    EventType<?> c1 = i1.next();
                    EventType<?> c2 = i2.next();
                    if(Objects.equals(c1, c2)) {
                        common = c1;
                    }
                }
                return (EventType<? extends E>) common;
            }

            @Override
            public InputHandlerTemplate<S, ? super E> promote(InputHandlerTemplate<S, ? super E> h,
                    EventType<? extends E> subTpe, EventType<? extends E> supTpe) {

                if(Objects.equals(subTpe, supTpe)) {
                    return h;
                }
                return (s, evt) -> {
                    if(isPrefixOf(subTpe, (EventType<? extends E>) evt.getEventType())) {
                        return h.process(s, evt);
                    } else {
                        return Result.PROCEED;
                    }
                };
            }

            @Override
            public InputHandlerTemplate<S, E> squash(InputHandlerTemplate<S, ? super E> v1, InputHandlerTemplate<S, ? super E> v2) {
                return sequence(v1, v2);
            }

        };
    }


    private static final List<EventType<?>> toList(EventType<?> t) {
        List<EventType<?>> l = new ArrayList<>();
        while(t != null) {
            l.add(t);
            t = t.getSuperType();
        }
        Collections.reverse(l);
        return l;
    }

    private PrefixTree<EventType<? extends E>, InputHandlerTemplate<S, ? super E>> handlerTree;

    public InputHandlerTemplateMap() {
        this(PrefixTree.empty(ops()));
    }

    private InputHandlerTemplateMap(PrefixTree<EventType<? extends E>, InputHandlerTemplate<S, ? super E>> handlerTree) {
        this.handlerTree = handlerTree;
    }

    public <F extends E> void insertAfter(EventType<? extends F> t, InputHandlerTemplate<S, ? super F> h) {
        InputHandlerTemplate<S, ? super E> handler = (InputHandlerTemplate<S, ? super E>) h;
        handlerTree = handlerTree.insert(t, handler, (h1, h2) -> sequence(h1, h2));
    }

    public <T> InputHandlerTemplateMap<T, E> map(
            Function<? super InputHandlerTemplate<S, ? super E>, ? extends InputHandlerTemplate<T, E>> f) {
        return new InputHandlerTemplateMap<>(handlerTree.map(f, ops()));
    }

    void forEach(HandlerTemplateConsumer<S, ? super E> f) {
        handlerTree.entries().forEach(th -> f.accept(th.getKey(), th.getValue()));
    }
}