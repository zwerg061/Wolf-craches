package de.wolfmod.creachreportfix.support;

import com.google.common.collect.ImmutableSet;
import com.mojang.logging.LogUtils;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.IForgeRegistry;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.locks.LockSupport;

/**
 * Creates immutable snapshots of live registry views.
 *
 * <p>Iterating a live registry while another thread registers content throws a
 * {@link java.util.ConcurrentModificationException}. A plain copy is not enough
 * because the copy itself iterates the live view, so the copy is retried until
 * the registry stops changing for a moment.</p>
 */
public final class RegistrySnapshots {
    private static final String LOG_PREFIX = "[creachreportfix]";
    private static final Logger LOGGER = LogUtils.getLogger();

    /** Number of copy attempts before falling back to an empty set. */
    private static final int MAX_ATTEMPTS = 40;

    private RegistrySnapshots() {
    }

    /**
     * Snapshots the keys of a registry. Accepts both the vanilla registry and the
     * Forge registry implementation, because Forge swaps them at runtime.
     */
    public static Set<ResourceLocation> keySetOf(Object registry) {
        Collection<ResourceLocation> liveKeys = liveKeysOf(registry);
        if (liveKeys == null) {
            return Collections.emptySet();
        }
        return snapshot(liveKeys);
    }

    /** Copies a live collection, retrying while it is modified by another thread. */
    public static <T> Set<T> snapshot(Collection<T> live) {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            try {
                return ImmutableSet.copyOf(live);
            } catch (RuntimeException ex) {
                // Registry changed while copying - wait a moment and try again.
                LockSupport.parkNanos(50_000L * (attempt + 1L));
            }
        }

        LOGGER.warn("{} Registry kept changing while copying; using an empty snapshot for this frame.", LOG_PREFIX);
        return ImmutableSet.of();
    }

    private static Collection<ResourceLocation> liveKeysOf(Object registry) {
        if (registry instanceof IForgeRegistry<?> forgeRegistry) {
            return forgeRegistry.getKeys();
        }
        if (registry instanceof Registry<?> vanillaRegistry) {
            return vanillaRegistry.keySet();
        }
        return null;
    }
}
