package net.gnius.logcall;

/**
 * Enum per definire i livelli di log supportati dall'annotazione @LogCall.
 * Questo permette di avere un controllo typsafe sul livello di logging.
 */
public enum LogLevel {
    TRACE,
    DEBUG,
    INFO,
    WARN,
    ERROR
}
