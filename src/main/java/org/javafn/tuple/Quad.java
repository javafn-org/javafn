package org.javafn.tuple;

import java.util.Iterator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * See documentation for {@link Pair}.
 */
public final class Quad<V1, V2, V3, V4> {

    public static <A, B, C, D> Stream<Quad<A, B, C, D>> zip(
            final Stream<A> streamA,
            final Stream<B> streamB,
            final Stream<C> streamC,
            final Stream<D> streamD) {
        final Iterator<A> itrA = streamA.iterator();
        final Iterator<B> itrB = streamB.iterator();
        final Iterator<C> itrC = streamC.iterator();
        final Iterator<D> itrD = streamD.iterator();
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(new Iterator<Quad<A, B, C, D>>() {
            @Override
            public boolean hasNext() { return itrA.hasNext() && itrB.hasNext() && itrC.hasNext() && itrD.hasNext(); }
            @Override
            public Quad<A, B, C, D> next() { return Quad.of(itrA.next(), itrB.next(), itrC.next(), itrD.next()); }
        }, Spliterator.ORDERED), false);
    }

    public static <A,B,C,D,R> Function<Quad<A,B,C,D>,R> Map(final QuadFunction<A, B, C, D, R> fn)
    { return quad -> quad.map(fn); }
    public static <A,B,C,D,R> Function<Quad<A,B,C,D>,Quad<R,B,C,D>> Map1(final Function<A, R> fn)
    { return quad -> quad.map1(fn); }
    public static <A,B,C,D,R> Function<Quad<A,B,C,D>,Quad<A,R,C,D>> Map2(final Function<B, R> fn)
    { return quad -> quad.map2(fn); }
    public static <A,B,C,D,R> Function<Quad<A,B,C,D>,Quad<A,B,R,D>> Map3(final Function<C, R> fn)
    { return quad -> quad.map3(fn); }
    public static <A,B,C,D,R> Function<Quad<A,B,C,D>,Quad<A,B,C,R>> Map4(final Function<D, R> fn)
    { return quad -> quad.map4(fn); }
    public static <A,B,C,D,R> Function<Quad<A,B,C,D>,Quad<R,B,C,D>> Map1(final QuadFunction<A, B, C, D, R> fn)
    { return quad -> quad.map1(fn); }
    public static <A,B,C,D,R> Function<Quad<A,B,C,D>,Quad<A,R,C,D>> Map2(final QuadFunction<A, B, C, D, R> fn)
    { return quad -> quad.map2(fn); }
    public static <A,B,C,D,R> Function<Quad<A,B,C,D>,Quad<A,B,R,D>> Map3(final QuadFunction<A, B, C, D, R> fn)
    { return quad -> quad.map3(fn); }
    public static <A,B,C,D,R> Function<Quad<A,B,C,D>,Quad<A,B,C,R>> Map4(final QuadFunction<A, B, C, D, R> fn)
    { return quad -> quad.map4(fn); }
    public static <A,B,C,D> Consumer<Quad<A,B,C,D>> Peek(final QuadConsumer<A, B, C, D> fn)
    { return quad -> quad.peek(fn); }
    public static <A,B,C,D> Consumer<Quad<A,B,C,D>> Peek1(final Consumer<A> fn) { return quad -> quad.peek1(fn); }
    public static <A,B,C,D> Consumer<Quad<A,B,C,D>> Peek2(final Consumer<B> fn) { return quad -> quad.peek2(fn); }
    public static <A,B,C,D> Consumer<Quad<A,B,C,D>> Peek3(final Consumer<C> fn) { return quad -> quad.peek3(fn); }
    public static <A,B,C,D> Consumer<Quad<A,B,C,D>> Peek4(final Consumer<D> fn) { return quad -> quad.peek4(fn); }
    public static <A,B,C,D> Predicate<Quad<A,B,C,D>> Filter(final QuadPredicate<A, B, C, D> fn)
    { return quad -> quad.filter(fn); }
    public static <A,B,C,D> Predicate<Quad<A,B,C,D>> Filter1(final Predicate<A> fn) { return quad -> quad.filter1(fn); }
    public static <A,B,C,D> Predicate<Quad<A,B,C,D>> Filter2(final Predicate<B> fn) { return quad -> quad.filter2(fn); }
    public static <A,B,C,D> Predicate<Quad<A,B,C,D>> Filter3(final Predicate<C> fn) { return quad -> quad.filter3(fn); }
    public static <A,B,C,D> Predicate<Quad<A,B,C,D>> Filter4(final Predicate<D> fn) { return quad -> quad.filter4(fn); }
    public static <A,B,C,D> Consumer<Quad<A,B,C,D>> ForEach(final QuadConsumer<A, B, C, D> fn)
    { return quad -> quad.forEach(fn); }
    public static <A, B, C, D> Function<Quad<A,B,C,D>, Trio<A, B, C>> ToTrio()
    { return quad -> Trio.of(quad._1(), quad._2(), quad._3()); }

    public static <A> Stream<Quad<A, A, A, A>> chunks(A[] a) {
        final int len = a.length;
        final int nChunks = len / 4;
        final int nChunked = nChunks * 4;
        final int rem = len % 4;
        return Stream.concat(
                IntStream.range(0, nChunks).map(i -> i * 4).mapToObj(i -> Quad.of(a[i], a[i+1], a[i+2], a[i+3])),
                rem == 0 ? Stream.empty() : Stream.of(Quad.of(
                        a[nChunked], rem == 2 ? a[nChunked + 1] : null, rem == 3 ? a[nChunked+2] : null, null
                )));
    }
    public static Stream<Quad<Double, Double, Double, Double>> chunks(double[] a) {
        final int len = a.length;
        final int nChunks = len / 4;
        final int nChunked = nChunks * 4;
        final int rem = len % 4;
        return Stream.concat(
                IntStream.range(0, nChunks).map(i -> i * 4).mapToObj(i -> Quad.of(a[i], a[i+1], a[i+2], a[i+3])),
                rem == 0 ? Stream.empty() : Stream.of(Quad.of(
                        a[nChunked], rem == 2 ? a[nChunked + 1] : null, rem == 3 ? a[nChunked+2] : null, null
                )));
    }
    public static Stream<Quad<Long, Long, Long, Long>> chunks(long[] a) {
        final int len = a.length;
        final int nChunks = len / 4;
        final int nChunked = nChunks * 4;
        final int rem = len % 4;
        return Stream.concat(
                IntStream.range(0, nChunks).map(i -> i * 4).mapToObj(i -> Quad.of(a[i], a[i+1], a[i+2], a[i+3])),
                rem == 0 ? Stream.empty() : Stream.of(Quad.of(
                        a[nChunked], rem == 2 ? a[nChunked + 1] : null, rem == 3 ? a[nChunked+2] : null, null
                )));
    }
    public static Stream<Quad<Integer, Integer, Integer, Integer>> chunks(int[] a) {
        final int len = a.length;
        final int nChunks = len / 4;
        final int nChunked = nChunks * 4;
        final int rem = len % 4;
        return Stream.concat(
                IntStream.range(0, nChunks).map(i -> i * 4).mapToObj(i -> Quad.of(a[i], a[i+1], a[i+2], a[i+3])),
                rem == 0 ? Stream.empty() : Stream.of(Quad.of(
                        a[nChunked], rem == 2 ? a[nChunked + 1] : null, rem == 3 ? a[nChunked+2] : null, null
                )));
    }

    public static <A> Stream<Quad<A, A, A, A>> windows(A[] a)
    { return IntStream.range(0, a.length - 3).mapToObj(i -> Quad.of(a[i], a[i+1], a[i+2], a[i+3])); }
    public static Stream<Quad<Double, Double, Double, Double>> windows(double[] a)
    { return IntStream.range(0, a.length - 3).mapToObj(i -> Quad.of(a[i], a[i+1], a[i+2], a[i+3])); }
    public static Stream<Quad<Long, Long, Long, Long>> windows(long[] a)
    { return IntStream.range(0, a.length - 3).mapToObj(i -> Quad.of(a[i], a[i+1], a[i+2], a[i+3])); }
    public static Stream<Quad<Integer, Integer, Integer, Integer>> windows(int[] a)
    { return IntStream.range(0, a.length - 3).mapToObj(i -> Quad.of(a[i], a[i+1], a[i+2], a[i+3])); }

    public static <A> Function<Quad<A,A,A,A>, Stream<A>> stream() { return Quad::stream; }
    public static <A> Stream<A> stream(final Quad<A,A,A,A> quad)
    { return Stream.of(quad._1(), quad._2(), quad._3(), quad._4()); }

    @FunctionalInterface public interface QuadFunction<A, B, C, D, R> { R apply(A a, B b, C c, D d); }
    @FunctionalInterface public interface QuadConsumer<A, B, C, D> { void accept(A a, B b, C c, D d); }
    @FunctionalInterface public interface QuadPredicate<A, B, C, D> { boolean test(A a, B b, C c, D d); }

    public static <A, B, C, D> Quad<A, B, C, D> of(final A a, final B b, final C c, final D d)
    { return new Quad<>(a, b, c, d); }

    private final V1 v1;
    private final V2 v2;
    private final V3 v3;
    private final V4 v4;

    private Quad(final V1 _v1, final V2 _v2, final V3 _v3, final V4 _v4) { v1 = _v1; v2 = _v2; v3 = _v3; v4 = _v4; }

    public V1 _1() { return v1; }
    public V2 _2() { return v2; }
    public V3 _3() { return v3; }
    public V4 _4() { return v4; }

    public <NV1> Quad<NV1, V2, V3, V4> _1(final NV1 nv1) { return Quad.of(nv1, v2, v3, v4); }
    public <NV2> Quad<V1, NV2, V3, V4> _2(final NV2 nv2) { return Quad.of(v1, nv2, v3, v4); }
    public <NV3> Quad<V1, V2, NV3, V4> _3(final NV3 nv3) { return Quad.of(v1, v2, nv3, v4); }
    public <NV4> Quad<V1, V2, V3, NV4> _4(final NV4 nv4) { return Quad.of(v1, v2, v3, nv4); }

    public <R> R map(final QuadFunction<V1, V2, V3, V4, R> fn) { return fn.apply(v1, v2, v3, v4); }
    public <NV1> Quad<NV1, V2, V3, V4> map1(final Function<V1, NV1> fn) { return Quad.of(fn.apply(v1), v2, v3, v4); }
    public <NV2> Quad<V1, NV2, V3, V4> map2(final Function<V2, NV2> fn) { return Quad.of(v1, fn.apply(v2), v3, v4); }
    public <NV3> Quad<V1, V2, NV3, V4> map3(final Function<V3, NV3> fn) { return Quad.of(v1, v2, fn.apply(v3), v4); }
    public <NV4> Quad<V1, V2, V3, NV4> map4(final Function<V4, NV4> fn) { return Quad.of(v1, v2, v3, fn.apply(v4)); }
    public <NV1> Quad<NV1, V2, V3, V4> map1(final QuadFunction<V1, V2, V3, V4, NV1> fn)
    { return Quad.of(fn.apply(v1, v2, v3, v4), v2, v3, v4); }
    public <NV2> Quad<V1, NV2, V3, V4> map2(final QuadFunction<V1, V2, V3, V4, NV2> fn)
    { return Quad.of(v1, fn.apply(v1, v2, v3, v4), v3, v4); }
    public <NV3> Quad<V1, V2, NV3, V4> map3(final QuadFunction<V1, V2, V3, V4, NV3> fn)
    { return Quad.of(v1, v2, fn.apply(v1, v2, v3, v4), v4); }
    public <NV4> Quad<V1, V2, V3, NV4> map4(final QuadFunction<V1, V2, V3, V4, NV4> fn)
    { return Quad.of(v1, v2, v3, fn.apply(v1, v2, v3, v4)); }

    public Quad<V1, V2, V3, V4> peek(final QuadConsumer<V1, V2, V3, V4> fn) { fn.accept(v1, v2, v3, v4); return this; }
    public Quad<V1, V2, V3, V4> peek1(final Consumer<V1> fn) { fn.accept(v1); return this; }
    public Quad<V1, V2, V3, V4> peek2(final Consumer<V2> fn) { fn.accept(v2); return this; }
    public Quad<V1, V2, V3, V4> peek3(final Consumer<V3> fn) { fn.accept(v3); return this; }
    public Quad<V1, V2, V3, V4> peek4(final Consumer<V4> fn) { fn.accept(v4); return this; }

    public boolean filter(final QuadPredicate<V1, V2, V3, V4> fn) { return fn.test(v1, v2, v3, v4); }
    public boolean filter1(final Predicate<V1> fn) { return fn.test(v1); }
    public boolean filter2(final Predicate<V2> fn) { return fn.test(v2); }
    public boolean filter3(final Predicate<V3> fn) { return fn.test(v3); }
    public boolean filter4(final Predicate<V4> fn) { return fn.test(v4); }

    public void forEach(final QuadConsumer<V1, V2, V3, V4> fn) { fn.accept(v1, v2, v3, v4); }

    @Override public int hashCode() { return Objects.hash(v1, v2, v3, v4); }

    @Override public boolean equals(final Object obj) {
        if (obj instanceof Quad) {
            final Quad<?, ?, ?, ?> that = (Quad<?, ?, ?, ?>) obj;
            return Objects.equals(v1, that.v1) && Objects.equals(v2, that.v2)
                    && Objects.equals(v3, that.v3) && Objects.equals(v4, that.v4);
        }
        return false;
    }

    @Override public String toString() { return "Quad.of(" + v1 + ", " + v2 + ", " + v3 + ", " + v4 + ")"; }
}
