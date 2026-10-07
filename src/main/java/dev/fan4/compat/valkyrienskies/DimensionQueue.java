package dev.fan4.compat.valkyrienskies;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/** Defers dimension mutations between physics ticks without disabling VS checks. */
public final class DimensionQueue {
    private record Mutation(String method, Object[] args) {}
    private static final class State {
        boolean started;
        boolean open;
        final ArrayDeque<Mutation> pending = new ArrayDeque<>();
    }
    private static final Map<Object, State> STATES = new WeakHashMap<>();
    private static final ClassValue<Map<String, Method>> METHODS = new ClassValue<>() {
        protected Map<String, Method> computeValue(Class<?> type) {
            Map<String, Method> result = new HashMap<>();
            for (Method m : type.getMethods()) {
                if (m.getName().equals("addDimension") || m.getName().equals("removeDimension") || m.getName().equals("updateDimension")) {
                    String key = m.getName() + "/" + m.getParameterCount();
                    if (result.putIfAbsent(key, m) != null) {
                        throw new IllegalStateException("Ambiguous VS dimension method: " + key);
                    }
                }
            }
            return result;
        }
    };
    private static State state(Object world) {
        synchronized (STATES) { return STATES.computeIfAbsent(world, unused -> new State()); }
    }
    public static boolean defer(Object world, String method, Object[] args) {
        State state = state(world);
        synchronized (state) {
            // Existing startup registration must stay synchronous. Only defer
            // operations once the first physics tick has actually started.
            if (!state.started || state.open) return false;
            state.pending.addLast(new Mutation(method, args.clone()));
            return true;
        }
    }
    public static void openAndFlush(Object world) {
        State state = state(world);
        synchronized (state) {
            state.started = true;
            state.open = true;
            while (!state.pending.isEmpty()) {
                Mutation mutation = state.pending.getFirst();
                Method method = METHODS.get(world.getClass()).get(mutation.method + "/" + mutation.args.length);
                if (method == null) throw new IllegalStateException("Missing VS dimension operation " + mutation.method);
                try {
                    method.invoke(world, mutation.args);
                } catch (InvocationTargetException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof RuntimeException runtime) throw runtime;
                    if (cause instanceof Error error) throw error;
                    throw new IllegalStateException("Deferred dimension update failed", cause);
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException("Cannot invoke VS dimension operation", e);
                }
                state.pending.removeFirst();
            }
        }
    }
    public static void close(Object world) {
        State state = state(world);
        synchronized (state) { state.open = false; }
    }
    public static void forget(Object world) {
        synchronized (STATES) { STATES.remove(world); }
    }
}
