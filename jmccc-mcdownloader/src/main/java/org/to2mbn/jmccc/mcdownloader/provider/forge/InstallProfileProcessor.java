package org.to2mbn.jmccc.mcdownloader.provider.forge;

import org.to2mbn.jmccc.internal.org.json.JSONObject;
import org.to2mbn.jmccc.internal.org.json.JSONTokener;
import org.to2mbn.jmccc.mcdownloader.download.tasks.ResultProcessor;
import org.to2mbn.jmccc.mcdownloader.provider.VersionJsonInstaller;
import org.to2mbn.jmccc.mcdownloader.provider.forge.fix.InstallerActionFix;
import org.to2mbn.jmccc.mcdownloader.provider.forge.fix.LauncherProfileFix;
import org.to2mbn.jmccc.mcdownloader.provider.forge.fix.LegacyVersionProfileFix;
import org.to2mbn.jmccc.option.MinecraftDirectory;
import org.to2mbn.jmccc.util.FileUtils;
import org.to2mbn.jmccc.util.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class InstallProfileProcessor implements ResultProcessor<byte[], String> {

    private final MinecraftDirectory mcdir;
    private final String installerArtifact;

    public InstallProfileProcessor(MinecraftDirectory mcdir, String installerArtifact) {
        this.mcdir = mcdir;
        this.installerArtifact = installerArtifact;
    }

    @Override
    public String process(byte[] arg) throws Exception {
        JSONObject installProfile = null;
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(arg))) {
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                if ("install_profile.json".equals(entry.getName())) {
                    installProfile = new JSONObject(new JSONTokener(new String(IOUtils.toByteArray(in), StandardCharsets.UTF_8)));
                    break;
                }
                in.closeEntry();
            }
        }

        if (installProfile == null) {
            throw new IOException("Forge installer is missing install_profile.json");
        }

        JSONObject versionInfo = processJson(installProfile);
        if (versionInfo != null) {
            LegacyVersionProfileFix.apply(installProfile, versionInfo);
            return new VersionJsonInstaller(mcdir).process(versionInfo);
        }

        String version = installProfile.getString("version");
        Path installer = mcdir.get(installerArtifact);
        FileUtils.prepareWrite(installer.toFile());
        try {
            Files.write(installer, arg);
            runInstaller(installer);
            return version;
        } finally {
            Files.deleteIfExists(installer);
        }
    }

    protected JSONObject processJson(JSONObject installprofile) {
        return installprofile.optJSONObject("versionInfo");
    }

    private void runInstaller(Path installerJar) throws Exception {
        //Create default launcher_profiles.json
        LauncherProfileFix.prepare(mcdir);

        //Run forge installer
        try (URLClassLoader cl = new URLClassLoader(new URL[]{installerJar.toFile().toURI().toURL()}, ClassLoader.getSystemClassLoader().getParent())) {
            Class<?> installer = cl.loadClass("net.minecraftforge.installer.SimpleInstaller");
            installer.getField("headless").setBoolean(null, true);
            Class<?> actions = cl.loadClass("net.minecraftforge.installer.actions.Actions");
            Class<?> progressCallback = cl.loadClass("net.minecraftforge.installer.actions.ProgressCallback");
            Method loadProfile = cl.loadClass("net.minecraftforge.installer.json.Util").getMethod("loadInstallProfile");
            Method getAction = actions.getMethod("getAction", loadProfile.getReturnType(), progressCallback);
            Object action = getAction.invoke(actions.getField("CLIENT").get(null), loadProfile.invoke(null), progressCallback.getField("TO_STD_OUT").get(null));
            Class<?> actionType = getAction.getReturnType();
            boolean installed = InstallerActionFix.run(action, actionType, mcdir.getRoot(), installerJar.toFile());
            if (!installed) {
                throw new IOException("Forge client installation failed");
            }
        }
    }
}
