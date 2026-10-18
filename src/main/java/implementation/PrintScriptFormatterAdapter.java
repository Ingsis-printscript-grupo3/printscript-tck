package implementation;

import interpreter.PrintScriptFormatter;
import kotlin.Unit;
import printscript.common.LanguageVersion;
import printscript.formatter.FormatterFactory;
import printscript.formatter.FormatterInterface;
import printscript.formatter.FormatterRulesLoader;
import printscript.formatter.rule.FormatterRuleRegistry;
import printscript.interpreter.env.MapEnvProvider;
import printscript.interpreter.input.QueueInput;
import printscript.runner.Engine;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.function.Consumer;

public class PrintScriptFormatterAdapter implements PrintScriptFormatter {
    // la interfaz del TCK no tiene donde reportar errores de formato, asi que se descartan
    @Override
    public void format(InputStream src, String version, InputStream config, Writer writer) {
        LanguageVersion languageVersion = AdapterSupport.parseVersion(version, ignored -> { });
        if (languageVersion == null) {
            return;
        }
        AdapterSupport.reportingErrors(ignored -> { }, () -> {
            FormatterRuleRegistry registry = new FormatterRuleRegistry();
            FormatterInterface formatter =
                FormatterFactory.INSTANCE.create(FormatterRulesLoader.INSTANCE.fromStream(config, registry), registry);
            withTempCopy(src, source -> new Engine(AdapterSupport.NO_OUTPUT, new QueueInput(), new MapEnvProvider()).format(
                () -> openUtf8(source),
                languageVersion,
                parsed -> Unit.INSTANCE,
                tokens -> {
                    formatter.format(tokens, writer);
                    return Unit.INSTANCE;
                }
            ));
        });
    }

    // el Engine lee el fuente dos veces al formatear y un InputStream no se puede rebobinar
    private static void withTempCopy(InputStream src, Consumer<Path> block) {
        try {
            Path source = Files.createTempFile("printscript-format", ".ps");
            try {
                Files.copy(src, source, StandardCopyOption.REPLACE_EXISTING);
                block.accept(source);
            } finally {
                Files.deleteIfExists(source);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Reader openUtf8(Path source) {
        try {
            return Files.newBufferedReader(source, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
