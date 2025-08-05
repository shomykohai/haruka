package io.github.shomy.haruka;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import lanchon.dexpatcher.annotation.DexAdd;

/*
 * This class is a small utility around reflection, that allows to access, modify and
 * invoke methods from classes and instances without needing to know the class import.
 */
@DexAdd
public class Reflector {
    private final static String TAG = "HarukaReflector";
    // The wrapped instance (if present)
    private Object instance;
    // The class of the instance, or the class itself if no instance is provided
    private final Class<?> clazz;


    /*
     * 
     * Reflector around an instance.
     * Useful for accessing fields dinamically and pass an instance to methods.
     * 
     */
    private Reflector(Object instance) {
        this.instance = instance;
        this.clazz = instance.getClass();
    }

    /*
     * 
     * Reflector around a class.
     * Used for passing a soon to be instantiated class to methods that needs it.
     * 
     */
    private Reflector(Class<?> clazz) {
        this.instance = null;
        this.clazz = clazz;
    }

    /*
     * Creates an instance of Reflector that wraps the provided instance.
     * Sets `this.instance` to the provided instance and `this.clazz` to the instance's class.
     * 
     * @param instance The instance to wrap.
     * @return A Reflector instance wrapping the provided instance.
     */
    public static Reflector of(Object instance) {
        return new Reflector(instance);
    }

    /*
     * Creates an instance of Reflector that wraps the provided class.
     * Sets `this.instance` to null and `this.clazz` to the provided class.
     * 
     * This is useful for later instanciating the class without knowing the class import
     * on the method you're calling (e.g. different SDKs with different import paths).
     * 
     * @param clazz The class to wrap.
     * @return A Reflector instance wrapping the provided class.
     */
    public static Reflector of(Class<?> clazz) {
        return new Reflector(clazz);
    }

    /* 
     * Gets the value of field of type `type` from the provided instance.
     * 
     * @param instance The instance from which to get the field value.
     * @param fieldName The name of the field to get.
     * @param type The class type of the field to get.
     * @return The value of the field, casted to the specified type.
    */
    public static <V> V get(Object instance, String fieldName, Class<V> type) {
        try {
            Field field = instance.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return type.cast(field.get(instance));
        } catch (Exception e) {
            throw new RuntimeException(TAG + ": Failed to get field `" + fieldName + "` from " +instance.getClass().toString(), e);
        }
    }

    /*
     * Gets the value of the specified field of type `type` from the instance wrapped by this Reflector.
     * 
     * @param fieldName The name of the field to get.
     * @param type The class type of the field to get.
     * @return The value of the field, casted to the specified type.
     */
    public <V> V get(String fieldName, Class<V> type) {
        try {
            Field field = this.clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            Object value = field.get(this.instance);
            return type.cast(value);
        } catch (Exception e) {
            throw new RuntimeException(TAG + ": Failed to get field `" + fieldName + "` from " + this.instance.getClass().toString(), e);
        }
    }

    /*
     * Sets the value of the specified field in the provided instance.
     * 
     * @param instance The instance in which to set the field value.
     * @param fieldName The name of the field to set.
     * @param value The value to set in the field.
     */
    public static void set(Object instance, String fieldName, Object value) {
        try {
            Class<?> clazz = instance.getClass();
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(instance, value);
        } catch (Exception e) {
            throw new RuntimeException(TAG + ": Failed to set field `" + fieldName + "` in " + instance.getClass().toString(), e);
        }
    }

    /*
     * Sets the value of the specified field in the instance wrapped by this Reflector.
     * 
     * @param fieldName The name of the field to set.
     * @param value The value to set in the field.
     * @return This Reflector instance for method chaining.
     */
    public Reflector set(String fieldName, Object value) {
        try {
            Field field = this.clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(this.instance, value);
            return this;
        } catch (Exception e) {
            throw new RuntimeException(TAG + ": Failed to set field `" + fieldName + "` in " + this.instance.getClass().toString(), e);
        }
    }

    /*
     * Invokes a method with the specified name and parameters on the provided target instance or class.
     * 
     * @param targetOrClass The target instance or class on which to invoke the method.
     * @param methodName The name of the method to invoke.
     * @param returnType The class type of the method's return value.
     * @param args The arguments to pass to the method.
     * @return The result of the method invocation, casted to the specified return type.
     */
    public static <R> R invoke(Object targetOrClass, String methodName, Class<R> returnType, Object... args) {
        try {
            // Determine the class of the target.
            Class<?> clazz = (targetOrClass instanceof Class<?>)
                    ? (Class<?>) targetOrClass
                    : targetOrClass.getClass();
            // Determine the instance to invoke the method on:
            // null -> We want to call a static method
            // non-null -> We want to call an instance method
            Object instance = (targetOrClass instanceof Class<?>)
                    ? null
                    : targetOrClass;
    
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.getName().equals(methodName) && matchParams(method.getParameterTypes(), args)) {
                    method.setAccessible(true);
                    return returnType.cast(method.invoke(instance, args));
                }
            }
            throw new NoSuchMethodException(TAG + ": No method named: " + methodName);
        } catch (Exception e) {
            throw new RuntimeException(TAG + ": Failed to invoke method: " + methodName, e);
        }
    }
    
    /*
     * Invokes a method with the specified name and parameters on the instance wrapped by this Reflector.
     * 
     * @param methodName The name of the method to invoke.
     * @param returnType The class type of the method's return value.
     * @param args The arguments to pass to the method.
     * @return The result of the method invocation, casted to the specified return type.
     */
    public <R> R invoke(String methodName, Class<R> returnType, Object... args) {
        try {
            for (Method method : this.clazz.getDeclaredMethods()) {
                if (method.getName().equals(methodName) && matchParams(method.getParameterTypes(), args)) {
                    method.setAccessible(true);
                    Object result = method.invoke(this.instance, args);
                    return returnType.cast(result);
                }
            }
            throw new NoSuchMethodException(TAG + ": No method named: " + methodName);
        } catch (Exception e) {
            throw new RuntimeException(TAG + ": Failed to invoke method: " + methodName, e);
        }
    }

    /*
     * Creates a new instance of the specified class using the constructor that matches the provided arguments.
     * 
     * @param type The class type to instantiate.
     * @param args The arguments to pass to the constructor.
     * @return A new instance of the specified class.
     */
    public static <T> T newInstance(Class<T> type, Object... args) {
        try {
            if (type.isInterface() || type.isPrimitive()) {
                throw new IllegalArgumentException(TAG + ": Cannot instantiate an interface or primitive type: " + type.getName());
            }

            for (Constructor<?> constructor : type.getDeclaredConstructors()) {
                if (matchParams(constructor.getParameterTypes(), args)) {
                    constructor.setAccessible(true);
                    return type.cast(constructor.newInstance(args));
                }
            }
            throw new NoSuchMethodException(TAG + ": No matching constructor found in " + type.getName());
        } catch (Exception e) {
            throw new RuntimeException(TAG + ": Failed to instantiate " + type.getName(), e);
        }
    }

    /*
     * Creates a new instance of the class wrapped by this Reflector using the constructor that matches the provided arguments.
     * This method can only be called if the Reflector is not wrapping an instance, but a class (this.instance == null).
     * Once this method is called, this.instance will be set to the newly created instance, and cannot be called again.
     * 
     * @param args The arguments to pass to the constructor.
     * @return A new instance of the class wrapped by this Reflector.
     */
    public Object newInstance(Object... args) {
        try {
            // The Reflector is already wrapping an instance, so we cannot create a new one.
            if (this.instance != null){
                throw new IllegalStateException(TAG + ": Cannot create new instance from an instance reflector.");
            }

            if (this.clazz.isInterface() || this.clazz.isPrimitive()) {
                throw new IllegalArgumentException(TAG + ": Cannot instantiate an interface or primitive type: " + clazz.getName());
            }

            for (Constructor<?> constructor : this.clazz.getDeclaredConstructors()) {
                if (matchParams(constructor.getParameterTypes(), args)) {
                    constructor.setAccessible(true);
                    
                    this.instance = constructor.newInstance(args);
                    return this.instance;
                }
            }
            throw new NoSuchMethodException(TAG + ": No matching constructor found");
        } catch (Exception e) {
            throw new RuntimeException(TAG + ": Failed to instantiate", e);
        }
    }

    /*
     * Checks if the provided parameter types match the types of the provided arguments.
     * 
     * @param paramTypes The parameter types to check against.
     * @param args The arguments to check.
     * @return true if the parameter types match the argument types, false otherwise.
     */
    private static boolean matchParams(Class<?>[] paramTypes, Object[] args) {
        if (paramTypes.length != args.length) return false;
        for (int i = 0; i < args.length; i++) {
            if (args[i] == null) {
                // A primitive type cannot be null, so return false if we don't wanna make
                // Android mad!!
                if (paramTypes[i].isPrimitive()) {
                    return false;
                }
            }
            else if (!isAssignable(paramTypes[i], args[i].getClass())) {
                return false;
            }
        }
        return true;
    }

    /*
     * Returns the wrapped instance, casted to the specified type.
     * If the instance is null, it returns null.
     * If the instance is not of the specified type, it throws a ClassCastException.
     * 
     * @return The wrapped instance, casted to the specified type.
     */
    @SuppressWarnings("unchecked")
    public <T> T getTarget() throws ClassCastException {
        if (this.instance == null) {
            return null;
        }
        // This shouldn't happen, but we check it just in case.
        if (this.instance != null && !this.clazz.isInstance(this.instance)) {
            throw new ClassCastException(TAG + ": Instance is not of type " + clazz.getName());
        }
        return (T) this.instance;
    }

    /*
     * Checks whether a parameter type can be assigned from an argument type.
     * Handles primitives types to avoid collisions in constructors.
     * 
     * @param paramType The class type of the parameter.
     * @param argType The class type of the argument.
     * @return true if the parameter type can be assigned from the argument type, false otherwise.
     */
    private static boolean isAssignable(Class<?> paramType, Class<?> argType) {
        if (paramType.isAssignableFrom(argType)) {
            return true;
        }
        if (paramType.isPrimitive()) {
            if (paramType == boolean.class && argType == Boolean.class) return true;
            if (paramType == byte.class && argType == Byte.class) return true;
            if (paramType == short.class && argType == Short.class) return true;
            if (paramType == int.class && argType == Integer.class) return true;
            if (paramType == long.class && argType == Long.class) return true;
            if (paramType == float.class && argType == Float.class) return true;
            if (paramType == double.class && argType == Double.class) return true;
            if (paramType == char.class && argType == Character.class) return true;
        }
        return false;
    }

    /*
     * Returns the class type wrapped by this Reflector.
     * 
     * @return The class type wrapped by this Reflector.
     */
    public Class<?> getType() {
        return this.clazz;
    }

    /*
     * Creates a new array, using reflection, of the specified component type and size.
     * 
     * @param clazz The class type of the array's components.
     * @param size The size of the array to create.
     * @return A new array of the specified component type and size.
     */
    public static Object newArray(Class<?> clazz, int size) {
        return Array.newInstance(clazz, size);
    }
}
