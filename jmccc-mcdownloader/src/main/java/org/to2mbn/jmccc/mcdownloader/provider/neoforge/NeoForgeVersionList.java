package org.to2mbn.jmccc.mcdownloader.provider.neoforge;

import org.to2mbn.jmccc.internal.org.json.JSONArray;
import org.to2mbn.jmccc.internal.org.json.JSONObject;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

public class NeoForgeVersionList implements Serializable {

    private static final long serialVersionUID = 1L;
    private final Map<String, List<NeoForgeVersion>> versions;
    private final Map<String, NeoForgeVersion> latests;
    private final Map<String, NeoForgeVersion> recommendeds;
    private final Map<String, NeoForgeVersion> neoForgeVersionMapping;
    private final NeoForgeVersion latest;
    private final NeoForgeVersion recommended;

    public NeoForgeVersionList(
            Map<String, List<NeoForgeVersion>> versions, Map<String, NeoForgeVersion> latests,
            Map<String, NeoForgeVersion> recommendeds, Map<String, NeoForgeVersion> neoForgeVersionMapping, NeoForgeVersion latest,
            NeoForgeVersion recommended) {
        Objects.requireNonNull(versions);
        Objects.requireNonNull(latests);
        Objects.requireNonNull(recommendeds);
        Objects.requireNonNull(neoForgeVersionMapping);
        this.versions = versions;
        this.latests = latests;
        this.recommendeds = recommendeds;
        this.neoForgeVersionMapping = neoForgeVersionMapping;
        this.latest = latest;
        this.recommended = recommended;
    }

    public static NeoForgeVersionList fromJson(JSONObject metaJson) {
        JSONArray jsonArray = metaJson.getJSONArray("versions");
        List<NeoForgeVersion> neoForgeVersionList = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            String neoForgeVersion = jsonArray.getString(i);
            neoForgeVersionList.add(new NeoForgeVersion(neoForgeVersion));
        }

        Map<String, List<NeoForgeVersion>> versions = new HashMap<>();
        Map<String, NeoForgeVersion> latests = new HashMap<>();
        Map<String, NeoForgeVersion> recommendeds = new HashMap<>();
        Map<String, NeoForgeVersion> neoForgeVersionMapping = new HashMap<>();
        for(NeoForgeVersion version : neoForgeVersionList) {
            String minecraftVersion = version.getMinecraftVersion();
            versions.computeIfAbsent(minecraftVersion, k -> new ArrayList<>()).add(version);
            latests.put(version.getMinecraftVersion(), version);
            if(!version.isBeta()) {
                recommendeds.put(version.getMinecraftVersion(), version);
            }
            neoForgeVersionMapping.put(version.getNeoForgeVersion(), version);
        }
        NeoForgeVersion latest = neoForgeVersionList.get(neoForgeVersionList.size() - 1);
        List<NeoForgeVersion> recommendedVersionList = neoForgeVersionList.stream().filter(version -> !version.isBeta()).collect(Collectors.toList());
        NeoForgeVersion recommended = recommendedVersionList.get(recommendedVersionList.size() - 1);

        return new NeoForgeVersionList(versions, latests, recommendeds, neoForgeVersionMapping, latest, recommended);
    }

    /**
     * Gets all the forge versions of given minecraft version.
     *
     * @return all the forge versions
     */
    public List<NeoForgeVersion> getVersions(String mcversion) {
        return versions.get(mcversion);
    }

    /**
     * Gets all the latest versions.
     *
     * @return a map including all the latest versions, key is the minecraft
     * version, value is the latest forge version of the minecraft
     * version
     */
    public Map<String, NeoForgeVersion> getLatests() {
        return latests;
    }

    /**
     * Gets all the recommended versions.
     *
     * @return a map including all the recommended versions, key is the
     * minecraft version, value is the recommended forge version of the
     * minecraft version
     */
    public Map<String, NeoForgeVersion> getRecommendeds() {
        return recommendeds;
    }

    /**
     * Gets the latest forge version.
     *
     * @return the latest forge version, null if unknown
     */
    public NeoForgeVersion getLatest() {
        return latest;
    }

    /**
     * Gets the latest forge version of the given minecraft version.
     *
     * @param mcversion the minecraft version
     * @return the latest forge version of <code>mcversion</code>, null if
     * unknown
     */
    public NeoForgeVersion getLatest(String mcversion) {
        return latests.get(mcversion);
    }

    /**
     * Gets the recommended forge version.
     *
     * @return the recommended forge version, null if unknown
     */
    public NeoForgeVersion getRecommended() {
        return recommended;
    }

    /**
     * Gets the recommended forge version of the given minecraft version.
     *
     * @param mcversion the minecraft version
     * @return the recommended forge version of <code>mcversion</code>, null if
     * unknown
     */
    public NeoForgeVersion getRecommended(String mcversion) {
        return recommendeds.get(mcversion);
    }

    public Map<String, NeoForgeVersion> getNeoForgeVersionMapping() {
        return neoForgeVersionMapping;
    }

    public NeoForgeVersion get(String mcversion, int buildNumber) {
        return versions.get(mcversion).get(buildNumber);
    }

    public NeoForgeVersion get(String neoForgeVersion) {
        return neoForgeVersionMapping.get(neoForgeVersion);
    }

    @Override
    public int hashCode() {
        return versions.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof NeoForgeVersionList) {
            NeoForgeVersionList another = (NeoForgeVersionList) obj;
            return Objects.equals(versions, another.versions) &&
                    Objects.equals(latests, another.latests) &&
                    Objects.equals(recommendeds, another.recommendeds) &&
                    Objects.equals(neoForgeVersionMapping, another.neoForgeVersionMapping) &&
                    Objects.equals(latest, another.latest) &&
                    Objects.equals(recommended, another.recommended);
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("NeoForgeVersionList [versions=%s, latests=%s, recommendeds=%s, forgeVersionMapping=%s, latest=%s, recommended=%s]", versions, latests, recommendeds, neoForgeVersionMapping, latest, recommended);
    }

}
