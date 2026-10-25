package org.javafn.result;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.stream.Collectors;

public class ResultDemo {
    public static void main(String[] args) {

        final LongResult res1 = testIt1();
        System.out.println(res1);
        final LongResult res2 = testIt2();
        System.out.println(res2);
        testIt3();
    }

    public static LongResult testIt1() {
        final int i;{
            final IntResult res1 = IntResult.ok(67);
            if (res1 instanceof IntResult.Err e) {
                return e.intoLong();
            }
            i = res1.expectOk();
        }

        return LongResult.ok(i);
    }

    public static LongResult testIt2() {
        final int i;{
            final IntResult res1 = IntResult.err("This is an error");
            if (res1 instanceof IntResult.Err e) {
                return e.intoLong();
            }
            i = res1.expectOk();
        }

        return LongResult.ok(i);
    }

    public static void testIt3() {
         final var theUrl = "http://localhost:8080/example";
         Result.ok(theUrl)
                 .flatMap(Try.toMap(URL::new))
                 .flatMap(Try.toMap(URL::openConnection))
                 .flatMap(Try.toMap(URLConnection::getInputStream))
                 .map(is -> new BufferedReader(new InputStreamReader(is)))
                 .map(BufferedReader::lines)
                 .ifErr(ex -> System.err.println("An exception occurred trying to fetch the url.  " + ex.message()))
                 .ifOk(lines -> System.out.println("URL contents from demoResult: "
                         + lines.collect(Collectors.joining("\n"))));
    }
}
