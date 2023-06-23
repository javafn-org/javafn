package org.javafn.tuple;

import java.util.Iterator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * A tuple similar to the Pair type in <a href="https://www.javatuples.org/">javatuples</a> that supports better
 * fluent functional usage.
 * <pre>{@code
 * IntStream.range(0, 10).mapToObj(i -> org.javatuples.Pair.with(i, i*2))
 *     .filter(pair -> pair.getValue0() % 2 == 0)
 *     .map(pair -> pair.getValue0() + pair.getValue1())
 *     .forEach(System.out::println);
 * }</pre>
 * versus
 * <pre>{@code
 * IntStream.range(0, 10).mapToObj(i -> Pair.of(i, i*2))
 *     .filter(pair -> pair.filter1(i -> i % 2 == 0)
 *     .map(pair -> pair.map( (l, r) -> l + r))
 *     .forEach(System.out::println);
 * }</pre>
 * Or better yet,
 * <pre>{@code
 * IntStream.range(0, 10).mapToObj(i -> Pair.of(i, i*2))
 *     .filter(Pair.Filter1(i -> i % 2 == 0)
 *     .map(Pair.Map( (l, r) -> l + r))
 *     .forEach(System.out::println);
 * }</pre>
 * Namely, we get to name the parameters, similar to destructuring in languages that support pattern matching.
 * Notice that there are two versions of most functions, lowercase methods and uppercase static methods.
 * The lowercase methods operate directly on the Pair, while the uppercase methods return a function
 * that accepts a pair and operates on that.  The latter are useful in streams to avoid creating a
 * variable named `pair` just to call a function on it.
 * <pre>{@code
 * stream.map(pair -> pair.map( (l, r) -> ...));
 * stream.map(Pair.Map( (l, r) -> ...));
 * }</pre>
 * <p>
 * We don't want to add any dependencies to this project, so we can't create a tool for converting from javatuples
 * if your code is already using them.  You can make one yourself, though, for example,
 * <pre>{@code
 * package org.javafn.tuple;
 *
 * public class Tuples {
 *     public static <A, B> Pair<A, B>  from(final org.javatuples.Pair<A, B>  jtPair) {
 *         return Pair.of(jtPair.getValue0(), jtPair.getValue1());
 *     }
 *     public static <A, B, C> Trio<A, B, C> from(final org.javatuples.Triplet<A, B, C>  jtTrio) {
 *         return Trio.of(jtTrio.getValue0(), jtTrio.getValue1(), jtTrio.getValue2());
 *     }
 *     public static <A, B, C, D> Quad<A, B, C, D>  from(final org.javatuples.Quartet<A, B, C, D>  jtQuad) {
 *         return Quad.of(jtQuad.getValue0(), jtQuad.getValue1(), jtQuad.getValue2(), jtQuad.getValue3());
 *     }
 *     public static <A, B> org.javatuples.Pair<A, B> to(final Pair<A, B> pair) {
 *         return new org.javatuples.Pair<>(pair._1(), pair._2());
 *     }
 *     public static <A, B, C> org.javatuples.Triplet<A, B, C>  to(final Trio<A, B, C>  trio) {
 *         return new org.javatuples.Triplet<>(trio._1(), trio._2(), trio._3());
 *     }
 *     public static <A, B, C, D> org.javatuples.Quartet<A, B, C, D>  to(final Quad<A, B, C, D>  quad) {
 *         return new org.javatuples.Quartet<>(quad._1(), quad._2(), quad._3(), quad._4());
 *     }
 *     private Tuples() { throw new AssertionError("This is a static class and should not be instantiated."); }
 * }
 * }</pre>
 */
public final class Pair<V1, V2> {

    /**
     * Return a new stream that forms pairs from the supplied streams.  The length of the new stream is
     * the shorter of the two.
     * <pre>{@code
     * final IntStream left = IntStream.range(0, 100);
     * final LongStream right = LongStream.range(1000, 2000);
     * final List<Pair<Integer, Long>> zipped = Pair.zip(left, right).collect(Collectors.toList());
     * System.out.println(zipped.size());
     * // 100
     * System.out.println(zipped.get(0));
     * // Pair(0, 1000)
     * }</pre>
     */
    public static <A, B> Stream<Pair<A, B>> zip(final Stream<A> streamA, final Stream<B> streamB) {
        final Iterator<A> itrA = streamA.iterator();
        final Iterator<B> itrB = streamB.iterator();
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(new Iterator<Pair<A, B>>() {
            @Override public boolean hasNext() { return itrA.hasNext() && itrB.hasNext(); }
            @Override public Pair<A, B> next() { return Pair.of(itrA.next(), itrB.next()); }
        }, Spliterator.ORDERED), false);
    }

    /**
     * Partition the supplied stream using the supplied predicate.  True instances will be returned in the first
     * element and false instances will be returned in the second element.
     * <pre>{@code
     * final Pair<Stream<Integer> Stream<Integer>> evenOdd =  Pair.partition(IntStream.range(0, 100), i -> i % 2 == 0);
     * final List<Integer> even = evenOdd._1().collect(Collectors.toList());
     * final List<Integer> odd = evenOdd._2().collect(Collectors.toList());
     * System.out.println(even.size() + ", " + odd.size());
     * // 50, 50
     * }</pre>
     */
    public static <X> Pair<Stream<X>, Stream<X>> partition(final Stream<X> stream, final Predicate<X> fn) {
        final Stream.Builder<X> trueStream = Stream.builder();
        final Stream.Builder<X> falseStream = Stream.builder();
        stream.forEach(x -> {
            if (fn.test(x)) trueStream.add(x);
            else falseStream.add(x);
        });
        return Pair.of(trueStream.build(), falseStream.build());
    }

    /**
     * Return a function that accepts a Pair and calls {@link Pair#map(BiFunction)} on it.
     */
    public static <A, B, R> Function<Pair<A, B>, R> Map(final BiFunction<A, B, R> fn) { return pair -> pair.map(fn); }
    /**
     * Return a function that accepts a Pair and calls {@link Pair#map1(Function)} on it.
     */
    public static <A, B, R> Function<Pair<A, B>, Pair<R, B>> Map1(final Function<A, R> fn)
    { return pair -> pair.map1(fn); }
    /**
     * Return a function that accepts a Pair and calls {@link Pair#map2(Function)} on it.
     */
    public static <A, B, R> Function<Pair<A, B>, Pair<A, R>> Map2(final Function<B, R> fn)
    { return pair -> pair.map2(fn); }
    /**
     * Return a function that accepts a Pair and calls {@link Pair#map1(BiFunction)} on it.
     */
    public static <A, B, R> Function<Pair<A, B>, Pair<R, B>> Map1(final BiFunction<A, B, R> fn)
    { return pair -> pair.map1(fn); }
    /**
     * Return a function that accepts a Pair and calls {@link Pair#map2(BiFunction)} on it.
     */
    public static <A, B, R> Function<Pair<A, B>, Pair<A, R>> Map2(final BiFunction<A, B, R> fn)
    { return pair -> pair.map2(fn); }
    /**
     * Return a function that accepts a Pair and calls {@link Pair#peek(BiConsumer)} on it.
     */
    public static <A, B> Consumer<Pair<A, B>> Peek(final BiConsumer<A, B> fn) { return pair -> pair.peek(fn); }
    /**
     * Return a function that accepts a Pair and calls {@link Pair#peek1(Consumer)} on it.
     */
    public static <A, B> Consumer<Pair<A, B>> Peek1(final Consumer<A> fn) { return pair -> pair.peek1(fn); }
    /**
     * Return a function that accepts a Pair and calls {@link Pair#peek2(Consumer)} on it.
     */
    public static <A, B> Consumer<Pair<A, B>> Peek2(final Consumer<B> fn) { return pair -> pair.peek2(fn); }
    /**
     * Return a function that accepts a Pair and calls {@link Pair#filter(BiPredicate)} on it.
     */
    public static <A, B> Predicate<Pair<A, B>> Filter(final BiPredicate<A, B> fn) { return pair -> pair.filter(fn); }
    /**
     * Return a function that accepts a Pair and calls {@link Pair#filter1(Predicate)} on it.
     */
    public static <A, B> Predicate<Pair<A, B>> Filter1(final Predicate<A> fn) { return pair -> pair.filter1(fn); }
    /**
     * Return a function that accepts a Pair and calls {@link Pair#filter2(Predicate)} on it.
     */
    public static <A, B> Predicate<Pair<A, B>> Filter2(final Predicate<B> fn) { return pair -> pair.filter2(fn); }
    /**
     * Return a function that accepts a Pair and calls {@link Pair#forEach(BiConsumer)} on it.
     */
    public static <A, B> Consumer<Pair<A, B>> ForEach(final BiConsumer<A, B> fn) { return pair -> pair.forEach(fn); }

    /**
     * Return a function that accepts a pair and uses the supplied function to generate a third element,
     * then creates a Trio from the two elements of the pair and the generated value.
     * <pre>{@code
     * final Pair<String, Integer> pair = Pair.of("42", 42);
     * final Trio<String, Integer, UUID> trio = Pair.ToTrio(UUID::randomUUID).apply(pair);
     * }</pre>
     * Useful in streams, i.e., you shouldn't need to call apply directly.
     * <pre>{@code
     * stream.map(Pair.ToTrio(() -> "A third value"));
     * }</pre>
     */
    public static <A, B, C> Function<Pair<A, B>, Trio<A, B, C>> ToTrio(final Supplier<C> fn)
    { return pair -> Trio.of(pair._1(), pair._2(), fn.get()); }

    /**
     * Return a function that accepts a pair and uses the supplied bi-function to generate a third element,
     * then creates a Trio from the two elements of the pair and the generated value.
     * <pre>{@code
     * final Pair<String, Integer> pair = Pair.of("42", 42);
     * final Trio<String, Integer, String> trio = Pair.ToTrio((s, i) -> s + ", " + Integer.toString(i)).apply(pair);
     * }</pre>
     * Useful in streams, i.e., you shouldn't need to call apply directly.
     * <pre>{@code
     * stream.map(Pair.ToTrio((key, newValue) -> someDatabase.get(key).replaceAndReturnPrevious(newValue)));
     * // Trio("somekey", "the new value", "the old value")
     * }</pre>
     */
    public static <A, B, C> Function<Pair<A, B>, Trio<A, B, C>> ToTrio(final BiFunction<A, B, C> fn) {
        return pair -> {
            final A a = pair._1();
            final B b = pair._2();
            return Trio.of(a, b, fn.apply(a, b));
        };
    }

    /**
     * Turn the supplied array into a stream of pairs where each element in the array appears once in the stream.
     * If the length of a is odd, the last element will contain null for the second pair element.
     * <pre>{@code
     * final Integer[] a = {1, 2, 3, 4, 5};
     * Pair.chunks(a).forEach(System.out::println);
     * // Prints
     * // Pair(1, 2)
     * // Pair(3, 4)
     * // Pair(5, null)
     * }</pre>
     * The elements are not copied, so changes made to the objects within the pairs will be reflected in
     * the supplied array.
     * @param a the array to chunk
     * @return the entries in the supplied array, chunked
     */
    public static <A> Stream<Pair<A, A>> chunks(A[] a) {
        final int len = a.length;
        return Stream.concat(
                IntStream.range(0, len / 2).map(i -> i * 2).mapToObj(i -> Pair.of(a[i], a[i+1])),
                len % 2 == 0 ? Stream.empty() : Stream.of(Pair.of(
                        a[len - 1], null
                )));
    }
    /** @see #chunks(Object[]) */
    public static Stream<Pair<Double, Double>> chunks(double[] a) {
        final int len = a.length;
        return Stream.concat(
                IntStream.range(0, len / 2).map(i -> i * 2).mapToObj(i -> Pair.of(a[i], a[i+1])),
                len % 2 == 0 ? Stream.empty() : Stream.of(Pair.of(
                        a[len - 1], null
                )));
    }
    /** @see #chunks(Object[]) */
    public static Stream<Pair<Long, Long>> chunks(long[] a) {
        final int len = a.length;
        return Stream.concat(
                IntStream.range(0, len / 2).map(i -> i * 2).mapToObj(i -> Pair.of(a[i], a[i+1])),
                len % 2 == 0 ? Stream.empty() : Stream.of(Pair.of(
                        a[len - 1], null
                )));
    }
    /** @see #chunks(Object[]) */
    public static Stream<Pair<Integer, Integer>> chunks(int[] a) {
        final int len = a.length;
        return Stream.concat(
                IntStream.range(0, len / 2).map(i -> i * 2).mapToObj(i -> Pair.of(a[i], a[i+1])),
                len % 2 == 0 ? Stream.empty() : Stream.of(Pair.of(
                        a[len - 1], null
                )));
    }

    /**
     * Turn the supplied array into a stream of pairs where each element in the array appears twice in the stream,
     * once as the second element of a pair and then again as the first element of a pair,
     * except for the first and last element, which appear once, as the first and last element respectively.
     * This function creates a "sliding window" of two elements which moves forward one element at a time.
     * <pre>{@code
     * final Integer[] a = {1, 2, 3, 4};
     * Pair.chunks(a).forEach(System.out::println);
     * // Prints
     * // Pair(1, 2)
     * // Pair(2, 3)
     * // Pair(3, 4)
     * }</pre>
     * The elements are not copied, so changes made to the objects within the pairs will be reflected in
     * the supplied array.
     * @param a the array over which is created a sliding window
     * @return a stream of pairs forming a sliding window over the supplied array
     */
    public static <A> Stream<Pair<A, A>> windows(A[] a)
    { return IntStream.range(0, a.length - 1).mapToObj(i -> Pair.of(a[i], a[i+1])); }
    /** @see #windows(Object[]) */
    public static Stream<Pair<Double, Double>> windows(double[] a)
    { return IntStream.range(0, a.length - 1).mapToObj(i -> Pair.of(a[i], a[i+1])); }
    /** @see #windows(Object[]) */
    public static Stream<Pair<Long, Long>> windows(long[] a)
    { return IntStream.range(0, a.length - 1).mapToObj(i -> Pair.of(a[i], a[i+1])); }
    /** @see #windows(Object[]) */
    public static Stream<Pair<Integer, Integer>> windows(int[] a)
    { return IntStream.range(0, a.length - 1).mapToObj(i -> Pair.of(a[i], a[i+1])); }

    /**
     * Return a function that accepts a Pair whose elements are the same type and returns a stream
     * of two elements which are the first and second elements of the pair respectively.
     */
    public static <A> Function<Pair<A,A>, Stream<A>> stream() { return Pair::stream; }

    /**
     * Accept a Pair whose elements are the same type and return a stream
     * of two elements which are the first and second elements of the pair respectively.
     */
    public static <A> Stream<A> stream(final Pair<A, A> pair) { return Stream.of(pair._1(), pair._2()); }

    /**
     * Create a Pair with the supplied values.
     */
    public static <A, B> Pair<A, B> of(final A a, final B b) { return new Pair<>(a, b); }

    private final V1 v1;
    private final V2 v2;

    private Pair(final V1 _v1, final V2 _v2) { v1 = _v1; v2 = _v2; }

    /** Get the first element of this pair */
    public V1 _1() { return v1; }
    /** Get the second element of this pair */
    public V2 _2() { return v2; }

    /**
     * Return a new pair using the supplied argument as the first element
     * and this pair's second element as the new pair's second element.
     * Note that the type signature can change.
     * <pre>{@code
     * final Pair<Integer, String> a = Pair.of(42, "forty-two");
     * final Pair<String, String> b = a._1("42");
     * }</pre>
     */
    public <NV1> Pair<NV1, V2> _1(final NV1 nv1) { return Pair.of(nv1, v2); }
    /**
     * Return a new pair using the supplied argument as the second element
     * and this pair's first element as the new pair's first element.
     * Note that the type signature can change.
     * <pre>{@code
     * final Pair<Integer, String> a = Pair.of(42, "forty-two");
     * final Pair<Integer, Long> b = a._2(42L);
     * }</pre>
     */
    public <NV2> Pair<V1, NV2> _2(final NV2 nv2) { return Pair.of(v1, nv2); }

    /**
     * Execute the supplied function on this Pair's values and return the result.
     * <pre>{@code
     * final Pair<Integer, String> pair = Pair.of(42, "forty-two");
     * System.out.println(pair.map( (i, s) -> Integer.toString(i) + ", " + s));
     * // Prints
     * // 42, forty-two
     * }</pre>
     */
    public <R> R map(final BiFunction<V1, V2, R> fn) { return fn.apply(v1, v2); }
    /**
     * Execute the supplied function on this Pair's first value and return a new pair where the
     * first element is the result of the mapping function and the second is this pair's second element.
     * <pre>{@code
     * final Pair<Integer, String> a = Pair.of(42, "forty-two");
     * final Pair<String, String> b = a.map1(Integer::toString);
     * }</pre>
     */
    public <NV1> Pair<NV1, V2> map1(final Function<V1, NV1> fn) { return Pair.of(fn.apply(v1), v2); }
    /**
     * Execute the supplied function on this Pair's second value and return a new pair where the
     * second element is the result of the mapping function and the first is this pair's first element.
     * <pre>{@code
     * final Pair<Integer, String> a = Pair.of(42, "forty-two");
     * final Pair<Integer, CustomValue> b = a.map1(key -> customDatabase.get(key));
     * }</pre>
     */
    public <NV2> Pair<V1, NV2> map2(final Function<V2, NV2> fn) { return Pair.of(v1, fn.apply(v2)); }
    /**
     * Execute the supplied function on this Pair's values and return a new pair where the
     * first element is the result of the mapping function and the second is this pair's second element.
     * <pre>{@code
     * final Pair<Integer, String> a = Pair.of(42, "forty-two");
     * System.out.println(a.map1( (i, s) -> Integer.toString(i) + ", " + s));
     * // Prints
     * // Pair("42, forty-two", "forty-two")
     * }</pre>
     */
    public <NV1> Pair<NV1, V2> map1(final BiFunction<V1, V2, NV1> fn) { return Pair.of(fn.apply(v1, v2), v2); }
    /**
     * Execute the supplied function on this Pair's values and return a new pair where the
     * second element is the result of the mapping function and the first is this pair's first element.
     * <pre>{@code
     * final Pair<Integer, String> a = Pair.of(42, "forty-two");
     * final Pair<Integer, CustomValue> b = a.map1(
     *          (intKey, propName) -> customDatabase.get(intKey).getProperty(propName)));
     * }</pre>
     */
    public <NV2> Pair<V1, NV2> map2(final BiFunction<V1, V2, NV2> fn) { return Pair.of(v1, fn.apply(v1, v2)); }

    /**
     * Execute the supplied consumer on this Pair's values and return this Pair unmodified.
     * Useful for inspections.
     * <pre>{@code
     * pair.peek( (l, r) -> System.out.println("Left: " + l + ", Right: " + r));
     * }</pre>
     */
    public Pair<V1, V2> peek(final BiConsumer<V1, V2> fn) { fn.accept(v1, v2); return this; }
    /**
     * Execute the supplied consumer on this Pair's values and return this Pair unmodified.
     * Useful for inspections.
     * <pre>{@code
     * pair.peek1(l -> System.out.println("Left: " + l));
     * }</pre>
     */
    public Pair<V1, V2> peek1(final Consumer<V1> fn) { fn.accept(v1); return this; }
    /**
     * Execute the supplied consumer on this Pair's values and return this Pair unmodified.
     * Useful for inspections.
     * <pre>{@code
     * pair.peek2(r -> System.out.println("Right: " + r));
     * }</pre>
     */
    public Pair<V1, V2> peek2(final Consumer<V2> fn) { fn.accept(v2); return this; }

    /**
     * Return the result of executing the supplied predicate on this pair's values.
     */
    public boolean filter(final BiPredicate<V1, V2> fn) { return fn.test(v1, v2); }
    /**
     * Return the result of executing the supplied predicate on this pair's first value.
     */
    public boolean filter1(final Predicate<V1> fn) { return fn.test(v1); }
    /**
     * Return the result of executing the supplied predicate on this pair's second value.
     */
    public boolean filter2(final Predicate<V2> fn) { return fn.test(v2); }

    /**
     * Execute the supplied consumer on this Pair's values.
     * Similar to {@link #peek(BiConsumer)}, except no value is returned.
     * Useful to ensure inspections will point out an unused value with peek but not for forEach.
     */
    public void forEach(final BiConsumer<V1, V2> fn) { fn.accept(v1, v2); }

    @Override public int hashCode() { return Objects.hash(v1, v2); }

    @Override public boolean equals(final Object obj) {
        if (obj instanceof Pair) {
            final Pair<?, ?> that = (Pair<?, ?>) obj;
            return Objects.equals(v1, that.v1) && Objects.equals(v2, that.v2);
        }
        return false;
    }

    @Override public String toString() { return "Pair.of(" + v1 + ", " + v2 + ")"; }
}
