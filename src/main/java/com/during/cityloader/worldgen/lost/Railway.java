package com.during.cityloader.worldgen.lost;

import com.during.cityloader.util.ChunkCoord;
import com.during.cityloader.worldgen.IDimensionInfo;
import com.during.cityloader.worldgen.LostCityProfile;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.during.cityloader.worldgen.lost.Railway.RailChunkType.DOUBLE_BEND;
import static com.during.cityloader.worldgen.lost.Railway.RailChunkType.GOING_DOWN_FURTHER;
import static com.during.cityloader.worldgen.lost.Railway.RailChunkType.GOING_DOWN_ONE_FROM_SURFACE;
import static com.during.cityloader.worldgen.lost.Railway.RailChunkType.GOING_DOWN_TWO_FROM_SURFACE;
import static com.during.cityloader.worldgen.lost.Railway.RailChunkType.HORIZONTAL;
import static com.during.cityloader.worldgen.lost.Railway.RailChunkType.NONE;
import static com.during.cityloader.worldgen.lost.Railway.RailChunkType.RAILS_END_HERE;
import static com.during.cityloader.worldgen.lost.Railway.RailChunkType.STATION_EXTENSION_SURFACE;
import static com.during.cityloader.worldgen.lost.Railway.RailChunkType.STATION_EXTENSION_UNDERGROUND;
import static com.during.cityloader.worldgen.lost.Railway.RailChunkType.STATION_SURFACE;
import static com.during.cityloader.worldgen.lost.Railway.RailChunkType.STATION_UNDERGROUND;
import static com.during.cityloader.worldgen.lost.Railway.RailChunkType.THREE_SPLIT;
import static com.during.cityloader.worldgen.lost.Railway.RailChunkType.VERTICAL;
import static com.during.cityloader.worldgen.lost.Railway.RailDirection.BI;
import static com.during.cityloader.worldgen.lost.Railway.RailDirection.EAST;
import static com.during.cityloader.worldgen.lost.Railway.RailDirection.WEST;

/**
 * LostCities 风格铁路网格判定。
 */
public final class Railway {

    public static final int RAILWAY_LEVEL_OFFSET = -3;

    public enum RailChunkType {
        NONE,
        STATION_SURFACE,
        STATION_UNDERGROUND,
        STATION_EXTENSION_SURFACE,
        STATION_EXTENSION_UNDERGROUND,
        RAILS_END_HERE,
        HORIZONTAL,
        VERTICAL,
        THREE_SPLIT,
        DOUBLE_BEND,
        GOING_DOWN_ONE_FROM_SURFACE,
        GOING_DOWN_TWO_FROM_SURFACE,
        GOING_DOWN_FURTHER;

        public boolean isStation() {
            return this == STATION_SURFACE
                    || this == STATION_UNDERGROUND
                    || this == STATION_EXTENSION_SURFACE
                    || this == STATION_EXTENSION_UNDERGROUND;
        }

        public boolean isSurface() {
            return this == STATION_SURFACE || this == STATION_EXTENSION_SURFACE;
        }
    }

    public enum RailDirection {
        BI,
        WEST,
        EAST
    }

    public static final class RailChunkInfo {
        private final RailChunkType type;
        private final RailDirection direction;
        private final int level;
        private final int rails;

        public static final RailChunkInfo NOTHING = new RailChunkInfo(NONE, BI, 0, 0);

        public RailChunkInfo(RailChunkType type, RailDirection direction, int level, int rails) {
            this.type = type;
            this.direction = direction;
            this.level = level;
            this.rails = rails;
        }

        public RailChunkType getType() {
            return type;
        }

        public RailDirection getDirection() {
            return direction;
        }

        public int getLevel() {
            return level;
        }

        public int getRails() {
            return rails;
        }
    }

    private static final Map<ChunkCoord, RailChunkInfo> RAIL_INFO = Collections.synchronizedMap(new HashMap<>());

    private Railway() {
    }

    public static void cleanCache() {
        RAIL_INFO.clear();
    }

    public static RailChunkInfo getRailChunkType(ChunkCoord coord, IDimensionInfo provider, LostCityProfile profile) {
        if (coord == null || provider == null || profile == null) {
            return RailChunkInfo.NOTHING;
        }
        RailChunkInfo cached = RAIL_INFO.get(coord);
        if (cached != null) {
            return cached;
        }

        RailChunkInfo info = getRailChunkTypeInternal(coord, provider, profile);
        if (info.getType().isStation()) {
            if (!profile.isRailwayStationsEnabled()) {
                info = RailChunkInfo.NOTHING;
            }
        } else if (!profile.isRailwaysEnabled()) {
            info = RailChunkInfo.NOTHING;
        }

        RAIL_INFO.put(coord, info);
        return info;
    }

    private static RailChunkInfo getRailChunkTypeInternal(ChunkCoord key, IDimensionInfo provider, LostCityProfile profile) {
        int chunkX = key.chunkX();
        int chunkZ = key.chunkZ();
        long seed = provider.getSeed() + chunkZ * 2600003897L + chunkX * 43600002517L;
        java.util.Random random = new java.util.Random(seed);
        float r = random.nextFloat();

        int mx = Math.floorMod(chunkX + 1, 20);
        int mz = Math.floorMod(chunkZ + 1, 20);

        if (mx == 0 && mz == 10) {
            if (!BuildingInfo.isCityRaw(key, provider, profile)) {
                if (profile.isRailwaysCanEnd()) {
                    boolean cityEast = BuildingInfo.isCityRaw(key.offset(10, 0), provider, profile)
                            || BuildingInfo.isCityRaw(key.offset(10, -10), provider, profile)
                            || BuildingInfo.isCityRaw(key.offset(10, 10), provider, profile);
                    boolean cityWest = BuildingInfo.isCityRaw(key.offset(-10, 0), provider, profile)
                            || BuildingInfo.isCityRaw(key.offset(-10, -10), provider, profile)
                            || BuildingInfo.isCityRaw(key.offset(-10, 10), provider, profile);
                    if (!cityEast && !cityWest) {
                        return RailChunkInfo.NOTHING;
                    }
                    if (!cityEast) {
                        return new RailChunkInfo(RAILS_END_HERE, WEST, RAILWAY_LEVEL_OFFSET, 3);
                    }
                    if (!cityWest) {
                        return new RailChunkInfo(RAILS_END_HERE, EAST, RAILWAY_LEVEL_OFFSET, 3);
                    }
                }
                return new RailChunkInfo(HORIZONTAL, BI, RAILWAY_LEVEL_OFFSET, 3);
            }
            return getStationType(key, provider, profile, r, 3);
        }

        if (mx == 10 && mz == 0) {
            if (!BuildingInfo.isCityRaw(key, provider, profile)) {
                if (profile.isRailwaysCanEnd()) {
                    boolean cityEast = BuildingInfo.isCityRaw(key.offset(10, -10), provider, profile)
                            || BuildingInfo.isCityRaw(key.offset(10, 10), provider, profile);
                    boolean cityWest = BuildingInfo.isCityRaw(key.offset(-10, -10), provider, profile)
                            || BuildingInfo.isCityRaw(key.offset(-10, 10), provider, profile);
                    if (!cityEast && !cityWest) {
                        return RailChunkInfo.NOTHING;
                    }
                    if (!cityEast) {
                        return new RailChunkInfo(RAILS_END_HERE, WEST, RAILWAY_LEVEL_OFFSET, 2);
                    }
                    if (!cityWest) {
                        return new RailChunkInfo(RAILS_END_HERE, EAST, RAILWAY_LEVEL_OFFSET, 2);
                    }
                }
                return new RailChunkInfo(HORIZONTAL, BI, RAILWAY_LEVEL_OFFSET, 2);
            }
            return getStationType(key, provider, profile, r, 2);
        }

        if (mx == 10 && mz == 10) {
            if (!BuildingInfo.isCityRaw(key, provider, profile)) {
                if (profile.isRailwaysCanEnd()) {
                    boolean cityEast = BuildingInfo.isCityRaw(key.offset(10, 0), provider, profile);
                    boolean cityWest = BuildingInfo.isCityRaw(key.offset(-10, 0), provider, profile);
                    if (!cityEast && !cityWest) {
                        return RailChunkInfo.NOTHING;
                    }
                    if (!cityEast) {
                        return new RailChunkInfo(RAILS_END_HERE, WEST, RAILWAY_LEVEL_OFFSET, 1);
                    }
                    if (!cityWest) {
                        return new RailChunkInfo(RAILS_END_HERE, EAST, RAILWAY_LEVEL_OFFSET, 1);
                    }
                }
                return new RailChunkInfo(HORIZONTAL, BI, RAILWAY_LEVEL_OFFSET, 1);
            }
            return getStationType(key, provider, profile, r, 1);
        }

        if (mx == 0 && mz == 0) {
            return RailChunkInfo.NOTHING;
        }

        if (mz == 0 || mz == 10) {
            if ((mx >= 16 && mz != 0) || (mx >= 6 && mx <= 9)) {
                ChunkCoord east = key.east();
                RailChunkInfo adjacent = getRailChunkType(east, provider, profile);
                RailDirection direction = adjacent.getDirection();
                if (direction == BI || adjacent.getType() == RAILS_END_HERE) {
                    direction = WEST;
                }
                return testAdjacentRailChunk(r, adjacent, direction, key.west(), provider, profile);
            }

            if ((mx >= 1 && mx <= 4 && mz != 0) || (mx >= 11 && mx <= 14)) {
                ChunkCoord west = key.west();
                RailChunkInfo adjacent = getRailChunkType(west, provider, profile);
                RailDirection direction = adjacent.getDirection();
                if (direction == BI || adjacent.getType() == RAILS_END_HERE) {
                    direction = EAST;
                }
                return testAdjacentRailChunk(r, adjacent, direction, key.east(), provider, profile);
            }

            if (mz == 0 && mx == 5) {
                if (profile.isRailwaysCanEnd()) {
                    boolean cityWest = BuildingInfo.isCityRaw(key.offset(-5, -10), provider, profile)
                            || BuildingInfo.isCityRaw(key.offset(-5, 10), provider, profile);
                    boolean cityEast = BuildingInfo.isCityRaw(key.offset(5, 0), provider, profile);
                    if (!cityEast && !cityWest) {
                        return RailChunkInfo.NOTHING;
                    }
                }
                return new RailChunkInfo(DOUBLE_BEND, EAST, RAILWAY_LEVEL_OFFSET, 1);
            }

            if (mz == 0 && mx == 15) {
                if (profile.isRailwaysCanEnd()) {
                    boolean cityEast = BuildingInfo.isCityRaw(key.offset(5, -10), provider, profile)
                            || BuildingInfo.isCityRaw(key.offset(5, 10), provider, profile);
                    boolean cityWest = BuildingInfo.isCityRaw(key.offset(-5, 0), provider, profile);
                    if (!cityEast && !cityWest) {
                        return RailChunkInfo.NOTHING;
                    }
                }
                return new RailChunkInfo(DOUBLE_BEND, WEST, RAILWAY_LEVEL_OFFSET, 1);
            }

            if (mz == 10 && mx == 5) {
                return new RailChunkInfo(THREE_SPLIT, EAST, RAILWAY_LEVEL_OFFSET, 3);
            }

            if (mz == 10 && mx == 15) {
                return new RailChunkInfo(THREE_SPLIT, WEST, RAILWAY_LEVEL_OFFSET, 3);
            }

            return RailChunkInfo.NOTHING;
        }

        if (mx == 5) {
            return new RailChunkInfo(VERTICAL, EAST, RAILWAY_LEVEL_OFFSET, 1);
        }
        if (mx == 15) {
            return new RailChunkInfo(VERTICAL, WEST, RAILWAY_LEVEL_OFFSET, 1);
        }

        return RailChunkInfo.NOTHING;
    }

    private static RailChunkInfo getStationType(ChunkCoord coord,
                                                IDimensionInfo provider,
                                                LostCityProfile profile,
                                                float r,
                                                int rails) {
        int cityLevel = BuildingInfo.getCityLevel(coord, provider);
        if (cityLevel > 2 || !profile.isRailwaySurfaceStationsEnabled()) {
            return new RailChunkInfo(STATION_UNDERGROUND, BI, RAILWAY_LEVEL_OFFSET, rails);
        }
        return r < 0.5f
                ? new RailChunkInfo(STATION_SURFACE, BI, cityLevel, rails)
                : new RailChunkInfo(STATION_UNDERGROUND, BI, RAILWAY_LEVEL_OFFSET, rails);
    }

    public static void removeRailChunkType(ChunkCoord coord) {
        RAIL_INFO.put(coord, RailChunkInfo.NOTHING);
    }

    private static RailChunkInfo testAdjacentRailChunk(float r,
                                                       RailChunkInfo adjacent,
                                                       RailDirection direction,
                                                       ChunkCoord coord,
                                                       IDimensionInfo provider,
                                                       LostCityProfile profile) {
        return switch (adjacent.getType()) {
            case NONE -> RailChunkInfo.NOTHING;
            case STATION_SURFACE -> {
                if (r < 0.4f) {
                    yield new RailChunkInfo(STATION_EXTENSION_SURFACE, direction, adjacent.getLevel(), adjacent.getRails());
                }
                if ((adjacent.getLevel() & 1) == 0) {
                    yield new RailChunkInfo(GOING_DOWN_ONE_FROM_SURFACE, direction, adjacent.getLevel() - 1, adjacent.getRails());
                }
                yield new RailChunkInfo(GOING_DOWN_TWO_FROM_SURFACE, direction, adjacent.getLevel() - 2, adjacent.getRails());
            }
            case STATION_UNDERGROUND -> r < 0.4f
                    ? new RailChunkInfo(STATION_EXTENSION_UNDERGROUND, direction, adjacent.getLevel(), adjacent.getRails())
                    : new RailChunkInfo(HORIZONTAL, direction, adjacent.getLevel(), adjacent.getRails());
            case STATION_EXTENSION_SURFACE -> {
                if ((adjacent.getLevel() & 1) == 0) {
                    yield new RailChunkInfo(GOING_DOWN_ONE_FROM_SURFACE, direction, adjacent.getLevel() - 1, adjacent.getRails());
                }
                yield new RailChunkInfo(GOING_DOWN_TWO_FROM_SURFACE, direction, adjacent.getLevel() - 2, adjacent.getRails());
            }
            case STATION_EXTENSION_UNDERGROUND -> new RailChunkInfo(HORIZONTAL, direction, adjacent.getLevel(), adjacent.getRails());
            case GOING_DOWN_FURTHER, GOING_DOWN_ONE_FROM_SURFACE, GOING_DOWN_TWO_FROM_SURFACE -> {
                if (adjacent.getLevel() == RAILWAY_LEVEL_OFFSET) {
                    yield new RailChunkInfo(HORIZONTAL, direction, adjacent.getLevel(), adjacent.getRails());
                }
                yield new RailChunkInfo(GOING_DOWN_FURTHER, direction, adjacent.getLevel() - 2, adjacent.getRails());
            }
            case RAILS_END_HERE -> {
                if (direction == adjacent.getDirection()) {
                    yield new RailChunkInfo(HORIZONTAL, direction, adjacent.getLevel(), adjacent.getRails());
                }
                yield RailChunkInfo.NOTHING;
            }
            case HORIZONTAL -> adjacent;
            default -> throw new IllegalStateException("Unexpected rail type: " + adjacent.getType());
        };
    }
}
