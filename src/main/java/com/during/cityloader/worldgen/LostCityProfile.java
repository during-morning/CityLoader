package com.during.cityloader.worldgen;

import java.util.ArrayList;
import java.util.List;

/**
 * 城市配置文件
 * 移植自 LostCities LostCityProfile，包含所有城市生成参数
 *
 * 默认值与 LostCities 1.20 一致
 */
public class LostCityProfile {

    private final String name;

    private boolean highwaysEnabled = true;
    private boolean railwaysEnabled = true;
    private boolean scatteredEnabled = true;
    private boolean damageEnabled = true;

    private String description = "Default generation, common cities, explosions";
    private String extraDescription = "";
    private String warning = "";
    private String worldStyle = "standard";
    private String iconFile = "";

    private int debrisToNearbyChunkFactor = 200;

    private String liquidBlock = "minecraft:water";
    private String baseBlock = "minecraft:stone";

    private float vineChance = 0.009f;
    private float chanceOfRandomLeafBlocks = 0.1f;
    private int thicknessOfRandomLeafBlocks = 2;
    private boolean avoidFoliage = false;

    private float scatteredChanceMultiplier = 1.0f;

    private boolean rubbleLayer = true;
    private float rubbleDirtScale = 3.0f;
    private float rubbleLeaveScale = 6.0f;

    private float ruinChance = 0.05f;
    private float ruinMinLevelPercent = 0.8f;
    private float ruinMaxLevelPercent = 1.0f;

    private int groundLevel = 71;
    private int seaLevel = -1;

    private boolean highwayRequiresTwoCities = true;
    private int highwayLevelFromCitiesMode = 0;
    private float highwayMainPerlinScale = 50.0f;
    private float highwaySecondaryPerlinScale = 10.0f;
    private float highwayPerlinFactor = 2.0f;
    private int highwayDistanceMask = 7;
    private boolean highwaySupports = true;

    private float railwayDungeonChance = 0.01f;
    private boolean railwaysCanEnd = false;
    private boolean railwaysEnabledFlag = true;
    private boolean railwayStationsEnabled = true;
    private boolean railwaySurfaceStationsEnabled = true;

    private boolean explosionsInCitiesOnly = true;

    private boolean editMode = false;

    private boolean generateNether = false;
    private boolean generateSpawners = true;
    private boolean generateLoot = true;
    private boolean generateLighting = false;
    private boolean avoidWater = false;

    private float explosionChance = 0.002f;
    private int explosionMinRadius = 15;
    private int explosionMaxRadius = 35;
    private int explosionMinHeight = 75;
    private int explosionMaxHeight = 90;

    private float miniExplosionChance = 0.03f;
    private int miniExplosionMinRadius = 5;
    private int miniExplosionMaxRadius = 12;
    private int miniExplosionMinHeight = 60;
    private int miniExplosionMaxHeight = 100;

    private double cityChance = 0.01;
    private int cityMinRadius = 50;
    private int cityMaxRadius = 128;

    private double cityPerlinScale = 3.0;
    private double cityPerlinInnerScale = 0.1;
    private double cityPerlinOffset = 0.1;

    private float cityThreshold = 0.2f;

    private int citySpawnDistance1 = 0;
    private int citySpawnDistance2 = 0;
    private double citySpawnMultiplier1 = 1.0;
    private double citySpawnMultiplier2 = 1.0;

    private float cityStyleThreshold = -1f;
    private String cityStyleAlternative = "";

    private boolean cityAvoidVoid = true;

    private boolean citySphere32Grid = false;
    private float citySphereFactor = 1.2f;
    private float citySphereChance = 0.7f;
    private float citySphereSurfaceVariation = 1.0f;
    private float citySphereOutsideSurfaceVariation = 1.0f;
    private float citySphereMonorailChance = 0.8f;
    private int citySphereClearAbove = 0;
    private int citySphereClearBelow = 0;
    private boolean citySphereClearAboveUntilAir = false;
    private boolean citySphereClearBelowUntilAir = false;
    private int citySphereOutsideGroundLevel = -1;
    private String citySphereOutsideProfile = "";
    private boolean citySphereOnlyPredefined = false;
    private int citySphereMonorailHeightOffset = -2;

    private int cityLevel0Height = 75;
    private int cityLevel1Height = 83;
    private int cityLevel2Height = 91;
    private int cityLevel3Height = 99;
    private int cityLevel4Height = 107;
    private int cityLevel5Height = 115;
    private int cityLevel6Height = 123;
    private int cityLevel7Height = 131;
    private int cityMinHeight = 50;
    private int cityMaxHeight = 150;

    private int oceanCorrectionBorder = 4;

    private int terrainFixLowerMinOffset = -4;
    private int terrainFixLowerMaxOffset = -3;
    private int terrainFixUpperMinOffset = -1;
    private int terrainFixUpperMaxOffset = 1;

    private float chestWithoutLootChance = 0.2f;
    private float buildingWithoutLootChance = 0.2f;
    private float buildingChance = 0.3f;
    private int buildingMinFloors = 0;
    private int buildingMaxFloors = 8;
    private int buildingMinFloorsChance = 4;
    private int buildingMaxFloorsChance = 6;
    private int buildingMinCellars = 0;
    private int buildingMaxCellars = 3;
    private float buildingDoorwayChance = 0.6f;
    private float buildingFrontChance = 0.2f;

    private float parkChance = 0.2f;
    private float corridorChance = 0.7f;
    private float bridgeChance = 0.7f;
    private float fountainChance = 0.05f;

    private boolean bridgeSupports = true;
    private boolean parkElevation = true;
    private boolean parkBorder = true;
    private int parkStreetThreshold = 3;

    private boolean multiUseCorner = false;
    private boolean useAvgHeightmap = false;

    private int bedrockLayer = 1;

    private float horizon = -1f;
    private float fogRed = -1.0f;
    private float fogGreen = -1.0f;
    private float fogBlue = -1.0f;
    private float fogDensity = -1.0f;

    private String spawnBiome = "";
    private String spawnCity = "";
    private String spawnSphere = "";
    private boolean spawnNotInBuilding = false;
    private boolean forceSpawnInBuilding = false;
    private List<String> forceSpawnBuildings = new ArrayList<>();
    private List<String> forceSpawnParts = new ArrayList<>();
    private int spawnCheckRadius = 200;
    private int spawnRadiusIncrease = 100;
    private int spawnCheckAttempts = 20000;

    private String landscapeType = "default";

    public LostCityProfile(String name) {
        this.name = name;
    }

    public LostCityProfile(String name, boolean highwaysEnabled, boolean railwaysEnabled, 
                           boolean scatteredEnabled, boolean damageEnabled) {
        this.name = name;
        this.highwaysEnabled = highwaysEnabled;
        this.railwaysEnabled = railwaysEnabled;
        this.scatteredEnabled = scatteredEnabled;
        this.damageEnabled = damageEnabled;
    }

    public LostCityProfile copy(String newName) {
        LostCityProfile copy = new LostCityProfile(newName);
        copy.copyFrom(this);
        return copy;
    }

    public void copyFrom(LostCityProfile other) {
        if (other == null) {
            return;
        }
        highwaysEnabled = other.highwaysEnabled;
        railwaysEnabled = other.railwaysEnabled;
        scatteredEnabled = other.scatteredEnabled;
        damageEnabled = other.damageEnabled;
        description = other.description;
        extraDescription = other.extraDescription;
        warning = other.warning;
        worldStyle = other.worldStyle;
        iconFile = other.iconFile;
        debrisToNearbyChunkFactor = other.debrisToNearbyChunkFactor;
        liquidBlock = other.liquidBlock;
        baseBlock = other.baseBlock;
        vineChance = other.vineChance;
        chanceOfRandomLeafBlocks = other.chanceOfRandomLeafBlocks;
        thicknessOfRandomLeafBlocks = other.thicknessOfRandomLeafBlocks;
        avoidFoliage = other.avoidFoliage;
        scatteredChanceMultiplier = other.scatteredChanceMultiplier;
        rubbleLayer = other.rubbleLayer;
        rubbleDirtScale = other.rubbleDirtScale;
        rubbleLeaveScale = other.rubbleLeaveScale;
        ruinChance = other.ruinChance;
        ruinMinLevelPercent = other.ruinMinLevelPercent;
        ruinMaxLevelPercent = other.ruinMaxLevelPercent;
        groundLevel = other.groundLevel;
        seaLevel = other.seaLevel;
        highwayRequiresTwoCities = other.highwayRequiresTwoCities;
        highwayLevelFromCitiesMode = other.highwayLevelFromCitiesMode;
        highwayMainPerlinScale = other.highwayMainPerlinScale;
        highwaySecondaryPerlinScale = other.highwaySecondaryPerlinScale;
        highwayPerlinFactor = other.highwayPerlinFactor;
        highwayDistanceMask = other.highwayDistanceMask;
        highwaySupports = other.highwaySupports;
        railwayDungeonChance = other.railwayDungeonChance;
        railwaysCanEnd = other.railwaysCanEnd;
        railwaysEnabledFlag = other.railwaysEnabledFlag;
        railwayStationsEnabled = other.railwayStationsEnabled;
        railwaySurfaceStationsEnabled = other.railwaySurfaceStationsEnabled;
        explosionsInCitiesOnly = other.explosionsInCitiesOnly;
        editMode = other.editMode;
        generateNether = other.generateNether;
        generateSpawners = other.generateSpawners;
        generateLoot = other.generateLoot;
        generateLighting = other.generateLighting;
        avoidWater = other.avoidWater;
        explosionChance = other.explosionChance;
        explosionMinRadius = other.explosionMinRadius;
        explosionMaxRadius = other.explosionMaxRadius;
        explosionMinHeight = other.explosionMinHeight;
        explosionMaxHeight = other.explosionMaxHeight;
        miniExplosionChance = other.miniExplosionChance;
        miniExplosionMinRadius = other.miniExplosionMinRadius;
        miniExplosionMaxRadius = other.miniExplosionMaxRadius;
        miniExplosionMinHeight = other.miniExplosionMinHeight;
        miniExplosionMaxHeight = other.miniExplosionMaxHeight;
        cityChance = other.cityChance;
        cityMinRadius = other.cityMinRadius;
        cityMaxRadius = other.cityMaxRadius;
        cityPerlinScale = other.cityPerlinScale;
        cityPerlinInnerScale = other.cityPerlinInnerScale;
        cityPerlinOffset = other.cityPerlinOffset;
        cityThreshold = other.cityThreshold;
        citySpawnDistance1 = other.citySpawnDistance1;
        citySpawnDistance2 = other.citySpawnDistance2;
        citySpawnMultiplier1 = other.citySpawnMultiplier1;
        citySpawnMultiplier2 = other.citySpawnMultiplier2;
        cityStyleThreshold = other.cityStyleThreshold;
        cityStyleAlternative = other.cityStyleAlternative;
        cityAvoidVoid = other.cityAvoidVoid;
        citySphere32Grid = other.citySphere32Grid;
        citySphereFactor = other.citySphereFactor;
        citySphereChance = other.citySphereChance;
        citySphereSurfaceVariation = other.citySphereSurfaceVariation;
        citySphereOutsideSurfaceVariation = other.citySphereOutsideSurfaceVariation;
        citySphereMonorailChance = other.citySphereMonorailChance;
        citySphereClearAbove = other.citySphereClearAbove;
        citySphereClearBelow = other.citySphereClearBelow;
        citySphereClearAboveUntilAir = other.citySphereClearAboveUntilAir;
        citySphereClearBelowUntilAir = other.citySphereClearBelowUntilAir;
        citySphereOutsideGroundLevel = other.citySphereOutsideGroundLevel;
        citySphereOutsideProfile = other.citySphereOutsideProfile;
        citySphereOnlyPredefined = other.citySphereOnlyPredefined;
        citySphereMonorailHeightOffset = other.citySphereMonorailHeightOffset;
        cityLevel0Height = other.cityLevel0Height;
        cityLevel1Height = other.cityLevel1Height;
        cityLevel2Height = other.cityLevel2Height;
        cityLevel3Height = other.cityLevel3Height;
        cityLevel4Height = other.cityLevel4Height;
        cityLevel5Height = other.cityLevel5Height;
        cityLevel6Height = other.cityLevel6Height;
        cityLevel7Height = other.cityLevel7Height;
        cityMinHeight = other.cityMinHeight;
        cityMaxHeight = other.cityMaxHeight;
        oceanCorrectionBorder = other.oceanCorrectionBorder;
        terrainFixLowerMinOffset = other.terrainFixLowerMinOffset;
        terrainFixLowerMaxOffset = other.terrainFixLowerMaxOffset;
        terrainFixUpperMinOffset = other.terrainFixUpperMinOffset;
        terrainFixUpperMaxOffset = other.terrainFixUpperMaxOffset;
        chestWithoutLootChance = other.chestWithoutLootChance;
        buildingWithoutLootChance = other.buildingWithoutLootChance;
        buildingChance = other.buildingChance;
        buildingMinFloors = other.buildingMinFloors;
        buildingMaxFloors = other.buildingMaxFloors;
        buildingMinFloorsChance = other.buildingMinFloorsChance;
        buildingMaxFloorsChance = other.buildingMaxFloorsChance;
        buildingMinCellars = other.buildingMinCellars;
        buildingMaxCellars = other.buildingMaxCellars;
        buildingDoorwayChance = other.buildingDoorwayChance;
        buildingFrontChance = other.buildingFrontChance;
        parkChance = other.parkChance;
        corridorChance = other.corridorChance;
        bridgeChance = other.bridgeChance;
        fountainChance = other.fountainChance;
        bridgeSupports = other.bridgeSupports;
        parkElevation = other.parkElevation;
        parkBorder = other.parkBorder;
        parkStreetThreshold = other.parkStreetThreshold;
        multiUseCorner = other.multiUseCorner;
        useAvgHeightmap = other.useAvgHeightmap;
        bedrockLayer = other.bedrockLayer;
        horizon = other.horizon;
        fogRed = other.fogRed;
        fogGreen = other.fogGreen;
        fogBlue = other.fogBlue;
        fogDensity = other.fogDensity;
        spawnBiome = other.spawnBiome;
        spawnCity = other.spawnCity;
        spawnSphere = other.spawnSphere;
        spawnNotInBuilding = other.spawnNotInBuilding;
        forceSpawnInBuilding = other.forceSpawnInBuilding;
        forceSpawnBuildings = new ArrayList<>(other.forceSpawnBuildings);
        forceSpawnParts = new ArrayList<>(other.forceSpawnParts);
        spawnCheckRadius = other.spawnCheckRadius;
        spawnRadiusIncrease = other.spawnRadiusIncrease;
        spawnCheckAttempts = other.spawnCheckAttempts;
        landscapeType = other.landscapeType;
    }

    public String getName() {
        return name;
    }

    public boolean isHighwaysEnabled() {
        return highwaysEnabled && highwayDistanceMask > 0;
    }

    public void setHighwaysEnabled(boolean highwaysEnabled) {
        this.highwaysEnabled = highwaysEnabled;
    }

    public boolean isRailwaysEnabled() {
        return railwaysEnabled && railwaysEnabledFlag;
    }

    public void setRailwaysEnabled(boolean railwaysEnabled) {
        this.railwaysEnabled = railwaysEnabled;
    }

    public boolean isScatteredEnabled() {
        return scatteredEnabled && scatteredChanceMultiplier > 0;
    }

    public void setScatteredEnabled(boolean scatteredEnabled) {
        this.scatteredEnabled = scatteredEnabled;
    }

    public boolean isDamageEnabled() {
        return damageEnabled && (explosionChance > 0 || miniExplosionChance > 0 || ruinChance > 0);
    }

    public void setDamageEnabled(boolean damageEnabled) {
        this.damageEnabled = damageEnabled;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getExtraDescription() {
        return extraDescription;
    }

    public void setExtraDescription(String extraDescription) {
        this.extraDescription = extraDescription;
    }

    public String getWarning() {
        return warning;
    }

    public void setWarning(String warning) {
        this.warning = warning;
    }

    public String getWorldStyle() {
        return worldStyle;
    }

    public void setWorldStyle(String worldStyle) {
        this.worldStyle = worldStyle;
    }

    public String getIconFile() {
        return iconFile;
    }

    public void setIconFile(String iconFile) {
        this.iconFile = iconFile;
    }

    public int getDebrisToNearbyChunkFactor() {
        return debrisToNearbyChunkFactor;
    }

    public void setDebrisToNearbyChunkFactor(int debrisToNearbyChunkFactor) {
        this.debrisToNearbyChunkFactor = debrisToNearbyChunkFactor;
    }

    public String getLiquidBlock() {
        return liquidBlock;
    }

    public void setLiquidBlock(String liquidBlock) {
        this.liquidBlock = liquidBlock;
    }

    public String getBaseBlock() {
        return baseBlock;
    }

    public void setBaseBlock(String baseBlock) {
        this.baseBlock = baseBlock;
    }

    public float getVineChance() {
        return vineChance;
    }

    public void setVineChance(float vineChance) {
        this.vineChance = vineChance;
    }

    public float getChanceOfRandomLeafBlocks() {
        return chanceOfRandomLeafBlocks;
    }

    public void setChanceOfRandomLeafBlocks(float chanceOfRandomLeafBlocks) {
        this.chanceOfRandomLeafBlocks = chanceOfRandomLeafBlocks;
    }

    public int getThicknessOfRandomLeafBlocks() {
        return thicknessOfRandomLeafBlocks;
    }

    public void setThicknessOfRandomLeafBlocks(int thicknessOfRandomLeafBlocks) {
        this.thicknessOfRandomLeafBlocks = thicknessOfRandomLeafBlocks;
    }

    public boolean isAvoidFoliage() {
        return avoidFoliage;
    }

    public void setAvoidFoliage(boolean avoidFoliage) {
        this.avoidFoliage = avoidFoliage;
    }

    public float getScatteredChanceMultiplier() {
        return scatteredChanceMultiplier;
    }

    public void setScatteredChanceMultiplier(float scatteredChanceMultiplier) {
        this.scatteredChanceMultiplier = scatteredChanceMultiplier;
    }

    public boolean isRubbleLayer() {
        return rubbleLayer;
    }

    public void setRubbleLayer(boolean rubbleLayer) {
        this.rubbleLayer = rubbleLayer;
    }

    public float getRubbleDirtScale() {
        return rubbleDirtScale;
    }

    public void setRubbleDirtScale(float rubbleDirtScale) {
        this.rubbleDirtScale = rubbleDirtScale;
    }

    public float getRubbleLeaveScale() {
        return rubbleLeaveScale;
    }

    public void setRubbleLeaveScale(float rubbleLeaveScale) {
        this.rubbleLeaveScale = rubbleLeaveScale;
    }

    public float getRuinChance() {
        return ruinChance;
    }

    public void setRuinChance(float ruinChance) {
        this.ruinChance = ruinChance;
    }

    public float getRuinMinLevelPercent() {
        return ruinMinLevelPercent;
    }

    public void setRuinMinLevelPercent(float ruinMinLevelPercent) {
        this.ruinMinLevelPercent = ruinMinLevelPercent;
    }

    public float getRuinMaxLevelPercent() {
        return ruinMaxLevelPercent;
    }

    public void setRuinMaxLevelPercent(float ruinMaxLevelPercent) {
        this.ruinMaxLevelPercent = ruinMaxLevelPercent;
    }

    public int getGroundLevel() {
        return groundLevel;
    }

    public void setGroundLevel(int groundLevel) {
        this.groundLevel = groundLevel;
    }

    public int getSeaLevel() {
        return seaLevel;
    }

    public void setSeaLevel(int seaLevel) {
        this.seaLevel = seaLevel;
    }

    public boolean isHighwayRequiresTwoCities() {
        return highwayRequiresTwoCities;
    }

    public void setHighwayRequiresTwoCities(boolean highwayRequiresTwoCities) {
        this.highwayRequiresTwoCities = highwayRequiresTwoCities;
    }

    public int getHighwayLevelFromCitiesMode() {
        return highwayLevelFromCitiesMode;
    }

    public void setHighwayLevelFromCitiesMode(int highwayLevelFromCitiesMode) {
        this.highwayLevelFromCitiesMode = highwayLevelFromCitiesMode;
    }

    public float getHighwayMainPerlinScale() {
        return highwayMainPerlinScale;
    }

    public void setHighwayMainPerlinScale(float highwayMainPerlinScale) {
        this.highwayMainPerlinScale = highwayMainPerlinScale;
    }

    public float getHighwaySecondaryPerlinScale() {
        return highwaySecondaryPerlinScale;
    }

    public void setHighwaySecondaryPerlinScale(float highwaySecondaryPerlinScale) {
        this.highwaySecondaryPerlinScale = highwaySecondaryPerlinScale;
    }

    public float getHighwayPerlinFactor() {
        return highwayPerlinFactor;
    }

    public void setHighwayPerlinFactor(float highwayPerlinFactor) {
        this.highwayPerlinFactor = highwayPerlinFactor;
    }

    public int getHighwayDistanceMask() {
        return highwayDistanceMask;
    }

    public void setHighwayDistanceMask(int highwayDistanceMask) {
        this.highwayDistanceMask = highwayDistanceMask;
    }

    public boolean isHighwaySupports() {
        return highwaySupports;
    }

    public void setHighwaySupports(boolean highwaySupports) {
        this.highwaySupports = highwaySupports;
    }

    public float getRailwayDungeonChance() {
        return railwayDungeonChance;
    }

    public void setRailwayDungeonChance(float railwayDungeonChance) {
        this.railwayDungeonChance = railwayDungeonChance;
    }

    public boolean isRailwaysCanEnd() {
        return railwaysCanEnd;
    }

    public void setRailwaysCanEnd(boolean railwaysCanEnd) {
        this.railwaysCanEnd = railwaysCanEnd;
    }

    public boolean isRailwaysEnabledFlag() {
        return railwaysEnabledFlag;
    }

    public void setRailwaysEnabledFlag(boolean railwaysEnabledFlag) {
        this.railwaysEnabledFlag = railwaysEnabledFlag;
    }

    public boolean isRailwayStationsEnabled() {
        return railwayStationsEnabled;
    }

    public void setRailwayStationsEnabled(boolean railwayStationsEnabled) {
        this.railwayStationsEnabled = railwayStationsEnabled;
    }

    public boolean isRailwaySurfaceStationsEnabled() {
        return railwaySurfaceStationsEnabled;
    }

    public void setRailwaySurfaceStationsEnabled(boolean railwaySurfaceStationsEnabled) {
        this.railwaySurfaceStationsEnabled = railwaySurfaceStationsEnabled;
    }

    public boolean isExplosionsInCitiesOnly() {
        return explosionsInCitiesOnly;
    }

    public void setExplosionsInCitiesOnly(boolean explosionsInCitiesOnly) {
        this.explosionsInCitiesOnly = explosionsInCitiesOnly;
    }

    public boolean isEditMode() {
        return editMode;
    }

    public void setEditMode(boolean editMode) {
        this.editMode = editMode;
    }

    public boolean isGenerateNether() {
        return generateNether;
    }

    public void setGenerateNether(boolean generateNether) {
        this.generateNether = generateNether;
    }

    public boolean isGenerateSpawners() {
        return generateSpawners;
    }

    public void setGenerateSpawners(boolean generateSpawners) {
        this.generateSpawners = generateSpawners;
    }

    public boolean isGenerateLoot() {
        return generateLoot;
    }

    public void setGenerateLoot(boolean generateLoot) {
        this.generateLoot = generateLoot;
    }

    public boolean isGenerateLighting() {
        return generateLighting;
    }

    public void setGenerateLighting(boolean generateLighting) {
        this.generateLighting = generateLighting;
    }

    public boolean isAvoidWater() {
        return avoidWater;
    }

    public void setAvoidWater(boolean avoidWater) {
        this.avoidWater = avoidWater;
    }

    public float getExplosionChance() {
        return explosionChance;
    }

    public void setExplosionChance(float explosionChance) {
        this.explosionChance = explosionChance;
    }

    public int getExplosionMinRadius() {
        return explosionMinRadius;
    }

    public void setExplosionMinRadius(int explosionMinRadius) {
        this.explosionMinRadius = explosionMinRadius;
    }

    public int getExplosionMaxRadius() {
        return explosionMaxRadius;
    }

    public void setExplosionMaxRadius(int explosionMaxRadius) {
        this.explosionMaxRadius = explosionMaxRadius;
    }

    public int getExplosionMinHeight() {
        return explosionMinHeight;
    }

    public void setExplosionMinHeight(int explosionMinHeight) {
        this.explosionMinHeight = explosionMinHeight;
    }

    public int getExplosionMaxHeight() {
        return explosionMaxHeight;
    }

    public void setExplosionMaxHeight(int explosionMaxHeight) {
        this.explosionMaxHeight = explosionMaxHeight;
    }

    public float getMiniExplosionChance() {
        return miniExplosionChance;
    }

    public void setMiniExplosionChance(float miniExplosionChance) {
        this.miniExplosionChance = miniExplosionChance;
    }

    public int getMiniExplosionMinRadius() {
        return miniExplosionMinRadius;
    }

    public void setMiniExplosionMinRadius(int miniExplosionMinRadius) {
        this.miniExplosionMinRadius = miniExplosionMinRadius;
    }

    public int getMiniExplosionMaxRadius() {
        return miniExplosionMaxRadius;
    }

    public void setMiniExplosionMaxRadius(int miniExplosionMaxRadius) {
        this.miniExplosionMaxRadius = miniExplosionMaxRadius;
    }

    public int getMiniExplosionMinHeight() {
        return miniExplosionMinHeight;
    }

    public void setMiniExplosionMinHeight(int miniExplosionMinHeight) {
        this.miniExplosionMinHeight = miniExplosionMinHeight;
    }

    public int getMiniExplosionMaxHeight() {
        return miniExplosionMaxHeight;
    }

    public void setMiniExplosionMaxHeight(int miniExplosionMaxHeight) {
        this.miniExplosionMaxHeight = miniExplosionMaxHeight;
    }

    public double getCityChance() {
        return cityChance;
    }

    public void setCityChance(double cityChance) {
        this.cityChance = cityChance;
    }

    public int getCityMinRadius() {
        return cityMinRadius;
    }

    public void setCityMinRadius(int cityMinRadius) {
        this.cityMinRadius = cityMinRadius;
    }

    public int getCityMaxRadius() {
        return cityMaxRadius;
    }

    public void setCityMaxRadius(int cityMaxRadius) {
        this.cityMaxRadius = cityMaxRadius;
    }

    public double getCityPerlinScale() {
        return cityPerlinScale;
    }

    public void setCityPerlinScale(double cityPerlinScale) {
        this.cityPerlinScale = cityPerlinScale;
    }

    public double getCityPerlinInnerScale() {
        return cityPerlinInnerScale;
    }

    public void setCityPerlinInnerScale(double cityPerlinInnerScale) {
        this.cityPerlinInnerScale = cityPerlinInnerScale;
    }

    public double getCityPerlinOffset() {
        return cityPerlinOffset;
    }

    public void setCityPerlinOffset(double cityPerlinOffset) {
        this.cityPerlinOffset = cityPerlinOffset;
    }

    public float getCityThreshold() {
        return cityThreshold;
    }

    public void setCityThreshold(float cityThreshold) {
        this.cityThreshold = cityThreshold;
    }

    public int getCitySpawnDistance1() {
        return citySpawnDistance1;
    }

    public void setCitySpawnDistance1(int citySpawnDistance1) {
        this.citySpawnDistance1 = citySpawnDistance1;
    }

    public int getCitySpawnDistance2() {
        return citySpawnDistance2;
    }

    public void setCitySpawnDistance2(int citySpawnDistance2) {
        this.citySpawnDistance2 = citySpawnDistance2;
    }

    public double getCitySpawnMultiplier1() {
        return citySpawnMultiplier1;
    }

    public void setCitySpawnMultiplier1(double citySpawnMultiplier1) {
        this.citySpawnMultiplier1 = citySpawnMultiplier1;
    }

    public double getCitySpawnMultiplier2() {
        return citySpawnMultiplier2;
    }

    public void setCitySpawnMultiplier2(double citySpawnMultiplier2) {
        this.citySpawnMultiplier2 = citySpawnMultiplier2;
    }

    public float getCityStyleThreshold() {
        return cityStyleThreshold;
    }

    public void setCityStyleThreshold(float cityStyleThreshold) {
        this.cityStyleThreshold = cityStyleThreshold;
    }

    public String getCityStyleAlternative() {
        return cityStyleAlternative;
    }

    public void setCityStyleAlternative(String cityStyleAlternative) {
        this.cityStyleAlternative = cityStyleAlternative;
    }

    public boolean isCityAvoidVoid() {
        return cityAvoidVoid;
    }

    public void setCityAvoidVoid(boolean cityAvoidVoid) {
        this.cityAvoidVoid = cityAvoidVoid;
    }

    public boolean isCitySphere32Grid() {
        return citySphere32Grid;
    }

    public boolean isCitySphereEnabled() {
        return citySphereChance > 0;
    }

    public void setCitySphere32Grid(boolean citySphere32Grid) {
        this.citySphere32Grid = citySphere32Grid;
    }

    public float getCitySphereFactor() {
        return citySphereFactor;
    }

    public void setCitySphereFactor(float citySphereFactor) {
        this.citySphereFactor = citySphereFactor;
    }

    public float getCitySphereChance() {
        return citySphereChance;
    }

    public void setCitySphereChance(float citySphereChance) {
        this.citySphereChance = citySphereChance;
    }

    public float getCitySphereSurfaceVariation() {
        return citySphereSurfaceVariation;
    }

    public void setCitySphereSurfaceVariation(float citySphereSurfaceVariation) {
        this.citySphereSurfaceVariation = citySphereSurfaceVariation;
    }

    public float getCitySphereOutsideSurfaceVariation() {
        return citySphereOutsideSurfaceVariation;
    }

    public void setCitySphereOutsideSurfaceVariation(float citySphereOutsideSurfaceVariation) {
        this.citySphereOutsideSurfaceVariation = citySphereOutsideSurfaceVariation;
    }

    public float getCitySphereMonorailChance() {
        return citySphereMonorailChance;
    }

    public void setCitySphereMonorailChance(float citySphereMonorailChance) {
        this.citySphereMonorailChance = citySphereMonorailChance;
    }

    public int getCitySphereClearAbove() {
        return citySphereClearAbove;
    }

    public void setCitySphereClearAbove(int citySphereClearAbove) {
        this.citySphereClearAbove = citySphereClearAbove;
    }

    public int getCitySphereClearBelow() {
        return citySphereClearBelow;
    }

    public void setCitySphereClearBelow(int citySphereClearBelow) {
        this.citySphereClearBelow = citySphereClearBelow;
    }

    public boolean isCitySphereClearAboveUntilAir() {
        return citySphereClearAboveUntilAir;
    }

    public void setCitySphereClearAboveUntilAir(boolean citySphereClearAboveUntilAir) {
        this.citySphereClearAboveUntilAir = citySphereClearAboveUntilAir;
    }

    public boolean isCitySphereClearBelowUntilAir() {
        return citySphereClearBelowUntilAir;
    }

    public void setCitySphereClearBelowUntilAir(boolean citySphereClearBelowUntilAir) {
        this.citySphereClearBelowUntilAir = citySphereClearBelowUntilAir;
    }

    public int getCitySphereOutsideGroundLevel() {
        return citySphereOutsideGroundLevel;
    }

    public void setCitySphereOutsideGroundLevel(int citySphereOutsideGroundLevel) {
        this.citySphereOutsideGroundLevel = citySphereOutsideGroundLevel;
    }

    public String getCitySphereOutsideProfile() {
        return citySphereOutsideProfile;
    }

    public void setCitySphereOutsideProfile(String citySphereOutsideProfile) {
        this.citySphereOutsideProfile = citySphereOutsideProfile;
    }

    public boolean isCitySphereOnlyPredefined() {
        return citySphereOnlyPredefined;
    }

    public void setCitySphereOnlyPredefined(boolean citySphereOnlyPredefined) {
        this.citySphereOnlyPredefined = citySphereOnlyPredefined;
    }

    public int getCitySphereMonorailHeightOffset() {
        return citySphereMonorailHeightOffset;
    }

    public void setCitySphereMonorailHeightOffset(int citySphereMonorailHeightOffset) {
        this.citySphereMonorailHeightOffset = citySphereMonorailHeightOffset;
    }

    public int getCityLevel0Height() {
        return cityLevel0Height;
    }

    public void setCityLevel0Height(int cityLevel0Height) {
        this.cityLevel0Height = cityLevel0Height;
    }

    public int getCityLevel1Height() {
        return cityLevel1Height;
    }

    public void setCityLevel1Height(int cityLevel1Height) {
        this.cityLevel1Height = cityLevel1Height;
    }

    public int getCityLevel2Height() {
        return cityLevel2Height;
    }

    public void setCityLevel2Height(int cityLevel2Height) {
        this.cityLevel2Height = cityLevel2Height;
    }

    public int getCityLevel3Height() {
        return cityLevel3Height;
    }

    public void setCityLevel3Height(int cityLevel3Height) {
        this.cityLevel3Height = cityLevel3Height;
    }

    public int getCityLevel4Height() {
        return cityLevel4Height;
    }

    public void setCityLevel4Height(int cityLevel4Height) {
        this.cityLevel4Height = cityLevel4Height;
    }

    public int getCityLevel5Height() {
        return cityLevel5Height;
    }

    public void setCityLevel5Height(int cityLevel5Height) {
        this.cityLevel5Height = cityLevel5Height;
    }

    public int getCityLevel6Height() {
        return cityLevel6Height;
    }

    public void setCityLevel6Height(int cityLevel6Height) {
        this.cityLevel6Height = cityLevel6Height;
    }

    public int getCityLevel7Height() {
        return cityLevel7Height;
    }

    public void setCityLevel7Height(int cityLevel7Height) {
        this.cityLevel7Height = cityLevel7Height;
    }

    public int getCityLevelHeight(int level) {
        return switch (level) {
            case 0 -> cityLevel0Height;
            case 1 -> cityLevel1Height;
            case 2 -> cityLevel2Height;
            case 3 -> cityLevel3Height;
            case 4 -> cityLevel4Height;
            case 5 -> cityLevel5Height;
            case 6 -> cityLevel6Height;
            case 7 -> cityLevel7Height;
            default -> throw new IllegalArgumentException("城市层级必须在 0-7 之间: " + level);
        };
    }

    public int getCityMinHeight() {
        return cityMinHeight;
    }

    public void setCityMinHeight(int cityMinHeight) {
        this.cityMinHeight = cityMinHeight;
    }

    public int getCityMaxHeight() {
        return cityMaxHeight;
    }

    public void setCityMaxHeight(int cityMaxHeight) {
        this.cityMaxHeight = cityMaxHeight;
    }

    public int getOceanCorrectionBorder() {
        return oceanCorrectionBorder;
    }

    public void setOceanCorrectionBorder(int oceanCorrectionBorder) {
        this.oceanCorrectionBorder = oceanCorrectionBorder;
    }

    public int getTerrainFixLowerMinOffset() {
        return terrainFixLowerMinOffset;
    }

    public void setTerrainFixLowerMinOffset(int terrainFixLowerMinOffset) {
        this.terrainFixLowerMinOffset = terrainFixLowerMinOffset;
    }

    public int getTerrainFixLowerMaxOffset() {
        return terrainFixLowerMaxOffset;
    }

    public void setTerrainFixLowerMaxOffset(int terrainFixLowerMaxOffset) {
        this.terrainFixLowerMaxOffset = terrainFixLowerMaxOffset;
    }

    public int getTerrainFixUpperMinOffset() {
        return terrainFixUpperMinOffset;
    }

    public void setTerrainFixUpperMinOffset(int terrainFixUpperMinOffset) {
        this.terrainFixUpperMinOffset = terrainFixUpperMinOffset;
    }

    public int getTerrainFixUpperMaxOffset() {
        return terrainFixUpperMaxOffset;
    }

    public void setTerrainFixUpperMaxOffset(int terrainFixUpperMaxOffset) {
        this.terrainFixUpperMaxOffset = terrainFixUpperMaxOffset;
    }

    public float getChestWithoutLootChance() {
        return chestWithoutLootChance;
    }

    public void setChestWithoutLootChance(float chestWithoutLootChance) {
        this.chestWithoutLootChance = chestWithoutLootChance;
    }

    public float getBuildingWithoutLootChance() {
        return buildingWithoutLootChance;
    }

    public void setBuildingWithoutLootChance(float buildingWithoutLootChance) {
        this.buildingWithoutLootChance = buildingWithoutLootChance;
    }

    public float getBuildingChance() {
        return buildingChance;
    }

    public void setBuildingChance(float buildingChance) {
        this.buildingChance = buildingChance;
    }

    public int getBuildingMinFloors() {
        return buildingMinFloors;
    }

    public void setBuildingMinFloors(int buildingMinFloors) {
        this.buildingMinFloors = buildingMinFloors;
    }

    public int getBuildingMaxFloors() {
        return buildingMaxFloors;
    }

    public void setBuildingMaxFloors(int buildingMaxFloors) {
        this.buildingMaxFloors = buildingMaxFloors;
    }

    public int getBuildingMinFloorsChance() {
        return buildingMinFloorsChance;
    }

    public void setBuildingMinFloorsChance(int buildingMinFloorsChance) {
        this.buildingMinFloorsChance = buildingMinFloorsChance;
    }

    public int getBuildingMaxFloorsChance() {
        return buildingMaxFloorsChance;
    }

    public void setBuildingMaxFloorsChance(int buildingMaxFloorsChance) {
        this.buildingMaxFloorsChance = buildingMaxFloorsChance;
    }

    public int getBuildingMinCellars() {
        return buildingMinCellars;
    }

    public void setBuildingMinCellars(int buildingMinCellars) {
        this.buildingMinCellars = buildingMinCellars;
    }

    public int getBuildingMaxCellars() {
        return buildingMaxCellars;
    }

    public void setBuildingMaxCellars(int buildingMaxCellars) {
        this.buildingMaxCellars = buildingMaxCellars;
    }

    public float getBuildingDoorwayChance() {
        return buildingDoorwayChance;
    }

    public void setBuildingDoorwayChance(float buildingDoorwayChance) {
        this.buildingDoorwayChance = buildingDoorwayChance;
    }

    public float getBuildingFrontChance() {
        return buildingFrontChance;
    }

    public void setBuildingFrontChance(float buildingFrontChance) {
        this.buildingFrontChance = buildingFrontChance;
    }

    public float getParkChance() {
        return parkChance;
    }

    public void setParkChance(float parkChance) {
        this.parkChance = parkChance;
    }

    public float getCorridorChance() {
        return corridorChance;
    }

    public void setCorridorChance(float corridorChance) {
        this.corridorChance = corridorChance;
    }

    public float getBridgeChance() {
        return bridgeChance;
    }

    public void setBridgeChance(float bridgeChance) {
        this.bridgeChance = bridgeChance;
    }

    public float getFountainChance() {
        return fountainChance;
    }

    public void setFountainChance(float fountainChance) {
        this.fountainChance = fountainChance;
    }

    public boolean isBridgeSupports() {
        return bridgeSupports;
    }

    public void setBridgeSupports(boolean bridgeSupports) {
        this.bridgeSupports = bridgeSupports;
    }

    public boolean isParkElevation() {
        return parkElevation;
    }

    public void setParkElevation(boolean parkElevation) {
        this.parkElevation = parkElevation;
    }

    public boolean isParkBorder() {
        return parkBorder;
    }

    public void setParkBorder(boolean parkBorder) {
        this.parkBorder = parkBorder;
    }

    public int getParkStreetThreshold() {
        return parkStreetThreshold;
    }

    public void setParkStreetThreshold(int parkStreetThreshold) {
        this.parkStreetThreshold = parkStreetThreshold;
    }

    public boolean isMultiUseCorner() {
        return multiUseCorner;
    }

    public void setMultiUseCorner(boolean multiUseCorner) {
        this.multiUseCorner = multiUseCorner;
    }

    public boolean isUseAvgHeightmap() {
        return useAvgHeightmap;
    }

    public void setUseAvgHeightmap(boolean useAvgHeightmap) {
        this.useAvgHeightmap = useAvgHeightmap;
    }

    public int getBedrockLayer() {
        return bedrockLayer;
    }

    public void setBedrockLayer(int bedrockLayer) {
        this.bedrockLayer = bedrockLayer;
    }

    public float getHorizon() {
        return horizon;
    }

    public void setHorizon(float horizon) {
        this.horizon = horizon;
    }

    public float getFogRed() {
        return fogRed;
    }

    public void setFogRed(float fogRed) {
        this.fogRed = fogRed;
    }

    public float getFogGreen() {
        return fogGreen;
    }

    public void setFogGreen(float fogGreen) {
        this.fogGreen = fogGreen;
    }

    public float getFogBlue() {
        return fogBlue;
    }

    public void setFogBlue(float fogBlue) {
        this.fogBlue = fogBlue;
    }

    public float getFogDensity() {
        return fogDensity;
    }

    public void setFogDensity(float fogDensity) {
        this.fogDensity = fogDensity;
    }

    public String getSpawnBiome() {
        return spawnBiome;
    }

    public void setSpawnBiome(String spawnBiome) {
        this.spawnBiome = spawnBiome;
    }

    public String getSpawnCity() {
        return spawnCity;
    }

    public void setSpawnCity(String spawnCity) {
        this.spawnCity = spawnCity;
    }

    public String getSpawnSphere() {
        return spawnSphere;
    }

    public void setSpawnSphere(String spawnSphere) {
        this.spawnSphere = spawnSphere;
    }

    public boolean isSpawnNotInBuilding() {
        return spawnNotInBuilding;
    }

    public void setSpawnNotInBuilding(boolean spawnNotInBuilding) {
        this.spawnNotInBuilding = spawnNotInBuilding;
    }

    public boolean isForceSpawnInBuilding() {
        return forceSpawnInBuilding;
    }

    public void setForceSpawnInBuilding(boolean forceSpawnInBuilding) {
        this.forceSpawnInBuilding = forceSpawnInBuilding;
    }

    public List<String> getForceSpawnBuildings() {
        return new ArrayList<>(forceSpawnBuildings);
    }

    public void setForceSpawnBuildings(List<String> forceSpawnBuildings) {
        this.forceSpawnBuildings = forceSpawnBuildings == null ? new ArrayList<>() : new ArrayList<>(forceSpawnBuildings);
    }

    public List<String> getForceSpawnParts() {
        return new ArrayList<>(forceSpawnParts);
    }

    public void setForceSpawnParts(List<String> forceSpawnParts) {
        this.forceSpawnParts = forceSpawnParts == null ? new ArrayList<>() : new ArrayList<>(forceSpawnParts);
    }

    public int getSpawnCheckRadius() {
        return spawnCheckRadius;
    }

    public void setSpawnCheckRadius(int spawnCheckRadius) {
        this.spawnCheckRadius = spawnCheckRadius;
    }

    public int getSpawnRadiusIncrease() {
        return spawnRadiusIncrease;
    }

    public void setSpawnRadiusIncrease(int spawnRadiusIncrease) {
        this.spawnRadiusIncrease = spawnRadiusIncrease;
    }

    public int getSpawnCheckAttempts() {
        return spawnCheckAttempts;
    }

    public void setSpawnCheckAttempts(int spawnCheckAttempts) {
        this.spawnCheckAttempts = spawnCheckAttempts;
    }

    public String getLandscapeType() {
        return landscapeType;
    }

    public void setLandscapeType(String landscapeType) {
        this.landscapeType = landscapeType;
    }
}
