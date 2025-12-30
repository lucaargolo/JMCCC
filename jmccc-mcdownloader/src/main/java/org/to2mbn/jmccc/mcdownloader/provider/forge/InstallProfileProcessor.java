package org.to2mbn.jmccc.mcdownloader.provider.forge;

import org.to2mbn.jmccc.internal.org.json.JSONObject;
import org.to2mbn.jmccc.internal.org.json.JSONTokener;
import org.to2mbn.jmccc.mcdownloader.download.tasks.ResultProcessor;
import org.to2mbn.jmccc.mcdownloader.provider.VersionJsonInstaller;
import org.to2mbn.jmccc.option.MinecraftDirectory;
import org.to2mbn.jmccc.util.IOUtils;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

class InstallProfileProcessor implements ResultProcessor<byte[], String> {

    private MinecraftDirectory mcdir;

    public InstallProfileProcessor(MinecraftDirectory mcdir) {
        this.mcdir = mcdir;
    }

    @Override
    public String process(byte[] arg) throws Exception {
        Path installer = mcdir.get("forge-installer.jar");
        ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(installer));

        String version = null;
        String newInstallerVersion = null;

        boolean asmServerToClientAction = false;

        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(arg))) {
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {

                // Read MANIFEST.MF to determine SimpleInstaller version
                if ("META-INF/MANIFEST.MF".equals(entry.getName())) {
                    Manifest manifest = new Manifest(in);
                    Attributes attributes = manifest.getEntries().get("net/minecraftforge/installer/");
                    String implVersion = attributes.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
                    if (!isVersionOlderThan22(implVersion)) {
                        asmServerToClientAction = true;
                    }

                    // Do not copy manifest (removes signature)
                    continue;
                }

                zos.putNextEntry(new ZipEntry(entry.getName()));

                if ("install_profile.json".equals(entry.getName())) {
                    byte[] bytes = IOUtils.toByteArray(in);
                    JSONObject installProfile = new JSONObject(new JSONTokener(new String(bytes)));
                    JSONObject versionInfo = processJson(installProfile);

                    // 1.12.2 2850-
                    if (versionInfo != null) {
                        version = new VersionJsonInstaller(mcdir).process(versionInfo);
                        in.closeEntry();
                        break;
                    }

                    newInstallerVersion = installProfile.optString("version");
                    zos.write(bytes);

                } else if ("net/minecraftforge/installer/SimpleInstaller.class".equals(entry.getName())) {

                    byte[] out = ForgeInstallerTweaker.tweakSimpleInstaller(in, asmServerToClientAction);
                    zos.write(out);

                } else {
                    zos.write(IOUtils.toByteArray(in));
                }

                in.closeEntry();
                zos.closeEntry();
            }
        }

        zos.close();

        if (version != null) {
            Files.delete(installer);
            return version;
        }

        // 1.12.2 2851+
        runInstaller(installer, asmServerToClientAction);
        Files.delete(installer);
        return newInstallerVersion;
    }

    private boolean isVersionOlderThan22(String version) {
        if (version == null) return false;

        String[] parts = version.split("\\.");
        int vMajor = parts.length > 0 ? Integer.parseInt(parts[0]) : 0;
        int vMinor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;

        return vMajor > 2 || (vMajor == 2 && vMinor >= 2);
    }

    protected JSONObject processJson(JSONObject installprofile) {
        return installprofile.optJSONObject("versionInfo");
    }

    private void runInstaller(Path installerJar, boolean tweaked) throws Exception {
        //Create default launcher_profiles.json
        Path launcherProfile = mcdir.get("launcher_profiles.json");
        if (!Files.exists(launcherProfile)) {
            Files.write(launcherProfile, "{}".getBytes(StandardCharsets.UTF_8));
        }

        //Run forge installer
        try (URLClassLoader cl = new URLClassLoader(new URL[]{installerJar.toFile().toURI().toURL()})) {
            Class<?> installer = cl.loadClass("net.minecraftforge.installer.SimpleInstaller");
            Method main = installer.getMethod("main", String[].class);
            //We have tweaked install server to install client
            main.invoke(null, (Object) new String[]{tweaked ? "--installServer" : "--installClient", mcdir.getAbsolutePath()});
        }
    }
}
