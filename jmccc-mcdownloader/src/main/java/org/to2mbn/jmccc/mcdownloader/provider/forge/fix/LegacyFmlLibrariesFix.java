package org.to2mbn.jmccc.mcdownloader.provider.forge.fix;

import org.to2mbn.jmccc.mcdownloader.download.cache.CacheNames;
import org.to2mbn.jmccc.mcdownloader.download.combine.CombinedDownloadTask;
import org.to2mbn.jmccc.mcdownloader.download.tasks.FileDownloadTask;
import org.to2mbn.jmccc.option.MinecraftDirectory;
import org.to2mbn.jmccc.util.ChecksumUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public final class LegacyFmlLibrariesFix {

    private static final String BASE_URL = "https://files.prismlauncher.org/fmllibs/";

    private LegacyFmlLibrariesFix() {
    }

    public static CombinedDownloadTask<Void> download(MinecraftDirectory directory, File universal) throws Exception {
        List<CombinedDownloadTask<?>> tasks = new ArrayList<>();
        try (URLClassLoader cl = new URLClassLoader(new URL[]{universal.toURI().toURL()}, ClassLoader.getSystemClassLoader().getParent())) {
            Class<?> librarySet;
            try {
                librarySet = cl.loadClass("cpw.mods.fml.relauncher.CoreFMLLibraries");
            } catch (ClassNotFoundException e) {
                return CombinedDownloadTask.multiple(new CombinedDownloadTask<?>[0]);
            }
            Properties properties = new Properties();
            try (InputStream stream = cl.getResourceAsStream("fmlversion.properties")) {
                properties.load(stream);
            }
            Class<?> injectionData = cl.loadClass("cpw.mods.fml.relauncher.FMLInjectionData");
            Field minecraftVersion = injectionData.getDeclaredField("mccversion");
            minecraftVersion.setAccessible(true);
            minecraftVersion.set(null, properties.getProperty("fmlbuild.mcversion", properties.getProperty("fmlbuild.mcclientversion")));
            String deobfuscationHash = properties.getProperty("fmlbuild.deobfuscation.hash");
            if (deobfuscationHash != null) {
                Field hash = injectionData.getDeclaredField("deobfuscationDataHash");
                hash.setAccessible(true);
                hash.set(null, deobfuscationHash);
            }
            Object libraries = librarySet.getConstructor().newInstance();
            String[] names = (String[]) librarySet.getMethod("getLibraries").invoke(libraries);
            String[] hashes = (String[]) librarySet.getMethod("getHashes").invoke(libraries);
            for (int i = 0; i < names.length; i++) {
                if (names[i].startsWith("/")) continue;
                File target = directory.get("lib/" + names[i]).toFile();
                String hash = hashes[i];
                if (ChecksumUtils.verify(target, hash, "SHA-1")) continue;
                tasks.add(CombinedDownloadTask.single(new FileDownloadTask(BASE_URL + names[i], target).andThen(arg -> {
                    if (!ChecksumUtils.verify(target, hash, "SHA-1")) {
                        throw new IOException("Forge library checksum mismatch: " + target.getName());
                    }
                    return null;
                }).cachePool(CacheNames.LIBRARY)));
            }
        }
        return CombinedDownloadTask.multiple(tasks.toArray(new CombinedDownloadTask<?>[0]));
    }
}
