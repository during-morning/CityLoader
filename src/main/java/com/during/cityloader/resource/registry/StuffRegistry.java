package com.during.cityloader.resource.registry;

import com.during.cityloader.resource.stuff.Stuff;
import java.util.logging.Logger;

/**
 * Registry for Stuff (decorations).
 */
public class StuffRegistry extends NamespacedRegistry<Stuff> {

    private final Logger logger;

    public StuffRegistry(Logger logger) {
        super(false); // Not thread-safe by default
        this.logger = logger;
    }

    @Override
    protected String getRegistryName() {
        return "StuffRegistry";
    }
}
