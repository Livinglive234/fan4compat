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
    /** Classpath presence check that neither loads nor initializes the class. */
    public static boolean present(String name) {
        return CompatCalls.class.getClassLoader().getResource(name.replace('.', '/') + ".class") != null;
    }
    public static Object call(Object receiver, String name, Object... args) {
        Class<?> owner = receiver instanceof Class<?> c ? c : receiver.getClass();
        Key key = new Key(name, Arrays.stream(args).map(a -> a == null ? Void.class : a.getClass()).toList());
        Method method;
        // A class whose methods cannot be listed stays on the linked path without trying (and failing) again each call.
        if (!(receiver instanceof Class<?>) && UNLISTABLE.get(owner)[0]) return linked(receiver, owner, key, name, args, null);
        try {
        method = METHODS.get(owner).computeIfAbsent(key, unused -> {
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
        } catch (LinkageError unlinkable) {
            // Another mod added methods whose signatures name classes that are not present (Porting Lib's plant and
            // block-state extensions, for one), so listing the class fails as a whole. Link only the method we want.
            if (receiver instanceof Class<?>) throw unlinkable;
            UNLISTABLE.get(owner)[0] = true;
            return linked(receiver, owner, key, name, args, unlinkable);
        }
        try { return method.invoke(receiver instanceof Class<?> ? null : receiver, args); }
        catch (InvocationTargetException e) { throw failure(e.getCause()); }
        catch (ReflectiveOperationException e) { throw failure(e); }
    }
    /** Owners whose {@code getMethods()} failed once; {@link ClassValue} offers no setter, so this holds a flag per class. */
    private static final ClassValue<boolean[]> UNLISTABLE = new ClassValue<>() {
        protected boolean[] computeValue(Class<?> owner) { return new boolean[1]; }
    };
    private static final ClassValue<Map<Key, MethodHandle>> LINKED = new ClassValue<>() {
        protected Map<Key, MethodHandle> computeValue(Class<?> owner) { return new ConcurrentHashMap<>(); }
    };
    /** Public instance methods of this one class with this name and argument count, read from its bytecode without linking any types. */
    private static List<String> descriptors(Class<?> declaring, String name, int count) {
        List<String> found = new ArrayList<>();
        try (java.io.InputStream input = declaring.getResourceAsStream("/" + declaring.getName().replace('.', '/') + ".class")) {
            if (input == null) return found;
            new org.objectweb.asm.ClassReader(input).accept(new org.objectweb.asm.ClassVisitor(org.objectweb.asm.Opcodes.ASM9) {
                @Override public org.objectweb.asm.MethodVisitor visitMethod(int access, String methodName, String descriptor, String signature, String[] exceptions) {
                    if ((access & org.objectweb.asm.Opcodes.ACC_PUBLIC) != 0 && (access & org.objectweb.asm.Opcodes.ACC_STATIC) == 0 && methodName.equals(name)
                        && org.objectweb.asm.Type.getArgumentTypes(descriptor).length == count) found.add(descriptor);
                    return null;
                }
            }, org.objectweb.asm.ClassReader.SKIP_CODE | org.objectweb.asm.ClassReader.SKIP_DEBUG | org.objectweb.asm.ClassReader.SKIP_FRAMES);
        } catch (java.io.IOException e) { throw failure(e); }
        return found;
    }
    private static Object linked(Object receiver, Class<?> owner, Key key, String name, Object[] args, LinkageError reason) {
        MethodHandle handle = LINKED.get(owner).computeIfAbsent(key, unused -> {
            for (Class<?> declaring = owner; declaring != null; declaring = declaring.getSuperclass()) {
                List<MethodHandle> found = new ArrayList<>();
                for (String descriptor : descriptors(declaring, name, args.length)) {
                    try {
                        MethodType signature = MethodType.fromMethodDescriptorString(descriptor, declaring.getClassLoader());
                        if (matches(signature.parameterArray(), args)) found.add(MethodHandles.publicLookup().findVirtual(declaring, name, signature));
                    } catch (ReflectiveOperationException | LinkageError skipped) { }
                }
                if (found.size() > 1) throw new IllegalStateException("Ambiguous compatibility call " + owner.getName() + "." + name);
                if (found.size() == 1) return found.get(0);
            }
            throw new IllegalStateException("Missing compatibility call " + owner.getName() + "." + name + " (listing the class failed" + (reason == null ? "" : ": " + reason) + ")");
        });
        List<Object> arguments = new ArrayList<>(); arguments.add(receiver); Collections.addAll(arguments, args);
        try { return handle.invokeWithArguments(arguments); }
        catch (Throwable e) { throw failure(e); }
    }
    public static Object exact(String owner, String name, String[] parameterNames, Object... args) {
        Class<?> c = type(owner);
        Class<?>[] parameters = Arrays.stream(parameterNames).map(CompatCalls::parameterType).toArray(Class<?>[]::new);
        Key key = new Key(name, List.of(parameters));
        MethodHandle handle = STATICS.get(c).computeIfAbsent(key, unused -> {
            // Class.getMethod resolves every signature on the Kotlin utility class,
            // including ClientWorld overloads unavailable on a dedicated server.
            // Read descriptors without linking their types, then link only our target.
            String arguments = MethodType.methodType(void.class, parameters).toMethodDescriptorString();
            arguments = arguments.substring(0, arguments.length() - 1);
            for (Class<?> declaring = c; declaring != null; declaring = declaring.getSuperclass()) {
                List<String> descriptors = new ArrayList<>();
                final String prefix = arguments;
                String resource = "/" + declaring.getName().replace('.', '/') + ".class";
                try (java.io.InputStream input = declaring.getResourceAsStream(resource)) {
                    if (input == null) throw new IllegalStateException("Missing compatibility bytecode " + declaring.getName());
                    new org.objectweb.asm.ClassReader(input).accept(new org.objectweb.asm.ClassVisitor(org.objectweb.asm.Opcodes.ASM9) {
                        @Override public org.objectweb.asm.MethodVisitor visitMethod(int access, String methodName, String descriptor, String signature, String[] exceptions) {
                            int required = org.objectweb.asm.Opcodes.ACC_PUBLIC | org.objectweb.asm.Opcodes.ACC_STATIC;
                            if ((access & required) == required && methodName.equals(name) && descriptor.startsWith(prefix)) descriptors.add(descriptor);
                            return null;
                        }
                    }, org.objectweb.asm.ClassReader.SKIP_CODE | org.objectweb.asm.ClassReader.SKIP_DEBUG | org.objectweb.asm.ClassReader.SKIP_FRAMES);
                } catch (java.io.IOException e) { throw failure(e); }
                if (descriptors.isEmpty()) continue;
                if (descriptors.size() != 1) throw new IllegalStateException("Ambiguous compatibility static call " + owner + "." + name);
                MethodType signature = MethodType.fromMethodDescriptorString(descriptors.get(0), c.getClassLoader());
                try { return MethodHandles.publicLookup().findStatic(c, name, signature); }
                catch (ReflectiveOperationException e) { throw failure(e); }
            }
            throw new IllegalStateException("Missing compatibility static call " + owner + "." + name);
        });
        try { return handle.invokeWithArguments(args); }
        catch (Throwable e) { throw failure(e); }
    }
    private static final ClassValue<Map<Key, MethodHandle>> STATICS = new ClassValue<>() {
        protected Map<Key, MethodHandle> computeValue(Class<?> owner) { return new ConcurrentHashMap<>(); }
    };
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
            if(p.isPrimitive()) p=switch(p.getName()) { case "boolean" -> Boolean.class; case "int" -> Integer.class; case "long" -> Long.class; case "double" -> Double.class; case "float" -> Float.class; case "byte" -> Byte.class; case "short" -> Short.class; case "char" -> Character.class; default -> p; };
            if(args[i]==null ? parameters[i].isPrimitive() : !p.isInstance(args[i])) return false;
        }
        return true;
    }
    private static RuntimeException failure(Throwable t) {
        if(t instanceof Error e) throw e;
        return t instanceof RuntimeException r ? r : new IllegalStateException("Compatibility call failed",t);
    }
}
