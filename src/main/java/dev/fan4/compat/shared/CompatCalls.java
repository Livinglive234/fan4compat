package dev.fan4.compat.shared;

import java.lang.reflect.*;
import java.lang.invoke.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Cached calls into exact-version optional mods; never initialize client classes on a server. */
public final class CompatCalls {
    private record Key(String name, List<Class<?>> parameters) {}
    private static final ClassValue<Map<Key, Method>> METHODS = new ClassValue<>() {
        protected Map<Key, Method> computeValue(Class<?> type) { return new ConcurrentHashMap<>(); }
    };
    private static final ClassValue<Map<String, Field>> FIELDS = new ClassValue<>() {
        protected Map<String, Field> computeValue(Class<?> type) { return new ConcurrentHashMap<>(); }
    };
    public static Class<?> type(String name) {
        try { return Class.forName(name, false, CompatCalls.class.getClassLoader()); }
        catch (ClassNotFoundException e) { throw new IllegalStateException("Missing compatibility class " + name, e); }
    }
    public static Object call(Object receiver, String name, Object... args) {
        Class<?> owner = receiver instanceof Class<?> c ? c : receiver.getClass();
        Key key = new Key(name, Arrays.stream(args).map(a -> a == null ? Void.class : a.getClass()).toList());
        Method method = METHODS.get(owner).computeIfAbsent(key, unused -> {
            // Overloaded Kotlin world helpers are selected explicitly where necessary.
            List<Method> candidates = Arrays.stream(owner.getMethods()).filter(m -> m.getName().equals(name) && matches(m.getParameterTypes(), args)).toList();
            // VS's obfuscated core may expose only bridge methods under the API name.
            // Include those adapters as well as unflagged covariant getters.
            // Only collapse identical parameter signatures; real overloads stay ambiguous.
            if(candidates.size()>1) {
                List<Method> all=candidates;
                candidates=all.stream().filter(m->all.stream().allMatch(other ->
                    Arrays.equals(m.getParameterTypes(),other.getParameterTypes())
                    && other.getReturnType().isAssignableFrom(m.getReturnType()))).toList();
            }
            if (candidates.size() != 1) throw new IllegalStateException("Ambiguous or missing compatibility call " + owner.getName()+"."+name+" "+key.parameters);
            Method m = candidates.get(0); m.setAccessible(true); return m;
        });
        try { return method.invoke(receiver instanceof Class<?> ? null : receiver, args); }
        catch (InvocationTargetException e) { throw failure(e.getCause()); }
        catch (ReflectiveOperationException e) { throw failure(e); }
    }
    public static Object exact(String owner, String name, String[] parameterNames, Object... args) {
        Class<?> c = type(owner);
        Class<?>[] parameters = Arrays.stream(parameterNames).map(CompatCalls::parameterType).toArray(Class<?>[]::new);
        Key key = new Key(name, List.of(parameters));
        Method m = METHODS.get(c).computeIfAbsent(key, unused -> {
            try { return c.getMethod(name, parameters); } catch (ReflectiveOperationException e) { throw failure(e); }
        });
        try { return m.invoke(null, args); }
        catch (InvocationTargetException e) { throw failure(e.getCause()); }
        catch (ReflectiveOperationException e) { throw failure(e); }
    }
    private static Class<?> parameterType(String n) { return switch(n) { case "boolean" -> boolean.class; case "int" -> int.class; case "double" -> double.class; case "long" -> long.class; default -> type(n); }; }
    private record TypedKey(String name,Class<?> result,List<Class<?>> parameters) {}
    private static final ClassValue<Map<TypedKey,MethodHandle>> TYPED=new ClassValue<>() {
        protected Map<TypedKey,MethodHandle> computeValue(Class<?> owner){return new ConcurrentHashMap<>();}
    };
    /** Resolve one virtual signature without enumerating unrelated mixin-added methods. */
    public static Object callTyped(String owner,String name,String result,Object receiver,String[] parameterNames,Object... args) {
        Class<?> c=type(owner),returns=parameterType(result);
        Class<?>[] parameters=Arrays.stream(parameterNames).map(CompatCalls::parameterType).toArray(Class<?>[]::new);
        TypedKey key=new TypedKey(name,returns,List.of(parameters));
        MethodHandle handle=TYPED.get(c).computeIfAbsent(key,unused->{
            try{return MethodHandles.publicLookup().findVirtual(c,name,MethodType.methodType(returns,parameters));}
            catch(ReflectiveOperationException e){throw failure(e);}
        });
        List<Object> arguments=new ArrayList<>();arguments.add(receiver);Collections.addAll(arguments,args);
        try{return handle.invokeWithArguments(arguments);}catch(Throwable e){throw failure(e);}
    }
    public static Object createExact(String owner,String[] parameterNames,Object... args) {
        Class<?>[] parameters=Arrays.stream(parameterNames).map(CompatCalls::parameterType).toArray(Class<?>[]::new);
        try { return type(owner).getConstructor(parameters).newInstance(args); }
        catch(InvocationTargetException e){throw failure(e.getCause());}
        catch(ReflectiveOperationException e){throw failure(e);}
    }
    public static Object create(String owner, Object... args) {
        List<Constructor<?>> found = Arrays.stream(type(owner).getConstructors()).filter(c -> matches(c.getParameterTypes(), args)).toList();
        if (found.size()!=1) throw new IllegalStateException("Missing/ambiguous compatibility constructor " + owner);
        try { return found.get(0).newInstance(args); }
        catch (InvocationTargetException e) { throw failure(e.getCause()); }
        catch (ReflectiveOperationException e) { throw failure(e); }
    }
    public static Object field(Object receiver, String name) {
        Class<?> c = receiver instanceof Class<?> t ? t : receiver.getClass();
        Field f = FIELDS.get(c).computeIfAbsent(name, unused -> {
            for (Class<?> t=c; t!=null; t=t.getSuperclass()) {
                try { Field result=t.getDeclaredField(name); result.setAccessible(true); return result; }
                catch (NoSuchFieldException ignored) { }
            }
            throw new IllegalStateException("Missing compatibility field " + c.getName()+"."+name);
        });
        try { return f.get(receiver instanceof Class<?> ? null : receiver); }
        catch (ReflectiveOperationException e) { throw failure(e); }
    }
    public static void writeField(Object receiver,String name,Object value) {
        Class<?> c=receiver instanceof Class<?> t?t:receiver.getClass();
        // Populate the same cached Field used by reads.
        field(receiver,name);
        try { FIELDS.get(c).get(name).set(receiver instanceof Class<?>?null:receiver,value); }
        catch(ReflectiveOperationException e){throw failure(e);}
    }
    private static boolean matches(Class<?>[] parameters, Object[] args) {
        if (parameters.length!=args.length) return false;
        for(int i=0;i<args.length;i++) {
            Class<?> p=parameters[i];
            if(p.isPrimitive()) p=switch(p.getName()) { case "boolean" -> Boolean.class; case "int" -> Integer.class; case "long" -> Long.class; case "double" -> Double.class; case "float" -> Float.class; default -> p; };
            if(args[i]==null ? parameters[i].isPrimitive() : !p.isInstance(args[i])) return false;
        }
        return true;
    }
    private static RuntimeException failure(Throwable t) {
        if(t instanceof Error e) throw e;
        return t instanceof RuntimeException r ? r : new IllegalStateException("Compatibility call failed",t);
    }
}
