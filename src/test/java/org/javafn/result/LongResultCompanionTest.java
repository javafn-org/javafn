package org.javafn.result;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import static org.junit.jupiter.api.Assertions.*;

class LongResultCompanionTest {

    @Test
    @DisplayName("LongResult.Oks.map delegates to LongResult.map")
    void testOksMap() {
        var res = LongResult.ok(123L);
        Function<LongResult, LongResult> mapper = LongResult.Oks.map(v -> v * 2);
        var result = mapper.apply(res);
        assertEquals(246L, result.expectOk());
    }

    @Test
    @DisplayName("LongResult.Oks.filter delegates to LongResult.filter")
    void testOksFilter() {
        var resOk = LongResult.ok(123L);
        var resErr = LongResult.err("fail");
        Predicate<LongResult> filter = LongResult.Oks.filter(v -> v > 100L);
        
        assertTrue(filter.test(resOk));
        assertTrue(filter.test(resErr)); // LongResult.filter returns true for Err
    }

    @Test
    @DisplayName("LongResult.Oks.isOkAnd delegates to LongResult.isOkAnd")
    void testOksIsOkAnd() {
        var resOk = LongResult.ok(123L);
        var resErr = LongResult.err("fail");
        Predicate<LongResult> isOkAnd = LongResult.Oks.isOkAnd(v -> v > 100L);
        
        assertTrue(isOkAnd.test(resOk));
        assertFalse(isOkAnd.test(resErr));
    }

    @Test
    @DisplayName("LongResult.Oks.failIf delegates to LongResult.failIf")
    void testOksFailIf() {
        var res = LongResult.ok(123L);
        Function<LongResult, LongResult> failIf = LongResult.Oks.failIf(v -> v > 100L, v -> AnyError.from("failed"));
        var result = failIf.apply(res);
        assertTrue(result.isErr());
    }

    @Test
    @DisplayName("LongResult.Oks.flatMap delegates to LongResult.flatMap")
    void testOksFlatMap() {
        var res = LongResult.ok(123L);
        Function<LongResult, LongResult> flatMap = LongResult.Oks.flatMap(v -> LongResult.ok(v + 1));
        var result = flatMap.apply(res);
        assertEquals(124L, result.expectOk());
    }

    @Test
    @DisplayName("LongResult.Oks.ifOk delegates to LongResult.ifOk")
    void testOksIfOk() {
        AtomicInteger count = new AtomicInteger();
        var res = LongResult.ok(123L);
        Consumer<LongResult> ifOk = LongResult.Oks.ifOk(v -> count.incrementAndGet());
        ifOk.accept(res);
        assertEquals(1, count.get());
    }

    @Test
    @DisplayName("LongResult.Errs.map delegates to LongResult.mapErr")
    void testErrsMap() {
        var res = LongResult.err("fail");
        Function<LongResult, LongResult> mapper = LongResult.Errs.map(e -> AnyError.from("mapped"));
        var result = mapper.apply(res);
        assertEquals("mapped", result.expectErr().message());
    }

    @Test
    @DisplayName("LongResult.Errs.filter delegates to LongResult.filterErr")
    void testErrsFilter() {
        var resOk = LongResult.ok(123L);
        var resErr = LongResult.err("fail");
        Predicate<LongResult> filter = LongResult.Errs.filter(e -> e.message().contains("fail"));
        
        assertTrue(filter.test(resOk)); // LongResult.filterErr returns true for Ok
        assertTrue(filter.test(resErr));
    }

    @Test
    @DisplayName("LongResult.Errs.isErrAnd delegates to LongResult.isErrAnd")
    void testErrsIsErrAnd() {
        var resOk = LongResult.ok(123L);
        var resErr = LongResult.err("fail");
        Predicate<LongResult> isErrAnd = LongResult.Errs.isErrAnd(e -> e.message().contains("fail"));
        
        assertFalse(isErrAnd.test(resOk));
        assertTrue(isErrAnd.test(resErr));
    }

    @Test
    @DisplayName("LongResult.Errs.recoverIf delegates to LongResult.recoverIf")
    void testErrsRecoverIf() {
        var res = LongResult.err("fail");
        Function<LongResult, LongResult> recoverIf = LongResult.Errs.recoverIf(e -> e.message().contains("fail"), e -> 123L);
        var result = recoverIf.apply(res);
        assertTrue(result.isOk());
        assertEquals(123L, result.expectOk());
    }

    @Test
    @DisplayName("LongResult.Errs.flatRecover delegates to LongResult.flatRecover")
    void testErrsFlatRecover() {
        var res = LongResult.err("fail");
        Function<LongResult, LongResult> flatRecover = LongResult.Errs.flatRecover(e -> LongResult.ok(123L));
        var result = flatRecover.apply(res);
        assertTrue(result.isOk());
        assertEquals(123L, result.expectOk());
    }

    @Test
    @DisplayName("LongResult.Errs.ifErr delegates to LongResult.ifErr")
    void testErrsIfErr() {
        AtomicInteger count = new AtomicInteger();
        var res = LongResult.err("fail");
        Consumer<LongResult> ifErr = LongResult.Errs.ifErr(e -> count.incrementAndGet());
        ifErr.accept(res);
        assertEquals(1, count.get());
    }
}
