package org.javafn.tuple;

import org.junit.Test;

import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class PairTest {

    @Test public void testConstruction() {
        final UUID uuid = UUID.randomUUID();
        final String string = UUID.randomUUID().toString();
        final Pair<UUID, String> pair = Pair.of(uuid, string);
        assertEquals("Expecting the first element to be the assigned uuid",
                uuid, pair._1());
        assertEquals("Expecting the second element to be the assigned string",
                string, pair._2());
    }

    @Test public void testEqualityAndHash() {
        // Construct the pair values from the string so that we get new objects, i.e., don't test reference equality
        final String s1 = UUID.randomUUID().toString();
        final String s2 = UUID.randomUUID().toString();
        final Pair<UUID, UUID> pair1 = Pair.of(UUID.fromString(s1), UUID.fromString(s2));
        final Pair<UUID, UUID> pair2 = Pair.of(UUID.fromString(s1), UUID.fromString(s2));
        assertEquals("Expected pairs from equal objects to be equal", pair1, pair2);
        assertEquals("Expected equal pairs to return the same hash", pair1.hashCode(), pair2.hashCode());
    }

    @Test public void testPartition() {
        final Random random = new Random();
        final Stream<Integer> source = IntStream.generate(() -> random.nextInt(100)).boxed().limit(100);
        final Pair<Stream<Integer>, Stream<Integer>> split = Pair.partition(source, i -> i > 50);
        assertEquals("Expecting 0 elements less than or equal to 50 in the first partition",
                0, split._1().filter(i -> i <= 50).count());
        assertEquals("Expecting 0 elements greater than 50 in the second partition",
                0, split._2().filter(i -> i > 50).count());
    }

    @Test public void testZip() {
        final int numElements = 100;
        final List<Pair<Integer, Long>> zipped = Pair.zip(
                IntStream.range(0, numElements).boxed(), LongStream.range(0, numElements).boxed())
                .collect(Collectors.toList());
        assertEquals("Wrong numer of elements", numElements, zipped.size());
        assertEquals("All elements should be equal",
                numElements, zipped.stream().map(Pair.Map1(i -> Long.valueOf(i))).filter(Pair.Filter(Objects::equals)).count());
    }

    @Test public void testZipWithDifferentLengths() {
        final int numElements = 100;
        assertEquals("Wrong numer of elements", numElements, Pair.zip(
                IntStream.range(0, numElements).boxed(),
                LongStream.range(0, numElements * 2).boxed())
                .count());
        assertEquals("Wrong numer of elements", numElements, Pair.zip(
                        IntStream.range(0, numElements * 2).boxed(),
                        LongStream.range(0, numElements).boxed())
                .count());
    }

    @Test public void testForEachAndPeek() {
        class InternalMutability {
            boolean mutated = false;
        }
        {
            final InternalMutability v1 = new InternalMutability();
            final InternalMutability v2 = new InternalMutability();
            final Pair<InternalMutability, InternalMutability> pair = Pair.of(v1, v2);
            pair.forEach((value1, value2) -> {
                value1.mutated = true;
                value2.mutated = true;
            });
            assertTrue("Expected v1 to be passed to the forEach method", v1.mutated);
            assertTrue("Expected v2 to be passed to the forEach method", v2.mutated);
        }
        {
            final InternalMutability v1 = new InternalMutability();
            final InternalMutability v2 = new InternalMutability();
            final Pair<InternalMutability, InternalMutability> pair = Pair.of(v1, v2);
            final Pair<InternalMutability, InternalMutability> peekedAt = pair.peek((value1, value2) -> {
                value1.mutated = true;
                value2.mutated = true;
            });
            assertTrue("Expected v1 to be passed to the peek method", v1.mutated);
            assertTrue("Expected v2 to be passed to the peek method", v2.mutated);
            assertEquals("Expecting the returned pair to be the same one that was peeked at",
                    pair, peekedAt);
        }
        {
            final InternalMutability v1 = new InternalMutability();
            final InternalMutability v2 = new InternalMutability();
            final Pair<InternalMutability, InternalMutability> pair = Pair.of(v1, v2);
            final Pair<InternalMutability, InternalMutability> peekedAt = pair.peek1(v -> v.mutated = true);
            assertTrue("Expected v1 to be passed to the peek1 method", v1.mutated);
            assertFalse("Expected v2 to not be passed to the peek1 method", v2.mutated);
            assertEquals("Expecting the returned pair to be the same one that was peeked at",
                    pair, peekedAt);
        }
        {
            final InternalMutability v1 = new InternalMutability();
            final InternalMutability v2 = new InternalMutability();
            final Pair<InternalMutability, InternalMutability> pair = Pair.of(v1, v2);
            final Pair<InternalMutability, InternalMutability> peekedAt = pair.peek2(v -> v.mutated = true);
            assertFalse("Expected v1 to not be passed to the peek2 method", v1.mutated);
            assertTrue("Expected v2 to be passed to the peek2 method", v2.mutated);
            assertEquals("Expecting the returned pair to be the same one that was peeked at",
                    pair, peekedAt);
        }
    }

    @Test public void testFilter() {
        final Pair<Integer, Integer> pair = Pair.of(0, 42);
        assertTrue("Expecting pair.filter((a,b) -> a + b == 42) to return true",
                pair.filter((a, b) -> a + b == 42));
        assertFalse("Expecting pair.filter((a,b) -> a - b == 42) to return false",
                pair.filter((a, b) -> a - b == 42));
        assertTrue("Expecting pair.filter1(a -> a == 0) to return true",
                pair.filter1(a -> a == 0));
        assertFalse("Expecting pair.filter1(a -> a == 42) to return false",
                pair.filter1(a -> a == 42));
        assertTrue("Expecting pair.filter2(a -> a == 42) to return true",
                pair.filter2(a -> a == 42));
        assertFalse("Expecting pair.filter2(a -> a == 0) to return false",
                pair.filter2(a -> a == 0));
    }

    @Test public void testMap() {
        final String s = "Forty-Two";
        final Pair<Integer, String> orig = Pair.of(42, s);
        assertEquals("Expecting map1 to have affected the first element",
                Pair.of(-42, s), orig.map1( i -> i - 84));
        assertEquals("Expecting map1 to have affected the first element",
                Pair.of(51, s), orig.map1((i, v) -> i + s.length()));
        assertEquals("Expecting map2 to have affected the second element",
                Pair.of(42, s + "!"), orig.map2( v -> v + "!"));
        assertEquals("Expecting map2 to have affected the second element",
                Pair.of(42,  s + "42"), orig.map2((i, v) -> v + i));
        assertEquals("Expected map to have produced a new element",
                42 + s, orig.map((i, v) -> i + v));
    }

    @Test public void testStream() {
        final UUID a = UUID.randomUUID();
        final UUID b = UUID.randomUUID();
        final List<UUID> collected = Pair.stream(Pair.of(a, b)).collect(Collectors.toList());
        assertEquals("Expected exactly two elements", 2, collected.size());
        assertEquals("Expected the first collected element to be the first pair element",
                a, collected.get(0));
        assertEquals("Expected the second collected element to be the second pair element",
                b, collected.get(1));
    }

    @Test public void testChunkedObject() {
        final int len = 100;
        final UUID[] uuidArray = IntStream.range(0, len).mapToObj(i -> UUID.randomUUID()).toArray(UUID[]::new);
        final List<Pair<UUID, UUID>> pairs = Pair.chunks(uuidArray).collect(Collectors.toList());
        assertEquals("Expecting the number of pairs to be exactly half the number of original items.",
                len / 2, pairs.size());
        Pair.zip(IntStream.range(0, len/2).map(i -> i * 2).boxed(), pairs.stream())
                .forEach(Pair.ForEach((i, pair) -> {
                    assertEquals(uuidArray[i], pair._1());
                    assertEquals(uuidArray[i+1], pair._2());
        }));
    }
    @Test public void testChunkedDouble() {
        final Random random = new Random();
        final int len = 20;
        final double[] doubleArray = DoubleStream.generate(random::nextDouble).limit(len).toArray();
        final List<Pair<Double, Double>> pairs = Pair.chunks(doubleArray).collect(Collectors.toList());
        assertEquals("Expecting the number of pairs to be exactly half the number of original items.",
                len / 2, pairs.size());
        Pair.zip(IntStream.range(0, len/2).map(i -> i * 2).boxed(), pairs.stream())
                .forEach(Pair.ForEach((i, pair) -> {
                    assertEquals(Double.valueOf(doubleArray[i]), pair._1());
                    assertEquals(Double.valueOf(doubleArray[i+1]), pair._2());
                }));
    }
    @Test public void testChunkedLong() {
        final Random random = new Random();
        final int len = 20;
        final long[] longArray = LongStream.generate(random::nextLong).limit(len).toArray();
        final List<Pair<Long, Long>> pairs = Pair.chunks(longArray).collect(Collectors.toList());
        assertEquals("Expecting the number of pairs to be exactly half the number of original items.",
                len / 2, pairs.size());
        Pair.zip(IntStream.range(0, len/2).map(i -> i * 2).boxed(), pairs.stream())
                .forEach(Pair.ForEach((i, pair) -> {
                    assertEquals(Long.valueOf(longArray[i]), pair._1());
                    assertEquals(Long.valueOf(longArray[i+1]), pair._2());
                }));
    }
    @Test public void testChunkedInt() {
        final Random random = new Random();
        final int len = 20;
        final int[] intArray = IntStream.generate(random::nextInt).limit(len).toArray();
        final List<Pair<Integer, Integer>> pairs = Pair.chunks(intArray).collect(Collectors.toList());
        assertEquals("Expecting the number of pairs to be exactly half the number of original items.",
                len / 2, pairs.size());
        Pair.zip(IntStream.range(0, len/2).map(i -> i * 2).boxed(), pairs.stream())
                .forEach(Pair.ForEach((i, pair) -> {
                    assertEquals(Integer.valueOf(intArray[i]), pair._1());
                    assertEquals(Integer.valueOf(intArray[i+1]), pair._2());
                }));
    }

    @Test public void testChunkedObjectOdd() {
        final Object o = new Object();
        final List<Pair<Object, Object>> s = Pair.chunks(new Object[] { o }).collect(Collectors.toList());
        assertEquals("Expecting exactly one pair to be generated from a single item", 1, s.size());
        final Pair<Object, Object> pair = s.get(0);
        assertEquals("Expecting the first pair element to be the object in the array.", o, pair._1());
        assertNull("Expecting the second pair element to be null", pair._2());
    }
    @Test public void testChunkedDoubleOdd() {
        final double[] d = new double[] { 0.0, 1.1, 2.2 };
        final List<Pair<Double, Double>> s = Pair.chunks(d).collect(Collectors.toList());
        assertEquals("Expecting exactly two pairs to be generated from three items", 2, s.size());
        final Pair<Double, Double> pair = s.get(1);
        assertEquals("Expecting the first pair element to be the last item from the array.", new Double(d[2]), pair._1());
        assertNull("Expecting the second pair element to be null", pair._2());
    }
    @Test public void testChunkedLongOdd() {
        final long[] longs = new long[] { 0L, 1L, 2L, 3L, 4L };
        final List<Pair<Long, Long>> s = Pair.chunks(longs).collect(Collectors.toList());
        assertEquals("Expecting exactly three pairs to be generated from five items", 3, s.size());
        final Pair<Long, Long> pair = s.get(2);
        assertEquals("Expecting the first pair element to be the last item from the array.", new Long(longs[4]), pair._1());
        assertNull("Expecting the second pair element to be null", pair._2());
    }
    @Test public void testChunkedIntOdd() {
        final int[] ints = new int[] { 0, 1, 2, 3, 4 };
        final List<Pair<Integer, Integer>> s = Pair.chunks(ints).collect(Collectors.toList());
        assertEquals("Expecting exactly three pairs to be generated from five items", 3, s.size());
        final Pair<Integer, Integer> pair = s.get(2);
        assertEquals("Expecting the first pair element to be the last item from the array.", new Integer(ints[4]), pair._1());
        assertNull("Expecting the second pair element to be null", pair._2());
    }

    @Test public void testWindowObj() {
        final int len = 20;
        final UUID[] uuidArray = IntStream.range(0, len).mapToObj(i -> UUID.randomUUID()).toArray(UUID[]::new);
        final List<Pair<UUID, UUID>> pairs = Pair.windows(uuidArray).collect(Collectors.toList());
        assertEquals("Expecting the number of windows to be 1 less than the original array size",
                len - 1, pairs.size());
        Pair.zip(IntStream.range(0, len - 1).boxed(), pairs.stream())
                .forEach(Pair.ForEach((i, pair) -> {
                    assertEquals(uuidArray[i], pair._1());
                    assertEquals(uuidArray[i+1], pair._2());
                }));
    }
    @Test public void testWindowDouble() {
        final Random random = new Random();
        final int len = 20;
        final double[] doubleArray = DoubleStream.generate(random::nextDouble).limit(len).toArray();
        final List<Pair<Double, Double>> pairs = Pair.windows(doubleArray).collect(Collectors.toList());
        assertEquals("Expecting the number of windows to be 1 less than the original array size",
                len - 1, pairs.size());
        Pair.zip(IntStream.range(0, len - 1).boxed(), pairs.stream())
                .forEach(Pair.ForEach((i, pair) -> {
                    assertEquals(Double.valueOf(doubleArray[i]), pair._1());
                    assertEquals(Double.valueOf(doubleArray[i+1]), pair._2());
                }));
    }
    @Test public void testWindowLong() {
        final Random random = new Random();
        final int len = 20;
        final long[] longArray = LongStream.generate(random::nextLong).limit(len).toArray();
        final List<Pair<Long, Long>> pairs = Pair.windows(longArray).collect(Collectors.toList());
        assertEquals("Expecting the number of windows to be 1 less than the original array size",
                len - 1, pairs.size());
        Pair.zip(IntStream.range(0, len - 1).boxed(), pairs.stream())
                .forEach(Pair.ForEach((i, pair) -> {
                    assertEquals(Long.valueOf(longArray[i]), pair._1());
                    assertEquals(Long.valueOf(longArray[i+1]), pair._2());
                }));
    }
    @Test public void testWindowInt() {
        final Random random = new Random();
        final int len = 20;
        final int[] intArray = IntStream.generate(random::nextInt).limit(len).toArray();
        final List<Pair<Integer, Integer>> pairs = Pair.windows(intArray).collect(Collectors.toList());
        assertEquals("Expecting the number of windows to be 1 less than the original array size",
                len - 1, pairs.size());
        Pair.zip(IntStream.range(0, len - 1).boxed(), pairs.stream())
                .forEach(Pair.ForEach((i, pair) -> {
                    assertEquals(Integer.valueOf(intArray[i]), pair._1());
                    assertEquals(Integer.valueOf(intArray[i+1]), pair._2());
                }));
    }
}
