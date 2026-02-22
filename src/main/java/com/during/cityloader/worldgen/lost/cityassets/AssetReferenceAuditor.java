package com.during.cityloader.worldgen.lost.cityassets;

import com.during.cityloader.util.ResourceLocation;
import com.during.cityloader.worldgen.lost.regassets.data.CityStyleSelector;
import com.during.cityloader.worldgen.lost.regassets.data.SelectorEntry;
import com.during.cityloader.worldgen.lost.regassets.data.WorldPartSettings;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * 资产引用审计器，用于在启动时发现缺失/错误引用，降低运行时静默失败。
 */
public final class AssetReferenceAuditor {

    private static final Logger LOGGER = Logger.getLogger("CityLoader");
    private static final int MAX_LOGGED_ISSUES = 200;
    private static final boolean VERBOSE_LOG = Boolean.parseBoolean(
            System.getProperty("cityloader.assetAuditVerbose", "false"));

    private AssetReferenceAuditor() {
    }

    public static AuditReport audit(World world) {
        AuditReport report = new AuditReport();
        auditWorldStyles(world, report);
        auditCityStyles(world, report);
        auditStyles(world, report);
        auditBuildings(world, report);
        auditMultiBuildings(world, report);
        auditPalettes(world, report);

        if (!report.issues.isEmpty()) {
            if (VERBOSE_LOG) {
                int logged = Math.min(MAX_LOGGED_ISSUES, report.issues.size());
                List<String> issueList = new ArrayList<>(report.issues);
                for (int i = 0; i < logged; i++) {
                    LOGGER.warning("资产引用异常: " + issueList.get(i));
                }
                if (report.issues.size() > logged) {
                    LOGGER.warning("资产引用异常已截断显示: " + report.issues.size() + " 条，仅展示前 " + logged + " 条");
                }
            } else {
                LOGGER.warning("资产引用审计发现异常: " + report.issues.size()
                        + " 条，使用 -Dcityloader.assetAuditVerbose=true 查看明细");
            }
        }
        LOGGER.info("资产引用审计完成: 缺失=" + report.missingReferences + ", 无效=" + report.invalidReferences);
        return report;
    }

    private static void auditWorldStyles(World world, AuditReport report) {
        for (WorldStyle worldStyle : AssetRegistries.WORLDSTYLES.getIterable()) {
            if (worldStyle == null) {
                continue;
            }
            for (CityStyleSelector selector : worldStyle.getCityStyleSelectors()) {
                if (selector == null) {
                    continue;
                }
                requireExists(
                        world,
                        report,
                        "WORLDSTYLE_CITYSTYLE",
                        worldStyle.getId(),
                        selector.getCityStyle(),
                        AssetRegistries.CITYSTYLES);
            }

            WorldPartSettings parts = worldStyle.getPartSettings();
            if (parts == null) {
                continue;
            }
            requireExists(world, report, "WORLDSTYLE_PART", worldStyle.getId(), parts.getOpen(), AssetRegistries.PARTS);
            requireExists(world, report, "WORLDSTYLE_PART", worldStyle.getId(), parts.getOpenBi(), AssetRegistries.PARTS);
            requireExists(world, report, "WORLDSTYLE_PART", worldStyle.getId(), parts.getBridge(), AssetRegistries.PARTS);
            requireExists(world, report, "WORLDSTYLE_PART", worldStyle.getId(), parts.getBridgeBi(), AssetRegistries.PARTS);
            requireExists(world, report, "WORLDSTYLE_PART", worldStyle.getId(), parts.getTunnel(), AssetRegistries.PARTS);
            requireExists(world, report, "WORLDSTYLE_PART", worldStyle.getId(), parts.getTunnelBi(), AssetRegistries.PARTS);
            requireExists(world, report, "WORLDSTYLE_PART", worldStyle.getId(), parts.getRails3Split(), AssetRegistries.PARTS);
            requireExists(world, report, "WORLDSTYLE_PART", worldStyle.getId(), parts.getRailsBend(), AssetRegistries.PARTS);
            requireExists(world, report, "WORLDSTYLE_PART", worldStyle.getId(), parts.getRailsDown1(), AssetRegistries.PARTS);
            requireExists(world, report, "WORLDSTYLE_PART", worldStyle.getId(), parts.getRailsDown2(), AssetRegistries.PARTS);
            requireExists(world, report, "WORLDSTYLE_PART", worldStyle.getId(), parts.getRailsFlat(), AssetRegistries.PARTS);
            requireExists(world, report, "WORLDSTYLE_PART", worldStyle.getId(), parts.getRailsHorizontal(), AssetRegistries.PARTS);
            requireExists(world, report, "WORLDSTYLE_PART", worldStyle.getId(), parts.getRailsHorizontalEnd(), AssetRegistries.PARTS);
            requireExists(world, report, "WORLDSTYLE_PART", worldStyle.getId(), parts.getRailsHorizontalWater(), AssetRegistries.PARTS);
            requireExists(world, report, "WORLDSTYLE_PART", worldStyle.getId(), parts.getRailsVertical(), AssetRegistries.PARTS);
            requireExists(world, report, "WORLDSTYLE_PART", worldStyle.getId(), parts.getRailsVerticalWater(), AssetRegistries.PARTS);
            requireExists(world, report, "WORLDSTYLE_PART", worldStyle.getId(), parts.getStationUnderground(), AssetRegistries.PARTS);
            requireExists(world, report, "WORLDSTYLE_PART", worldStyle.getId(), parts.getStationUndergroundStairs(), AssetRegistries.PARTS);
        }
    }

    private static void auditCityStyles(World world, AuditReport report) {
        for (CityStyle cityStyle : AssetRegistries.CITYSTYLES.getIterable()) {
            if (cityStyle == null) {
                continue;
            }
            requireExists(world, report, "CITYSTYLE_INHERIT", cityStyle.getId(), cityStyle.getInherit(), AssetRegistries.CITYSTYLES);
            requireExists(world, report, "CITYSTYLE_STYLE", cityStyle.getId(), cityStyle.getStyle(), AssetRegistries.STYLES);

            auditSelectorEntries(world, report, "CITYSTYLE_BUILDING", cityStyle.getId(), cityStyle.getSelector("buildings"), AssetRegistries.BUILDINGS);
            auditSelectorEntries(world, report, "CITYSTYLE_MULTIBUILDING", cityStyle.getId(), cityStyle.getSelector("multibuildings"), AssetRegistries.MULTI_BUILDINGS);
            auditSelectorEntries(world, report, "CITYSTYLE_PART", cityStyle.getId(), cityStyle.getSelector("parts"), AssetRegistries.PARTS);
            auditSelectorEntries(world, report, "CITYSTYLE_PART", cityStyle.getId(), cityStyle.getSelector("bridges"), AssetRegistries.PARTS);
            auditSelectorEntries(world, report, "CITYSTYLE_PART", cityStyle.getId(), cityStyle.getSelector("fronts"), AssetRegistries.PARTS);
            auditSelectorEntries(world, report, "CITYSTYLE_PART", cityStyle.getId(), cityStyle.getSelector("stairs"), AssetRegistries.PARTS);
            auditSelectorEntries(world, report, "CITYSTYLE_PART", cityStyle.getId(), cityStyle.getSelector("fountains"), AssetRegistries.PARTS);
            auditSelectorEntries(world, report, "CITYSTYLE_PART", cityStyle.getId(), cityStyle.getSelector("parks"), AssetRegistries.PARTS);
            auditSelectorEntries(world, report, "CITYSTYLE_PART", cityStyle.getId(), cityStyle.getSelector("raildungeons"), AssetRegistries.PARTS);
            auditSelectorPaletteEntries(world, report, "CITYSTYLE_PALETTE", cityStyle.getId(), cityStyle.getSelector("palettes"));

            for (String legacy : cityStyle.getBuildings()) {
                requireExists(world, report, "CITYSTYLE_LEGACY_BUILDING", cityStyle.getId(), legacy, AssetRegistries.BUILDINGS);
            }
            for (String legacy : cityStyle.getMultiBuildings()) {
                requireExists(world, report, "CITYSTYLE_LEGACY_MULTIBUILDING", cityStyle.getId(), legacy, AssetRegistries.MULTI_BUILDINGS);
            }
        }
    }

    private static void auditStyles(World world, AuditReport report) {
        for (Style style : AssetRegistries.STYLES.getIterable()) {
            if (style == null) {
                continue;
            }
            for (List<Style.WeightedPalette> group : style.getRandomPaletteChoices()) {
                if (group == null) {
                    continue;
                }
                for (Style.WeightedPalette weighted : group) {
                    if (weighted == null) {
                        continue;
                    }
                    requirePaletteExists(world, report, "STYLE_RANDOM_PALETTE", style.getId(), weighted.palette());
                }
            }

            auditStyleBuildingEntries(world, report, style.getId(), style.getSelectors().getBuildings());
            auditSelectorEntries(world, report, "STYLE_MULTIBUILDING", style.getId(), style.getSelectors().getMultiBuildings(), AssetRegistries.MULTI_BUILDINGS);
            auditSelectorEntries(world, report, "STYLE_PART", style.getId(), style.getSelectors().getParts(), AssetRegistries.PARTS);
            auditSelectorPaletteEntries(world, report, "STYLE_PALETTE", style.getId(), style.getSelectors().getPalettes());
        }
    }

    private static void auditBuildings(World world, AuditReport report) {
        for (Building building : AssetRegistries.BUILDINGS.getIterable()) {
            if (building == null) {
                continue;
            }
            requirePaletteExists(world, report, "BUILDING_REF_PALETTE", building.getId(), building.getRefPalette());
            for (String part : building.getPartNames()) {
                requireExists(world, report, "BUILDING_PART", building.getId(), part, AssetRegistries.PARTS);
            }
            for (String part : building.getPartNames2()) {
                requireExists(world, report, "BUILDING_PART2", building.getId(), part, AssetRegistries.PARTS);
            }
        }
    }

    private static void auditMultiBuildings(World world, AuditReport report) {
        for (MultiBuilding multi : AssetRegistries.MULTI_BUILDINGS.getIterable()) {
            if (multi == null || multi.getBuildings() == null) {
                continue;
            }
            for (List<String> row : multi.getBuildings()) {
                if (row == null) {
                    continue;
                }
                for (String buildingName : row) {
                    requireExists(world, report, "MULTIBUILDING_SLOT", multi.getId(), buildingName, AssetRegistries.BUILDINGS);
                }
            }
        }
    }

    private static void auditPalettes(World world, AuditReport report) {
        for (Palette palette : AssetRegistries.PALETTES.getIterable()) {
            if (palette == null) {
                continue;
            }
            for (Palette.Entry entry : palette.getEntries().values()) {
                if (entry == null) {
                    continue;
                }
                if (entry.fromPalette() != null && !entry.fromPalette().isBlank() && entry.fromPalette().length() > 1) {
                    requirePaletteExists(world, report, "PALETTE_FROMPALETTE", palette.getId(), entry.fromPalette());
                }
                if (entry.variant() != null && !entry.variant().isBlank()) {
                    requireExists(world, report, "PALETTE_VARIANT", palette.getId(), entry.variant(), AssetRegistries.VARIANTS);
                }
            }
        }
    }

    private static <T extends ILostCityAsset, R extends com.during.cityloader.worldgen.lost.regassets.IAsset> void auditSelectorEntries(
            World world,
            AuditReport report,
            String type,
            ResourceLocation owner,
            List<SelectorEntry> entries,
            RegistryAssetRegistry<T, R> registry) {
        if (entries == null || entries.isEmpty()) {
            return;
        }
        for (SelectorEntry entry : entries) {
            if (entry == null) {
                continue;
            }
            requireExists(world, report, type, owner, entry.getValue(), registry);
        }
    }

    private static void auditStyleBuildingEntries(
            World world,
            AuditReport report,
            ResourceLocation owner,
            List<SelectorEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return;
        }
        for (SelectorEntry entry : entries) {
            if (entry == null) {
                continue;
            }
            requireExistsInBuildingOrPart(world, report, "STYLE_BUILDING", owner, entry.getValue());
        }
    }

    private static void auditSelectorPaletteEntries(
            World world,
            AuditReport report,
            String type,
            ResourceLocation owner,
            List<SelectorEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return;
        }
        for (SelectorEntry entry : entries) {
            if (entry == null) {
                continue;
            }
            requirePaletteExists(world, report, type, owner, entry.getValue());
        }
    }

    private static <T extends ILostCityAsset, R extends com.during.cityloader.worldgen.lost.regassets.IAsset> void requireExists(
            World world,
            AuditReport report,
            String type,
            ResourceLocation owner,
            String rawReference,
            RegistryAssetRegistry<T, R> registry) {
        if (rawReference == null || rawReference.isBlank()) {
            return;
        }

        String raw = rawReference.trim().toLowerCase(Locale.ROOT);
        List<ResourceLocation> candidates = resolveCandidates(owner, raw);
        for (ResourceLocation candidate : candidates) {
            if (registry.get(world, candidate) != null) {
                return;
            }
        }

        report.missingReferences++;
        report.issues.add(type + " owner=" + owner + " reference=" + rawReference + " candidates=" + candidates);
    }

    private static void requireExistsInBuildingOrPart(
            World world,
            AuditReport report,
            String type,
            ResourceLocation owner,
            String rawReference) {
        if (rawReference == null || rawReference.isBlank()) {
            return;
        }

        String raw = rawReference.trim().toLowerCase(Locale.ROOT);
        List<ResourceLocation> candidates = resolveCandidates(owner, raw);
        for (ResourceLocation candidate : candidates) {
            if (AssetRegistries.BUILDINGS.get(world, candidate) != null
                    || AssetRegistries.PARTS.get(world, candidate) != null) {
                return;
            }
        }

        report.missingReferences++;
        report.issues.add(type + " owner=" + owner + " reference=" + rawReference + " candidates=" + candidates);
    }

    private static void requirePaletteExists(
            World world,
            AuditReport report,
            String type,
            ResourceLocation owner,
            String rawReference) {
        if (rawReference == null || rawReference.isBlank()) {
            return;
        }

        String raw = rawReference.trim().toLowerCase(Locale.ROOT);
        List<ResourceLocation> candidates = resolveCandidates(owner, raw);
        if (!raw.contains(":") && !raw.startsWith("palette_")) {
            candidates.addAll(resolveCandidates(owner, "palette_" + raw));
        }

        for (ResourceLocation candidate : candidates) {
            if (AssetRegistries.PALETTES.get(world, candidate) != null) {
                return;
            }
        }

        report.missingReferences++;
        report.issues.add(type + " owner=" + owner + " reference=" + rawReference + " candidates=" + candidates);
    }

    private static List<ResourceLocation> resolveCandidates(ResourceLocation owner, String raw) {
        List<ResourceLocation> candidates = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return candidates;
        }

        if (raw.contains(":")) {
            ResourceLocation explicit = ResourceLocation.parse(raw);
            if (explicit != null) {
                candidates.add(explicit);
            }
            return candidates;
        }

        if (owner != null) {
            candidates.add(new ResourceLocation(owner.getNamespace(), raw));
        }
        if (owner == null || !"lostcities".equals(owner.getNamespace())) {
            candidates.add(new ResourceLocation("lostcities", raw));
        }
        return candidates;
    }

    public static final class AuditReport {
        private int missingReferences;
        private int invalidReferences;
        private final LinkedHashSet<String> issues = new LinkedHashSet<>();

        public int getMissingReferences() {
            return missingReferences;
        }

        public int getInvalidReferences() {
            return invalidReferences;
        }

        public List<String> getIssues() {
            return List.copyOf(new ArrayList<>(issues));
        }
    }
}
