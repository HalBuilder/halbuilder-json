package com.theoryinpractise.halbuilder.json;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.theoryinpractise.halbuilder.api.Representation;
import com.theoryinpractise.halbuilder.api.RepresentationFactory;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.testng.Assert.assertFalse;

public class TestIssue18 {

  @Test
  public void test() throws IOException {

    RepresentationFactory representationFactory = new JsonRepresentationFactory()
        .withFlag(RepresentationFactory.COALESCE_LINKS);

    Representation embedded = representationFactory.newRepresentation("http://localhost/embedded");
    embedded.withLink("applicants", "http://localhost/applicants");
    Representation representation = representationFactory.newRepresentation("http://localhost/self");
    representation.withRepresentation("items", embedded);

    String json = representation.toString(RepresentationFactory.HAL_JSON);
    ObjectNode objectNode = (ObjectNode) new ObjectMapper().readTree(json);

    assertFalse(objectNode.get("_embedded").get("items").get("_links").get("applicants").isArray());
  }

}
