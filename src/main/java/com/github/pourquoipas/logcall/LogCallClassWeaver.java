package com.github.pourquoipas.logcall;

import com.github.pourquoipas.logcall.LogCall;
import com.github.pourquoipas.logcall.LogCallAdvice;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.pool.TypePool;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Stream;

/**
 * A compile-time class weaver using ByteBuddy.
 * This version uses the robust `Advice` API and the external `LogCallAdvice`
 * to implement the full logging functionality.
 */
public class LogCallClassWeaver {

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Usage: java LogCallClassWeaver <classes_directory>");
            System.exit(1);
        }

        File classesDir = new File(args[0]);
        if (!classesDir.isDirectory()) {
            System.err.println("Error: " + args[0] + " is not a valid directory.");
            System.exit(1);
        }

        System.out.println("### ByteBuddy CTI: Starting class weaving in directory: " + classesDir.getAbsolutePath());

        // Use a robust ClassFileLocator to find all necessary classes.
        ClassFileLocator classFileLocator = new ClassFileLocator.Compound(
                new ClassFileLocator.ForFolder(classesDir),
                ClassFileLocator.ForClassLoader.of(Thread.currentThread().getContextClassLoader()),
                ClassFileLocator.ForClassLoader.ofPlatformLoader()
        );
        TypePool typePool = TypePool.Default.of(classFileLocator);

        ByteBuddy byteBuddy = new ByteBuddy().with(TypeValidation.DISABLED);

        try (Stream<Path> paths = Files.walk(classesDir.toPath())) {
            paths.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".class"))
                    .forEach(classFile -> {
                        String className = getClassName(classesDir.toPath(), classFile);
                        // Avoid transforming the weaver, the advice, or the annotation itself.
                        if (className == null || className.equals(LogCallClassWeaver.class.getName()) || className.equals(LogCallAdvice.class.getName())) {
                            return;
                        }

                        try {
                            TypePool.Resolution resolution = typePool.describe(className);
                            if (!resolution.isResolved()) {
                                System.err.println("### ByteBuddy CTI: Could not resolve class from pool: " + className);
                                return;
                            }
                            TypeDescription typeDescription = resolution.resolve();

                            // Find methods annotated with your existing @LogCall annotation.
                            boolean hasAnnotatedMethod = ElementMatchers.declaresMethod(
                                    ElementMatchers.isAnnotatedWith(LogCall.class)
                            ).matches(typeDescription);

                            if (hasAnnotatedMethod) {
                                System.out.println("### ByteBuddy CTI: Found target for transformation: " + typeDescription.getName());

                                // Use the .visit() method with Advice.to()
                                // This applies the enter/exit logic from LogCallAdvice to the matched methods.
                                DynamicType.Unloaded<?> unloaded = byteBuddy.redefine(typeDescription, classFileLocator)
                                        .visit(Advice.to(LogCallAdvice.class).on(ElementMatchers.isAnnotatedWith(LogCall.class)))
                                        .make();

                                byte[] originalBytes = Files.readAllBytes(classFile);
                                byte[] transformedBytes = unloaded.getBytes();

                                if (!Arrays.equals(originalBytes, transformedBytes)) {
                                    Files.write(classFile, transformedBytes);
                                    System.out.println("### ByteBuddy CTI: Successfully transformed: " + typeDescription.getName());
                                }
                            }
                        } catch (Exception e) {
                            System.err.println("### ByteBuddy CTI: Error processing " + className + ": " + e.getMessage());
                            e.printStackTrace(System.err);
                        }
                    });
        }
        System.out.println("### ByteBuddy CTI: Class weaving complete.");
    }

    private static String getClassName(Path baseDir, Path classFile) {
        try {
            String relativePath = baseDir.relativize(classFile).toString();
            return relativePath.substring(0, relativePath.length() - ".class".length())
                    .replace(File.separatorChar, '.');
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}



