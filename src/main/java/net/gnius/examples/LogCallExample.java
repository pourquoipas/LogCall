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
package net.gnius.examples;

import net.gnius.logcall.LogCall;
import net.gnius.logcall.LogLevel;

import java.math.BigDecimal;

public class LogCallExample {


    @LogCall(level = LogLevel.INFO, logParameters = true, logReturn = true, logException = true)
    public String processData(String name, int value) {
        System.out.println(">>> Eseguendo la logica di business in processData...");
        if (value < 0) {
            throw new IllegalArgumentException("Il valore non può essere negativo");
        }
        return "Risultato per " + name + " è " + (value * 2);
    }

    @LogCall(logStackTrace = true)
    public void anotherMethod() {
        System.out.println(">>> Eseguendo anotherMethod...");
    }

    @LogCall(
            level = LogLevel.WARN,
            customLog = "Chiamata a 'criticalOperation'. Parametro amount: {amount}. Stack:\n{stacktrace}"
    )
    public void criticalOperation(BigDecimal amount, String user) {
        System.out.println(">>> Eseguendo criticalOperation...");
    }

    // Il main per testare l'applicazione
    public static void main(String[] args) {
        LogCallExample service = new LogCallExample();
        service.processData("Test1", 100);
        service.anotherMethod();
        service.criticalOperation(new BigDecimal("123.45"), "admin");
        try {
            service.processData("Test2", -5);
        } catch (Exception e) {
            // L'eccezione verrà loggata automaticamente dall'aspect
            System.out.println(">>>> Eccezione comunque rilanciata: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
