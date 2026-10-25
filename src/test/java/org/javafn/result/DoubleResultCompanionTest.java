package org.javafn.result;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import static org.junit.jupiter.api.Assertions.*;

class DoubleResultCompanionTest {

    @Test
    @DisplayName("DoubleResult.Oks.map delegates to DoubleResult.map")
    void testOksMap() {
        var res = DoubleResult.ok(123.4);
        Function<DoubleResult, DoubleResult> mapper = DoubleResult.Oks.map(v -> v * 2);
        var result = mapper.apply(res);
        assertEquals(246.8, result.expectOk(), 0.0001);
    }

    @Test
    @DisplayName("DoubleResult.Oks.filter delegates to DoubleResult.filter")
    void testOksFilter() {
        var resOk = DoubleResult.ok(123.4);
        var resErr = DoubleResult.err("fail");
        Predicate<DoubleResult> filter = DoubleResult.Oks.filter(v -> v > 100.0);
        
        assertTrue(filter.test(resOk));
        assertTrue(filter.test(resErr)); // DoubleResult.filter returns true for Err
    }

    @Test
    @DisplayName("DoubleResult.Oks.isOkAnd delegates to DoubleResult.isOkAnd")
    void testOksIsOkAnd() {
        var resOk = DoubleResult.ok(123.4);
        var resErr = DoubleResult.err("fail");
        Predicate<DoubleResult> isOkAnd = DoubleResult.Oks.isOkAnd(v -> v > 100.0);
        
        assertTrue(isOkAnd.test(resOk));
        assertFalse(isOkAnd.test(resErr));
    }

    @Test
    @DisplayName("DoubleResult.Oks.failIf delegates to DoubleResult.failIf")
    void testOksFailIf() {
        var res = DoubleResult.ok(123.4);
        Function<DoubleResult, DoubleResult> failIf = DoubleResult.Oks.failIf(v -> v > 100.0, v -> AnyError.from("failed"));
        var result = failIf.apply(res);
        assertTrue(result.isErr());
    }

    @Test
    @DisplayName("DoubleResult.Oks.flatMap delegates to DoubleResult.flatMap")
    void testOksFlatMap() {
        var res = DoubleResult.ok(123.4);
        Function<DoubleResult, DoubleResult> flatMap = DoubleResult.Oks.flatMap(v -> DoubleResult.ok(v + 1.0));
        var result = flatMap.apply(res);
        assertEquals(124.4, result.expectOk(), 0.0001);
    }

    @Test
    @DisplayName("DoubleResult.Oks.ifOk delegates to DoubleResult.ifOk")
    void testOksIfOk() {
        AtomicInteger count = new AtomicInteger();
        var res = DoubleResult.ok(123.4);
        Consumer<DoubleResult> ifOk = DoubleResult.Oks.ifOk(v -> count.incrementAndGet());
        ifOk.accept(res);
        assertEquals(1, count.get());
    }

    @Test
    @DisplayName("DoubleResult.Errs.map delegates to DoubleResult.mapErr")
    void testErrsMap() {
        var res = DoubleResult.err("fail");
        Function<DoubleResult, DoubleResult> mapper = DoubleResult.Errs.map(e -> AnyError.from("mapped"));
        var result = mapper.apply(res);
        assertEquals("mapped", result.expectErr().message());
    }

    @Test
    @DisplayName("DoubleResult.Errs.filter delegates to DoubleResult.filterErr")
    void testErrsFilter() {
        var resOk = DoubleResult.ok(123.4);
        var resErr = DoubleResult.err("fail");
        Predicate<DoubleResult> filter = DoubleResult.Errs.filter(e -> e.message().contains("fail"));
        
        assertTrue(filter.test(resOk)); // DoubleResult.filterErr returns true for Ok
        assertTrue(filter.test(resErr));
    }

    @Test
    @DisplayName("DoubleResult.Errs.isErrAnd delegates to DoubleResult.isErrAnd")
    void testErrsIsErrAnd() {
        var resOk = DoubleResult.ok(123.4);
        var resErr = DoubleResult.err("fail");
        Predicate<DoubleResult> isErrAnd = DoubleResult.Errs.isErrAnd(e -> e.message().contains("fail"));
        
        assertFalse(isErrAnd.test(resOk));
        assertTrue(isErrAnd.test(resErr));
    }

    @Test
    @DisplayName("DoubleResult.Errs.recoverIf delegates to DoubleResult.recoverIf")
    void testErrsRecoverIf() {
        var res = DoubleResult.err("fail");
        Function<DoubleResult, DoubleResult> recoverIf = DoubleResult.Errs.recoverIf(e -> e.message().contains("fail"), e -> 123.4);
        var result = recoverIf.apply(res);
        assertTrue(result.isOk());
        assertEquals(123.4, result.expectOk(), 0.0001);
    }

    @Test
    @DisplayName("DoubleResult.Errs.flatRecover delegates to DoubleResult.flatRecover")
    void testErrsFlatRecover() {
        var res = DoubleResult.err("fail");
        Function<DoubleResult, DoubleResult> flatRecover = DoubleResult.Errs.flatRecover(e -> DoubleResult.ok(123.4));
        var result = flatRecover.apply(res);
        assertTrue(result.isOk());
        assertEquals(123.4, result.expectOk(), 0.0001);
    }

    @Test
    @DisplayName("DoubleResult.Errs.ifErr delegates to DoubleResult.ifErr")
    void testErrsIfErr() {
        AtomicInteger count = new AtomicInteger();
        var res = DoubleResult.err("fail");
        Consumer<DoubleResult> ifErr = DoubleResult.Errs.ifErr(e -> count.incrementAndGet());
        ifErr.accept(res);
        assertEquals(1, count.get());
    }
}
