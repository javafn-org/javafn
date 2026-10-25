package org.javafn.result;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class ResultBatchTest {

    @Test
    @org.junit.jupiter.api.DisplayName("empty batch is empty")
    void testEmpty() {
        var batch = ResultBatch.<String>empty();
        assertFalse(batch.hasErrors());
        assertFalse(batch.hasOks());
        assertEquals(0, batch.size());
        assertTrue(batch.errors().isEmpty());
        assertTrue(batch.oks().isEmpty());
    }

    @Test
    @org.junit.jupiter.api.DisplayName("batch of all oks captures all oks")
    void testOfAllOks() {
        var r1 = Result.ok("one");
        var r2 = Result.ok("two");
        var batch = ResultBatch.of(r1, r2);

        assertFalse(batch.hasErrors());
        assertTrue(batch.hasOks());
        assertTrue(batch.isOnlyOks());
        assertEquals(2, batch.size());
        assertEquals(List.of("one", "two"), batch.oks());
    }

    @Test
    @org.junit.jupiter.api.DisplayName("batch of all errs captures all errs")
    void testOfAllErrs() {
        var r1 = Result.<String>err("err1");
        var r2 = Result.<String>err("err2");
        var batch = ResultBatch.of(r1, r2);

        assertTrue(batch.hasErrors());
        assertFalse(batch.hasOks());
        assertTrue(batch.isOnlyErrors());
        assertEquals(2, batch.size());
        assertEquals(2, batch.errors().size());
        assertEquals("err1", batch.errors().get(0).message());
        assertEquals("err2", batch.errors().get(1).message());
    }

    @Test
    @org.junit.jupiter.api.DisplayName("mixed batch captures both oks and errs")
    void testOfMixed() {
        var r1 = Result.ok("ok1");
        var r2 = Result.<String>err("err2");
        var r3 = Result.ok("ok3");
        var batch = ResultBatch.of(r1, r2, r3);

        assertTrue(batch.hasErrors());
        assertTrue(batch.hasOks());
        assertTrue(batch.hasBoth());
        assertEquals(3, batch.size());
        assertEquals(List.of("ok1", "ok3"), batch.oks());
        assertEquals(1, batch.errors().size());
        assertEquals("err2", batch.errors().get(0).message());
    }

    @Test
    @org.junit.jupiter.api.DisplayName("collector gathers mixed results correctly")
    void testCollector() {
        var stream = Stream.of(
                Result.ok("ok1"),
                Result.<String>err("err2"),
                Result.ok("ok3")
        );
        var batch = stream.collect(ResultBatch.collector());

        assertTrue(batch.hasBoth());
        assertEquals(3, batch.size());
        assertEquals(List.of("ok1", "ok3"), batch.oks());
        assertEquals(1, batch.errors().size());
        assertEquals("err2", batch.errors().get(0).message());
    }

    @Test
    @org.junit.jupiter.api.DisplayName("map transforms oks and errs")
    void testMap() {
        var batch = ResultBatch.of(
                Result.ok("ok1"),
                Result.err("err1")
        );

        var mapped = batch.map(
                err -> AnyError.from("mapped " + err.message()),
                String::toUpperCase
        );

        assertEquals(List.of("OK1"), mapped.oks());
        assertEquals("mapped err1", mapped.errors().get(0).message());
    }

    @Test
    @org.junit.jupiter.api.DisplayName("mapErrs transforms only errs")
    void testMapErrs() {
        var batch = ResultBatch.of(
                Result.ok("ok1"),
                Result.err("err1")
        );

        var mapped = batch.mapErrs(err -> AnyError.from("mapped " + err.message()));

        assertEquals(List.of("ok1"), mapped.oks());
        assertEquals("mapped err1", mapped.errors().get(0).message());
    }

    @Test
    @org.junit.jupiter.api.DisplayName("mapOks transforms only oks")
    void testMapOks() {
        var batch = ResultBatch.of(
                Result.ok("ok1"),
                Result.err("err1")
        );

        var mapped = batch.mapOks(String::toUpperCase);

        assertEquals(List.of("OK1"), mapped.oks());
        assertEquals("err1", mapped.errors().get(0).message());
    }

    @Test
    @org.junit.jupiter.api.DisplayName("fold of only oks produces ok result with list")
    void testFoldOnlyOks() {
        var batch = ResultBatch.of(Result.ok("ok1"), Result.ok("ok2"));
        var folded = batch.fold();

        assertTrue(folded.isOk());
        assertEquals(List.of("ok1", "ok2"), folded.expectOk());
    }

    @Test
    @org.junit.jupiter.api.DisplayName("fold of only errs produces joined err result")
    void testFoldOnlyErrs() {
        var batch = ResultBatch.of(Result.err("err1"), Result.<String>err("err2"));
        var folded = batch.fold();

        assertTrue(folded.isErr());
        assertEquals("err1\nerr2", folded.expectErr().message());
    }

    @Test
    @org.junit.jupiter.api.DisplayName("fold of mixed results produces err result")
    void testFoldMixed() {
        var batch = ResultBatch.of(Result.ok("ok1"), Result.err("err2"));
        var folded = batch.fold();

        assertTrue(folded.isErr());
        assertEquals("err2", folded.expectErr().message());
    }

    @Test
    @org.junit.jupiter.api.DisplayName("reduce aggregates oks and errs")
    void testReduce() {
        var batch = ResultBatch.of(Result.ok("ok1"), Result.err("err2"));
        var reduced = batch.reduce((errs, oks) -> "Errors: " + errs.size() + ", Oks: " + oks.size());

        assertEquals("Errors: 1, Oks: 1", reduced);
    }

    @Test
    @org.junit.jupiter.api.DisplayName("consume processes oks and errs")
    void testConsume() {
        var batch = ResultBatch.of(Result.ok("ok1"), Result.err("err2"));
        final int[] count = {0};
        batch.consume((errs, oks) -> count[0] = errs.size() + oks.size());

        assertEquals(2, count[0]);
    }

    @Test
    @org.junit.jupiter.api.DisplayName("append combines two batches")
    void testAppend() {
        var batch1 = ResultBatch.of(Result.ok("ok1"));
        var batch2 = ResultBatch.of(Result.<String>err("err2"));
        var combined = batch1.append(batch2);

        assertEquals(2, combined.size());
        assertEquals(List.of("ok1"), combined.oks());
        assertEquals(1, combined.errors().size());
        assertEquals("err2", combined.errors().get(0).message());
    }

    @Test
    @org.junit.jupiter.api.DisplayName("append null returns original batch")
    void testAppendNull() {
        var batch1 = ResultBatch.of(Result.ok("ok1"));
        var combined = batch1.append(null);

        assertEquals(batch1, combined);
    }
}
