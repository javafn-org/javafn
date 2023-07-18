package org.javafn.tuple;

import org.junit.Test;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

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
}
