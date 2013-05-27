package com.theoryinpractise.halbuilder.json;

import com.theoryinpractise.halbuilder.DefaultRepresentationFactory;

/**
 * Simple representation factory configured for JSON usage.
 */
public class JsonRepresentationFactory extends DefaultRepresentationFactory {
    public JsonRepresentationFactory() {
        withRenderer(HAL_JSON, JsonRepresentationWriter.class);
        withReader(HAL_JSON, JsonRepresentationReader.class);
    }
}
