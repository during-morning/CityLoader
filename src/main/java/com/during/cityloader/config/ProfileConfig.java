package com.during.cityloader.config;

import com.during.cityloader.worldgen.LostCityProfile;
import org.bukkit.World;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Profile配置容器
 * 负责管理Profile定义与维度覆盖逻辑
 *
 * @author During
 * @since 1.4.0
 */
public class ProfileConfig {

    private final String selectedProfile;
    private final List<String> dimensionsWithProfiles;
    private final Map<String, LostCityProfile> profiles;

    public ProfileConfig(String selectedProfile,
                         List<String> dimensionsWithProfiles,
                         Map<String, LostCityProfile> profiles) {
        this.selectedProfile = selectedProfile == null ? "" : selectedProfile.trim();
        this.dimensionsWithProfiles = dimensionsWithProfiles == null
                ? List.of()
                : List.copyOf(dimensionsWithProfiles);
        this.profiles = profiles == null ? new HashMap<>() : new HashMap<>(profiles);
    }

    public String getSelectedProfile() {
        return selectedProfile;
    }

    public List<String> getDimensionsWithProfiles() {
        return Collections.unmodifiableList(dimensionsWithProfiles);
    }

    public Map<String, LostCityProfile> getProfiles() {
        return Collections.unmodifiableMap(profiles);
    }

    public LostCityProfile getProfile(String name) {
        if (name == null || name.isBlank()) {
            return profiles.get("default");
        }
        LostCityProfile profile = profiles.get(name);
        if (profile != null) {
            return profile;
        }
        return profiles.get("default");
    }

    public String resolveProfileName(World world) {
        if (selectedProfile != null && !selectedProfile.isBlank()) {
            return selectedProfile;
        }

        String dimensionProfile = resolveDimensionProfile(world);
        if (dimensionProfile != null && !dimensionProfile.isBlank()) {
            return dimensionProfile;
        }

        return "default";
    }

    public LostCityProfile resolveProfile(World world) {
        return getProfile(resolveProfileName(world));
    }

    public LostCityProfile resolveOutsideProfile(LostCityProfile profile) {
        if (profile == null) {
            return profiles.get("default");
        }
        String outsideProfile = profile.getCitySphereOutsideProfile();
        if (outsideProfile == null || outsideProfile.isBlank()) {
            return profile;
        }
        LostCityProfile resolved = profiles.get(outsideProfile);
        return resolved == null ? profile : resolved;
    }

    public boolean hasDimensionProfile(World world) {
        String profileName = resolveDimensionProfile(world);
        return profileName != null && !profileName.isBlank();
    }

    /**
     * 判断指定世界是否启用城市生成。
     * 规则：
     * 1) 指定了 selected-profile：所有世界启用；
     * 2) dimensions-with-profiles 显式映射：该世界启用；
     * 3) 否则保持兼容：仅 NORMAL 世界启用。
     */
    public boolean isGenerationEnabled(World world) {
        if (world == null) {
            return false;
        }
        if (selectedProfile != null && !selectedProfile.isBlank()) {
            return true;
        }
        if (hasDimensionProfile(world)) {
            return true;
        }
        return world.getEnvironment() == World.Environment.NORMAL;
    }

    private String resolveDimensionProfile(World world) {
        if (world == null) {
            return null;
        }

        String key = world.getKey().toString();
        for (String entry : dimensionsWithProfiles) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            String[] split = entry.split("=", 2);
            if (split.length != 2) {
                continue;
            }
            if (split[0].trim().equalsIgnoreCase(key)) {
                return split[1].trim();
            }
        }
        return null;
    }
}
