package com.during.cityloader.worldgen.lost.cityassets;

import com.during.cityloader.util.ResourceLocation;
import com.during.cityloader.worldgen.lost.regassets.BuildingPartRE;
import com.during.cityloader.worldgen.lost.regassets.PaletteRE;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 建筑部件类
 */
public class BuildingPart implements ILostCityAsset {

    private final ResourceLocation name;
    private final String[] slices;
    private final List<List<String>> sliceLayers;
    private final int width;
    private final int height;
    private final int depth;
    private final String palette;
    private final Palette localPalette;
    private final Map<String, Object> metadata;

    public BuildingPart(BuildingPartRE object) {
        this.name = object.getRegistryName();
        this.width = object.getWidth();
        this.height = object.getHeight();
        this.depth = object.getDepth();
        this.slices = object.getSlices().toArray(new String[0]);
        this.sliceLayers = object.getSliceLayers();
        this.palette = object.getPalette();
        this.metadata = object.getMetadata();

        PaletteRE inline = object.getLocalPalette();
        if (inline != null) {
            ResourceLocation inlineId = new ResourceLocation(
                    name.getNamespace(),
                    name.getPath() + "$inline");
            inline.setRegistryName(inlineId);
            this.localPalette = new Palette(inline);
        } else {
            this.localPalette = null;
        }
    }

    @Override
    public String getName() {
        return name.toString();
    }

    @Override
    public ResourceLocation getId() {
        return name;
    }

    public String[] getSlices() {
        return slices;
    }

    public List<List<String>> getSliceLayers() {
        return sliceLayers;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getDepth() {
        return depth;
    }

    public String getPalette() {
        return palette;
    }

    public Palette getLocalPalette() {
        return localPalette;
    }

    public Map<String, Object> getMetadata() {
        return metadata == null ? Map.of() : Collections.unmodifiableMap(metadata);
    }
}
