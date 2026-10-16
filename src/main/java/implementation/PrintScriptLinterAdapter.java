package implementation;

import interpreter.ErrorHandler;
import interpreter.PrintScriptLinter;
import kotlin.Unit;
import printscript.common.LanguageVersion;
import printscript.interpreter.env.MapEnvProvider;
import printscript.interpreter.input.QueueInput;
import printscript.linter.LinterFactory;
import printscript.linter.LinterInterface;
import printscript.runner.Engine;
import printscript.runner.LintResult;

import java.io.InputStream;

public class PrintScriptLinterAdapter implements PrintScriptLinter {
    @Override
    public void lint(InputStream src, String version, InputStream config, ErrorHandler handler) {
        LanguageVersion languageVersion = AdapterSupport.parseVersion(version, handler::reportError);
        if (languageVersion == null) {
            return;
        }
        AdapterSupport.reportingErrors(handler::reportError, () -> {
            LinterInterface linter = LinterFactory.INSTANCE.fromStream(config, languageVersion);
            LintResult result = new Engine(AdapterSupport.NO_OUTPUT, new QueueInput(), new MapEnvProvider()).lint(
                AdapterSupport.utf8Reader(src),
                languageVersion,
                parsed -> Unit.INSTANCE,
                statements -> {
                    linter.analyze(statements, warning -> {
                        handler.reportError(warning.getMessage());
                        return Unit.INSTANCE;
                    });
                    return Unit.INSTANCE;
                }
            );
            if (result instanceof LintResult.Failure failure) {
                handler.reportError(failure.getMessage());
            }
        });
    }
}
