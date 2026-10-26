package org.javafn.codeGen;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Downloads JDK source files from OpenJDK GitHub mirrors with fallbacks.
 */
public class JdkSourceDownloader {

    public record DownloadResult(String source, String sourceUrl) {}

    private final HttpClient httpClient;

    public JdkSourceDownloader() {
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    /**
     * Downloads the source code for a given class from OpenJDK.
     *
     * @param javaVersion the Java major or full version (e.g. "17" or "17.0.12")
     * @param module      the JDK module (e.g. "java.base")
     * @param className   the fully-qualified class name (e.g. "java.lang.Integer")
     * @return the raw Java source file contents and the URL it was downloaded from
     * @throws IOException if all URL attempts fail
     */
    public DownloadResult downloadSource(String javaVersion, String module, String className) throws IOException {
        List<String> candidateUrls = buildCandidateUrls(javaVersion, module, className);

        List<String> errors = new ArrayList<>();
        for (String url : candidateUrls) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("User-Agent", "curl/7.79.1")
                        .timeout(Duration.ofSeconds(30))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    return new DownloadResult(response.body(), url);
                } else {
                    errors.add(url + " -> HTTP " + response.statusCode());
                }
            } catch (Exception e) {
                errors.add(url + " -> " + e.getMessage());
            }
        }

        throw new IOException("Failed to download source for " + className + " after trying:\n" +
                String.join("\n", errors));
    }

    /**
     * Builds prioritized candidate URLs for the source file.
     */
    List<String> buildCandidateUrls(String javaVersion, String module, String className) {
        String relativePath = "src/" + module + "/share/classes/" + className.replace('.', '/') + ".java";
        String major = normalizeMajor(javaVersion);

        List<String> urls = new ArrayList<>();

        // Priority 1: Exact current runtime version tag in update repo if version is detailed (e.g., jdk-17.0.12-ga)
        String runtimeVersion = System.getProperty("java.version");
        if (runtimeVersion != null && runtimeVersion.startsWith(major)) {
            urls.add("https://raw.githubusercontent.com/openjdk/jdk" + major + "u/jdk-" + runtimeVersion + "-ga/" + relativePath);
        }

        // Priority 2: General update repository GA tags (e.g. jdk17u)
        urls.add("https://raw.githubusercontent.com/openjdk/jdk" + major + "u/jdk-" + major + "-ga/" + relativePath);

        // Priority 3: General JDK release tag in main openjdk/jdk repo
        urls.add("https://raw.githubusercontent.com/openjdk/jdk/jdk-" + major + "-ga/" + relativePath);

        // Priority 4: Branch HEADs for the major update repo
        urls.add("https://raw.githubusercontent.com/openjdk/jdk" + major + "u/master/" + relativePath);
        urls.add("https://raw.githubusercontent.com/openjdk/jdk" + major + "u/dev/" + relativePath);

        return urls;
    }

    private static String normalizeMajor(String javaVersion) {
        if (javaVersion == null || javaVersion.isBlank()) {
            return "17";
        }
        // Handle "1.8" -> "8", "17" -> "17", "17.0.12" -> "17"
        String cleaned = javaVersion.replace("VERSION_", "").replace("1.", "");
        int dotIdx = cleaned.indexOf('.');
        return dotIdx > 0 ? cleaned.substring(0, dotIdx) : cleaned;
    }
}
