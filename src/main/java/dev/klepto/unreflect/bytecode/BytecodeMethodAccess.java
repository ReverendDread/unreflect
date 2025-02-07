package dev.klepto.unreflect.bytecode;

import dev.klepto.unreflect.MethodAccess;
import dev.klepto.unreflect.bytecode.asm.InvokableAccessor;
import dev.klepto.unreflect.reflection.ReflectionMethodAccess;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

/**
 * Bytecode access to a method of a class. Delegates all methods to {@link ReflectionMethodAccess} except the
 * {@link MethodAccess#invoke(Object...)} method for which it uses direct accessor.
 *
 * @author <a href="http://github.com/klepto">Augustinas R.</a>
 */
@RequiredArgsConstructor
@Getter
public class BytecodeMethodAccess implements MethodAccess {

    @Delegate(excludes = Overrides.class)
    private final ReflectionMethodAccess delegate;
    private final InvokableAccessor accessor;

    @Override
    public MethodAccess unreflect() {
        return this;
    }

    @Override
    public MethodAccess reflect() {
        return delegate;
    }

    @Override
    public <T> T invoke(Object... args) {
        return (T) accessor.invoke(object(), args);
    }

    private interface Overrides {
        void unreflect();
        void reflect();
        void invoke(Object... args);
    }

}
