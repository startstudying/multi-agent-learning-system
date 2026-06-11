package com.learningos.rag.parser;

import com.learningos.config.RagParserOcrProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Component
public class ProcessOcrFallbackProvider implements OcrFallbackProvider {

    private static final String PROVIDER = "process";

    private final RagParserOcrProperties.ProcessProperties properties;

    @Autowired
    public ProcessOcrFallbackProvider(RagParserOcrProperties properties) {
        this(properties == null ? null : properties.process());
    }

    public ProcessOcrFallbackProvider(RagParserOcrProperties.ProcessProperties properties) {
        this.properties = properties == null
                ? new RagParserOcrProperties.ProcessProperties("", Duration.ofSeconds(10), 200_000)
                : properties;
    }

    @Override
    public String provider() {
        return PROVIDER;
    }

    @Override
    public OcrFallbackResult extractText(ParseInput input) {
        List<String> command = parseCommand(properties.command());
        if (command.isEmpty()) {
            return unavailable();
        }
        Process process = null;
        try {
            process = new ProcessBuilder(command).start();
            Process runningProcess = process;
            CompletableFuture<StreamReadResult> stdout = CompletableFuture.supplyAsync(
                    () -> readLimited(runningProcess.getInputStream(), maxOutputChars() + 1)
            );
            CompletableFuture<Void> stderr = CompletableFuture.runAsync(
                    () -> drain(runningProcess.getErrorStream())
            );
            writeInput(process.getOutputStream(), input);
            boolean finished = process.waitFor(properties.timeout().toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                return failed();
            }
            stderr.join();
            StreamReadResult stdoutResult = stdout.join();
            if (process.exitValue() != 0 || stdoutResult.overflow()) {
                return failed();
            }
            String text = stdoutResult.text().strip();
            if (text.isBlank()) {
                return unavailable();
            }
            return new OcrFallbackResult(OcrFallbackResult.Status.SUCCEEDED, "OCR_PROVIDER_SUCCEEDED", text);
        } catch (IOException | RuntimeException exception) {
            return failed();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            if (process != null) {
                process.destroyForcibly();
            }
            return failed();
        }
    }

    private int maxOutputChars() {
        return Math.min(properties.maxOutputChars(), ParserResourceLimits.MAX_EXTRACTED_CHARS);
    }

    private void writeInput(OutputStream outputStream, ParseInput input) throws IOException {
        try (OutputStream output = outputStream) {
            if (input != null && input.bytes().length > 0) {
                output.write(input.bytes());
            }
        }
    }

    private StreamReadResult readLimited(InputStream inputStream, int maxBytes) {
        try (InputStream input = inputStream;
             ByteArrayOutputStream output = new ByteArrayOutputStream(Math.min(maxBytes, 8192))) {
            byte[] buffer = new byte[4096];
            boolean overflow = false;
            int read;
            while ((read = input.read(buffer)) != -1) {
                int remaining = maxBytes - output.size();
                if (remaining > 0) {
                    output.write(buffer, 0, Math.min(read, remaining));
                }
                if (read > remaining) {
                    overflow = true;
                }
            }
            return new StreamReadResult(output.toString(StandardCharsets.UTF_8), overflow);
        } catch (IOException exception) {
            return new StreamReadResult("", true);
        }
    }

    private void drain(InputStream inputStream) {
        try (InputStream input = inputStream) {
            byte[] buffer = new byte[4096];
            while (input.read(buffer) != -1) {
                // stderr is consumed to avoid process blocking, but never exposed.
            }
        } catch (IOException ignored) {
            // Safe failure is decided from exit status/stdout path.
        }
    }

    private List<String> parseCommand(String command) {
        if (command == null || command.isBlank()) {
            return List.of();
        }
        List<String> args = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        for (int i = 0; i < command.length(); i++) {
            char value = command.charAt(i);
            if (value == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
                continue;
            }
            if (value == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
                continue;
            }
            if (Character.isWhitespace(value) && !inSingleQuote && !inDoubleQuote) {
                if (!current.isEmpty()) {
                    args.add(current.toString());
                    current.setLength(0);
                }
                continue;
            }
            current.append(value);
        }
        if (inSingleQuote || inDoubleQuote) {
            return List.of();
        }
        if (!current.isEmpty()) {
            args.add(current.toString());
        }
        return List.copyOf(args);
    }

    private OcrFallbackResult unavailable() {
        return new OcrFallbackResult(OcrFallbackResult.Status.UNAVAILABLE, "OCR_PROVIDER_UNAVAILABLE", "");
    }

    private OcrFallbackResult failed() {
        return new OcrFallbackResult(OcrFallbackResult.Status.FAILED, "OCR_PROVIDER_FAILED", "");
    }

    private record StreamReadResult(String text, boolean overflow) {
    }
}
