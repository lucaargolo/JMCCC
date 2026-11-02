package org.to2mbn.jmccc.mcdownloader.provider.forge.fix;

import org.to2mbn.jmccc.option.MinecraftDirectory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

public final class LauncherProfileFix {

    private static final byte[] EMPTY_PROFILE = "{}".getBytes(StandardCharsets.UTF_8);

    private LauncherProfileFix() {
    }

    public static void prepare(MinecraftDirectory directory) throws IOException {
        try {
            Files.write(directory.get("launcher_profiles.json"), EMPTY_PROFILE, StandardOpenOption.CREATE_NEW);
        } catch (FileAlreadyExistsException ignored) {
        }
    }
}
