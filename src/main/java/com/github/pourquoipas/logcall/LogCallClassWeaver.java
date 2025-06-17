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

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.pool.TypePool;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Stream;

/**
 * A compile-time class weaver using ByteBuddy.
 * This version uses the robust `Advice` API and is idempotent, preventing classes
 * from being woven more than once.
 */
public class LogCallClassWeaver {

    /**
     * A private marker annotation to detect if a class has already been transformed.
     * This prevents the weaver from applying advice multiple times to the same class.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    private @interface AlreadyWoven {
    }

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

                            // --- IDEMPOTENCY CHECK ---
                            // Skip this class if it already has our marker annotation.
                            if (ElementMatchers.isAnnotatedWith(AlreadyWoven.class).matches(typeDescription)) {
                                return;
                            }

                            boolean hasAnnotatedMethod = ElementMatchers.declaresMethod(
                                    ElementMatchers.isAnnotatedWith(LogCall.class)
                            ).matches(typeDescription);

                            if (hasAnnotatedMethod) {
                                System.out.println("### ByteBuddy CTI: Found target for transformation: " + typeDescription.getName());

                                DynamicType.Unloaded<?> unloaded = byteBuddy.redefine(typeDescription, classFileLocator)
                                        .visit(Advice.to(LogCallAdvice.class).on(ElementMatchers.isAnnotatedWith(LogCall.class)))
                                        // Add the marker annotation to prevent re-weaving in the future.
                                        .annotateType(AnnotationDescription.Builder.ofType(AlreadyWoven.class).build())
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



