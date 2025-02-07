package dev.klepto.unreflect;

import com.google.common.reflect.TypeToken;
import dev.klepto.unreflect.property.Named;
import dev.klepto.unreflect.util.JdkInternals;
import lombok.val;
import one.util.streamex.StreamEx;

import javax.annotation.Nullable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;

/**
 * A wrapper for java types enabling simple generic lookup, instance allocation and superclass and subtype resolving.
 *
 * @author <a href="http://github.com/klepto">Augustinas R.</a>
 */
public class UnreflectType implements Named {

    private final TypeToken typeToken;

    private UnreflectType(TypeToken<?> typeToken) {
        this.typeToken = typeToken;
    }

    /**
     * Returns a reflection-based {@link ClassAccess} for this type.
     *
     * @return a reflection access for class that this type represents
     */
    public ClassAccess<?> reflect() {
        return Unreflect.reflect(toClass());
    }

    /**
     * Returns a bytecode-based {@link ClassAccess} for this type.
     *
     * @return a bytecode access for class that this type represents
     */
    public ClassAccess<?> unreflect() {
        return Unreflect.unreflect(toClass());
    }

    /**
     * Allocates a new instance of this type without calling any of its constructors. Enables initialization of any
     * object of any type regardless of constructor visibility. Note that any important initialization code might be
     * skipped.
     *
     * @param <T> generic type for automatic return value casting
     * @return a new instance of this type
     */
    public <T> T allocate() {
        return (T) JdkInternals.allocateInstance(toClass());
    }

    /**
     * Returns the name of this type.
     *
     * @return the name of this type
     */
    @Override
    public String name() {
        return toClass().getName();
    }

    /**
     * Returns the superclass type of this type, or null if this type represents {@link java.lang.Object}.
     *
     * @return the superclass type, or null if superclass doesn't exist
     */
    @Nullable
    public UnreflectType superType() {
        val superClass = toClass().getSuperclass();
        if (superClass == null) {
            return null;
        }

        val superTypeToken = typeToken.getSupertype(superClass);
        return UnreflectType.of(superTypeToken);
    }

    /**
     * Returns the superclass type of this type at given index (depth), or null if superclass at given depth doesn't
     * exist.
     *
     * @param index the superclass index
     * @return the superclass type at a given depth
     */
    @Nullable
    public UnreflectType superType(int index) {
        return superTypes().skip(index).findFirst().orElse(null);
    }

    /**
     * Returns a stream of all superclass types up to {@link java.lang.Object}.
     *
     * @return a stream containing all superclass types
     */
    public StreamEx<UnreflectType> superTypes() {
        val result = new ArrayList<UnreflectType>();
        var superType = (UnreflectType) this;
        while ((superType = superType.superType()) != null) {
            result.add(superType);
        }
        return StreamEx.of(result);
    }

    /**
     * Returns a stream of all implemented types, this includes both super classes and implemented interfaces.
     *
     * @return a stream containing all implemented types
     */
    public StreamEx<UnreflectType> subTypes() {
        return StreamEx.of(typeToken.getTypes()).map(UnreflectType::of);
    }

    /**
     * Returns a stream of all generic types associated with this type. Sometimes due to type erasure generics are not
     * preserved in which case, empty stream will be returned.
     *
     * @return a stream containing generic types associated with this type
     */
    public StreamEx<UnreflectType> genericTypes() {
        val parameterizedType = (ParameterizedType) typeToken.getType();
        return StreamEx.of(parameterizedType.getActualTypeArguments()).map(UnreflectType::of);
    }

    /**
     * Returns the first generic type associated with this type, or null if this type has no associated generics
     *
     * @return the first generic type associated with this type, or null if unavailable
     */
    @Nullable
    public UnreflectType genericType() {
        return genericTypes().findFirst().orElse(null);
    }

    /**
     * Returns the generic type at a given index (slot), or null if generic type is unavailable. For example, if this
     * type is {@code Map<String, Integer>} calling {@code genericType(1)} will return type of {@link Integer}. Can be
     * chained to resolve generics at any depth.
     *
     * @param index the slot of the generic
     * @return the generic type at a given slot
     */
    public UnreflectType genericType(int index) {
        return genericTypes().skip(index).findFirst().orElse(null);
    }

    /**
     * Returns a component type of this type, or null if this type has no component type. Only applicable to array types.
     *
     * @return the component type of this array type, or null if no component type present
     * @see UnreflectType#isArray()
     */
    public UnreflectType componentType() {
        val componentType = typeToken.getComponentType();
        if (componentType == null) {
            return null;
        }

        return of(componentType);
    }

    /**
     * Checks if this type matches the given value type.
     *
     * @param object the value
     * @return true if this type is assignable for a given value
     */
    public boolean matches(Object object) {
        return matches(of(object).toClass());
    }

    /**
     * Checks if this type matches the given class
     *
     * @param type the class type
     * @return true if this type is assignable from a given class
     */
    public boolean matches(Class<?> type) {
        return typeToken.isSupertypeOf(type);
    }

    /**
     * Checks if this type is an array type.
     *
     * @return true if this type is an array type
     */
    public boolean isArray() {
        return typeToken.isArray();
    }

    /**
     * Checks if this type is a primitive type.
     *
     * @return true if this type is a primitive type
     */
    public boolean isPrimitive() {
        return typeToken.isPrimitive();
    }

    /**
     * Returns {@link Type} representation of this type.
     *
     * @return a reflection representation of this type
     */
    public Type toType() {
        return typeToken.getType();
    }

    /**
     * Returns {@link Class} representation of this type.
     *
     * @return a class representation of this type
     */
    public Class<?> toClass() {
        return typeToken.getRawType();
    }

    @Override
    public String toString() {
        val type = isArray() ? componentType() : this;
        val simpleName = type.toClass().getSimpleName();
        return simpleName + (isArray() ? "[]" : "");
    }

    @Override
    public boolean equals(Object object) {
        return typeToken.equals(object);
    }

    @Override
    public int hashCode() {
        return typeToken.hashCode();
    }

    /**
     * Creates a type representation of a given value, value can be class, type, typetoken or just a regular value in
     * which case it's class is going to be used as type.
     *
     * @param object the value
     * @return the type representation of a given value
     */
    public static UnreflectType of(Object object) {
        if (object instanceof Class<?> type) {
            return of(type);
        }

        if (object instanceof Type type) {
            return of(type);
        }

        if (object instanceof TypeToken<?> type) {
            return of(type);
        }

        return of(object.getClass());
    }

    /**
     * Creates a type representation for a given {@link Type}.
     *
     * @param type the reflection type
     * @return the type representation of a given reflection type
     */
    public static UnreflectType of(Type type) {
        return of(TypeToken.of(type));
    }

    /**
     * Creates a type representation for a given {@link Class}.
     *
     * @param type the class type
     * @return the type representation of a given class type
     */
    public static UnreflectType of(Class<?> type) {
        return of(TypeToken.of(type));
    }

    /**
     * Creates a type representation for a given guava's {@link TypeToken}.
     *
     * @param type the guava type
     * @return the type representation of a given guava type
     */
    public static UnreflectType of(TypeToken<?> type) {
        return new UnreflectType(type);
    }

}