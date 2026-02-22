package com.during.cityloader.resource.registry;

import com.during.cityloader.resource.scattered.Scattered;
import java.util.logging.Logger;

/**
 * Registry for Scattered structures.
 */
public class ScatteredRegistry extends NamespacedRegistry<Scattered> {

    private final Logger logger;

    public ScatteredRegistry(Logger logger) {
        super(false); // Not thread-safe by default
        this.logger = logger;
    }

    @Override
    protected String getRegistryName() {
        return "ScatteredRegistry";
    }
}
