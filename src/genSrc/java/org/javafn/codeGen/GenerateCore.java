package org.javafn.codeGen;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.InstanceOfExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.TypePatternExpr;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.Node;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Generates org.javafn.result.Results containing parse methods extracted from JDK sources.
 */
public class GenerateCore {

    private static final Set<String> DELEGATING_PARSE_METHODS = Set.of(
            "parseInt", "parseUnsignedInt",
            "parseLong", "parseUnsignedLong");

    private static final String REQUIRE_NON_NULL_METHODS = """
                public static <T> Result<T> requireNonNull(@Nullable final T value) {
                   return requireNonNull(value, "variable");
                }

                public static <T> Result<T> requireNonNull(@Nullable final T value, @NonNull final String variableName) {
                    if (value == null)
                        return Result.err(variableName + " cannot be null");
                    return Result.ok(value);
                }
            """;

    private static final String DECODE_METHODS = """
            private record DecodedPrefix(boolean negative, int radix, String remainder) {
                static Result<DecodedPrefix> decodePrefix(String nm) {
                    if (nm.isEmpty())
                        return Result.err("Zero length string");

                    final boolean negative;

                    final int firstIndex;
                    final char firstChar = nm.charAt(0);
                    if (firstChar == '-') {
                        negative = true;
                        firstIndex = 1;
                    } else if (firstChar == '+') {
                        negative = false;
                        firstIndex = 1;
                    } else {
                        negative = false;
                        firstIndex = 0;
                    }

                    final int radix;
                    final int index;
                    if (nm.startsWith("0x", firstIndex) || nm.startsWith("0X", firstIndex)) {
                        index = firstIndex + 2;
                        radix = 16;
                    } else if (nm.startsWith("#", firstIndex)) {
                        index = firstIndex + 1;
                        radix = 16;
                    } else if (nm.startsWith("0", firstIndex) && nm.length() > 1 + firstIndex) {
                        index = firstIndex + 1;
                        radix = 8;
                    } else {
                        index = firstIndex;
                        radix = 10;
                    }

                    if (nm.startsWith("-", index) || nm.startsWith("+", index))
                        return Result.err("Sign character in wrong position");
                    return Result.ok(new DecodedPrefix(negative, radix, nm.substring(index)));
                }
            }

            public static Result<Integer> decodeInt(String nm) {
                final DecodedPrefix p;
                {
                    final Result<DecodedPrefix> maybeDecoded = DecodedPrefix.decodePrefix(nm);
                    if (maybeDecoded instanceof Result.Err<DecodedPrefix> err) return err.into();
                    p = maybeDecoded.expectOk();
                }

                return parseInt(p.remainder, p.radix)
                        .map(v -> p.negative ? -v : v)
                        .flatRecover(ignored -> parseInt(p.negative ? ("-" + p.remainder) : p.remainder, p.radix))
                        .mapToObj(Integer::valueOf);
            }

            public static Result<Long> decodeLong(String nm) {
                final DecodedPrefix p;
                {
                    final Result<DecodedPrefix> maybeDecoded = DecodedPrefix.decodePrefix(nm);
                    if (maybeDecoded instanceof Result.Err<DecodedPrefix> err) return err.into();
                    p = maybeDecoded.expectOk();
                }

                return parseLong(p.remainder, p.radix)
                        .map(v -> p.negative ? -v : v)
                        .flatRecover(ignored -> parseLong(p.negative ? ("-" + p.remainder) : p.remainder, p.radix))
                        .mapToObj(Long::valueOf);
            }
        """;

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            throw new IllegalArgumentException("Target directory argument required");
        }
        Path targetDir = Paths.get(args[0]);
        String javaVersion = args.length > 1 ? args[1] : "17";

        StaticJavaParser.getParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17);

        Path packageDir = targetDir.resolve("org/javafn/result");
        Files.createDirectories(packageDir);

        JdkSourceDownloader downloader = new JdkSourceDownloader();

        // Download sources and record origin URLs
        JdkSourceDownloader.DownloadResult intResult = downloader.downloadSource(javaVersion, "java.base", "java.lang.Integer");
        JdkSourceDownloader.DownloadResult longResult = downloader.downloadSource(javaVersion, "java.base", "java.lang.Long");
        JdkSourceDownloader.DownloadResult nfeResult = downloader.downloadSource(javaVersion, "java.base", "java.lang.NumberFormatException");

        // Parse with JavaParser and extract relevant parse methods
        List<MethodDeclaration> intMethods = extractParseMethods(intResult.source(), Set.of("parseInt", "parseUnsignedInt"));
        List<MethodDeclaration> longMethods = extractParseMethods(longResult.source(), Set.of("parseLong", "parseUnsignedLong"));
        List<MethodDeclaration> nfeMethods = extractNfeMethods(nfeResult.source());

        Map<String, String> sourceUrls = new LinkedHashMap<>();
        sourceUrls.put("java.lang.Integer", intResult.sourceUrl());
        sourceUrls.put("java.lang.Long", longResult.sourceUrl());
        sourceUrls.put("java.lang.NumberFormatException", nfeResult.sourceUrl());

        List<MethodDeclaration> allMethods = Stream.of(
                nfeMethods.stream(),
                intMethods.stream().map(GenerateCore::transformParseMethod),
                longMethods.stream().map(GenerateCore::transformParseMethod)
                )
                .flatMap(Function.identity())
                .toList();

        String classContent = buildResultsClass(sourceUrls, allMethods);
        Files.writeString(packageDir.resolve("Results.java"), classContent, StandardCharsets.UTF_8);
    }

    private static List<MethodDeclaration> extractNfeMethods(String sourceCode) {
        CompilationUnit cu = StaticJavaParser.parse(sourceCode);
        List<MethodDeclaration> result = new ArrayList<>();
        for (ClassOrInterfaceDeclaration type : cu.findAll(ClassOrInterfaceDeclaration.class)) {
            for (MethodDeclaration method : type.getMethods()) {
                String name = method.getNameAsString();
                if (name.startsWith("for") || name.startsWith("from")) {
                    // Update access to public static
                    method.setModifiers(Modifier.Keyword.STATIC);
                    method.setType("AnyError");
                    method.getThrownExceptions().clear();

                    // Transform constructors new NumberFormatException(...) -> AnyError.from(...)
                    method.findAll(ObjectCreationExpr.class).forEach(creation -> {
                        if (creation.getType().getNameAsString().equals("NumberFormatException")) {
                            MethodCallExpr anyErrorFrom = new MethodCallExpr(
                                    new NameExpr("AnyError"),
                                    "from",
                                    creation.getArguments()
                            );
                            creation.replace(anyErrorFrom);
                        }
                    });

                    result.add(method);
                }
            }
        }
        return result;
    }

    private static MethodDeclaration transformParseMethod(MethodDeclaration method) {
        // Change return type
        String returnTypeName = method.getType().asString();
        String resultType;
        String resultScope;
        switch (returnTypeName) {
            case "int" -> {
                resultType = "IntResult";
                resultScope = "IntResult";
            }
            case "long" -> {
                resultType = "LongResult";
                resultScope = "LongResult";
            }

            default -> {
                resultType = "Result<" + returnTypeName + ">";
                resultScope = "Result";
            }
        }
        method.setType(resultType);
        method.getThrownExceptions().clear();

        // Replace calls to NumberFormatException.for* with for* (now in same class Results)
        method.findAll(MethodCallExpr.class).forEach(call -> {
            if (call.getScope().isPresent()
                    && call.getScope().get().toString().equals("NumberFormatException")) {
                call.removeScope();
            }
        });

        // Replace throw statements: throw <expr>; -> return <ResultType>.err(<transformedExpr>);
        method.findAll(ThrowStmt.class).forEach(throwStmt -> {
            Expression expr = throwStmt.getExpression();
            Expression errArg = expr;
            if (expr instanceof ObjectCreationExpr creation) {
                if (creation.getType().getNameAsString().equals("NumberFormatException")) {
                    errArg = new MethodCallExpr(
                            new NameExpr("AnyError"),
                            "from",
                            creation.getArguments()
                    );
                }
            }
            ReturnStmt returnStmt = new ReturnStmt(
                    new MethodCallExpr(new NameExpr(resultScope), "err", com.github.javaparser.ast.NodeList.nodeList(errArg))
            );
            throwStmt.replace(returnStmt);
        });

        // Replace return statements: return <expr>; -> return <ResultType>.ok(<expr>);
        // Note: do not wrap if already returning ResultType (e.g. from the throw rewrites or delegation)
        transformResultAssignments(method, resultType);
        transformRequireNonNull(method, resultType);
        method.findAll(ReturnStmt.class).forEach(returnStmt -> {
            if (returnStmt.getExpression().isPresent()) {
                Expression expr = returnStmt.getExpression().get();
                // Check if this return statement was generated by our throw replacement
                if (expr instanceof MethodCallExpr call
                        && call.getScope().isPresent()
                        && call.getScope().get().toString().equals(resultType)) {
                    return;
                }
                // If it delegates to another parse method that now returns a Result type, return as-is
                if (expr instanceof MethodCallExpr call
                        && DELEGATING_PARSE_METHODS.contains(call.getNameAsString())
                        && call.getScope().isEmpty()) {
                    return;
                }
                // If it returns an instanceof pattern variable (e.g. return asErr),
                // it is already an error value and should not be wrapped
                if (expr instanceof NameExpr nameExpr
                        && nameExpr.getNameAsString().startsWith("as")) {
                    return;
                }
                // If it returns a method call on an instanceof pattern variable
                // (e.g. return asErr.intoInt()), it is already an error value
                if (expr instanceof MethodCallExpr callExpr
                        && callExpr.getScope().isPresent()
                        && callExpr.getScope().get() instanceof NameExpr scopeName
                        && scopeName.getNameAsString().startsWith("as")) {
                    return;
                }
                ReturnStmt okReturn = new ReturnStmt(
                        new MethodCallExpr(new NameExpr(resultScope), "ok", com.github.javaparser.ast.NodeList.nodeList(expr))
                );
                returnStmt.replace(okReturn);
            }
        });
        return method;
    }

    /**
     * Transform variable declarations that initialize from a delegating parse method
     * (which now returns a Result type) into an escape-hatch pattern:
     * <pre>
     *   long first;
     *   {
     *     LongResult maybeFirst = parseLong(s, 0, len - 1, radix);
     *     if (maybeFirst instanceof LongResult.Err asErr)
     *         return asErr;
     *     first = maybeFirst.expectOk();
     *   }
     * </pre>
     */
    private static void transformResultAssignments(MethodDeclaration method, String resultType) {
        List<VariableDeclarator> toTransform = new ArrayList<>();
        method.findAll(VariableDeclarator.class).forEach(varDecl -> {
            if (varDecl.getInitializer().isPresent()) {
                Expression init = varDecl.getInitializer().get();
                if (init instanceof MethodCallExpr call
                        && DELEGATING_PARSE_METHODS.contains(call.getNameAsString())
                        && call.getScope().isEmpty()) {
                    toTransform.add(varDecl);
                }
            }
        });

        for (VariableDeclarator varDecl : toTransform) {
            String varName = varDecl.getNameAsString();
            Expression init = varDecl.getInitializer().get();

            // Replace initializer with none:  long first = parseLong(...)  ->  long first;
            varDecl.setInitializer((Expression) null);

            String resultVarName = "maybe"
                    + Character.toUpperCase(varName.charAt(0))
                    + varName.substring(1);

            // LongResult maybeFirst = parseLong(...);
            VariableDeclarator resultVarDecl = new VariableDeclarator(
                    new com.github.javaparser.ast.type.ClassOrInterfaceType(null, resultType),
                    new com.github.javaparser.ast.expr.SimpleName(resultVarName), init);
            VariableDeclarationExpr resultDecl = new VariableDeclarationExpr(resultVarDecl);

            // if (maybeFirst instanceof *Result.Err asErr) return asErr;
            com.github.javaparser.ast.type.ClassOrInterfaceType errType =
                    new com.github.javaparser.ast.type.ClassOrInterfaceType(
                            new com.github.javaparser.ast.type.ClassOrInterfaceType(resultType), "Err");
            TypePatternExpr typePattern = new TypePatternExpr(
                    com.github.javaparser.ast.NodeList.nodeList(), errType,
                    new com.github.javaparser.ast.expr.SimpleName("asErr"));
            InstanceOfExpr instanceofExpr = new InstanceOfExpr(
                    new NameExpr(resultVarName), errType, typePattern);
            ReturnStmt returnErr = new ReturnStmt(new NameExpr("asErr"));
            IfStmt ifErr = new IfStmt(instanceofExpr, returnErr, null);

            // first = maybeFirst.expectOk();
            MethodCallExpr expectOkCall = new MethodCallExpr(new NameExpr(resultVarName), "expectOk");
            AssignExpr assign = new AssignExpr(
                    new NameExpr(varName), expectOkCall, AssignExpr.Operator.ASSIGN);
            ExpressionStmt assignStmt = new ExpressionStmt(assign);

            BlockStmt block = new BlockStmt();
            block.addStatement(resultDecl);
            block.addStatement(ifErr);
            block.addStatement(assignStmt);

            // Insert the block after the VariableDeclarationExpr in its parent block
            Optional<Node> parentOpt = varDecl.getParentNode();
            if (parentOpt.isPresent() && parentOpt.get() instanceof VariableDeclarationExpr varDeclExpr) {
                Optional<Node> stmtOpt = varDeclExpr.getParentNode();
                if (stmtOpt.isPresent() && stmtOpt.get() instanceof ExpressionStmt exprStmt) {
                    Optional<Node> blockOpt = exprStmt.getParentNode();
                    if (blockOpt.isPresent() && blockOpt.get() instanceof BlockStmt blockStmt) {
                        List<Statement> stmts = blockStmt.getStatements();
                        int index = stmts.indexOf(exprStmt);
                        if (index >= 0) {
                            stmts.add(index + 1, block);
                        }
                    }
                }
            }
        }
    }

    /**
     * Transform Objects.requireNonNull(s) calls into the escape-hatch pattern:
     * <pre>
     * {
     *     Result<CharSequence> maybeErr = requireNonNull(s);
     *     if (maybeErr instanceof Result.Err<CharSequence> asErr)
     *         return asErr.intoInt();
     * }
     * </pre>
     * The into*() method name depends on the enclosing method's result type.
     */
    private static void transformRequireNonNull(MethodDeclaration method, String resultType) {
        List<MethodCallExpr> toTransform = new ArrayList<>();
        method.findAll(MethodCallExpr.class).forEach(call -> {
            if (call.getNameAsString().equals("requireNonNull")
                    && call.getScope().isPresent()
                    && call.getScope().get().toString().equals("Objects")) {
                toTransform.add(call);
            }
        });

        String intoMethod = switch (resultType) {
            case "IntResult" -> "intoInt";
            case "LongResult" -> "intoLong";
            default -> "into";
        };

        for (MethodCallExpr call : toTransform) {
            // Result<CharSequence> maybeErr = requireNonNull(s);
            ClassOrInterfaceType resultGenericType = new ClassOrInterfaceType(null, "Result");
            ClassOrInterfaceType charSeqType = new ClassOrInterfaceType(null, "CharSequence");
            resultGenericType.setTypeArguments(NodeList.nodeList(charSeqType));

            MethodCallExpr requireNonNullCall = new MethodCallExpr(
                    null, "requireNonNull", call.getArguments());
            VariableDeclarator varDecl = new VariableDeclarator(
                    resultGenericType,
                    new SimpleName("maybeErr"),
                    requireNonNullCall);
            VariableDeclarationExpr varDeclExpr = new VariableDeclarationExpr(varDecl);

            // if (maybeErr instanceof Result.Err<CharSequence> asErr) return asErr.intoInt();
            ClassOrInterfaceType errType = new ClassOrInterfaceType(
                    new ClassOrInterfaceType(null, "Result"), "Err");
            ClassOrInterfaceType charSeqErrType = new ClassOrInterfaceType(null, "CharSequence");
            errType.setTypeArguments(NodeList.nodeList(charSeqErrType));

            TypePatternExpr typePattern = new TypePatternExpr(
                    NodeList.nodeList(), errType, new SimpleName("asErr"));
            InstanceOfExpr instanceofExpr = new InstanceOfExpr(
                    new NameExpr("maybeErr"), errType, typePattern);

            MethodCallExpr intoCall = new MethodCallExpr(
                    new NameExpr("asErr"), intoMethod);
            ReturnStmt returnErr = new ReturnStmt(intoCall);
            IfStmt ifErr = new IfStmt(instanceofExpr, returnErr, null);

            BlockStmt block = new BlockStmt();
            block.addStatement(varDeclExpr);
            block.addStatement(ifErr);

            // Replace the ExpressionStmt containing the call with the block
            Optional<Node> parentOpt = call.getParentNode();
            if (parentOpt.isPresent() && parentOpt.get() instanceof ExpressionStmt exprStmt) {
                Optional<Node> blockOpt = exprStmt.getParentNode();
                if (blockOpt.isPresent() && blockOpt.get() instanceof BlockStmt blockStmt) {
                    List<Statement> stmts = blockStmt.getStatements();
                    int index = stmts.indexOf(exprStmt);
                    if (index >= 0) {
                        stmts.set(index, block);
                    }
                }
            }
        }
    }

    private static List<MethodDeclaration> extractParseMethods(String sourceCode, Set<String> methodNames) {
        CompilationUnit cu = StaticJavaParser.parse(sourceCode);
        List<MethodDeclaration> result = new ArrayList<>();
        for (ClassOrInterfaceDeclaration type : cu.findAll(ClassOrInterfaceDeclaration.class)) {
            for (MethodDeclaration method : type.getMethods()) {
                if (methodNames.contains(method.getNameAsString()) && method.isPublic() && method.isStatic()) {
                    result.add(method);
                }
            }
        }
        return result;
    }

    private static String buildResultsClass(Map<String, String> sourceUrls, List<MethodDeclaration> methods) {
        StringBuilder sb = new StringBuilder();

        // License and provenance banner
        sb.append("""
                /**
                 * Portions Copyright (c) 1994, 2021, Oracle and/or its affiliates. All rights reserved.
                 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
                 * <br/>
                 * This code is free software; you can redistribute it and/or modify it
                 * under the terms of the GNU General Public License version 2 only, as
                 * published by the Free Software Foundation.  Oracle designates this
                 * particular file as subject to the \\"Classpath\\" exception as provided
                 * by Oracle in the LICENSE file that accompanied this code.
                 * <br/>
                 * This code is distributed in the hope that it will be useful, but WITHOUT
                 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
                 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
                 * version 2 for more details (a copy is included in the LICENSE file that
                 * accompanied this code).
                 * <br/>
                 * Extracted and generated from OpenJDK source files:
                """);
        sourceUrls.forEach((key, value) -> sb.append(" *   - ").append(key).append(": ").append(value).append("\n"));

        sb.append("""
                 */
                package org.javafn.result;
                
                import org.javafn.utils.Expect;
                import edu.umd.cs.findbugs.annotations.NonNull;
                import edu.umd.cs.findbugs.annotations.Nullable;
                
                /**
                 * Various utilities extracted from OpenJDK, refactored to return Results instead of throwing exceptions.
                 */
                public final class Results {
                
                    private Results() {Expect.staticConstructorNotCalled(Results.class);}
                
                """);

        sb.append(REQUIRE_NON_NULL_METHODS)
                .append('\n')
                .append(DECODE_METHODS)
                .append('\n');

        for (MethodDeclaration method : methods) {
            String methodStr = method.toString();
            // Indent method content by 4 spaces
            for (String line : methodStr.split("\n", -1)) {
                if (line.isBlank()) {
                    sb.append("\n");
                } else {
                    sb.append("    ").append(line).append("\n");
                }
            }
            sb.append("\n");
        }

        sb.append("}\n");
        return sb.toString();
    }
}
