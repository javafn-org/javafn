package org.javafn.result;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.fail;

public class ResultsTest {

    private static Result<Integer> checkInt(String val, int expected) {
        return Results.decodeInt(val)
                .failIfMsg(n -> n != expected,
                        n -> "Integer.decode failed. String:" + val + " Result:" + n);
    }

    private static VoidResult checkIntFailure(String val, String message) {
        return Results.decodeInt(val)
                .reduce(expectedErr -> VoidResult.ok(),
                        unexpectedOk -> VoidResult.err(message));
    }

    private static Result<Long> checkLong(String val, long expected) {
        return Results.decodeLong(val)
                .failIfMsg(n -> n != expected,
                        n -> "Long.decode failed. String:" + val + " Result:" + n);
    }

    private static VoidResult checkLongFailure(String val, String message) {
        return Results.decodeLong(val)
                .reduce(expectedErr -> VoidResult.ok(),
                        unexpectedOk -> VoidResult.err(message));
    }

    @Test
    @DisplayName("Results.decodeLong parses examples from jdk source")
    void testDecodeLong() {
        Stream.of(
                checkLong(new String(""+Long.MIN_VALUE), Long.MIN_VALUE),
                checkLong(new String(""+Long.MAX_VALUE), Long.MAX_VALUE),

                checkLong("10",   10L),
                checkLong("0x10", 16L),
                checkLong("0X10", 16L),
                checkLong("010",  8L),
                checkLong("#10",  16L),

                checkLong("+10",   10L),
                checkLong("+0x10", 16L),
                checkLong("+0X10", 16L),
                checkLong("+010",  8L),
                checkLong("+#10",  16L),

                checkLong("-10",   -10L),
                checkLong("-0x10", -16L),
                checkLong("-0X10", -16L),
                checkLong("-010",  -8L),
                checkLong("-#10",  -16L),

                checkLong(Long.toString(Long.MIN_VALUE), Long.MIN_VALUE),
                checkLong(Long.toString(Long.MAX_VALUE), Long.MAX_VALUE)
        )
                .collect(Result.batch())
                .fold()
                .ifErr(err -> fail(err.message()));

        Stream.of(
                checkLongFailure("0x-10",   "Long.decode allows negative sign in wrong position."),
                checkLongFailure("0x+10",   "Long.decode allows positive sign in wrong position."),

                checkLongFailure("+",       "Raw plus sign allowed."),
                checkLongFailure("-",       "Raw minus sign allowed."),

                checkLongFailure(BigInteger.valueOf(Long.MIN_VALUE).subtract(BigInteger.ONE).toString(),
                        "Out of range"),
                checkLongFailure(BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE).toString(),
                        "Out of range"),

                checkLongFailure("", "Empty String"),
                checkLongFailure(null, "Long.decode(null) expected to throw NPE")
        )
                .collect(VoidResult.batch())
                .fold()
                .ifErr(err -> fail(err.message()));
    }

    @Test
    @DisplayName("Results.decodeInt parses examples from jdk source")
    void testDecodeInt() {
        Stream.of(
                checkInt(new String("" + Integer.MIN_VALUE), Integer.MIN_VALUE),
                checkInt(new String("" + Integer.MAX_VALUE), Integer.MAX_VALUE),

                checkInt("10", 10),
                checkInt("0x10", 16),
                checkInt("0X10", 16),
                checkInt("010", 8),
                checkInt("#10", 16),

                checkInt("+10", 10),
                checkInt("+0x10", 16),
                checkInt("+0X10", 16),
                checkInt("+010", 8),
                checkInt("+#10", 16),

                checkInt("-10", -10),
                checkInt("-0x10", -16),
                checkInt("-0X10", -16),
                checkInt("-010", -8),
                checkInt("-#10", -16),

                checkInt(Long.toString(Integer.MIN_VALUE), Integer.MIN_VALUE),
                checkInt(Long.toString(Integer.MAX_VALUE), Integer.MAX_VALUE)
        )
                .collect(Result.batch())
                .fold()
                .ifErr(err -> fail(err.message()));

        Stream.of(
                checkIntFailure("0x-10",   "Integer.decode allows negative sign in wrong position."),
                checkIntFailure("0x+10",   "Integer.decode allows positive sign in wrong position."),

                checkIntFailure("+",       "Raw plus sign allowed."),
                checkIntFailure("-",       "Raw minus sign allowed."),

                checkIntFailure(Long.toString((long)Integer.MIN_VALUE - 1L), "Out of range"),
                checkIntFailure(Long.toString((long)Integer.MAX_VALUE + 1L), "Out of range"),

                checkIntFailure("", "Empty String"),
                checkIntFailure(null, "Integer.decode(null) expected to throw NPE"))
                .collect(VoidResult.batch()
        )
                .fold()
                .ifErr(err -> fail(err.message()));
    }
}
