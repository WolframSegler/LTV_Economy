package wfg_ltv_econ.util;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class Reflection {
    private static final Class<?> fieldClass;
    private static final MethodHandle setFieldHandle;
    private static final MethodHandle getFieldHandle;
    private static final MethodHandle getFieldNameHandle;
    private static final MethodHandle setFieldAccessibleHandle;

    private static final Class<?> methodClass;
    private static final MethodHandle getMethodNameHandle;
    private static final MethodHandle invokeMethodHandle;
    private static final MethodHandles.Lookup lookup = MethodHandles.lookup();

    static {
        try {
            fieldClass = Class.forName("java.lang.reflect.Field", false, Class.class.getClassLoader());
            setFieldHandle = lookup.findVirtual(fieldClass, "set",
                    MethodType.methodType(void.class, Object.class, Object.class));
            getFieldHandle = lookup.findVirtual(fieldClass, "get", MethodType.methodType(Object.class, Object.class));
            getFieldNameHandle = lookup.findVirtual(fieldClass, "getName", MethodType.methodType(String.class));
            setFieldAccessibleHandle = lookup.findVirtual(fieldClass, "setAccessible",
                    MethodType.methodType(void.class, boolean.class));

            methodClass = Class.forName("java.lang.reflect.Method", false, Class.class.getClassLoader());
            getMethodNameHandle = lookup.findVirtual(methodClass, "getName", MethodType.methodType(String.class));
            invokeMethodHandle = lookup.findVirtual(methodClass, "invoke",
                    MethodType.methodType(Object.class, Object.class, Object[].class));
        } catch (Throwable t) {
            throw new RuntimeException("Failed to initialize Reflection.java", t);
        }
    }

    public static void set(String fieldName, Object instanceToModify, Object newValue) {
        try {
            Field field;
            try {
                field = instanceToModify.getClass().getField(fieldName);
            } catch (NoSuchFieldException e) {
                field = instanceToModify.getClass().getDeclaredField(fieldName);
            }
            setFieldAccessibleHandle.invoke(field, true);
            setFieldHandle.invoke(field, instanceToModify, newValue);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static Object get(String fieldName, Object instanceToGetFrom) {
        try {
            Field field;
            try {
                field = instanceToGetFrom.getClass().getField(fieldName);
            } catch (NoSuchFieldException e) {
                field = instanceToGetFrom.getClass().getDeclaredField(fieldName);
            }
            setFieldAccessibleHandle.invoke(field, true);
            return getFieldHandle.invoke(field, instanceToGetFrom);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static Object getStatic(String fieldName, Class<?> clazz) {
        try {
            Field field;
            try {
                field = clazz.getField(fieldName);
            } catch (NoSuchFieldException e) {
                field = clazz.getDeclaredField(fieldName);
            }
            field.setAccessible(true);
            return field.get(null); // null because static field
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean hasMethodOfName(String name, Object instance, boolean contains) {
        Method[] methods = instance.getClass().getDeclaredMethods();
        for (Method m : methods) {
            try {
                String methodName = (String) getMethodNameHandle.invoke(m);
                if (contains ? methodName.contains(name) : methodName.equals(name)) {
                    return true;
                }
            } catch (Throwable ignored) {
            }
        }
        return false;
    }

    public static boolean hasMethodOfName(String name, Object instance) {
        return hasMethodOfName(name, instance, false);
    }

    public static boolean hasVariableOfName(String name, Object instance) {
        java.lang.reflect.Field[] fields = instance.getClass().getDeclaredFields();
        for (java.lang.reflect.Field f : fields) {
            try {
                String fieldName = (String) getFieldNameHandle.invoke(f);
                if (fieldName.equals(name)) {
                    return true;
                }
            } catch (Throwable ignored) {
            }
        }
        return false;
    }

    public static Object instantiateExact(Class<?> clazz, Class<?>[] paramTypes, Object... arguments) {
        try {
            MethodType methodType = MethodType.methodType(void.class, paramTypes);
            MethodHandle constructor = lookup.findConstructor(clazz, methodType);
            return constructor.invokeWithArguments(arguments);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static Object instantiate(Class<?> clazz, Object... arguments) {
        try {
            Class<?>[] argsTypes = new Class<?>[arguments.length];
            for (int i = 0; i < arguments.length; i++) {
                Class<?> c = arguments[i].getClass();
                argsTypes[i] = c.isPrimitive() ? c : c;
            }
            MethodType methodType = MethodType.methodType(void.class, argsTypes);
            MethodHandle constructor = lookup.findConstructor(clazz, methodType);
            return constructor.invokeWithArguments(arguments);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static Object invokeStatic(String methodName, Class<?> cls, Class<?> returnType, Class<?>[] paramTypes,
            Object... arguments) {
        try {
            MethodType methodType = MethodType.methodType(returnType, paramTypes);
            MethodHandle staticMethod = lookup.findStatic(cls, methodName, methodType);
            return staticMethod.invokeWithArguments(arguments);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static Object invokeExact(String methodName, Object instance, Class<?>[] paramTypes, Object... arguments) {
        return invokeExact(methodName, instance, paramTypes, arguments, false);
    }

    public static Object invokeExact(String methodName, Object instance, Class<?>[] paramTypes, Object[] arguments,
            boolean declared) {
        try {
            Method method = null;
            if (!declared) {
                Class<?> cls = instance.getClass();
                while (cls != Object.class) {
                    try {
                        method = cls.getMethod(methodName, paramTypes);
                        break;
                    } catch (NoSuchMethodException e) {
                        cls = cls.getSuperclass();
                    }
                }
            } else {
                method = instance.getClass().getDeclaredMethod(methodName, paramTypes);
            }
            return invokeMethodHandle.invoke(method, instance, arguments);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static Object invoke(String methodName, Object instance, Object... arguments) {
        return invoke(methodName, instance, false, arguments);
    }

    public static Object invoke(String methodName, Object instance, boolean declared, Object... arguments) {
        try {
            Class<?> cls = instance.getClass();
            Class<?>[] argTypes = new Class<?>[arguments.length];
            for (int i = 0; i < arguments.length; i++) {
                Class<?> c = arguments[i].getClass();
                argTypes[i] = c.isPrimitive() ? c : c;
            }
            Method method;
            if (!declared) {
                method = cls.getMethod(methodName, argTypes);
            } else {
                method = cls.getDeclaredMethod(methodName, argTypes);
            }
            return invokeMethodHandle.invoke(method, instance, arguments);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

}
