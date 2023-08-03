package org.javafn.tuple;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;

public class IdxTest {


	@Test
	public void testCorrectIndexEnumerateInline() {
		final int numItems = 100;
		final List<Idx<UUID>> idxList =  Stream.generate(UUID::randomUUID).limit(numItems)
				.map(Idx.enumerate())
				.collect(Collectors.toList());
		for (int i = 0; i < numItems; i++) {
			assertEquals("Expected the list index and Idx.i() values to be the same",
					i, idxList.get(i).i());
		}
	}

	@Test
	public void testCorrectIndexEnumerateWrapper() {
		final int numItems = 100;
		final List<Idx<UUID>> idxList =  Idx.enumerate(
				Stream.generate(UUID::randomUUID).limit(numItems))
				.collect(Collectors.toList());
		for (int i = 0; i < numItems; i++) {
			assertEquals("Expected the list index and Idx.i() values to be the same",
					i, idxList.get(i).i());
		}
	}

	@Test
	public void testCorrectIndexMapValOnly() {
		final int numItems = 100;
		final List<Idx<String>> idxList =  Stream.generate(UUID::randomUUID).limit(numItems)
				.map(Idx.enumerate())
				.map(Idx.Map(UUID::toString))
				.collect(Collectors.toList());
		for (int i = 0; i < numItems; i++) {
			assertEquals("Expected the list index and Idx.i() values to be the same",
					i, idxList.get(i).i());
		}
	}
	@Test
	public void testCorrectIndexMapBoth() {
		final int numItems = 100;
		final List<Idx<String>> idxList =  Stream.generate(UUID::randomUUID).limit(numItems)
				.map(Idx.enumerate())
				.map(Idx.Map( (i, val) -> val.toString()))
				.collect(Collectors.toList());
		for (int i = 0; i < numItems; i++) {
			assertEquals("Expected the list index and Idx.i() values to be the same",
					i, idxList.get(i).i());
		}
	}

	@Test
	public void testParallel() {
		final int numItems = 1000;
		final int[] expected = IntStream.range(0, numItems).toArray();
		{
			final int[] actual = Stream.generate(UUID::randomUUID).limit(numItems)
					.map(Idx.enumerateParallel())
					.parallel()
					.mapToInt(Idx::i)
					.toArray();
			Arrays.sort(actual);
			assertArrayEquals("Expected the arrays to be equal; using map inline", expected, actual);
		}
		{
			final int[] actual = Idx.enumerateParallel(
					Stream.generate(UUID::randomUUID).limit(numItems))
					.parallel()
					.mapToInt(Idx::i)
					.toArray();
			Arrays.sort(actual);
			assertArrayEquals("Expected the arrays to be equal; using static stream wrapper", expected, actual);
		}
	}
}
