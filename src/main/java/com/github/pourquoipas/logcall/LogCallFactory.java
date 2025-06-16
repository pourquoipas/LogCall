package com.github.pourquoipas.logcall;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class LogCallFactory {

    private static final Map<Class<?>, Class<?>> proxyCache = new ConcurrentHashMap<>();

    private LogCallFactory() {}

    public static <T> T create(Class<T> clazz) {
        return create(clazz, new Class[0], new Object[0]);
    }

    public static <T> T create(Class<T> clazz, Object... constructorArgs) {
        if (constructorArgs == null || constructorArgs.length == 0) {
            return create(clazz);
        }
        Class<?>[] argTypes = Arrays.stream(constructorArgs)
                .map(Object::getClass)
                .toArray(Class[]::new);
        return create(clazz, argTypes, constructorArgs);
    }

    public static <T> T create(Class<T> clazz, Class<?>[] constructorArgTypes, Object[] constructorArgs) {
        try {
            Class<?> proxyClass = proxyCache.computeIfAbsent(clazz, c -> {
                return new ByteBuddy()
                        .subclass(c)
                        .method(ElementMatchers.isAnnotatedWith(LogCall.class))
                        .intercept(MethodDelegation.to(LogCallInterceptor.class))
                        .make()
                        .load(c.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                        .getLoaded();
            });

            return (T) proxyClass.getConstructor(constructorArgTypes).newInstance(constructorArgs);

        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            // Cerca un costruttore compatibile in caso di mismatch (es. int vs Integer)
            try {
                return (T) proxyCache.get(clazz).getConstructors()[0].newInstance(constructorArgs);
            } catch (Exception innerEx) {
                // Se anche il fallback fallisce, lancia l'eccezione originale
                throw new IllegalStateException("Impossibile creare un'istanza proxy per " + clazz.getName() + ". Verifica i parametri del costruttore.", e);
            }
        }
    }
}
