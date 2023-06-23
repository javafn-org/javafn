package org.javafn.demo;

import org.javafn.result.Result;
import org.javafn.result.Result.Ok;
import org.javafn.result.VoidResult;

import java.util.Arrays;
import java.util.stream.Stream;

public class DemoPathValidation {
    public static void main(final String[] args) {
        Stream.of( "/this/is/a/good/path",
                        "/this/is/not/../a/good/path",
                        "/this/is/not/~/a/good/path",
                        "/this/is/not/*/a/good/path",
                        "//*/../~/:/one_safe_part")
                .forEach(path -> {
                    final VoidResult<String> result = demoValidation(path);
                    result.peek(
                            err -> System.err.println("Error in '" + path + "':\n" + err),
                            () -> System.out.println("No error in '" + path + "'.")
                    );
                });
    }

    public static VoidResult<String> demoValidation(final String untrustedPath) {
        return
                // Split the path on '/' and stream the parts
                Arrays.stream(
                                // If the string starts with '/', we'll get an initial empty part
                                (untrustedPath.startsWith("/")
                                        ? untrustedPath.substring(1)
                                        : untrustedPath
                                ).split("/"))
                        // Turn each part into an Ok Result
                        .map(Result::<String, String>ok)
                        // Map this ok Result to an err if any condition holds.
                        // Each Result represents a single path part (directory), so there could be multiple errors
                        // resulting from a call to validate.
                        // If any of these results turn into an Err, the remaining tests aren't run
                        // on that path part, although the remaining parts will still be processed.
                        .map(Ok.FilterMap(
                                String::isEmpty,
                                badPart -> "A portion of the path is empty.  This is not normally critical, but we disallow it."))
                        .map(Ok.FilterMap(
                                part -> part.equals(".."),
                                badPart -> "The path part '" + badPart + "' is not allowed because it can be used to navigate to higher directories."))
                        .map(Ok.FilterMap(
                                part -> part.equals("~"),
                                badPart -> "The path part '" + badPart + "' is not allowed because it points to the current user's (e.g., tomcat) home directory."))
                        .map(Ok.FilterMap(
                                part -> part.contains("/"),
                                badPart -> "The sanitize function should not have been called with a part containing '/', but it was; something about this filename is unsafe."))
                        .map(Ok.FilterMap(
                                part -> part.contains("*"),
                                badPart -> "Path parts cannot contain '*' because this is a special character mapping all file name patterns."))
                        .map(Ok.FilterMap(
                                part -> part.contains(":"),
                                badPart -> "TBH, I'm not sure why ':' is considered invalid in filenames, but it's recommended to be removed as well."))
                        // If the result is still an ok, then this path part is good.  We can drop it; we don't need it anymore.
                        .map(Ok.MapToVoid(ignored -> {} ))
                        // Gather all of the Results
                        .collect(VoidResult.collector())
                        .fold(
                                // Collect the errors into one big string if there are errors.
                                errs -> String.join("\n", errs)
                        );
    }
}
