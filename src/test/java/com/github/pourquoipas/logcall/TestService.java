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

