package implementation;

import interpreter.PrintScriptInterpreter;
import interpreter.PrintScriptLinter;
import org.junit.Test;
import util.ErrorCollector;
import util.PrintCollector;
import util.QueueInputProvider;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;
import static util.Queues.toQueue;

// comportamientos de los adaptadores que la suite de la catedra no cubre
public class AdaptersTest {
    private final PrintScriptInterpreter interpreter = new CustomImplementationFactory().interpreter();
    private final PrintScriptLinter linter = new CustomImplementationFactory().linter();

    @Test
    public void interpreterReportsUnknownVersion() {
        ErrorCollector errors = new ErrorCollector();

        interpreter.execute(stream("println(1);"), "9.9", new PrintCollector(), errors, new QueueInputProvider(toQueue(emptyList())));

        assertEquals(List.of("Unknown version: 9.9"), errors.getErrors());
    }

    @Test
    public void linterReportsUnknownVersion() {
        ErrorCollector errors = new ErrorCollector();

        linter.lint(stream("println(1);"), "9.9", stream("{}"), errors);

        assertEquals(List.of("Unknown version: 9.9"), errors.getErrors());
    }

    @Test
    public void readInputWithEmptyPromptPrintsNothingBeforeTheOutput() {
        PrintCollector prints = new PrintCollector();
        ErrorCollector errors = new ErrorCollector();

        interpreter.execute(
            stream("let name: string = readInput(\"\");\nprintln(name);"),
            "1.1",
            prints,
            errors,
            new QueueInputProvider(toQueue(List.of("world")))
        );

        assertEquals(List.of("world"), prints.getMessages());
        assertEquals(emptyList(), errors.getErrors());
    }

    @Test
    public void readInputPrintsThePromptBeforeTheOutput() {
        PrintCollector prints = new PrintCollector();
        ErrorCollector errors = new ErrorCollector();

        interpreter.execute(
            stream("let name: string = readInput(\"Name:\");\nprintln(name);"),
            "1.1",
            prints,
            errors,
            new QueueInputProvider(toQueue(List.of("world")))
        );

        assertEquals(List.of("Name:", "world"), prints.getMessages());
        assertEquals(emptyList(), errors.getErrors());
    }

    private static InputStream stream(String text) {
        return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
    }
}
