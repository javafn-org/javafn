package org.javafn.tuple;

import java.util.Iterator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * See documentation for {@link Pair}.
 */
public final class Trio<V1, V2, V3> {

    public static <A, B, C> Stream<Trio<A, B, C>> zip(
            final Stream<A> streamA,
            final Stream<B> streamB,
            final Stream<C> streamC) {
        final Iterator<A> itrA = streamA.iterator();
        final Iterator<B> itrB = streamB.iterator();
        final Iterator<C> itrC = streamC.iterator();
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(new Iterator<Trio<A, B, C>>() {
            @Override public boolean hasNext() { return itrA.hasNext() && itrB.hasNext() && itrC.hasNext(); }
            @Override public Trio<A, B, C> next() { return Trio.of(itrA.next(), itrB.next(), itrC.next()); }
        }, Spliterator.ORDERED), false);
    }

    public static <A, B, C, R> Function<Trio<A, B, C>, R> Map(final TriFunction<A, B, C, R> fn)
    { return trio -> trio.map(fn); }
    public static <A, B, C, NA> Function<Trio<A, B, C>, Trio<NA, B, C>> Map1(final Function<A,NA> fn)
    { return trio -> trio.map1(fn); }
    public static <A, B, C, NB> Function<Trio<A, B, C>, Trio<A, NB, C>> Map2(final Function<B, NB> fn)
    { return trio -> trio.map2(fn); }
    public static <A, B, C, NC> Function<Trio<A, B, C>, Trio<A, B, NC>> Map3(final Function<C, NC> fn)
    { return trio -> trio.map3(fn); }
    public static <A, B, C, NA> Function<Trio<A, B, C>, Trio<NA, B, C>> Map1(final TriFunction<A, B, C, NA> fn)
    { return trio -> trio.map1(fn); }
    public static <A, B, C, NB> Function<Trio<A, B, C>, Trio<A, NB, C>> Map2(final TriFunction<A, B, C, NB> fn)
    { return trio -> trio.map2(fn); }
    public static <A, B, C, NC> Function<Trio<A, B, C>, Trio<A, B, NC>> Map3(final TriFunction<A, B, C, NC> fn)
    { return trio -> trio.map3(fn); }

    public static <A, B, C> Consumer<Trio<A, B, C>> Peek(final TriConsumer<A, B, C> fn)
    { return trio -> trio.peek(fn); }
    public static <A, B, C> Consumer<Trio<A, B, C>> Peek1(final Consumer<A> fn) { return trio -> trio.peek1(fn); }
    public static <A, B, C> Consumer<Trio<A, B, C>> Peek2(final Consumer<B> fn) { return trio -> trio.peek2(fn); }
    public static <A, B, C> Consumer<Trio<A, B, C>> Peek3(final Consumer<C> fn) { return trio -> trio.peek3(fn); }

    public static <A, B, C> Predicate<Trio<A, B, C>> Filter(final TriPredicate<A, B, C> fn)
    { return trio -> trio.filter(fn); }
    public static <A, B, C> Predicate<Trio<A, B, C>> Filter1(final Predicate<A> fn) { return trio -> trio.filter1(fn); }
    public static <A, B, C> Predicate<Trio<A, B, C>> Filter2(final Predicate<B> fn) { return trio -> trio.filter2(fn); }
    public static <A, B, C> Predicate<Trio<A, B, C>> Filter3(final Predicate<C> fn) { return trio -> trio.filter3(fn); }

    public static <A, B, C> Consumer<Trio<A, B, C>> ForEach(final TriConsumer<A, B, C> fn)
    { return trio -> trio.forEach(fn); }

    public static <A, B, C, D> Function<Trio<A, B, C>, Quad<A, B, C, D>> ToQuad(final Supplier<D> fn)
    { return trio -> Quad.of(trio._1(), trio._2(), trio._3(), fn.get()); }

    public static <A, B, C, D> Function<Trio<A, B, C>, Quad<A, B, C, D>> ToQuad(final TriFunction<A, B, C, D> fn) {
        return trio -> {
            final A a = trio._1();
            final B b = trio._2();
            final C c = trio._3();
            return Quad.of(a, b, c, fn.apply(a, b, c));
        };
    }

    public static <A, B, C> Function<Trio<A,B,C>, Pair<A, B>> ToPair() { return trio -> Pair.of(trio._1(), trio._2()); }

    public static <A> Stream<Trio<A, A, A>> chunks(A[] a) {
        final int len = a.length;
        final int nChunks = len / 3;
        final int nChunked = nChunks * 3;
        final int rem = len % 3;
        return Stream.concat(
                IntStream.range(0, nChunks).map(i -> i * 3).mapToObj(i -> Trio.of(a[i], a[i+1], a[i+2])),
                rem == 0 ? Stream.empty() : Stream.of(Trio.of(
                        a[nChunked], rem == 2 ? a[nChunked + 1] : null, null
                )));
    }
    public static Stream<Trio<Double, Double, Double>> chunks(double[] a) {
        final int len = a.length;
        final int nChunks = len / 3;
        final int nChunked = nChunks * 3;
        final int rem = len % 3;
        return Stream.concat(
                IntStream.range(0, nChunks).map(i -> i * 3).mapToObj(i -> Trio.of(a[i], a[i+1], a[i+2])),
                rem == 0 ? Stream.empty() : Stream.of(Trio.of(
                        a[nChunked], rem == 2 ? a[nChunked + 1] : null, null
                )));
    }
    public static Stream<Trio<Long, Long, Long>> chunks(long[] a) {
        final int len = a.length;
        final int nChunks = len / 3;
        final int nChunked = nChunks * 3;
        final int rem = len % 3;
        return Stream.concat(
                IntStream.range(0, nChunks).map(i -> i * 3).mapToObj(i -> Trio.of(a[i], a[i+1], a[i+2])),
                rem == 0 ? Stream.empty() : Stream.of(Trio.of(
                        a[nChunked], rem == 2 ? a[nChunked + 1] : null, null
                )));
    }
    public static Stream<Trio<Integer, Integer, Integer>> chunks(int[] a) {
        final int len = a.length;
        final int nChunks = len / 3;
        final int nChunked = nChunks * 3;
        final int rem = len % 3;
        return Stream.concat(
                IntStream.range(0, nChunks).map(i -> i * 3).mapToObj(i -> Trio.of(a[i], a[i+1], a[i+2])),
                rem == 0 ? Stream.empty() : Stream.of(Trio.of(
                        a[nChunked], rem == 2 ? a[nChunked + 1] : null, null
                )));
    }

    public static <A> Stream<Trio<A, A, A>> windows(A[] a)
    { return IntStream.range(0, a.length - 2).mapToObj(i -> Trio.of(a[i], a[i+1], a[i+2])); }
    public static Stream<Trio<Double, Double, Double>> windows(double[] a)
    { return IntStream.range(0, a.length - 2).mapToObj(i -> Trio.of(a[i], a[i+1], a[i+2])); }
    public static Stream<Trio<Long, Long, Long>> windows(long[] a)
    { return IntStream.range(0, a.length - 2).mapToObj(i -> Trio.of(a[i], a[i+1], a[i+2])); }
    public static Stream<Trio<Integer, Integer, Integer>> windows(int[] a)
    { return IntStream.range(0, a.length - 2).mapToObj(i -> Trio.of(a[i], a[i+1], a[i+2])); }

    public static <A> Function<Trio<A,A,A>, Stream<A>> stream() { return Trio::stream; }
    public static <A> Stream<A> stream(final Trio<A, A, A> trio) { return Stream.of(trio._1(), trio._2(), trio._3()); }

    @FunctionalInterface public interface TriFunction<A, B, C, R> { R apply(A a, B b, C c); }
    @FunctionalInterface public interface TriConsumer<A, B, C> { void accept(A a, B b, C c); }
    @FunctionalInterface public interface TriPredicate<A, B, C> { boolean test(A a, B b, C c); }

    public static <A, B, C> Trio<A, B, C> of(final A a, final B b, final C c) { return new Trio<A, B, C>(a, b, c); }

    private final V1 v1;
    private final V2 v2;
    private final V3 v3;

    private Trio(final V1 _v1, final V2 _v2, final V3 _v3) { v1 = _v1; v2 = _v2; v3 = _v3; }

    public V1 _1() { return v1; }
    public V2 _2() { return v2; }
    public V3 _3() { return v3; }

    public <NV1> Trio<NV1, V2, V3> _1(final NV1 nv1) { return Trio.of(nv1, v2, v3); }
    public <NV2> Trio<V1, NV2, V3> _2(final NV2 nv2) { return Trio.of(v1, nv2, v3); }
    public <NV3> Trio<V1, V2, NV3> _3(final NV3 nv3) { return Trio.of(v1, v2, nv3); }

    public <R> R map(final TriFunction<V1, V2, V3, R> fn) { return fn.apply(v1, v2, v3); }
    public <NV1> Trio<NV1, V2, V3> map1(final Function<V1,NV1> fn) { return Trio.of(fn.apply(v1), v2, v3); }
    public <NV2> Trio<V1, NV2, V3> map2(final Function<V2, NV2> fn) { return Trio.of(v1, fn.apply(v2), v3); }
    public <NV3> Trio<V1, V2, NV3> map3(final Function<V3, NV3> fn) { return Trio.of(v1, v2, fn.apply(v3)); }
    public <NV1> Trio<NV1, V2, V3> map1(final TriFunction<V1, V2, V3, NV1> fn)
    { return Trio.of(fn.apply(v1, v2, v3), v2, v3); }
    public <NV2> Trio<V1, NV2, V3> map2(final TriFunction<V1, V2, V3, NV2> fn)
    { return Trio.of(v1, fn.apply(v1, v2, v3), v3); }
    public <NV3> Trio<V1, V2, NV3> map3(final TriFunction<V1, V2, V3, NV3> fn)
    { return Trio.of(v1, v2, fn.apply(v1, v2, v3)); }

    public Trio<V1, V2, V3> peek(final TriConsumer<V1, V2, V3> fn) { fn.accept(v1, v2, v3); return this; }
    public Trio<V1, V2, V3> peek1(final Consumer<V1> fn) { fn.accept(v1); return this; }
    public Trio<V1, V2, V3> peek2(final Consumer<V2> fn) { fn.accept(v2); return this; }
    public Trio<V1, V2, V3> peek3(final Consumer<V3> fn) { fn.accept(v3); return this; }

    public boolean filter(final TriPredicate<V1, V2, V3> fn) { return fn.test(v1, v2, v3); }
    public boolean filter1(final Predicate<V1> fn) { return fn.test(v1); }
    public boolean filter2(final Predicate<V2> fn) { return fn.test(v2); }
    public boolean filter3(final Predicate<V3> fn) { return fn.test(v3); }

    public void forEach(final TriConsumer<V1, V2, V3> fn) { fn.accept(v1, v2, v3); }

    @Override public int hashCode() { return Objects.hash(v1, v2, v3); }

    @Override public boolean equals(final Object obj) {
        if (obj instanceof Trio) {
            final Trio<?, ?, ?> that = (Trio<?, ?, ?>) obj;
            return Objects.equals(v1, that.v1) && Objects.equals(v2, that.v2) && Objects.equals(v3, that.v3);
        }
        return false;
    }

    @Override public String toString() { return "Trio.of(" + v1 + ", " + v2 + ", " + v3 + ")"; }
}
