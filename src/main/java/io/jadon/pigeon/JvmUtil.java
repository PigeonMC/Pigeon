package io.jadon.pigeon;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

public class JvmUtil {

    private static Unsafe unsafe;

    static {
        try {
            Field singleoneInstanceField = Unsafe.class.getDeclaredField("theUnsafe");
            singleoneInstanceField.setAccessible(true);
            unsafe = (Unsafe) singleoneInstanceField.get(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static <T> T getUnsafeInstance(Class<T> clazz) {
        T t = null;
        try {
            t = (T) unsafe.allocateInstance(clazz);
        } catch (Exception e) {
            sneakyThrow(e);
        }
        return t;
    }

    public static <E extends Throwable> void sneakyThrow(Throwable e) throws E {
        throw (E) e;
    }

}
