package com.github.pourquoipas.logcall;

import net.bytebuddy.implementation.bind.annotation.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

public class LogCallInterceptor {

    @RuntimeType // Important for automatic type conversions
    public static Object intercept(
            @This Object obj,                         // The instance of the intercepted object
            @AllArguments Object[] allArguments,      // All method arguments as an array
            @Origin java.lang.reflect.Method method,  // The Method object of the intercepted method
            @SuperCall Callable<Object> superMethod   // <--- CHANGE THIS: Callable<?> to Callable<Object>
    ) throws Throwable {

        // Your existing logic here
        Logger logger = LogManager.getLogger(method.getDeclaringClass());

        System.out.println("### INTERCEPTOR: METHOD INTERCEPTED: " + method.getName() + " of class " + method.getDeclaringClass().getSimpleName());
        System.out.println("### INTERCEPTOR: Logger obtained: " + logger.getName() + " (Enabled for INFO: " + logger.isInfoEnabled() + ")");

        LogCall logCall = method.getAnnotation(LogCall.class);

        if (!isLoggerEnabled(logger, logCall.level())) {
            // If logging is not enabled, directly call the original method and return
            return superMethod.call();
        }

        long startTime = System.currentTimeMillis();
        Object result = null;
        Throwable exception = null;

        try {
            // Call the original method
            result = superMethod.call();
            return result; // Return the result of the original method
        } catch (Throwable t) {
            exception = t;
            throw t; // Re-throw the original exception
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            String logMessage = buildLogMessage(logCall, method, allArguments, result, exception, duration);
            log(logger, logCall.level(), logMessage);
            System.out.println("### INTERCEPTOR: Log message processed.");
        }
    }



    private static String buildLogMessage(LogCall logCall, Method method, Object[] args, Object result, Throwable exception, long duration) {
        // ... (your existing implementation) ...
        if (logCall.customLog() != null && !logCall.customLog().isEmpty()) {
            return formatCustomLog(logCall.customLog(), method, args, result, exception);
        }

        StringBuilder logMessage = new StringBuilder();
        logMessage.append("Method '").append(method.getName()).append("'");

        if (logCall.logParameters() && method.getParameterCount() > 0) {
            logMessage.append(" | Params: [");
            logMessage.append(Arrays.stream(args)
                    .map(arg -> Objects.toString(arg, "null"))
                    .collect(Collectors.joining(", ")));
            logMessage.append("]");
        }

        if (exception != null) {
            logMessage.append(" | Threw Exception: ").append(exception.getClass().getSimpleName());
        } else if (logCall.logReturn() && method.getReturnType() != void.class) {
            logMessage.append(" | Return: ").append(Objects.toString(result, "null"));
        }

        logMessage.append(" | Duration: ").append(duration).append("ms");

        if (logCall.logStackTrace()) {
            logMessage.append("\nCall Stack Trace:\n").append(getCleanStackTrace(new Throwable(), method.getName()));
        }

        if (exception != null && logCall.logException()) {
            logMessage.append("\nException Stack Trace:\n").append(getCleanStackTrace(exception, method.getName()));
        }
        return logMessage.toString();
    }

    private static String formatCustomLog(String pattern, Method method, Object[] args, Object result, Throwable exception) {
        // ... (your existing implementation) ...
        String log = pattern;
        log = log.replace("{methodName}", method.getName());
        log = log.replace("{className}", method.getDeclaringClass().getSimpleName());

        if (log.contains("{params}")) {
            log = log.replace("{params}", Arrays.stream(args).map(arg -> Objects.toString(arg, "null")).collect(Collectors.joining(", ")));
        }

        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < args.length; i++) {
            log = log.replace("{param[" + i + "]}", Objects.toString(args[i], "null"));
            if (i < parameters.length) {
                // Funziona solo se il codice è compilato con il flag -parameters
                log = log.replace("{" + parameters[i].getName() + "}", Objects.toString(args[i], "null"));
            }
        }

        if (log.contains("{return}")) {
            log = log.replace("{return}", Objects.toString(result, "null"));
        }

        Throwable causeForStack = exception != null ? exception : new Throwable();
        if (log.contains("{stacktrace}")) {
            log = log.replace("{stacktrace}", getCleanStackTrace(causeForStack, method.getName()));
        } else if (log.contains("{exception}") && exception != null) {
            log = log.replace("{exception}", getCleanStackTrace(exception, method.getName()));
        }
        return log;
    }

    private static String getCleanStackTrace(Throwable throwable, String methodName) {
        // ... (your existing implementation) ...
        StringWriter sw = new StringWriter();
        throwable.printStackTrace(new PrintWriter(sw));
        String fullStackTrace = sw.toString();

        return Arrays.stream(fullStackTrace.split("\r?\n"))
                .filter(line ->
                        !line.contains("com.github.pourquoipas.logcall.LogCallInterceptor.intercept") &&
                                !line.contains("net.bytebuddy")
                )
                .collect(Collectors.joining("\n"));
    }

    // Change parameter type to Log4j2's Logger
    private static void log(Logger logger, LogLevel level, String message) {
        switch (level) {
            case TRACE: logger.trace(message); break;
            case DEBUG: logger.debug(message); break;
            case INFO:  logger.info(message);  break;
            case WARN:  logger.warn(message);  break;
            case ERROR: logger.error(message); break;
        }
    }

    // Change parameter type to Log4j2's Logger
    private static boolean isLoggerEnabled(Logger logger, LogLevel level) {
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
