package org.javafn.result;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class IntResultBatchTest {

    @Test
    @DisplayName("empty batch is empty")
    void testEmpty() {
        var batch = IntResultBatch.empty();
        assertFalse(batch.hasErrors());
        assertFalse(batch.hasOks());
        assertEquals(0, batch.size());
        assertTrue(batch.errors().isEmpty());
        assertEquals(0, batch.oks().length);
    }

    @Test
    @DisplayName("batch of all oks captures all oks as array")
    void testOfAllOks() {
        var r1 = IntResult.ok(1);
        var r2 = IntResult.ok(2);
        var batch = IntResultBatch.of(r1, r2);

        assertFalse(batch.hasErrors());
        assertTrue(batch.hasOks());
        assertTrue(batch.isOnlyOks());
        assertEquals(2, batch.size());
        assertArrayEquals(new int[]{1, 2}, batch.oks());
    }

    @Test
    @DisplayName("batch of all errs captures all errs")
    void testOfAllErrs() {
        var r1 = IntResult.err("err1"); // IntResult.err actually returns IntResult, so this is fine
        var r2 = IntResult.err("err2");
        var batch = IntResultBatch.of(r1, r2);

        assertTrue(batch.hasErrors());
        assertFalse(batch.hasOks());
        assertTrue(batch.isOnlyErrors());
        assertEquals(2, batch.size());
        assertEquals(2, batch.errors().size());
        assertEquals("err1", batch.errors().get(0).message());
        assertEquals("err2", batch.errors().get(1).message());
    }

    @Test
    @DisplayName("mixed batch captures both oks and errs")
    void testOfMixed() {
        var r1 = IntResult.ok(1);
        var r2 = IntResult.err("err2");
        var r3 = IntResult.ok(3);
        var batch = IntResultBatch.of(r1, r2, r3);

        assertTrue(batch.hasErrors());
        assertTrue(batch.hasOks());
        assertTrue(batch.hasBoth());
        assertEquals(3, batch.size());
        assertArrayEquals(new int[]{1, 3}, batch.oks());
        assertEquals(1, batch.errors().size());
        assertEquals("err2", batch.errors().get(0).message());
    }

    @Test
    @DisplayName("collector gathers mixed results correctly")
    void testCollector() {
        var stream = Stream.of(
                IntResult.ok(1),
                IntResult.err("err2"),
                IntResult.ok(3)
        );
        var batch = stream.collect(IntResultBatch.collector());

        assertTrue(batch.hasBoth());
        assertEquals(3, batch.size());
        assertArrayEquals(new int[]{1, 3}, batch.oks());
        assertEquals(1, batch.errors().size());
        assertEquals("err2", batch.errors().get(0).message());
    }

    @Test
    @DisplayName("map transforms oks and errs")
    void testMap() {
        var batch = IntResultBatch.of(
                IntResult.ok(1),
                IntResult.err("err1")
        );

        var mapped = batch.map(
                err -> AnyError.from("mapped " + err.message()),
                ok -> ok * 10
        );

        assertArrayEquals(new int[]{10}, mapped.oks());
        assertEquals("mapped err1", mapped.errors().get(0).message());
    }

    @Test
    @DisplayName("mapErrs transforms only errs")
    void testMapErrs() {
        var batch = IntResultBatch.of(
                IntResult.ok(1),
                IntResult.err("err1")
        );

        var mapped = batch.mapErrs(err -> AnyError.from("mapped " + err.message()));

        assertArrayEquals(new int[]{1}, mapped.oks());
        assertEquals("mapped err1", mapped.errors().get(0).message());
    }

    @Test
    @DisplayName("mapOks transforms only oks")
    void testMapOks() {
        var batch = IntResultBatch.of(
                IntResult.ok(1),
                IntResult.err("err1")
        );

        var mapped = batch.mapOks(ok -> ok * 10);

        assertArrayEquals(new int[]{10}, mapped.oks());
        assertEquals("err1", mapped.errors().get(0).message());
    }

    @Test
    @DisplayName("mapOksToObjs transforms oks to object list")
    void testMapOksToObjs() {
        var batch = IntResultBatch.of(
                IntResult.ok(1),
                IntResult.err("err1")
        );

        var mapped = batch.mapOksToObjs(ok -> "Val: " + ok);

        assertEquals(List.of("Val: 1"), mapped.oks());
        assertEquals("err1", mapped.errors().get(0).message());
    }

    @Test
    @DisplayName("fold of only oks produces ok result with array")
    void testFoldOnlyOks() {
        var batch = IntResultBatch.of(IntResult.ok(1), IntResult.ok(2));
        var folded = batch.fold();

        assertTrue(folded.isOk());
        assertArrayEquals(new int[]{1, 2}, folded.expectOk());
    }

    @Test
    @DisplayName("fold of only errs produces joined err result")
    void testFoldOnlyErrs() {
        var batch = IntResultBatch.of(IntResult.err("err1"), IntResult.err("err2"));
        var folded = batch.fold();

        assertTrue(folded.isErr());
        assertEquals("err1\nerr2", folded.expectErr().message());
    }

    @Test
    @DisplayName("fold of mixed results produces err result")
    void testFoldMixed() {
        var batch = IntResultBatch.of(IntResult.ok(1), IntResult.err("err2"));
        var folded = batch.fold();

        assertTrue(folded.isErr());
        assertEquals("err2", folded.expectErr().message());
    }

    @Test
    @DisplayName("reduce aggregates oks and errs")
    void testReduce() {
        var batch = IntResultBatch.of(IntResult.ok(1), IntResult.err("err2"));
        var reduced = batch.reduce((errs, oks) -> "Errors: " + errs.size() + ", Oks: " + oks.length);

        assertEquals("Errors: 1, Oks: 1", reduced);
    }

    @Test
    @DisplayName("consume processes oks and errs")
    void testConsume() {
        var batch = IntResultBatch.of(IntResult.ok(1), IntResult.err("err2"));
        final int[] count = {0};
        batch.consume((errs, oks) -> count[0] = errs.size() + oks.length);

        assertEquals(2, count[0]);
    }

    @Test
    @DisplayName("append combines two batches")
    void testAppend() {
        var batch1 = IntResultBatch.of(IntResult.ok(1));
        var batch2 = IntResultBatch.of(IntResult.err("err2"));
        var combined = batch1.append(batch2);

        assertEquals(2, combined.size());
        assertArrayEquals(new int[]{1}, combined.oks());
        assertEquals(1, combined.errors().size());
        assertEquals("err2", combined.errors().get(0).message());
    }

    @Test
    @DisplayName("append null returns original batch")
    void testAppendNull() {
        var batch1 = IntResultBatch.of(IntResult.ok(1));
        var combined = batch1.append(null);

        assertEquals(batch1, combined);
    }
}
