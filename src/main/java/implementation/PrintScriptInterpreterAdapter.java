package implementation;

import interpreter.ErrorHandler;
import interpreter.InputProvider;
import interpreter.PrintEmitter;
import interpreter.PrintScriptInterpreter;
import kotlin.Unit;
import printscript.common.LanguageVersion;
import printscript.interpreter.env.SystemEnvProvider;
import printscript.runner.Engine;
import printscript.runner.ExecutionResult;

import java.io.InputStream;

public class PrintScriptInterpreterAdapter implements PrintScriptInterpreter {
    private static final int RESERVE_BYTES = 256 * 1024;

    @Override
    public void execute(InputStream src, String version, PrintEmitter emitter, ErrorHandler handler, InputProvider provider) {
        LanguageVersion languageVersion = AdapterSupport.parseVersion(version, handler::reportError);
        if (languageVersion == null) {
            return;
        }
        byte[] reserve = new byte[RESERVE_BYTES];
        try {
            // el TCK prueba readEnv con variables reales, por eso SystemEnvProvider
            Engine engine = new Engine(emitter::print, new PromptingInput(emitter, provider), new SystemEnvProvider());
            ExecutionResult result = engine.execute(AdapterSupport.utf8Reader(src), languageVersion, parsed -> Unit.INSTANCE);
            if (result instanceof ExecutionResult.Failure failure) {
                handler.reportError(failure.getMessage());
            }
        } catch (OutOfMemoryError e) {
            reserve = null;
            System.gc();
            handler.reportError("Java heap space");
        } catch (Throwable t) {
            reserve = null;
            handler.reportError(t.getMessage() != null ? t.getMessage() : t.toString());
        } finally {
            reserve = null;
        }
    }

    // el prompt del readInput se imprime por el canal de salida y la respuesta se pide por el de entrada
    private record PromptingInput(PrintEmitter emitter, InputProvider provider)
            implements printscript.interpreter.input.InputProvider {
        @Override
        public String readInput(String prompt) {
            if (!prompt.isEmpty()) {
                emitter.print(prompt);
            }
            return provider.input(prompt);
        }
    }
}
