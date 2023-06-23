package org.javafn.result;

import org.junit.Test;

import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ResultCollectionTest {

	@Test
	public void testSingletonResultCollectionErr() {
		final ResultCollection<String, UUID> singletonErr = ResultCollection.singleton(Result.err("This is an err"));
		assertTrue("Expecting the error singleton to have errors.", singletonErr.hasErrs());
		assertFalse("Expecting the error singleton to not have oks.", singletonErr.hasOks());
		assertFalse("Expecting the error singleton to not have both.", singletonErr.hasBoth());
		assertEquals("Expecting the singleton error to have only one error.", 1, singletonErr.getErrs().size());
	}

	@Test public void testSingletonResultCollectionOk() {
		final ResultCollection<String, UUID> singletonOk = ResultCollection.singleton(Result.ok(UUID.randomUUID()));
		assertTrue("Expecting the ok singleton to have oks.", singletonOk.hasOks());
		assertFalse("Expecting the ok singleton to not have errs.", singletonOk.hasErrs());
		assertFalse("Expecting the ok singleton to not have both.", singletonOk.hasBoth());
		assertEquals("Expecting the singleton oks to have only one ok.", 1, singletonOk.getOks().size());
	}

	@Test public void testMultipleResultCollectionErr() {
		final int numErrs = 10;
		final ResultCollection<String, UUID> errResults = IntStream.range(0, numErrs)
				.mapToObj(i -> Result.<String, UUID>err(Integer.toString(i)))
				.collect(Result.collector());
		assertTrue("Expecting only err results", errResults.hasErrs());
		assertFalse("Expecting only err results", errResults.hasOks());
		assertFalse("Expecting only err results", errResults.hasBoth());
		assertEquals("Expecting the number of generated errors to have been collected.",
				numErrs, errResults.getErrs().size());
	}

	@Test public void testMultipleResultCollectionOk() {
		final int numOks = 10;
		final ResultCollection<String, UUID> okResults = IntStream.range(0, numOks)
				.mapToObj(ignored -> Result.<String, UUID>ok(UUID.randomUUID()))
				.collect(Result.collector());
		assertTrue("Expecting only ok results", okResults.hasOks());
		assertFalse("Expecting only ok results", okResults.hasErrs());
		assertFalse("Expecting only ok results", okResults.hasBoth());
		assertEquals("Expecting the number of generated oks to have been collected.",
				numOks, okResults.getOks().size());
	}

	@Test public void testMultipleResultCollectionBoth() {
		final int numBoth = 10;
		final ResultCollection<String, UUID> okResults = IntStream.range(0, numBoth * 2)
				.mapToObj(i -> i % 2 == 0
						? Result.<String, UUID>err(Integer.toString(i))
						: Result.<String, UUID>ok(UUID.randomUUID()))
				.collect(Result.collector());
		assertTrue("Expecting ok results", okResults.hasOks());
		assertTrue("Expecting err results", okResults.hasErrs());
		assertTrue("Expecting both ok and err results", okResults.hasBoth());
		assertEquals("Expecting the number of generated errs to have been collected.",
				numBoth, okResults.getErrs().size());
		assertEquals("Expecting the number of generated oks to have been collected.",
				numBoth, okResults.getOks().size());
	}
}
