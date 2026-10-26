package org.javafn.codeGen;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.ThrowStmt;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Generates org.javafn.result.Numbers containing parse methods extracted from JDK sources.
 */
public class GenerateCore {

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
        JdkSourceDownloader.DownloadResult nfeResult = downloader.downloadSource(javaVersion, "java.base", "java.lang.NumberFormatException");

        // Parse with JavaParser and extract relevant parse methods
        List<MethodDeclaration> intMethods = extractParseMethods(intResult.source(), Set.of("parseInt", "parseUnsignedInt"));
        List<MethodDeclaration> nfeMethods = extractNfeMethods(nfeResult.source());

        Map<String, String> sourceUrls = new LinkedHashMap<>();
        sourceUrls.put("java.lang.Integer", intResult.sourceUrl());
        sourceUrls.put("java.lang.NumberFormatException", nfeResult.sourceUrl());

        List<MethodDeclaration> allMethods = new ArrayList<>();
        allMethods.addAll(nfeMethods);
        for (MethodDeclaration m : intMethods) {
            transformParseMethod(m);
            allMethods.add(m);
        }

        String classContent = buildNumbersClass(sourceUrls, allMethods);
        Files.writeString(packageDir.resolve("Numbers.java"), classContent, StandardCharsets.UTF_8);
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

    private static void transformParseMethod(MethodDeclaration method) {
        // Change return type
        String returnTypeName = method.getType().asString();
        String resultType = "int".equals(returnTypeName) ? "IntResult" : "Result<" + returnTypeName + ">";
        String resultScope = "int".equals(returnTypeName) ? "IntResult" : "Result";
        method.setType(resultType);
        method.getThrownExceptions().clear();

        // Replace calls to NumberFormatException.for* with for* (now in same class Numbers)
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
        method.findAll(ReturnStmt.class).forEach(returnStmt -> {
            if (returnStmt.getExpression().isPresent()) {
                Expression expr = returnStmt.getExpression().get();
                // Check if this return statement was generated by our throw replacement
                if (expr instanceof MethodCallExpr call
                        && call.getScope().isPresent()
                        && call.getScope().get().toString().equals(resultType)) {
                    return;
                }
                // If it delegates to another parseInt/parseUnsignedInt, that method already returns IntResult
                if (expr instanceof MethodCallExpr call
                        && (call.getNameAsString().equals("parseInt") || call.getNameAsString().equals("parseUnsignedInt"))) {
                    return;
                }
                ReturnStmt okReturn = new ReturnStmt(
                        new MethodCallExpr(new NameExpr(resultScope), "ok", com.github.javaparser.ast.NodeList.nodeList(expr))
                );
                returnStmt.replace(okReturn);
            }
        });
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

    private static String buildNumbersClass(Map<String, String> sourceUrls, List<MethodDeclaration> methods) {
        StringBuilder sb = new StringBuilder();

        // License and provenance banner
        sb.append("/*\n");
        sb.append(" * Portions Copyright (c) 1994, 2021, Oracle and/or its affiliates. All rights reserved.\n");
        sb.append(" * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.\n");
        sb.append(" *\n");
        sb.append(" * This code is free software; you can redistribute it and/or modify it\n");
        sb.append(" * under the terms of the GNU General Public License version 2 only, as\n");
        sb.append(" * published by the Free Software Foundation.  Oracle designates this\n");
        sb.append(" * particular file as subject to the \"Classpath\" exception as provided\n");
        sb.append(" * by Oracle in the LICENSE file that accompanied this code.\n");
        sb.append(" *\n");
        sb.append(" * This code is distributed in the hope that it will be useful, but WITHOUT\n");
        sb.append(" * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or\n");
        sb.append(" * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License\n");
        sb.append(" * version 2 for more details (a copy is included in the LICENSE file that\n");
        sb.append(" * accompanied this code).\n");
        sb.append(" *\n");
        sb.append(" * Extracted and generated from OpenJDK source files:\n");
        for (Map.Entry<String, String> entry : sourceUrls.entrySet()) {
            sb.append(" *   - ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        sb.append(" */\n\n");

        sb.append("package org.javafn.result;\n\n");
        sb.append("import java.util.Objects;\n\n");
        sb.append("/**\n");
        sb.append(" * Numeric parsing utilities extracted from OpenJDK.\n");
        sb.append(" */\n");
        sb.append("public final class Numbers {\n\n");
        sb.append("    private Numbers() {}\n\n");

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
