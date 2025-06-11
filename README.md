### 2. File README.md per GitHub

Questo è un file `README.md` professionale e ben strutturato che puoi usare come pagina principale del tuo progetto su GitHub.


# @LogCall - Logging AOP per Java

`@LogCall` è una libreria Java che fornisce un'annotazione per aggiungere logging dichiarativo ai tuoi metodi con zero codice boilerplate. Sfruttando la potenza di **AspectJ** e **SLF4J**, puoi loggare chiamate a metodi, parametri, valori di ritorno, eccezioni e tempi di esecuzione semplicemente aggiungendo l'annotazione `@LogCall`.

## Caratteristiche Principali

- **Zero Boilerplate**: Dimentica `logger.info("Entering method...")`. Aggiungi solo l'annotazione.
- **Configurabile**: Controlla il livello di log (TRACE, DEBUG, INFO, etc.).
- **Dettagliato**: Logga automaticamente parametri, valori di ritorno e tempi di esecuzione.
- **Gestione Errori**: Logga automaticamente lo stack trace delle eccezioni.
- **Flessibile**: Usa un pattern di log custom per un output personalizzato.
- **Performante**: Utilizza il weaving a tempo di compilazione di AspectJ per un overhead minimo a runtime.
- **Universale**: Si integra con qualsiasi framework di logging che supporti SLF4J (Log4j2, Logback, etc.).

## Come Funziona

Questa libreria non si basa sulla reflection a runtime. Utilizza invece l'**Aspect-Oriented Programming (AOP)** tramite AspectJ. Un plugin Maven speciale ("weaver") modifica il bytecode delle tue classi a tempo di compilazione, iniettando la logica di logging in modo trasparente attorno ai metodi annotati con `@LogCall`.

## Setup del Progetto

Segui questi passaggi per integrare `@LogCall` nel tuo progetto Maven.

### Passaggio 1: Aggiungi la Dipendenza

Aggiungi la dipendenza della libreria `logcall` al tuo `pom.xml`.

```xml
<dependency>
    <groupId>net.gnius</groupId>
    <artifactId>logcall</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

### Passaggio 2: Configura il Plugin AspectJ

L'iniezione del codice di logging è gestita dal `aspectj-maven-plugin`. Aggiungilo alla sezione `<build><plugins>` del tuo `pom.xml`.

```xml
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>aspectj-maven-plugin</artifactId>
    <version>1.14.0</version>
    <configuration>
        <complianceLevel>1.8</complianceLevel>
        <source>1.8</source>
        <target>1.8</target>
        <aspectLibraries>
            <!-- Dice ad AspectJ di cercare gli Aspect nella nostra libreria -->
            <aspectLibrary>
                <groupId>net.gnius</groupId>
                <artifactId>logcall</artifactId>
            </aspectLibrary>
        </aspectLibraries>
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>compile</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```
**Nota:** Se stai sviluppando la libreria e l'applicazione nello stesso progetto multimodulo, potresti non aver bisogno della sezione `<aspectLibraries>`. È fondamentale quando `logcall` è una dipendenza esterna (un file JAR).

### Passaggio 3: Configura un Backend di Logging (SLF4J)

`@LogCall` usa SLF4J. Devi quindi fornire un'implementazione. L'esempio seguente usa **Log4j2**.

Aggiungi le dipendenze necessarie al `pom.xml`:
```xml
<!-- Binding SLF4J con Log4j2 -->
<dependency>
    <groupId>org.apache.logging.log4j</groupId>
    <artifactId>log4j-slf4j2-impl</artifactId>
    <version>2.23.1</version>
</dependency>
```

Crea un file `src/main/resources/log4j2.xml` per configurare dove e come scrivere i log:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<Configuration status="WARN">
    <Appenders>
        <Console name="Console" target="SYSTEM_OUT">
            <PatternLayout pattern="%d{HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n"/>
        </Console>
    </Appenders>
    <Loggers>
        <Root level="info">
            <AppenderRef ref="Console"/>
        </Root>
        
        <!-- Esempio: setta un livello più basso per il tuo package -->
        <Logger name="com.myapp.package" level="trace" additivity="false">
            <AppenderRef ref="Console"/>
        </Logger>
    </Loggers>
</Configuration>
```

## Esempi di Utilizzo

### Log di Base (Parametri e Ritorno)
```java
import net.gnius.logcall.LogCall;
import net.gnius.logcall.LogLevel;

public class MyService {
    @LogCall(level = LogLevel.INFO, logParameters = true, logReturn = true)
    public String process(String input) {
        return "Processed: " + input;
    }
}
```
**Output del Log:**
`INFO com.mycompany.MyService - Method 'process' | Params: [Hello] | Return: Processed: Hello | Duration: 2ms`

### Log di Eccezioni
```java
public class MyService {
    @LogCall(level = LogLevel.ERROR, logException = true)
    public void validate(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("Value cannot be negative");
        }
    }
}
```
**Output del Log:**
`ERROR com.mycompany.MyService - Method 'validate' | Threw Exception: IllegalArgumentException | Duration: 5ms
Exception Stack Trace:
java.lang.IllegalArgumentException: Value cannot be negative
	at com.mycompany.MyService.validate(MyService.java:10)
    ...`

### Log con Pattern Custom
```java
public class MyService {
    @LogCall(customLog = "Calling {methodName} for user {user}. Result: {return}")
    public String getUserData(String user) {
        return "DataFor" + user;
    }
}
```
**Output del Log:**
`TRACE com.mycompany.MyService - Calling getUserData for user JohnDoe. Result: DataForJohnDoe`

#### Placeholder per `customLog`
| Placeholder   | Descrizione                                                     |
|---------------|-----------------------------------------------------------------|
| `{methodName}`| Il nome del metodo annotato.                                    |
| `{className}` | Il nome semplice della classe.                                  |
| `{params}`    | Una lista di tutti i parametri separati da virgola.             |
| `{param[i]}`  | Il valore del parametro all'indice `i` (es. `{param[0]}`).      |
| `{*nomeParam*}` | Il valore del parametro con quel nome (richiede Java 8+ e flag `-parameters`). |
| `{return}`    | Il valore di ritorno del metodo.                                |
| `{stacktrace}`| Lo stack trace (della chiamata o dell'eccezione se presente). |
| `{exception}` | Lo stack trace specifico di un'eccezione (se sollevata).      |

### Opzioni dell'annotazione `@LogCall`
| Attributo         | Tipo          | Descrizione                                                                      | 
|-------------------|---------------|----------------------------------------------------------------------------------|
| `level`           | `LogLevel`    | Il livello di log (TRACE, DEBUG, INFO, WARN, ERROR). Default: WARN.              |
| `logParameters`   | `boolean`     | Logga i parametri del metodo. Default: false.                                    |
| `logReturn`       | `boolean`     | Logga il valore di ritorno del metodo. Default: false.                           |
| `logException`    | `boolean`     | Logga le eccezioni lanciate dal metodo. Default: false.                          |
| `customLog`       | `String`      | Un pattern di log personalizzato. Usa i placeholder per formattare il messaggio. |
| `logStacktrace` | `boolean`     | Logga lo stack trace della chiamata o dell'eccezione. Default: false.            |
