package org.javafn.demo;

import org.javafn.result.Try;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DemoGetUrlWithResult {

    public static void main(final String[] args) {
        Stream.of( "not_a_url", "http://example.com")
                .forEach(url -> {
                    demoResult(url);
                    demoWithoutResult(url);
                });
    }

    private static void demoResult(final String theUrl) {
        Try.get(() -> new URL(theUrl))
                .asOk().flatMap(Try.Map(URL::openConnection))
                .asOk().flatMap(Try.Map(URLConnection::getInputStream))
                .asOk().map(is -> new BufferedReader(new InputStreamReader(is)))
                .asOk().map(BufferedReader::lines)
                .asErr().peek(ex -> System.err.println("An exception occurred trying to fetch the url."))
                .asErr().peek(Exception::printStackTrace)
                .asOk().opt()
                .ifPresent(lines -> System.out.println("URL contents from demoResult: " + lines.collect(Collectors.joining("\n"))));
    }

    private static void demoWithoutResult(final String theUrl) {
        final StringBuilder content = new StringBuilder();
        try {
            final URL url = new URL(theUrl);
            final URLConnection urlConnection = url.openConnection();
            final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                content.append(line).append("\n");
            }
            bufferedReader.close();
        } catch (Exception e) {
            System.err.println("An exception occurred trying to fetch the url.");
            e.printStackTrace();
            return;
        }
        System.out.println("URL contents from demoWithoutResult: " + content);
    }
}
