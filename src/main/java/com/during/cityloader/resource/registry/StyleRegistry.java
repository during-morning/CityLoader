package com.during.cityloader.resource.registry;

import com.during.cityloader.resource.style.Style;
import java.util.logging.Logger;

/**
 * Registry for managing Style definitions.
 */
public class StyleRegistry extends NamespacedRegistry<Style> {

    private final Logger logger;

    public StyleRegistry(Logger logger) {
        super(false); // Not thread-safe
        this.logger = logger;
    }

    @Override
    protected String getRegistryName() {
        return "StyleRegistry";
    }
}
