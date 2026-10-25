package org.javafn.result;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class IntResultTest {

    @Test
    @DisplayName("IntResult.ok factory creates an Ok instance with value")
    void testOkFactory() {
        var res = IntResult.ok(123);
        assertTrue(res.isOk());
        assertFalse(res.isErr());
        assertEquals(123, res.expectOk());
    }

    @Test
    @DisplayName("IntResult.err factories create Err instances")
    void testErrFactories() {
        // String err
        var res1 = IntResult.err("error message");
        assertTrue(res1.isErr());
        assertEquals("error message", res1.expectErr().message());

        // List err
        var messages = List.of("err1", "err2");
        var res2 = IntResult.err(messages);
        assertTrue(res2.isErr());
        AnyError res2Err = res2.expectErr();
        assertInstanceOf(AnyError.ErrorList.class, res2Err);
        AnyError.ErrorList res2ErrLst = (AnyError.ErrorList) res2Err;
        assertEquals(res2ErrLst.errors().size(), messages.size());
        assertEquals(res2ErrLst.errors().stream().map(AnyError::message).toList(), messages);

        // Exception err
        var ex = new RuntimeException("exception");
        var res3 = IntResult.<RuntimeException>err(ex);
        assertTrue(res3.isErr());
        assertEquals(ex, res3.expectErr().except());

        // Enum err
        enum MyError { FAIL }
        var res4 = IntResult.<MyError>err(MyError.FAIL);
        assertTrue(res4.isErr());
        AnyError res4Err = res4.expectErr();
        assertInstanceOf(AnyError.EnumError.class, res4Err);
        AnyError.EnumError<? extends Enum<?>> res4ErrEnum = (AnyError.EnumError<?>) res4.expectErr();
        assertEquals(MyError.class, res4ErrEnum.type());
        assertEquals(MyError.FAIL, res4ErrEnum.error());
    }

    @Test
    @DisplayName("expectOk throws exception when IntResult is Err")
    void testExpectOkFailure() {
        var res = IntResult.err("fail");
        assertThrows(IllegalStateException.class, res::expectOk);
    }

    @Test
    @DisplayName("expectErr throws exception when IntResult is Ok")
    void testExpectErrFailure() {
        var res = IntResult.ok(123);
        assertThrows(IllegalStateException.class, res::expectErr);
    }
}
