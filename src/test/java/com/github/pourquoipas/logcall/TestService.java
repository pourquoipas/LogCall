package com.github.pourquoipas.logcall;

// Classe fittizia su cui testare l'annotazione
public class TestService {


    static {
        System.out.println("### TestHelper: Static TestService initializer ###");
    }

    public TestService() {
        System.out.println("### TestHelper: Requesting new TestService instance (forcing class load)... ###");
    }

    @LogCall(level = LogLevel.INFO, logParameters = true, logReturn = true)
    public String simpleLog(String param1, int param2) {
        return "OK-" + param1;
    }

    @LogCall(level = LogLevel.WARN, logException = true)
    public void exceptionLog(String input) {
        throw new IllegalStateException("Test Exception");
    }

    @LogCall(logStackTrace = true)
    public String stackTraceLog() {
        // do nothing
        return "logStackTraceLog";
    }

    @LogCall(customLog = "Custom log for {methodName} with param {p1} and return {return}")
    public String customLog(String p1) {
        return "CustomReturn";
    }
}

