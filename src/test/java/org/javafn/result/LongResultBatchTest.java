package org.javafn.result;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class LongResultBatchTest {

    @Test
    @DisplayName("empty batch is empty")
    void testEmpty() {
        var batch = LongResultBatch.empty();
        assertFalse(batch.hasErrors());
        assertFalse(batch.hasOks());
        assertEquals(0, batch.size());
        assertTrue(batch.errors().isEmpty());
        assertEquals(0, batch.oks().length);
    }

    @Test
    @DisplayName("batch of all oks captures all oks as array")
    void testOfAllOks() {
        var r1 = LongResult.ok(1L);
        var r2 = LongResult.ok(2L);
        var batch = LongResultBatch.of(r1, r2);

        assertFalse(batch.hasErrors());
        assertTrue(batch.hasOks());
        assertTrue(batch.isOnlyOks());
        assertEquals(2, batch.size());
        assertArrayEquals(new long[]{1L, 2L}, batch.oks());
    }

    @Test
    @DisplayName("batch of all errs captures all errs")
    void testOfAllErrs() {
        var r1 = LongResult.err("err1");
        var r2 = LongResult.err("err2");
        var batch = LongResultBatch.of(r1, r2);

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
        var r1 = LongResult.ok(1L);
        var r2 = LongResult.err("err2");
        var r3 = LongResult.ok(3L);
        var batch = LongResultBatch.of(r1, r2, r3);

        assertTrue(batch.hasErrors());
        assertTrue(batch.hasOks());
        assertTrue(batch.hasBoth());
        assertEquals(3, batch.size());
        assertArrayEquals(new long[]{1L, 3L}, batch.oks());
        assertEquals(1, batch.errors().size());
        assertEquals("err2", batch.errors().get(0).message());
    }

    @Test
    @DisplayName("collector gathers mixed results correctly")
    void testCollector() {
        var stream = Stream.of(
                LongResult.ok(1L),
                LongResult.err("err2"),
                LongResult.ok(3L)
        );
        var batch = stream.collect(LongResultBatch.collector());

        assertTrue(batch.hasBoth());
        assertEquals(3, batch.size());
        assertArrayEquals(new long[]{1L, 3L}, batch.oks());
        assertEquals(1, batch.errors().size());
        assertEquals("err2", batch.errors().get(0).message());
    }

    @Test
    @DisplayName("map transforms oks and errs")
    void testMap() {
        var batch = LongResultBatch.of(
                LongResult.ok(1L),
                LongResult.err("err1")
        );

        var mapped = batch.map(
                err -> AnyError.from("mapped " + err.message()),
                ok -> ok * 10L
        );

        assertArrayEquals(new long[]{10L}, mapped.oks());
        assertEquals("mapped err1", mapped.errors().get(0).message());
    }

    @Test
    @DisplayName("mapErrs transforms only errs")
    void testMapErrs() {
        var batch = LongResultBatch.of(
                LongResult.ok(1L),
                LongResult.err("err1")
        );

        var mapped = batch.mapErrs(err -> AnyError.from("mapped " + err.message()));

        assertArrayEquals(new long[]{1L}, mapped.oks());
        assertEquals("mapped err1", mapped.errors().get(0).message());
    }

    @Test
    @DisplayName("mapOks transforms only oks")
    void testMapOks() {
        var batch = LongResultBatch.of(
                LongResult.ok(1L),
                LongResult.err("err1")
        );

        var mapped = batch.mapOks(ok -> ok * 10L);

        assertArrayEquals(new long[]{10L}, mapped.oks());
        assertEquals("err1", mapped.errors().get(0).message());
    }

    @Test
    @DisplayName("mapOksToObjs transforms oks to object list")
    void testMapOksToObjs() {
        var batch = LongResultBatch.of(
                LongResult.ok(1L),
                LongResult.err("err1")
        );

        var mapped = batch.mapOksToObjs(ok -> "Val: " + ok);

        assertEquals(List.of("Val: 1"), mapped.oks());
        assertEquals("err1", mapped.errors().get(0).message());
    }

    @Test
    @DisplayName("fold of only oks produces ok result with array")
    void testFoldOnlyOks() {
        var batch = LongResultBatch.of(LongResult.ok(1L), LongResult.ok(2L));
        var folded = batch.fold();

        assertTrue(folded.isOk());
        assertArrayEquals(new long[]{1L, 2L}, folded.expectOk());
    }

    @Test
    @DisplayName("fold of only errs produces joined err result")
    void testFoldOnlyErrs() {
        var batch = LongResultBatch.of(LongResult.err("err1"), LongResult.err("err2"));
        var folded = batch.fold();

        assertTrue(folded.isErr());
        assertEquals("err1\nerr2", folded.expectErr().message());
    }

    @Test
    @DisplayName("fold of mixed results produces err result")
    void testFoldMixed() {
        var batch = LongResultBatch.of(LongResult.ok(1L), LongResult.err("err2"));
        var folded = batch.fold();

        assertTrue(folded.isErr());
        assertEquals("err2", folded.expectErr().message());
    }

    @Test
    @DisplayName("reduce aggregates oks and errs")
    void testReduce() {
        var batch = LongResultBatch.of(LongResult.ok(1L), LongResult.err("err2"));
        var reduced = batch.reduce((errs, oks) -> "Errors: " + errs.size() + ", Oks: " + oks.length);

        assertEquals("Errors: 1, Oks: 1", reduced);
    }

    @Test
    @DisplayName("consume processes oks and errs")
    void testConsume() {
        var batch = LongResultBatch.of(LongResult.ok(1L), LongResult.err("err2"));
        final int[] count = {0};
        batch.consume((errs, oks) -> count[0] = errs.size() + oks.length);

        assertEquals(2, count[0]);
    }

    @Test
    @DisplayName("append combines two batches")
    void testAppend() {
        var batch1 = LongResultBatch.of(LongResult.ok(1L));
        var batch2 = LongResultBatch.of(LongResult.err("err2"));
        var combined = batch1.append(batch2);

        assertEquals(2, combined.size());
        assertArrayEquals(new long[]{1L}, combined.oks());
        assertEquals(1, combined.errors().size());
        assertEquals("err2", combined.errors().get(0).message());
    }

    @Test
    @DisplayName("append null returns original batch")
    void testAppendNull() {
        var batch1 = LongResultBatch.of(LongResult.ok(1L));
        var combined = batch1.append(null);

        assertEquals(batch1, combined);
    }
}
