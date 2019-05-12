package com.theoryinpractise.halbuilder.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.theoryinpractise.halbuilder.DefaultRepresentationFactory;
import com.theoryinpractise.halbuilder.api.ContentRepresentation;
import com.theoryinpractise.halbuilder.api.RepresentationWriter;
import com.theoryinpractise.halbuilder.impl.ContentType;
import java.io.Reader;
import javax.annotation.Nullable;

/** Simple representation factory configured for JSON usage. */
public class JsonRepresentationFactory extends DefaultRepresentationFactory {

  @Nullable private ObjectMapper mapper;

  public JsonRepresentationFactory() {
    this(null);
  }

  public JsonRepresentationFactory(@Nullable ObjectMapper mapper) {
    withRenderer(HAL_JSON, JsonRepresentationWriter.class);
    withReader(HAL_JSON, JsonRepresentationReader.class);
    this.mapper = mapper;
  }

  @Override
  public ContentRepresentation readRepresentation(String contentType, Reader reader) {
    if (mapper != null && new ContentType(contentType).matches(HAL_JSON)) {
      return new JsonRepresentationReader(this, mapper).read(reader);
    }
    return super.readRepresentation(contentType, reader);
  }

  @Override
  public RepresentationWriter<String> lookupRenderer(String contentType) {
    if (mapper != null && new ContentType(contentType).matches(HAL_JSON)) {
      return new JsonRepresentationWriter(mapper);
    }
    return super.lookupRenderer(contentType);
  }
}
