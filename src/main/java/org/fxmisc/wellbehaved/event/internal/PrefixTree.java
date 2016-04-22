package org.fxmisc.wellbehaved.event.internal;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Prefix tree (Trie) with an additional property that no data is stored in
 * internal nodes.
 *
 * @param <K> type of "strings" used to index values
 * @param <V> type of values (data) indexed by this trie
 */
public abstract class PrefixTree<K, V> {

    public static interface Ops<K, V> {
        boolean isPrefixOf(K k1, K k2);
        K commonPrefix(K k1, K k2);
        V promote(V v, K oldKey, K newKey);
        V squash(V v1, V v2);
    }

    private static class Empty<K, V> extends PrefixTree<K, V> {

        public Empty(Ops<K, V> ops) {
            super(ops);
        }

        @Override
        public Stream<Entry<K, V>> entries() {
            return Stream.empty();
        }

        @Override
        public PrefixTree<K, V> insert(K key, V value, BiFunction<? super V, ? super V, ? extends V> combine) {
            return insertInside(key, value, combine);
        }

        @Override
        PrefixTree<K, V> insertInside(K key, V value, BiFunction<? super V, ? super V, ? extends V> combine) {
            return new Data<>(ops, key, value);
        }

        @Override
        public <W> PrefixTree<K, W> map(
                Function<? super V, ? extends W> f,
                Ops<K, W> ops) {
            return new Empty<>(ops);
        }

    }

    private static abstract class NonEmpty<K, V> extends PrefixTree<K, V> {
        public NonEmpty(Ops<K, V> ops) {
            super(ops);
        }

        abstract K getPrefix();
        abstract Data<K, V> collapse();

        @Override
        public abstract <W> NonEmpty<K, W> map(Function<? super V, ? extends W> f, Ops<K, W> ops);

        @Override
        abstract NonEmpty<K, V> insertInside(K key, V value, BiFunction<? super V, ? super V, ? extends V> combine);

        @Override
        public PrefixTree<K, V> insert(K key, V value, BiFunction<? super V, ? super V, ? extends V> combine) {
            if(ops.isPrefixOf(key, getPrefix())) { // key is a prefix of this tree
                return new Data<>(ops, key, value).insertInside(collapse(), flip(combine));
            } else if(ops.isPrefixOf(getPrefix(), key)) { // key is inside this tree
                return insertInside(key, value, combine);
            } else {
                return new Branch<>(ops, this, new Data<>(ops, key, value));
            }
        }
    }

    private static class Branch<K, V> extends NonEmpty<K, V> {
        private final K prefix;
        private final List<NonEmpty<K, V>> subTrees;

        Branch(Ops<K, V> ops, K prefix, List<NonEmpty<K, V>> subTrees) {
            super(ops);

            assert Objects.equals(prefix, subTrees.stream().map(NonEmpty::getPrefix).reduce(ops::commonPrefix).get());
            assert subTrees.stream().noneMatch(tree -> Objects.equals(tree.getPrefix(), prefix));

            this.prefix = prefix;
            this.subTrees = subTrees;
        }

        private Branch(Ops<K, V> ops, NonEmpty<K, V> t1, NonEmpty<K, V> t2) {
            this(ops, ops.commonPrefix(t1.getPrefix(), t2.getPrefix()), Arrays.asList(t1, t2));
        }

        @Override
        K getPrefix() {
            return prefix;
        }

        @Override
        public Stream<Entry<K, V>> entries() {
            return subTrees.stream().flatMap(tree -> tree.entries());
        }

        @Override
        Data<K, V> collapse() {
            return subTrees.stream()
                    .map(tree -> tree.collapse().promote(prefix))
                    .reduce(Data::squash).get();
        }

        @Override
        NonEmpty<K, V> insertInside(K key, V value, BiFunction<? super V, ? super V, ? extends V> combine) {
            assert ops.isPrefixOf(prefix, key);

            if(Objects.equals(key, prefix)) {
                return new Data<>(ops, key, value).insertInside(collapse(), flip(combine));
            }

            // try to find a sub-tree that has common prefix with key longer than this branch's prefix
            for(int i = 0; i < subTrees.size(); ++i) {
                NonEmpty<K, V> st = subTrees.get(i);
                K commonPrefix = ops.commonPrefix(key, st.getPrefix());
                if(!Objects.equals(commonPrefix, prefix)) {
                    if(Objects.equals(commonPrefix, st.getPrefix())) {
                        // st contains key, insert inside st
                        return replaceBranch(i, st.insertInside(key, value, combine));
                    } else if(Objects.equals(commonPrefix, key)) {
                        // st is under key, insert st inside Data(key, value)
                        return replaceBranch(i, new Data<>(ops, key, value).insertInside(st.collapse(), flip(combine)));
                    } else {
                        return replaceBranch(i, new Branch<>(ops, st, new Data<>(ops, key, value)));
                    }
                }
            }

            // no branch intersects key, adjoin Data(key, value) to this branch
            List<NonEmpty<K, V>> branches = new ArrayList<>(subTrees.size() + 1);
            branches.addAll(subTrees);
            branches.add(new Data<>(ops, key, value));
            return new Branch<>(ops, prefix, branches);
        }

        private Branch<K, V> replaceBranch(int i, NonEmpty<K, V> replacement) {
            assert ops.isPrefixOf(prefix, replacement.getPrefix());
            assert ops.isPrefixOf(replacement.getPrefix(), subTrees.get(i).getPrefix());

            ArrayList<NonEmpty<K, V>> branches = new ArrayList<>(subTrees);
            branches.set(i, replacement);
            return new Branch<>(ops, prefix, branches);
        }

        @Override
        public <W> NonEmpty<K, W> map(
                Function<? super V, ? extends W> f,
                Ops<K, W> ops) {
            List<NonEmpty<K, W>> mapped = new ArrayList<>(subTrees.size());
            for(NonEmpty<K, V> tree: subTrees) {
                mapped.add(tree.map(f, ops));
            }
            return new Branch<>(ops, prefix, mapped);
        }
    }

    private static class Data<K, V> extends NonEmpty<K, V> {
        private final K key;
        private final V value;

        Data(Ops<K, V> ops, K key, V value) {
            super(ops);
            this.key = key;
            this.value = value;
        }

        @Override
        K getPrefix() {
            return key;
        }

        @Override
        public Stream<Entry<K, V>> entries() {
            return Stream.of(new SimpleEntry<>(key, value));
        }

        @Override
        Data<K, V> collapse() {
            return this;
        }

        @Override
        NonEmpty<K, V> insertInside(K key, V value, BiFunction<? super V, ? super V, ? extends V> combine) {
            assert ops.isPrefixOf(this.key, key);
            return new Data<>(
                    this.ops,
                    this.key,
                    combine.apply(this.value, ops.promote(value, key, this.key)));
        }

        NonEmpty<K, V> insertInside(NonEmpty<K, V> tree, BiFunction<? super V, ? super V, ? extends V> combine) {
            Data<K, V> d = tree.collapse();
            return insertInside(d.key, d.value, combine);
        }

        Data<K, V> promote(K key) {
            assert ops.isPrefixOf(key, this.key);
            return new Data<>(ops, key, ops.promote(value, this.key, key));
        }

        Data<K, V> squash(Data<K, V> that) {
            assert Objects.equals(this.key, that.key);
            return new Data<>(ops, key, ops.squash(this.value, that.value));
        }

        @Override
        public <W> Data<K, W> map(
                Function<? super V, ? extends W> f,
                Ops<K, W> ops) {
            return new Data<>(ops, key, f.apply(value));
        }
    }

    public static <K, V> PrefixTree<K, V> empty(Ops<K, V> ops) {
        return new Empty<>(ops);
    }

    private static <A, B, C> BiFunction<B, A, C> flip(BiFunction<A, B, C> f) {
        return (a, b) -> f.apply(b, a);
    }


    final Ops<K, V> ops;

    private PrefixTree(Ops<K, V> ops) {
        this.ops = ops;
    }

    public abstract Stream<Map.Entry<K, V>> entries();
    public abstract PrefixTree<K, V> insert(K key, V value, BiFunction<? super V, ? super V, ? extends V> combine);
    public abstract <W> PrefixTree<K, W> map(Function<? super V, ? extends W> f, Ops<K, W> ops);

    public final PrefixTree<K, V> map(Function<? super V, ? extends V> f) {
        return map(f, ops);
    }

    abstract PrefixTree<K, V> insertInside(K key, V value, BiFunction<? super V, ? super V, ? extends V> combine);
}
