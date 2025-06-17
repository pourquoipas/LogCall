# LogCall - Automatic Method Call Logging

LogCall is a lightweight Java library that provides a powerful `@LogCall` annotation to automatically log method calls, including parameters, return values, execution time, and exceptions, using compile-time bytecode weaving with ByteBuddy.

This approach means there is zero performance overhead from reflection at runtime, as the logging logic is injected directly into your class files during the build process.

---

## Features

- **Declarative Logging**: Simply add the `@LogCall` annotation to any method to enable logging.
- **Highly Configurable**: Customize the log level, and choose whether to log parameters, return values, exceptions, and stack traces.
- **Custom Log Messages**: Define your own log message patterns with placeholders for method details (e.g., `{methodName}`, `{params}`, `{return}`, and even parameter names like `{myParam}`).
- **Zero Runtime Overhead**: All bytecode manipulation happens at compile time, so your production code runs at full speed.
- **Log4j Integration**: Seamlessly integrates with Log4j 2 for robust and flexible logging.

---

## License

This library is licensed under the **Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International**.

For the full license text, please visit: [http://creativecommons.org/licenses/by-nc-sa/4.0/](http://creativecommons.org/licenses/by-nc-sa/4.0/)

---

## Installation

To use LogCall in your Maven project, add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.github.pourquoipas</groupId>
    <artifactId>logcall</artifactId>
    <version>1.0.0</version> <!-- Use the latest version -->
</dependency>
```

---

## Configuration for Compile-Time Weaving

The core of LogCall is a compile-time weaver that modifies your `.class` files. You need to configure the `exec-maven-plugin` to run this weaver after your project's classes have been compiled.

Add the following plugin configuration to the `<build><plugins>` section of your `pom.xml`:

```xml
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>exec-maven-plugin</artifactId>
    <version>3.1.0</version>
    <executions>
        <execution>
            <id>weave-classes</id>
            <phase>process-classes</phase> <!-- This runs after the 'compile' phase -->
            <goals>
                <goal>java</goal>
            </goals>
            <configuration>
                <mainClass>com.github.pourquoipas.logcall.LogCallClassWeaver</mainClass>
                <arguments>
                    <!-- This points the weaver to your compiled classes -->
                    <argument>${project.build.outputDirectory}</argument>
                </arguments>
                <!--
                   The weaver needs its own dependencies (like ByteBuddy and Log4j)
                   on the classpath to run correctly.
                -->
                <includePluginDependencies>true</includePluginDependencies>
            </configuration>
        </execution>
    </executions>
    <!--
       Define the weaver and its dependencies here so the plugin can find them.
    -->
    <dependencies>
        <dependency>
            <groupId>com.github.pourquoipas</groupId>
            <artifactId>logcall</artifactId>
            <version>1.0.0</version> <!-- Use the same version as your project dependency -->
        </dependency>
    </dependencies>
</plugin>
```

**Important:** For logging parameter names (e.g., `{amount}`), you must configure the `maven-compiler-plugin` to include parameter name information in the bytecode.

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>3.8.1</version>
    <configuration>
        <source>1.8</source> <!-- Or your Java version -->
        <target>1.8</target> <!-- Or your Java version -->
        <parameters>true</parameters> <!-- This is the crucial flag -->
    </configuration>
</plugin>
```

---

## Usage

Using `@LogCall` is straightforward. Simply annotate any method you wish to log. The library includes a `LogCallExample` class (which is not part of the final JAR) to demonstrate its usage.

### Basic Logging

Log method entry, parameters, return value, and exceptions at the INFO level.

```java
import com.github.pourquoipas.logcall.LogCall;
import com.github.pourquoipas.logcall.LogLevel;

public class MyService {

    @LogCall(level = LogLevel.INFO, logParameters = true, logReturn = true, logException = true)
    public String processData(String name, int value) {
        if (value < 0) {
            throw new IllegalArgumentException("Value cannot be negative");
        }
        return "Result for " + name + " is " + (value * 2);
    }
}
```

### Stack Trace Logging

Log the call stack trace, which is useful for debugging but can be expensive.

```java
@LogCall(logStackTrace = true)
public void anotherMethod() {
    // ... business logic ...
}
```

### Custom Log Messages

Define a completely custom log message pattern using placeholders. You can reference parameters by name if you've enabled the `-parameters` compiler flag.

```java
import java.math.BigDecimal;

public class PaymentService {
    @LogCall(
            level = LogLevel.WARN,
            customLog = "Critical call to '{methodName}'. User: {user}, Amount: {amount}."
    )
    public void criticalOperation(BigDecimal amount, String user) {
        // ... critical business logic ...
    }
}
```

**Supported Placeholders for `customLog`:**
- `{methodName}`: The name of the annotated method.
- `{className}`: The simple name of the class.
- `{params}`: A comma-separated list of all parameter values.
- `{param[i]}`: The value of the parameter at index `i` (e.g., `{param[0]}`).
- `{[paramName]}`: The value of the parameter by its name (e.g., `{user}`, `{amount}`). **Requires `-parameters` compiler flag.**
- `{return}`: The value returned by the method.
- `{exception}`: The stack trace of any exception thrown.

---

## How It Works

This library uses **ByteBuddy**, a powerful code generation and manipulation library.

1.  Your Java code is compiled into `.class` files by the `maven-compiler-plugin`.
2.  The `exec-maven-plugin` then runs `LogCallClassWeaver`.
3.  The weaver scans your compiled classes for the `@LogCall` annotation.
4.  For each annotated method, it uses ByteBuddy's `Advice` API to inject the logging logic from `LogCallAdvice` directly into the method's bytecode.
5.  It also adds a private `@AlreadyWoven` annotation to the class to ensure it is never woven more than once.

The final `.class` files in your `target/classes` directory contain the logging calls, ready to be packaged into a JAR.
