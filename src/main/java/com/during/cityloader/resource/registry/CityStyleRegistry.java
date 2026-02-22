package com.during.cityloader.resource.registry;

import com.during.cityloader.resource.style.CityStyle;
import java.util.logging.Logger;

/**
 * Registry for managing CityStyle definitions.
 */
public class CityStyleRegistry extends NamespacedRegistry<CityStyle> {

    private final Logger logger;

    public CityStyleRegistry(Logger logger) {
        super(false); // Not thread-safe
        this.logger = logger;
    }

    @Override
    protected String getRegistryName() {
        return "CityStyleRegistry";
    }
}
