/*
 * Copyright (c) 2025 Gianluca Terenziani
 *
 * Questo file è parte di LogCall.
 * SafeJson è distribuito sotto i termini della licenza
 * Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International.
 *
 * Dovresti aver ricevuto una copia della licenza insieme a questo progetto.
 * In caso contrario, la puoi trovare su: http://creativecommons.org/licenses/by-nc-sa/4.0/
 */
package net.gnius.logcall;

import org.apache.logging.log4j.LogManager;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Aspetto AspectJ che implementa la logica per l'annotazione @LogCall.
 * Intercetta l'esecuzione dei metodi annotati e produce i log richiesti.
 */
@Aspect
public class LogCallAspect {

    @Pointcut("execution(* *(..)) && @annotation(logCall)")
    public void methodAnnotatedWithLogCall(LogCall logCall) {}

    @Around("methodAnnotatedWithLogCall(logCall)")
    public Object logMethodCall(ProceedingJoinPoint joinPoint, LogCall logCall) throws Throwable {
        Logger logger = LoggerFactory.getLogger(joinPoint.getSignature().getDeclaringType());

/*
        // Enhanced debug information
        System.out.println(">>>>>>>>CALLED logMethodCall: " + logger.getName() +
                " for " + joinPoint.getSignature().getName() +
                " | Log Level: " + logCall.level() +
                " | Is Enabled: " + isLoggerEnabled(logger, logCall.level()));

*/

        // Check if the logger has a ListAppender by class name
        // Retrieve the logger name
        String loggerName = joinPoint.getSignature().getDeclaringTypeName();

        // Get the Log4j core logger directly
        org.apache.logging.log4j.core.Logger coreLogger =
                (org.apache.logging.log4j.core.Logger) LogManager.getLogger(loggerName);

        // Check if the logger has a ListAppender by class name
        boolean hasListAppender = coreLogger.getAppenders().values().stream()
                .anyMatch(appender -> appender.getClass().getSimpleName().equals("ListAppender"));

/*
        System.out.println("Logger: " + coreLogger.getName() +
                " | Has ListAppender: " + hasListAppender +
                " | Appenders: " + coreLogger.getAppenders().keySet());
*/


        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String methodName = method.getName();
        String className = method.getDeclaringClass().getSimpleName();
        Object[] methodArgs = joinPoint.getArgs();
        String[] paramNames = signature.getParameterNames();

        long startTime = System.currentTimeMillis();
        Object result = null;
        Throwable exception = null;

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable t) {
            exception = t;
            throw t;
        } finally {
            long duration = System.currentTimeMillis() - startTime;

            if (logCall.customLog() != null && !logCall.customLog().isEmpty()) {
                String customLogMessage = formatCustomLog(logCall.customLog(), methodName, className, paramNames, methodArgs, result, exception);
                log(logger, logCall.level(), customLogMessage);
            } else {
                StringBuilder logMessage = new StringBuilder();
                logMessage.append("Method '").append(methodName).append("'");

                if (logCall.logParameters() && paramNames != null && paramNames.length > 0) {
                    logMessage.append(" | Params: [");
                    logMessage.append(Arrays.stream(methodArgs)
                            .map(arg -> Objects.toString(arg, "null"))
                            .collect(Collectors.joining(", ")));
                    logMessage.append("]");
                }

                if (exception != null) {
                    logMessage.append(" | Threw Exception: ").append(exception.getClass().getSimpleName());
                } else if (logCall.logReturn() && signature.getReturnType() != void.class) {
                    logMessage.append(" | Return: ").append(Objects.toString(result, "null"));
                }

                logMessage.append(" | Duration: ").append(duration).append("ms");

                if (logCall.logStackTrace()) {
                    StringWriter sw = new StringWriter();
                    new Throwable("Stack trace for call").printStackTrace(new PrintWriter(sw));
                    logMessage.append("\nCall Stack Trace:\n").append(cleanStackTrace(sw.toString()));
                }

                if (exception != null && logCall.logException()) {
                    StringWriter sw = new StringWriter();
                    exception.printStackTrace(new PrintWriter(sw));
                    logMessage.append("\nException Stack Trace:\n").append(cleanStackTrace(sw.toString()));
                }

                log(logger, logCall.level(), logMessage.toString());
            }
        }
    }
    // Helper method to check if the logger is enabled for the specified level
    private boolean isLoggerEnabled(Logger logger, LogLevel level) {
        switch (level) {
            case DEBUG: return logger.isDebugEnabled();
            case INFO: return logger.isInfoEnabled();
            case WARN: return logger.isWarnEnabled();
            case ERROR: return logger.isErrorEnabled();
            case TRACE: return logger.isTraceEnabled();
            default: return false;
        }
    }
    /**
     * Pulisce uno stack trace rimuovendo le righe relative all'aspect di logging.
     * @param fullStackTrace Lo stack trace completo come stringa.
     * @return Lo stack trace pulito.
     */
    private String cleanStackTrace(String fullStackTrace) {
        if (fullStackTrace == null || fullStackTrace.isEmpty()) {
            return "";
        }
return Arrays.stream(fullStackTrace.split("\r?\n"))
                .filter(line -> {
                    String trimmed = line.trim();
                    return !trimmed.startsWith("at net.gnius.logcall.LogCallAspect") &&
                           !trimmed.startsWith("net.gnius.logcall.LogCallAspect");
                })
                .collect(Collectors.joining("\n"));
    }

    private String formatCustomLog(String pattern, String methodName, String className, String[] paramNames, Object[] args, Object result, Throwable exception) {
        String log = pattern;
        log = log.replace("{methodName}", methodName);
        log = log.replace("{className}", className);

        if (log.contains("{params}")) {
            String allParams = Arrays.stream(args)
                    .map(arg -> Objects.toString(arg, "null"))
                    .collect(Collectors.joining(", "));
            log = log.replace("{params}", allParams);
        }

        if(paramNames != null) {
            for (int i = 0; i < args.length; i++) {
                log = log.replace("{param[" + i + "]}", Objects.toString(args[i], "null"));
                if (i < paramNames.length) {
                    log = log.replace("{" + paramNames[i] + "}", Objects.toString(args[i], "null"));
                }
            }
        }

        if (log.contains("{return}")) {
            log = log.replace("{return}", Objects.toString(result, "null"));
        }

        if (log.contains("{stacktrace}")) {
            StringWriter sw = new StringWriter();
            String prefix = "";
            if (exception != null) {
                prefix = "Method raised Exception: " + exception.getMessage() + "\n";
                exception.printStackTrace(new PrintWriter(sw));
            } else {
                StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
                if (stackTrace != null && stackTrace.length > 1) {
                    for (int i = 1; i < stackTrace.length; i++) {
                        StackTraceElement ste = stackTrace[i];
                        if (ste != null) {
                            sw.write(ste.toString() + "\n");
                        }
                    }
                }
            }// Capture the current stack trace
            // (exception != null ? exception : new Throwable("Stack trace for call")).printStackTrace(new PrintWriter(sw));
            String cleanTrace = prefix + cleanStackTrace(sw.toString());
            log = log.replace("{stacktrace}", cleanTrace);
        } else if (log.contains("{exception}") && exception != null) {
            StringWriter sw = new StringWriter();
            String prefix = "Method raised Exception: " + exception.getMessage() + "\n";
            exception.printStackTrace(new PrintWriter(sw));
            String cleanTrace = prefix + cleanStackTrace(sw.toString());
            log = log.replace("{exception}", cleanTrace);
        }

        return log;
    }

    private void log(Logger logger, LogLevel level, String message) {
        switch (level) {
            case DEBUG:
                if (logger.isDebugEnabled()) logger.debug(message);
                break;
            case INFO:
                if (logger.isInfoEnabled()) logger.info(message);
                break;
            case WARN:
                if (logger.isWarnEnabled()) logger.warn(message);
                break;
            case ERROR:
                if (logger.isErrorEnabled()) logger.error(message);
                break;
            case TRACE:
            default:
                if (logger.isTraceEnabled()) logger.trace(message);
                break;
        }
    }
}
