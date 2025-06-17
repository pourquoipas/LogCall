/*
 * Copyright (c) 2025 Gianluca Terenziani
 *
 * Questo file è parte di LogCall.
 * LogCall è distribuito sotto i termini della licenza
 * Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International.
 *
 * Dovresti aver ricevuto una copia della licenza insieme a questo progetto.
 * In caso contrario, la puoi trovare su: http://creativecommons.org/licenses/by-nc-sa/4.0/
 */
package com.github.pourquoipas.logcall;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


public class TestLogCall {

    // Un appender custom per catturare i log in una lista.
    private static class ListAppender extends AbstractAppender {
        private final List<String> messages = new ArrayList<>();

        protected ListAppender(String name, org.apache.logging.log4j.core.Filter filter, org.apache.logging.log4j.core.Layout<? extends Serializable> layout) {
            super(name, filter, layout, false, Property.EMPTY_ARRAY);
        }

        @Override
        public void append(LogEvent event) {
            if (!isStarted()) {
                return;
            }
            // Use getFormattedMessage() for raw message, or toSerializable for formatted by layout
            // PatternLayout.createDefaultLayout() is often just the message without extra info in toString
            // If you need the full pattern, apply it here or ensure layout is configured.
            // For simple capture, event.getMessage().getFormattedMessage() is often enough
            messages.add(event.getMessage().getFormattedMessage()); // Capture raw message for easier assertion
        }

        public List<String> getMessages() {
            return messages;
        }

        public void clear() {
            messages.clear();
        }
    }

    private ListAppender listAppender;
    private LoggerContext ctx; // Reference to the LoggerContext
    private org.apache.logging.log4j.core.Logger testServiceLog4jLogger; // Log4j2 Logger instance


/*
    // This static block will execute the very first time TestLogCallAspect is loaded
    static {
        System.out.println("### STATIC INIT: Attempting very early ByteBuddy agent installation... ###");
        try {
            // Install the ByteBuddyAgent into the current JVM process
            ByteBuddyAgent.install();

            new AgentBuilder.Default()
                    // Keep your specific matcher. Adding nameStartsWith is a good practice.
                    .type(ElementMatchers.nameStartsWith("com.github.pourquoipas.logcall.")
                            .or(ElementMatchers.nameStartsWith("com.github.pourquoipas.examples."))
                            .and(ElementMatchers.declaresMethod(ElementMatchers.isAnnotatedWith(LogCall.class)))
                    )
                    .transform((builder, typeDescription, classLoader, module, protectionDomain) -> {
                        System.out.println("### LogCall Agent (Static Init): TRANSFORMING CLASS: " + typeDescription.getName());
                        return builder
                                .method(ElementMatchers.isAnnotatedWith(LogCall.class))
                                .intercept(MethodDelegation.to(LogCallInterceptor.class));
                    })
                    .with(AgentBuilder.Listener.StreamWriting.toSystemOut()) // Still good for debug
                    .installOnByteBuddyAgent(); // Install on the agent installed by ByteBuddyAgent.install()
            System.out.println("### STATIC INIT: ByteBuddy agent installed successfully. ###");
        } catch (Throwable t) {
            System.err.println("### STATIC INIT: CRITICAL ERROR during early agent installation: " + t.getMessage());
            t.printStackTrace(System.err);
            // Optionally re-throw a RuntimeException to fail the test early if agent fails
            // throw new RuntimeException("Failed to install ByteBuddy agent in static initializer", t);
        }
    }
*/



/*

    // Static block or @BeforeAll for one-time agent installation
    @BeforeAll
    static void installByteBuddyAgent() {
        System.out.println("### ByteBuddyAgent: Dynamically installing agent for tests... ###");
        // Install the ByteBuddyAgent into the current JVM process
        ByteBuddyAgent.install();

        // This is where you put the *exact same* AgentBuilder logic from your YourByteBuddyAgent.premain
        new AgentBuilder.Default()
                .type(ElementMatchers.declaresMethod(ElementMatchers.isAnnotatedWith(LogCall.class)))
                .transform((builder, typeDescription, classLoader, module, protectionDomain) -> {
                    System.out.println("### LogCall Agent (Dynamic): Transforming class: " + typeDescription.getName());
                    return builder
                            .method(ElementMatchers.isAnnotatedWith(LogCall.class))
                            .intercept(MethodDelegation.to(LogCallInterceptor.class));
                })
                .with(AgentBuilder.Listener.StreamWriting.toSystemOut()) // Keep debug listeners
                .installOnByteBuddyAgent(); // *** This is the key change! ***
        // Installs on the agent installed by ByteBuddyAgent.install()
        System.out.println("### ByteBuddyAgent: Agent installed successfully! ###");
    }
*/


    @BeforeEach
    void setUp() {
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>BEFORE EACH TEST<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");

        // *** IMPORTANT: Ensure Log4j2 is fully shut down from previous tests ***
        // This is more robust than just ctx.stop()/start() for complete isolation
        LogManager.shutdown(); // Give it a short timeout to clear resources

        // Get the LoggerContext. Using true often forces a new context if default was loaded.
        ctx = (LoggerContext) LogManager.getContext(false); // Can be true or false, depends on how other things initialize
        ctx.reconfigure(); // Forces Log4j2 to reload its configuration (e.g., log4j2.xml if present)

        // Reset the configuration within the context
        Configuration config = ctx.getConfiguration();
        // Remove all existing appenders from root and any specific loggers to ensure clean slate
        config.getRootLogger().getAppenders().keySet().forEach(config.getRootLogger()::removeAppender);
        config.getLoggers().values().forEach(lc -> lc.getAppenders().keySet().forEach(lc::removeAppender));
        // Remove any existing loggers that might have been configured by XML or previous tests
        new ArrayList<>(config.getLoggers().keySet()).forEach(config::removeLogger); // Avoid ConcurrentModificationException

        // Create and start the ListAppender
        listAppender = new ListAppender("ListAppender", null, PatternLayout.newBuilder().withPattern("%msg%n").build());
        listAppender.start();

        // Add the custom appender to the configuration
        config.addAppender(listAppender);

        // Get the specific logger (which AspectJ's woven code will use)
        String loggerName = TestService.class.getName();
        testServiceLog4jLogger = (org.apache.logging.log4j.core.Logger) LogManager.getLogger(loggerName);

        // Define a LoggerConfig for this specific logger, ensuring it captures everything
        // IMPORTANT: Use LoggerConfig for the logger that AspectJ will get.
        // AspectJ's LoggerFactory.getLogger(declaringType) will get a logger for TestService.class.
        LoggerConfig loggerConfig = config.getLoggerConfig(loggerName); // Get existing or implicitly create
        if (!loggerConfig.getName().equals(loggerName)) { // If it's a parent config (e.g., root), create a new one for TestService.class
            loggerConfig = new LoggerConfig(loggerName, Level.ALL, false);
            config.addLogger(loggerName, loggerConfig);
        }
        loggerConfig.setLevel(Level.ALL); // Set level to ALL to capture everything
        loggerConfig.addAppender(listAppender, Level.ALL, null); // Add appender to this specific logger config
        loggerConfig.setAdditive(false); // Crucial: Prevent this logger's events from going to parent loggers (like root)

        // Update the LoggerContext for changes to take effect
        ctx.updateLoggers();

        // Debugging: Verify appenders and levels
        System.out.println("Configured Logger: " + testServiceLog4jLogger.getName() + " Level: " + testServiceLog4jLogger.getLevel());
        System.out.println("Is LoggerConfig Additive: " + config.getLoggerConfig(loggerName).isAdditive());
        System.out.println("TestService Logger Appenders (from config): " + config.getLoggerConfig(loggerName).getAppenders().keySet());
        System.out.println("ListAppender is started: " + listAppender.isStarted());
        System.out.println("Initial messages size: " + listAppender.getMessages().size()); // Should be 0
    }

    @AfterEach
    void tearDown() {
        System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<AFTER EACH TEST>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");

        // Clean up the appender and reset Log4j2 for subsequent tests
        if (listAppender != null && listAppender.isStarted()) {
            Configuration config = ctx.getConfiguration();

            // Get the specific LoggerConfig to remove the appender
            LoggerConfig loggerConfig = config.getLoggerConfig(testServiceLog4jLogger.getName());

            // Check if the appender is attached to this logger config and remove it
            if (loggerConfig != null && loggerConfig.getAppenders().containsKey(listAppender.getName())) {
                loggerConfig.removeAppender(listAppender.getName());
            }

            // Remove the LoggerConfig for the TestService class to clean up
            config.removeLogger(testServiceLog4jLogger.getName());

            ctx.updateLoggers(); // Apply changes to the context
            listAppender.stop(); // Stop the appender itself
        }

        // Full shutdown to ensure no lingering Log4j2 context affects next test
        LogManager.shutdown();
    }

    @Test
    void testSimpleLog_logsParametersAndReturn() {
        // Arrange
        TestService service = new TestService();
        // TestService service = LogCallFactory.create(TestService.class);
        // Act
        service.simpleLog("Test", 123);

        // Assert
        assertEquals(1, listAppender.getMessages().size(), "Expected 1 log message for simpleLog");
        String logMessage = listAppender.getMessages().get(0);
        assertTrue(logMessage.contains("Method 'simpleLog'"), "Log message should contain method name");
        assertTrue(logMessage.contains("| Params: [Test, 123]"), "Log message should contain parameters");
        assertTrue(logMessage.contains("| Return: OK-Test"), "Log message should contain return value");
    }

    @Test
    void testExceptionLog_logsExceptionStackTrace() {
        // Arrange
        TestService service = new TestService();
        // TestService service = LogCallFactory.create(TestService.class);

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> service.exceptionLog("fail"));

        assertEquals(1, listAppender.getMessages().size(), "Expected 1 log message for exceptionLog");
        String logMessage = listAppender.getMessages().get(0);

        assertTrue(logMessage.contains("Method 'exceptionLog'"), "Log message should contain method name");
        assertTrue(logMessage.contains("| Threw Exception: IllegalStateException"), "Log message should indicate exception");
        assertTrue(logMessage.contains("Exception Stack Trace:"), "Log message should contain 'Exception Stack Trace:'");
        assertTrue(logMessage.contains("java.lang.IllegalStateException: Test Exception"), "Log message should contain exception details");
        // Verifica che lo stacktrace non contenga l'aspect stesso
        assertFalse(logMessage.contains("com.github.pourquoipas.logcall.LogCallAspect"), "Log message should not contain AspectJ internal classes");
    }

    @Test
    void testStackTraceLog_logsCallStack() {
        // Arrange
        TestService service = new TestService();
        // TestService service = LogCallFactory.create(TestService.class);

        // Act
        service.stackTraceLog();

        // Assert
        assertEquals(1, listAppender.getMessages().size(), "Expected 1 log message for stackTraceLog");
        String logMessage = listAppender.getMessages().get(0);
        System.out.println(">>>>><<<<<>>><<<<<<>>>>>\n" + logMessage);

        assertTrue(logMessage.contains("Method 'stackTraceLog'"), "Log message should contain method name");
        assertTrue(logMessage.contains("Call Stack Trace:"), "Log message should contain 'Call Stack Trace:'");
        assertTrue(logMessage.contains("at com.github.pourquoipas.logcall.TestLogCall.testStackTraceLog"), "Log message should contain calling test method");
        assertFalse(logMessage.contains("com.github.pourquoipas.logcall.LogCallAspect"), "Log message should not contain AspectJ internal classes");
    }

    @Test
    void testCustomLog_formatsMessageCorrectly() {
        // Arrange
        TestService service = new TestService();
        // TestService service = LogCallFactory.create(TestService.class);

        // Act
        service.customLog("ParameterValue");

        // Assert
        assertEquals(1, listAppender.getMessages().size(), "Expected 1 log message for customLog");
        String logMessage = listAppender.getMessages().get(0);
        assertTrue(logMessage.contains("Custom log for customLog with param ParameterValue and return CustomReturn"), "Custom log message should be correctly formatted: " + logMessage) ;
    }
}
