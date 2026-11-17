package de.wolfmod.creachreportfix.support;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reflective field access for guarding mod classes.
 *
 * <p>Guard mixins must never fail to apply just because a mod renamed one of its
 * private fields, so the field is looked up at runtime instead of using a
 * {@code @Shadow}. A missing field is treated as "everything is fine" and the
 * original mod code simply runs.</p>
 */
public final class FieldLookup {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, Optional<Field>> CACHE = new ConcurrentHashMap<>();

    private FieldLookup() {
    }

    /**
     * @return {@code false} when the field exists and currently holds {@code null};
     *         {@code true} when it holds a value or cannot be inspected at all.
     */
    public static boolean hasValue(Object instance, String fieldName) {
        Field field = find(instance.getClass(), fieldName);
        if (field == null) {
            return true;
        }

        try {
            return field.get(instance) != null;
        } catch (ReflectiveOperationException | RuntimeException ex) {
            LOGGER.debug("[creachreportfix] Could not read field {}", fieldName, ex);
            return true;
        }
    }

    private static Field find(Class<?> type, String fieldName) {
        return CACHE.computeIfAbsent(type.getName() + '#' + fieldName, key -> lookup(type, fieldName)).orElse(null);
    }

    private static Optional<Field> lookup(Class<?> type, String fieldName) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                Field field = current.getDeclaredField(fieldName);
                field.setAccessible(true);
                return Optional.of(field);
            } catch (NoSuchFieldException ignored) {
                // Keep walking up the hierarchy.
            } catch (RuntimeException ex) {
                LOGGER.debug("[creachreportfix] Field {} is not accessible", fieldName, ex);
                return Optional.empty();
            }
        }
        return Optional.empty();
    }
}
