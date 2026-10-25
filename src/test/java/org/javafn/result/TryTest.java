package org.javafn.result;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.util.function.IntFunction;
import java.util.function.LongFunction;
import java.util.function.DoubleFunction;
import static org.junit.jupiter.api.Assertions.*;

class TryTest {

    @Test
    @DisplayName("Try.toGet returns Ok on success")
    void testToGetSuccess() {
        var res = Try.toGet(() -> "success");
        assertTrue(res.isOk());
        assertEquals("success", res.expectOk());
    }

    @Test
    @DisplayName("Try.toGet returns Err on exception")
    void testToGetFailure() {
        var ex = new RuntimeException("boom");
        var res = Try.toGet(() -> {
            throw ex;
        });
        assertTrue(res.isErr());
        assertEquals(ex, res.expectErr().except());
    }

    @Test
    @DisplayName("Try.toGetInt returns Ok on success")
    void testToGetIntSuccess() {
        var res = Try.toGetInt(() -> 123);
        assertTrue(res.isOk());
        assertEquals(123, res.expectOk());
    }

    @Test
    @DisplayName("Try.toGetInt returns Err on exception")
    void testToGetIntFailure() {
        var ex = new RuntimeException("boom");
        var res = Try.toGetInt(() -> {
            throw ex;
        });
        assertTrue(res.isErr());
        assertEquals(ex, res.expectErr().except());
    }

    @Test
    @DisplayName("Try.toGetLong returns Ok on success")
    void testToGetLongSuccess() {
        var res = Try.toGetLong(() -> 123L);
        assertTrue(res.isOk());
        assertEquals(123L, res.expectOk());
    }

    @Test
    @DisplayName("Try.toGetLong returns Err on exception")
    void testToGetLongFailure() {
        var ex = new RuntimeException("boom");
        var res = Try.toGetLong(() -> {
            throw ex;
        });
        assertTrue(res.isErr());
        assertEquals(ex, res.expectErr().except());
    }

    @Test
    @DisplayName("Try.toGetDouble returns Ok on success")
    void testToGetDoubleSuccess() {
        var res = Try.toGetDouble(() -> 123.4);
        assertTrue(res.isOk());
        assertEquals(123.4, res.expectOk());
    }

    @Test
    @DisplayName("Try.toGetDouble returns Err on exception")
    void testToGetDoubleFailure() {
        var ex = new RuntimeException("boom");
        var res = Try.toGetDouble(() -> {
            throw ex;
        });
        assertTrue(res.isErr());
        assertEquals(ex, res.expectErr().except());
    }

    @Test
    @DisplayName("Try.to returns Ok when runnable succeeds")
    void testToSuccess() {
        var res = Try.to(() -> {});
        assertTrue(res.isOk());
    }

    @Test
    @DisplayName("Try.to returns Err when runnable fails")
    void testToFailure() {
        var ex = new RuntimeException("boom");
        var res = Try.to(() -> {
            throw ex;
        });
        assertTrue(res.isErr());
        assertEquals(ex, res.expectErr().except());
    }

    @Test
    @DisplayName("Try.toUse returns function that captures success and failure")
    void testToUse() {
        final var ex = new RuntimeException("boom");
        var fn = Try.toUse((String s) -> {
            if ("fail".equals(s)) throw ex;
        });
        
        assertTrue(fn.apply("success").isOk());
        var failRes = fn.apply("fail");
        assertTrue(failRes.isErr());
        assertEquals(ex, failRes.expectErr().except());
    }

    @Test
    @DisplayName("Try.toMap returns function that captures success and failure")
    void testToMap() {
        var ex = new RuntimeException("boom");
        var fn = Try.toMap((String s) -> {
            if ("fail".equals(s)) throw ex;
            return s + "-ok";
        });
        
        var okRes = fn.apply("hi");
        assertTrue(okRes.isOk());
        assertEquals("hi-ok", okRes.expectOk());
        
        var errRes = fn.apply("fail");
        assertTrue(errRes.isErr());
        assertEquals(ex, errRes.expectErr().except());
    }

    @Test
    @DisplayName("Try.toMapToInt returns function that captures success and failure")
    void testToMapToInt() {
        var ex = new RuntimeException("boom");
        var fn = Try.toMapToInt((String s) -> {
            if ("fail".equals(s)) throw ex;
            return Integer.parseInt(s);
        });
        
        var okRes = fn.apply("123");
        assertTrue(okRes.isOk());
        assertEquals(123, okRes.expectOk());
        
        var errRes = fn.apply("fail");
        assertTrue(errRes.isErr());
        assertEquals(ex, errRes.expectErr().except());
    }

    @Test
    @DisplayName("Try.toMapToLong returns function that captures success and failure")
    void testToMapToLong() {
        var ex = new RuntimeException("boom");
        var fn = Try.toMapToLong((String s) -> {
            if ("fail".equals(s)) throw ex;
            return Long.parseLong(s);
        });
        
        var okRes = fn.apply("123");
        assertTrue(okRes.isOk());
        assertEquals(123L, okRes.expectOk());
        
        var errRes = fn.apply("fail");
        assertTrue(errRes.isErr());
        assertEquals(ex, errRes.expectErr().except());
    }

    @Test
    @DisplayName("Try.toMapToDouble returns function that captures success and failure")
    void testToMapToDouble() {
        var ex = new RuntimeException("boom");
        var fn = Try.toMapToDouble((String s) -> {
            if ("fail".equals(s)) throw ex;
            return Double.parseDouble(s);
        });
        
        var okRes = fn.apply("123.4");
        assertTrue(okRes.isOk());
        assertEquals(123.4, okRes.expectOk());
        
        var errRes = fn.apply("fail");
        assertTrue(errRes.isErr());
        assertEquals(ex, errRes.expectErr().except());
    }

    @Test
    @DisplayName("Try.toMapFromInt returns function that captures success and failure")
    void testToMapFromInt() {
        var ex = new RuntimeException("boom");
        IntFunction<Result<String>> fn = Try.toMapFromInt(i -> {
            if (i < 0) throw ex;
            return "val-" + i;
        });
        
        var okRes = fn.apply(10);
        assertTrue(okRes.isOk());
        assertEquals("val-10", okRes.expectOk());
        
        var errRes = fn.apply(-1);
        assertTrue(errRes.isErr());
        assertEquals(ex, errRes.expectErr().except());
    }

    @Test
    @DisplayName("Try.toMapFromLong returns function that captures success and failure")
    void testToMapFromLong() {
        var ex = new RuntimeException("boom");
        LongFunction<Result<String>> fn = Try.toMapFromLong(l -> {
            if (l < 0) throw ex;
            return "val-" + l;
        });
        
        var okRes = fn.apply(10L);
        assertTrue(okRes.isOk());
        assertEquals("val-10", okRes.expectOk());
        
        var errRes = fn.apply(-1L);
        assertTrue(errRes.isErr());
        assertEquals(ex, errRes.expectErr().except());
    }

    @Test
    @DisplayName("Try.toMapFromDouble returns function that captures success and failure")
    void testToMapFromDouble() {
        var ex = new RuntimeException("boom");
        DoubleFunction<Result<String>> fn = Try.toMapFromDouble(d -> {
            if (d < 0) throw ex;
            return "val-" + d;
        });
        
        var okRes = fn.apply(10.0);
        assertTrue(okRes.isOk());
        assertEquals("val-10.0", okRes.expectOk());
        
        var errRes = fn.apply(-1.0);
        assertTrue(errRes.isErr());
        assertEquals(ex, errRes.expectErr().except());
    }
}
