package com.during.cityloader.worldgen.lost.regassets.data;

/**
 * 预定义建筑记录
 * 移植自 LostCities，表示预定义城市中指定位置的建筑
 *
 * @param building      建筑名称（引用 AssetRegistries 中的建筑资源）
 * @param relChunkX     相对于城市中心的区块X偏移
 * @param relChunkZ     相对于城市中心的区块Z偏移
 * @param multi         是否为多区块建筑（2x2）
 * @param preventRuins  是否阻止废墟化
 */
public record PredefinedBuilding(String building, int relChunkX, int relChunkZ,
                                 boolean multi, boolean preventRuins) {

    /**
     * 简化构造：不阻止废墟化、非多区块
     */
    public PredefinedBuilding(String building, int relChunkX, int relChunkZ) {
        this(building, relChunkX, relChunkZ, false, false);
    }
}
