package com.github.pourquoipas.logcall;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * An Advice class that provides the full logging functionality.
 * This version correctly handles methods with a void return type and uses public
 * helper methods to prevent IllegalAccessError.
 */
public class LogCallAdvice {

    /**
     * This advice is executed at the beginning of the instrumented method.
     *
     * @return The start time in milliseconds.
     */
    @Advice.OnMethodEnter
    public static long enter() {
        return System.currentTimeMillis();
    }

    /**
     * This advice is executed at the end of the instrumented method.
     *
     * @param method       The original method that was instrumented.
     * @param allArguments The arguments passed to the original method.
     * @param startTime    The start time captured by the enter advice.
     * @param result       The value returned by the method. Using Typing.DYNAMIC handles void methods correctly.
     * @param exception    The exception thrown by the method, or null if it completed normally.
     */
    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void exit(
            @Advice.Origin Method method,
            @Advice.AllArguments Object[] allArguments,
            @Advice.Enter long startTime,
            @Advice.Return(typing = Assigner.Typing.DYNAMIC) Object result,
            @Advice.Thrown Throwable exception
    ) {
        LogCall logCall = method.getAnnotation(LogCall.class);
        if (logCall == null) {
            return;
        }

        Logger logger = LogManager.getLogger(method.getDeclaringClass());

        if (isLoggerEnabled(logger, logCall.level())) {
            long duration = System.currentTimeMillis() - startTime;
            String logMessage = buildLogMessage(logCall, method, allArguments, result, exception, duration);
            log(logger, logCall.level(), logMessage);
        }
    }

    // --- Helper methods must be public to be accessible from the woven class ---

    public static String buildLogMessage(LogCall logCall, Method method, Object[] args, Object result, Throwable exception, long duration) {
        if (logCall.customLog() != null && !logCall.customLog().isEmpty()) {
            return formatCustomLog(logCall.customLog(), method, args, result, exception);
        }

        StringBuilder logMessage = new StringBuilder();
        logMessage.append("Method '").append(method.getName()).append("'");

        if (logCall.logParameters() && method.getParameterCount() > 0) {
            logMessage.append(" | Params: [");
            logMessage.append(Arrays.stream(args).map(arg -> Objects.toString(arg, "null")).collect(Collectors.joining(", ")));
            logMessage.append("]");
        }

        if (exception != null) {
            logMessage.append(" | Threw Exception: ").append(exception.getClass().getSimpleName());
        } else if (logCall.logReturn() && method.getReturnType() != void.class) {
            logMessage.append(" | Return: ").append(Objects.toString(result, "null"));
        }

        logMessage.append(" | Duration: ").append(duration).append("ms");

        if (logCall.logStackTrace()) {
            logMessage.append("\nCall Stack Trace:\n").append(getCleanStackTrace(new Throwable()));
        }

        if (exception != null && logCall.logException()) {
            logMessage.append("\nException Stack Trace:\n").append(getCleanStackTrace(exception));
        }
        return logMessage.toString();
    }

    public static String formatCustomLog(String pattern, Method method, Object[] args, Object result, Throwable exception) {
        String log = pattern.replace("{methodName}", method.getName())
                .replace("{className}", method.getDeclaringClass().getSimpleName());

        if (log.contains("{params}")) {
            log = log.replace("{params}", Arrays.stream(args).map(arg -> Objects.toString(arg, "null")).collect(Collectors.joining(", ")));
        }

        // --- Logic to replace parameters by name and index ---
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < args.length; i++) {
            // Replace by index, e.g., {param[0]}
            log = log.replace("{param[" + i + "]}", Objects.toString(args[i], "null"));

            // Replace by name, e.g., {p1}. This requires the -parameters flag during compilation.
            if (i < parameters.length) {
                log = log.replace("{" + parameters[i].getName() + "}", Objects.toString(args[i], "null"));
            }
        }

        if (log.contains("{return}")) {
            log = log.replace("{return}", Objects.toString(result, "null"));
        }
        if (log.contains("{exception}") && exception != null) {
            log = log.replace("{exception}", getCleanStackTrace(exception));
        }
        return log;
    }

    public static String getCleanStackTrace(Throwable throwable) {
        StringWriter sw = new StringWriter();
        throwable.printStackTrace(new PrintWriter(sw));
        return Arrays.stream(sw.toString().split("\r?\n"))
                .filter(line -> !line.contains("com.github.pourquoipas.logcall.LogCallAdvice"))
                .collect(Collectors.joining("\n"));
    }

    public static void log(Logger logger, LogLevel level, String message) {
        switch (level) {
            case TRACE: logger.trace(message); break;
            case DEBUG: logger.debug(message); break;
            case INFO:  logger.info(message);  break;
            case WARN:  logger.warn(message);  break;
            case ERROR: logger.error(message); break;
        }
    }

    public static boolean isLoggerEnabled(Logger logger, LogLevel level) {
        switch (level) {
            case TRACE: return logger.isTraceEnabled();
            case DEBUG: return logger.isDebugEnabled();
            case INFO:  return logger.isInfoEnabled();
            case WARN:  return logger.isWarnEnabled();
            case ERROR: return logger.isErrorEnabled();
            default:    return false;
        }
    }
}
