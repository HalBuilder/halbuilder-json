package com.theoryinpractise.halbuilder.json;

import com.theoryinpractise.halbuilder.api.ReadableRepresentation;
import com.theoryinpractise.halbuilder.api.RepresentationFactory;

import java.io.InputStreamReader;
import java.io.Reader;

import static com.theoryinpractise.halbuilder.api.RepresentationFactory.HAL_JSON;

public interface ResourceReader {

  RepresentationFactory representationFactory();

  default ReadableRepresentation readJson(String path) {
    return readJson(new InputStreamReader(ResourceReader.class.getResourceAsStream(path)));
  }

  default ReadableRepresentation readJson(Reader reader) {
    return representationFactory().readRepresentation(HAL_JSON, reader);
  }

}
