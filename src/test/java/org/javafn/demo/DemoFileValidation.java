package org.javafn.demo;

import org.javafn.result.Result;
import org.javafn.tuple.Pair;
import org.javafn.tuple.Trio;

import java.io.File;
import java.util.stream.Stream;

public class DemoFileValidation {
    public static void main(final String[] args) {
        Stream.of("/",
                System.getProperty("user.home"),
                "demoDirectoryValidationAndCreation_safeToDelete",
                "/var/log/private")
                .forEach(path ->
                    checkDirectory(path)
                            .peek(System.err::println,
                                    ok -> System.out.println("The file " + ok.getAbsolutePath() + " is valid."))
                );
        new File( "demoDirectoryValidationAndCreation_safeToDelete").delete();
    }

    private static Result<String, File> checkDirectory(final String dir) {
        return Result.<String, File>ok(new File(dir))
                .asOk().map(file -> Pair.of(file, file.exists()))
                .asOk().map(Pair.ToTrio((file, exists) -> !exists && file.mkdirs()))
                .asOk().filterMap(
                        Trio.Filter((file, exists, created) -> !exists && !created),
                        Trio.Map((file, e, c) -> "The directory '" + file.getAbsolutePath() + "' does not exist and cannot be created."))
                .asOk().peek(Trio.Peek((file, exists, created) -> {
                    if (!exists && created) {
                        System.out.println("Info: File " + file.getAbsolutePath() + " was created.");
                    } }))
                .asOk().map(Trio::_1)
                .asOk().map(file -> Pair.of(file, file.isDirectory()))
                .asOk().filterMap(Pair.Filter2(isDir -> !isDir),
                        Pair.Map((file, d) -> "The directory '" + file.getAbsolutePath() + "' exists but is a regular file, not a directory."))
                .asOk().map(Pair::_1)
                .asOk().map(file -> Pair.of(file, file.canRead()))
                .asOk().filterMap(Pair.Filter2(canRead -> !canRead),
                        Pair.Map((file, r) -> "The directory '" + file.getAbsolutePath() + "' cannot be read."))
                .asOk().map(Pair::_1)
                .asOk().map(file -> Pair.of(file, file.canWrite()))
                .asOk().filterMap(Pair.Filter2(canWrite -> !canWrite),
                        Pair.Map((file, r) -> "The directory '" + file.getAbsolutePath() + "' cannot be written to."))
                .asOk().map(Pair::_1);
    }
}
