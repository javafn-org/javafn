package org.javafn.tuple;

import org.junit.Test;

import java.util.UUID;
import java.util.stream.IntStream;

public class IdxTest {

	@Test
	public void testIndexing() {
		IntStream.range(0, 100).mapToObj(i -> UUID.randomUUID())
				.map(Idx.thisStream())
				.forEach(idx -> System.out.printf("String value %d is mapped to index %d.%n", idx.val().getMostSignificantBits(), idx.i()));
	}
}
