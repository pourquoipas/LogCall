package com.github.pourquoipas.logcall;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.instrument.Instrumentation;

public class LogCallAgent {

    public static void premain(String agentArgs, Instrumentation inst) {
        System.out.println("### LogCall ByteBuddy Agent is loaded! ###");

        new AgentBuilder.Default()
                // Define which classes to transform: those that declare methods annotated with @LogCall
                .type(ElementMatchers.declaresMethod(ElementMatchers.isAnnotatedWith(LogCall.class)))
                .transform((builder, typeDescription, classLoader, module, protectionDomain) -> {
                    System.out.println("### LogCall Agent: Transforming class: " + typeDescription.getName());
                    // Define how to transform: intercept methods annotated with @LogCall
                    return builder
                            .method(ElementMatchers.isAnnotatedWith(LogCall.class))
                            .intercept(MethodDelegation.to(LogCallInterceptor.class)); // <-- Points to your Interceptor!
                })
                // Add listeners for debugging ByteBuddy's actions
                .with(AgentBuilder.Listener.StreamWriting.toSystemOut()
//                        .withErrorListener(AgentBuilder.Listener.StreamWriting.toSystemError())
//                        .withIgnored(AgentBuilder.Listener.StreamWriting.toSystemError())
//                        .withTransformationListener(AgentBuilder.Listener.StreamWriting.toSystemOut())
                )
                .installOn(inst); // Install the agent on the JVM's Instrumentation instance
    }

}
