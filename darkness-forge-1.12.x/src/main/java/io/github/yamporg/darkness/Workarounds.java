package io.github.yamporg.darkness;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.common.MetadataCollection;
import net.minecraftforge.fml.common.ModMetadata;
import net.minecraftforge.fml.common.discovery.ASMDataTable;

/**
 * Workarounds to ease migration from Mixins to FML coremod. Specifically, we still use mcmod.info
 * for metadata as well as @Config annotations.
 */
public final class Workarounds {
    /**
     * A quick and dirty workaround to discover our mod metadata from mcmod.info. This is needed
     * because we are not the only coremod that puts mcmod.info file in classpath. And hell it’s so
     * more convenient to parse mcmod.info instead of manually initializing metadata.
     */
    public static ModMetadata parseMetadata() {
        ClassLoader cl = Workarounds.class.getClassLoader();
        Enumeration<URL> mi = null;
        try {
            mi = cl.getResources("mcmod.info");
        } catch (IOException ignored) {
            return null;
        }
        for (URL url : Collections.list(mi)) {
            InputStream is = null;
            try {
                is = url.openStream();
            } catch (IOException ignored) {
                continue;
            }
            MetadataCollection mc = MetadataCollection.from(is, ModContainer.MOD_ID);
            try {
                is.close();
            } catch (IOException ignored) {
            }

            ModMetadata md = mc.getMetadataForId(ModContainer.MOD_ID, new HashMap<>());
            if (md.autogenerated) {
                continue;
            }
            return md;
        }
        return null;
    }

    /**
     * Inject ModConfig into classes known to ConfigManager. This is a terrible hack. We need this
     * because I couldn’t find a better way to use @Config annotation with coremod that injects mod
     * container. Note that we intentionally use reflection instead of public loadData API because
     * for some reason Forge thinks that it’s OK to behave differently in development environment.
     * That is, ModConfig already exists in (and only in) development environment, and we don’t want
     * to insert it twice, otherwise horrible things may happen (and I don’t want to debug them).
     */
    public static void injectConfig() {
        final Map<String, Multimap<Config.Type, ASMDataTable.ASMData>> asmData;
        try {
            Field f = ConfigManager.class.getDeclaredField("asm_data");
            f.setAccessible(true);
            @SuppressWarnings("unchecked")
            final Map<String, Multimap<Config.Type, ASMDataTable.ASMData>> v =
                    (Map<String, Multimap<Config.Type, ASMDataTable.ASMData>>) f.get(null);
            asmData = v;
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
            return;
        }
        asmData.computeIfAbsent(
                ModContainer.MOD_ID,
                k -> {
                    Multimap<Config.Type, ASMDataTable.ASMData> map = ArrayListMultimap.create();
                    map.put(
                            Config.Type.INSTANCE,
                            new ASMDataTable.ASMData(
                                    null,
                                    null,
                                    ModConfig.class.getName(),
                                    null,
                                    new HashMap<String, Object>() {
                                        {
                                            put("category", "main");
                                        }
                                    }));
                    return map;
                });
    }
}
