package org.to2mbn.jmccc.mcdownloader.provider.neoforge.fix;

import org.to2mbn.jmccc.internal.org.json.JSONObject;
import org.to2mbn.jmccc.internal.org.json.JSONTokener;
import org.to2mbn.jmccc.mcdownloader.download.cache.CacheNames;
import org.to2mbn.jmccc.mcdownloader.download.combine.CombinedDownloadTask;
import org.to2mbn.jmccc.mcdownloader.download.tasks.MemoryDownloadTask;
import org.to2mbn.jmccc.mcdownloader.provider.neoforge.NeoForgeVersion;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public final class LegacyNeoForgeFix {

    private static final String METADATA_PATH = "net/neoforged/forge/maven-metadata.xml";
    private static final String INSTALL_PROFILE = "install_profile.json";
    private static final String VERSION_JSON = "version.json";
    private static final String MANIFEST = "META-INF/MANIFEST.MF";
    private static final String PROFILE_NAME = "NeoForge";
    private static final Pattern VERSION_PATTERN = Pattern.compile("^(?:1\\.20\\.1-)?(47\\.[\\w.\\-+]+)$");

    public static final String MINECRAFT_VERSION = "1.20.1";
    public static final String ARTIFACT_ID = "forge";

    private LegacyNeoForgeFix() {
    }

    public static CombinedDownloadTask<List<NeoForgeVersion>> versionList(String repositoryUrl) {
        return CombinedDownloadTask.single(
                new MemoryDownloadTask(repositoryUrl + METADATA_PATH)
                        .cacheable()
                        .cachePool(CacheNames.NEOFORGE_VERSION_META))
                .andThen(LegacyNeoForgeFix::parseVersions);
    }

    public static NeoForgeVersion resolve(String version) {
        String canonical = normalize(version);
        return canonical == null ? null : NeoForgeVersion.legacy(canonical);
    }

    public static String normalize(String version) {
        if (!version.startsWith("47.") && !version.startsWith("1.20.1-47.")) return null;
        Matcher matcher = VERSION_PATTERN.matcher(version);
        return matcher.matches() ? matcher.group(1) : null;
    }

    public static byte[] adaptInstaller(byte[] installer, NeoForgeVersion version) throws IOException {
        if (!version.isLegacy()) {
            return installer;
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream(installer.length);
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(installer));
             ZipOutputStream zip = new ZipOutputStream(out)) {
            byte[] buffer = new byte[8192];
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                String name = entry.getName();
                if (isSignatureFile(name)) {
                    in.closeEntry();
                    continue;
                }
                byte[] data;
                if (MANIFEST.equals(name)) {
                    data = sanitizeManifest(readEntry(in, buffer));
                    if (data == null) {
                        in.closeEntry();
                        continue;
                    }
                } else if (INSTALL_PROFILE.equals(name) || VERSION_JSON.equals(name)) {
                    JSONObject json = new JSONObject(new JSONTokener(new String(readEntry(in, buffer), StandardCharsets.UTF_8)));
                    if (INSTALL_PROFILE.equals(name)) {
                        json.put("version", version.getVersionName());
                        json.put("profile", PROFILE_NAME);
                    } else {
                        json.put("id", version.getVersionName());
                    }
                    data = json.toString().getBytes(StandardCharsets.UTF_8);
                } else {
                    zip.putNextEntry(copyEntry(entry));
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        zip.write(buffer, 0, read);
                    }
                    zip.closeEntry();
                    in.closeEntry();
                    continue;
                }

                zip.putNextEntry(copyEntry(entry));
                zip.write(data);
                zip.closeEntry();
                in.closeEntry();
            }
        }
        return out.toByteArray();
    }

    private static List<NeoForgeVersion> parseVersions(byte[] metadata) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

        Document document;
        try (ByteArrayInputStream in = new ByteArrayInputStream(metadata)) {
            document = factory.newDocumentBuilder().parse(in);
        }

        NodeList nodes = document.getElementsByTagName("version");
        Map<String, NeoForgeVersion> versions = new LinkedHashMap<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            String rawVersion = nodes.item(i).getTextContent().trim();
            NeoForgeVersion version = resolve(rawVersion);
            if (version == null) {
                throw new IllegalArgumentException("Unexpected NeoForge legacy version: " + rawVersion);
            }

            if (!versions.containsKey(version.getNeoForgeVersion())) {
                versions.put(version.getNeoForgeVersion(), version);
            }
        }
        return new ArrayList<>(versions.values());
    }

    private static byte[] sanitizeManifest(byte[] data) throws IOException {
        Manifest manifest = new Manifest(new ByteArrayInputStream(data));
        removeDigestAttributes(manifest.getMainAttributes());
        Iterator<Map.Entry<String, Attributes>> entries = manifest.getEntries().entrySet().iterator();
        while (entries.hasNext()) {
            Attributes attributes = entries.next().getValue();
            removeDigestAttributes(attributes);
            if (attributes.isEmpty()) {
                entries.remove();
            }
        }

        if (manifest.getMainAttributes().isEmpty() && manifest.getEntries().isEmpty()) {
            return null;
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        manifest.write(out);
        return out.toByteArray();
    }

    private static void removeDigestAttributes(Attributes attributes) {
        Iterator<Object> iterator = attributes.keySet().iterator();
        while (iterator.hasNext()) {
            String name = iterator.next().toString().toLowerCase(Locale.ROOT);
            if (name.contains("digest") || name.equals("signature-version")) {
                iterator.remove();
            }
        }
    }

    private static byte[] readEntry(ZipInputStream in, byte[] buffer) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

    private static ZipEntry copyEntry(ZipEntry entry) {
        ZipEntry copy = new ZipEntry(entry.getName());
        if (entry.getTime() != -1) {
            copy.setTime(entry.getTime());
        }
        return copy;
    }

    private static boolean isSignatureFile(String name) {
        if (!name.regionMatches(true, 0, "META-INF/", 0, 9)) return false;
        String upper = name.toUpperCase(Locale.ROOT);
        return upper.endsWith(".SF") || upper.endsWith(".SIG") || upper.endsWith(".RSA") || upper.endsWith(".DSA") || upper.endsWith(".EC") || upper.startsWith("META-INF/SIG-");
    }
}
