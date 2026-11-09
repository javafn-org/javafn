package org.javafn.result;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the {@link AnyResult} interface across every backing implementation
 * ({@link Result}, {@link IntResult}, {@link LongResult}, {@link DoubleResult},
 * {@link VoidResult}), for both the {@code Ok} and {@code Err} variants of each.
 * <p>
 * All functions are tested uniformly over a list of implementations so that any
 * deviation in any backing implementation is surfaced here.
 */
class AnyResultTest {

    // -----------------------------------------------------------------------
    // Tested values
    // -----------------------------------------------------------------------

    static final String OK_STRING = "ok value";
    static final Integer OK_INT = 42;
    static final Long OK_LONG = 42L;
    static final Double OK_DOUBLE = 42.0;
    static final VoidResult.Unit OK_UNIT = VoidResult.Unit.NIL;

    static final String ERR_MESSAGE = "any result error";
    static final AnyError ERR = AnyError.from(ERR_MESSAGE);
    static final AnyError MAPPED_ERR = AnyError.from("mapped error");

    // -----------------------------------------------------------------------
    // Lists of every implementation type, for both variants
    // -----------------------------------------------------------------------

    /**
     * One {@code Ok} instance of each AnyResult implementation type.
     */
    static final List<AnyResult<?, ?>> OKS = List.of(
            Result.ok(OK_STRING),
            IntResult.ok(OK_INT),
            LongResult.ok(OK_LONG),
            DoubleResult.ok(OK_DOUBLE),
            VoidResult.ok()
    );

    /**
     * One {@code Err} instance of each AnyResult implementation type.
     */
    static final List<AnyResult<?, ?>> ERRS = List.of(
            Result.<String>err(ERR),
            IntResult.err(ERR),
            LongResult.err(ERR),
            DoubleResult.err(ERR),
            VoidResult.err(ERR)
    );

    static List<AnyResult<?, ?>> oks() {
        return OKS;
    }

    static List<AnyResult<?, ?>> errs() {
        return ERRS;
    }

    // -----------------------------------------------------------------------
    // isOk / isErr
    // -----------------------------------------------------------------------

    @ParameterizedTest
    @MethodSource("oks")
    @DisplayName("isOk returns true for every Ok implementation")
    void isOkTrueForOk(AnyResult<?, ?> res) {
        assertTrue(res.isOk());
        assertFalse(res.isErr());
    }

    @ParameterizedTest
    @MethodSource("errs")
    @DisplayName("isErr returns true for every Err implementation")
    void isErrTrueForErr(AnyResult<?, ?> res) {
        assertFalse(res.isOk());
        assertTrue(res.isErr());
    }

    @Test
    @DisplayName("implementation lists cover each backing type once per variant")
    void listsCoverEachImplementation() {
        assertEquals(5, OKS.size());
        assertEquals(5, ERRS.size());
    }

    // -----------------------------------------------------------------------
    // optErr / expectErr
    // -----------------------------------------------------------------------

    @ParameterizedTest
    @MethodSource("oks")
    @DisplayName("optErr is empty for every Ok implementation")
    void optErrEmptyForOk(AnyResult<?, ?> res) {
        assertTrue(res.optErr().isEmpty());
    }

    @ParameterizedTest
    @MethodSource("errs")
    @DisplayName("optErr contains the error for every Err implementation")
    void optErrContainsErrorForErr(AnyResult<?, ?> res) {
        assertTrue(res.optErr().isPresent());
        assertEquals(ERR, res.optErr().get());
    }

    @ParameterizedTest
    @MethodSource("oks")
    @DisplayName("expectErr throws for every Ok implementation")
    void expectErrThrowsForOk(AnyResult<?, ?> res) {
        assertThrows(IllegalStateException.class, res::expectErr);
    }

    @ParameterizedTest
    @MethodSource("errs")
    @DisplayName("expectErr returns the error for every Err implementation")
    void expectErrReturnsErrorForErr(AnyResult<?, ?> res) {
        assertEquals(ERR, res.expectErr());
    }

    // -----------------------------------------------------------------------
    // filterErr / isErrAnd
    // -----------------------------------------------------------------------

    static final Predicate<AnyError> ERR_MATCHES = e -> e.message().equals(ERR_MESSAGE);
    static final Predicate<AnyError> ERR_DOES_NOT_MATCH = e -> e.message().equals("something else");

    @ParameterizedTest
    @MethodSource("oks")
    @DisplayName("filterErr returns true for every Ok implementation")
    void filterErrTrueForOk(AnyResult<?, ?> res) {
        assertTrue(res.filterErr(ERR_MATCHES));
        assertTrue(res.filterErr(ERR_DOES_NOT_MATCH));
    }

    @ParameterizedTest
    @MethodSource("errs")
    @DisplayName("filterErr applies the predicate to the error for every Err implementation")
    void filterErrAppliesPredicateForErr(AnyResult<?, ?> res) {
        assertTrue(res.filterErr(ERR_MATCHES));
        assertFalse(res.filterErr(ERR_DOES_NOT_MATCH));
    }

    @ParameterizedTest
    @MethodSource("oks")
    @DisplayName("isErrAnd returns false for every Ok implementation")
    void isErrAndFalseForOk(AnyResult<?, ?> res) {
        assertFalse(res.isErrAnd(ERR_MATCHES));
        assertFalse(res.isErrAnd(ERR_DOES_NOT_MATCH));
    }

    @ParameterizedTest
    @MethodSource("errs")
    @DisplayName("isErrAnd applies the predicate to the error for every Err implementation")
    void isErrAndAppliesPredicateForErr(AnyResult<?, ?> res) {
        assertTrue(res.isErrAnd(ERR_MATCHES));
        assertFalse(res.isErrAnd(ERR_DOES_NOT_MATCH));
    }

    // -----------------------------------------------------------------------
    // res
    // -----------------------------------------------------------------------

    @ParameterizedTest
    @MethodSource("oks")
    @DisplayName("res returns this for every Ok implementation")
    void resReturnsThisForOk(AnyResult<?, ?> res) {
        assertSame(res, res.res());
    }

    @ParameterizedTest
    @MethodSource("errs")
    @DisplayName("res returns this for every Err implementation")
    void resReturnsThisForErr(AnyResult<?, ?> res) {
        assertSame(res, res.res());
    }

    // -----------------------------------------------------------------------
    // boxed
    // -----------------------------------------------------------------------

    @ParameterizedTest
    @MethodSource("oks")
    @DisplayName("boxed returns an Ok Result for every Ok implementation")
    void boxedOkForOk(AnyResult<?, ?> res) {
        Result<?> boxed = res.boxed();
        assertInstanceOf(Result.class, boxed);
        assertTrue(boxed.isOk());
        assertFalse(boxed.isErr());
    }

    @ParameterizedTest
    @MethodSource("errs")
    @DisplayName("boxed returns an Err Result for every Err implementation")
    void boxedErrForErr(AnyResult<?, ?> res) {
        Result<?> boxed = res.boxed();
        assertInstanceOf(Result.class, boxed);
        assertTrue(boxed.isErr());
        assertFalse(boxed.isOk());
        assertEquals(ERR, boxed.expectErr());
    }

    // -----------------------------------------------------------------------
    // mapErr
    // -----------------------------------------------------------------------

    static final Function<AnyError, AnyError> MAP_ERR = e -> MAPPED_ERR;

    @ParameterizedTest
    @MethodSource("oks")
    @DisplayName("mapErr returns this unmodified for every Ok implementation")
    void mapErrReturnsThisForOk(AnyResult<?, ?> res) {
        AnyResult<?, ?> mapped = res.mapErr(MAP_ERR);
        assertSame(res, mapped);
        assertTrue(mapped.isOk());
    }

    @ParameterizedTest
    @MethodSource("errs")
    @DisplayName("mapErr maps the error and returns a new Err for every Err implementation")
    void mapErrMapsErrorForErr(AnyResult<?, ?> res) {
        AnyResult<?, ?> mapped = res.mapErr(MAP_ERR);
        assertNotSame(res, mapped);
        assertTrue(mapped.isErr());
        assertEquals(MAPPED_ERR, mapped.expectErr());
    }

    // -----------------------------------------------------------------------
    // ifErr
    // -----------------------------------------------------------------------

    @ParameterizedTest
    @MethodSource("oks")
    @DisplayName("ifErr performs no operation and returns this for every Ok implementation")
    void ifErrNoOpForOk(AnyResult<?, ?> res) {
        boolean[] invoked = { false };
        Consumer<AnyError> fn = e -> invoked[0] = true;
        AnyResult<?, ?> returned = res.ifErr(fn);
        assertSame(res, returned);
        assertFalse(invoked[0]);
    }

    @ParameterizedTest
    @MethodSource("errs")
    @DisplayName("ifErr applies the consumer and returns this for every Err implementation")
    void ifErrAppliesConsumerForErr(AnyResult<?, ?> res) {
        boolean[] invoked = { false };
        Consumer<AnyError> fn = e -> {
            assertEquals(ERR, e);
            invoked[0] = true;
        };
        AnyResult<?, ?> returned = res.ifErr(fn);
        assertSame(res, returned);
        assertTrue(invoked[0]);
    }
}
