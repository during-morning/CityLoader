package com.during.cityloader.resource.registry;

import com.during.cityloader.resource.MultiBuilding;
import java.util.logging.Logger;

/**
 * Registry for managing MultiBuilding definitions.
 */
public class MultiBuildingRegistry extends NamespacedRegistry<MultiBuilding> {

    private final Logger logger;

    public MultiBuildingRegistry(Logger logger) {
        super(false); // Not thread-safe by default
        this.logger = logger;
    }

    @Override
    protected String getRegistryName() {
        return "MultiBuildingRegistry";
    }
}
