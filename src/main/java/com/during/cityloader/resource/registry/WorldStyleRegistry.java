package com.during.cityloader.resource.registry;

import com.during.cityloader.resource.style.WorldStyle;
import java.util.logging.Logger;

/**
 * Registry for managing WorldStyle definitions.
 */
public class WorldStyleRegistry extends NamespacedRegistry<WorldStyle> {

    private final Logger logger;

    public WorldStyleRegistry(Logger logger) {
        super(false); // Not thread-safe
        this.logger = logger;
    }

    @Override
    protected String getRegistryName() {
        return "WorldStyleRegistry";
    }
}
