package org.javafn.result;

import org.javafn.result.Result.Err;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.stream.Collectors;

import static org.javafn.result.Result.ok;

public class ResultDemo {
    public static void main(String[] args) {
        final Result<Long> res1 = testIt1();
        System.out.println(res1);
        final Result<Long> res2 = testIt2();
        System.out.println(res2);
        testIt3();
    }

    public static Result<Long> testIt1() {
        final Integer i;{
            final var res1 = Result.ok(67);
            if (res1 instanceof Err<?> e) {
                return e.into();
            }
            i = res1.expectOk();
        }

        return ok(i.longValue());
    }

    public static Result<Long> testIt2() {
        final Integer i;{
            final Result<Integer> res1 = Result.err("This is an error");
            if (res1 instanceof Err<?> e) {
                return e.into();
            }
            i = res1.expectOk();
        }

        return ok(i.longValue());
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
