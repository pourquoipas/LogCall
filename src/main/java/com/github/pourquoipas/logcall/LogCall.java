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
package com.github.pourquoipas.logcall;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotazione per loggare automaticamente le chiamate a un metodo.
 * Utilizza AspectJ per intercettare l'esecuzione del metodo.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface LogCall {
    /**
     * Specifica il livello di log da utilizzare.
     * Default: TRACE, il più basso.
     * @return Il livello di log.
     */
    LogLevel level() default LogLevel.WARN;

    /**
     * Definisce se loggare i parametri di input del metodo.
     * Default: false.
     * @return true per loggare i parametri, altrimenti false.
     */
    boolean logParameters() default false;

    /**
     * Definisce se loggare il valore di ritorno del metodo.
     * Funziona solo per metodi che non restituiscono void.
     * Default: false.
     * @return true per loggare il valore di ritorno, altrimenti false.
     */
    boolean logReturn() default false;

    /**
     * Definisce se loggare lo stack trace della chiamata.
     * Può essere utile per debug ma è un'operazione costosa.
     * Default: false.
     * @return true per loggare lo stack trace, altrimenti false.
     */
    boolean logStackTrace() default false;

    /**
     * Definisce se loggare lo stack trace dell'eccezione in caso di errore.
     * Se true, lo stack trace completo dell'eccezione verrà loggato.
     * Default: false.
     * @return true per loggare lo stack trace dell'eccezione, altrimenti false.
     */
    boolean logException() default false;

    /**
     * Permette di definire un pattern di log customizzato.
     * Placeholder supportati:
     * - {methodName}: Nome del metodo.
     * - {className}: Nome della classe.
     * - {params}: Rappresentazione in stringa di tutti i parametri.
     * - {param[i]}: Valore del parametro all'indice i (es. {param[0]}).
     * - {return}: Valore di ritorno del metodo.
     * - {stacktrace}: Stack trace della chiamata (o dell'eccezione se presente).
     * Se non specificato, verrà utilizzato un formato di log di default.
     * @return La stringa del pattern di log.
     */
    String customLog() default "";
}
