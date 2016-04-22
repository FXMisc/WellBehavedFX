package org.fxmisc.wellbehaved.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;

import javafx.event.Event;
import javafx.event.EventType;

import org.fxmisc.wellbehaved.event.InputHandler.Result;
import org.fxmisc.wellbehaved.event.InputMap.HandlerConsumer;
import org.fxmisc.wellbehaved.event.internal.PrefixTree;
import org.fxmisc.wellbehaved.event.internal.PrefixTree.Ops;

class InputHandlerMap<E extends Event> {

    private final BiFunction<InputHandler<? super E>, InputHandler<? super E>, InputHandler<E>> SEQ =
            (h1, h2) -> evt -> {
                switch(h1.process(evt)) {
                    case PROCEED: return h2.process(evt);
                    case CONSUME: return Result.CONSUME;
                    case IGNORE:  return Result.IGNORE;
                    default: throw new AssertionError("unreachable code");
                }
            };

    private final Ops<EventType<? extends E>, InputHandler<? super E>> OPS = new Ops<EventType<? extends E>, InputHandler<? super E>>() {

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
        public InputHandler<? super E> promote(InputHandler<? super E> h,
                EventType<? extends E> subTpe, EventType<? extends E> supTpe) {

            if(Objects.equals(subTpe, supTpe)) {
                return h;
            }
            return evt -> {
                if(isPrefixOf(subTpe, (EventType<? extends E>) evt.getEventType())) {
                    return h.process(evt);
                } else {
                    return Result.PROCEED;
                }
            };
        }

        @Override
        public InputHandler<E> squash(InputHandler<? super E> v1, InputHandler<? super E> v2) {
            return SEQ.apply(v1, v2);
        }

    };


    private static final List<EventType<?>> toList(EventType<?> t) {
        List<EventType<?>> l = new ArrayList<>();
        while(t != null) {
            l.add(t);
            t = t.getSuperType();
        }
        Collections.reverse(l);
        return l;
    }

    private PrefixTree<EventType<? extends E>, InputHandler<? super E>> handlerTree = PrefixTree.empty(OPS);

    public <F extends E> void insertAfter(EventType<? extends F> t, InputHandler<? super F> h) {
        InputHandler<? super E> handler = (InputHandler<? super E>) h;
        handlerTree = handlerTree.insert(t, handler, SEQ);
    }

    void forEach(HandlerConsumer<? super E> f) {
        handlerTree.entries().forEach(th -> f.accept(th.getKey(), th.getValue()));
    }
}