package org.javafn.result;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.Problem;
import com.github.javaparser.printer.DefaultPrettyPrinter;
import org.javafn.result.Result.Oks;
import org.javafn.utils.Expect;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.function.Predicate.not;
import static org.javafn.result.Result.err;
import static org.javafn.result.Result.ok;

public class GenerateResultHelper {

    public static final String PACKAGE_NAME = "org.javafn.result";

    private static Result<String> readUrl(final String url) {
        try (final InputStream in = URI.create(url).toURL().openStream()) {
            return ok(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (final IOException e) {
            return err(e);
        }
    }

    public static void main(final String[] args) throws IOException {
        final File in = new File("src/codeGen/java/org/javafn/result");
        final File out = new File("build/generated/main/java/org/javafn/result");
        if (!out.exists()) {
            if (!out.mkdirs()) {
                throw new IOException("Error: Unable to create output directory " + out.getAbsolutePath());
            }
        }
        final var parser = new JavaParser(new ParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17));

        final var printer = new DefaultPrettyPrinter();

        copyPrimaryFiles(parser, printer, in, out);

        final Result<String> intFromUrl = readUrl("https://github.com/openjdk/jdk17/blob/master/src/java.base/share/classes/java/lang/Integer.java");
        final var res = intFromUrl.map(parser::parse)
                .failIf(not(ParseResult::isSuccessful),
                        pr -> AnyError.from(pr.getProblems().stream().map(Problem::getVerboseMessage).collect(Collectors.joining("\n"))))
                .map(pr -> pr.getResult().orElseThrow(Expect.optionalPresent()))
                .map(cu -> cu.);

    }

    static void copyPrimaryFiles(final JavaParser parser, final DefaultPrettyPrinter printer, final File sourceDirectory, final File destDirectory) {
        final ResultBatch<Void> res = Stream.of(
                        "AnyError.java",
                        "Try.java",
                        "ResultBatch.java",
                        "Result.java",
                        "DoubleResult.java",
                        "LongResult.java",
                        "IntResult.java")
                .map(Try.toMap(f -> parser.parse(sourceDirectory.toPath().resolve(f))))
                .map(Oks.failIf(
                        not(ParseResult::isSuccessful),
                        pr -> AnyError.from(pr.getProblems().stream().map(Problem::getVerboseMessage).collect(Collectors.joining("\n")))))
                .map(Oks.map(pr -> pr.getResult().orElseThrow(Expect.optionalPresent())))
                .map(Oks.failIf(
                        cu -> cu.getPrimaryTypeName().isEmpty(),
                        cu -> AnyError.from("Compilation unit does not have a primary type name! " + cu)))
                .map(Oks.flatMap(cu ->
                        Try.toGet(() -> cu.getPrimaryTypeName().orElseThrow(Expect.optionalPresent()))
                                .flatMap(name -> Try.toGet(() -> new PrintWriter(new FileOutputStream(destDirectory.toPath().resolve(name + ".java").toFile()))))
                                .flatMap(pw -> Try.to(() -> {
                                    pw.write(printer.print(cu));
                                    pw.close();
                                }))))
                .collect(Result.batch());
        if (res.hasErrors()) {
            throw new RuntimeException("Errors were encountered writing the main files to the generated directory:\n" + AnyError.joined(res.errors()));
        }
    }
}
