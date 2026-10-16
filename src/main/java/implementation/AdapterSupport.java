package implementation;

import printscript.common.LanguageVersion;
import printscript.interpreter.output.Output;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

// lo que comparten los tres adaptadores al traducir las interfaces del TCK al Engine
final class AdapterSupport {
    // formatear y lintear no imprimen, pero el Engine siempre pide un Output
    static final Output NO_OUTPUT = line -> { };

    private AdapterSupport() {
    }

    // el TCK espera los errores por callback, nunca una excepcion
    static LanguageVersion parseVersion(String version, Consumer<String> onError) {
        try {
            return LanguageVersion.Companion.parse(version);
        } catch (IllegalArgumentException e) {
            onError.accept("Unknown version: " + version);
            return null;
        }
    }

    // el Engine ya devuelve sus errores como resultado; esto cubre lo que corre afuera, como leer la config
    static void reportingErrors(Consumer<String> onError, Runnable block) {
        try {
            block.run();
        } catch (OutOfMemoryError e) {
            onError.accept("Java heap space");
        } catch (Throwable t) {
            onError.accept(t.getMessage() != null ? t.getMessage() : t.toString());
        }
    }

    static Reader utf8Reader(InputStream src) {
        return new BufferedReader(new InputStreamReader(src, StandardCharsets.UTF_8));
    }
}
