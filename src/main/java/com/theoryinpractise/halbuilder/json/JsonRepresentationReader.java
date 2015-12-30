package com.theoryinpractise.halbuilder.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;
import com.google.common.io.CharStreams;
import com.theoryinpractise.halbuilder.AbstractRepresentationFactory;
import com.theoryinpractise.halbuilder.api.RepresentationException;
import com.theoryinpractise.halbuilder.api.RepresentationReader;
import com.theoryinpractise.halbuilder.impl.api.Support;
import com.theoryinpractise.halbuilder.impl.representations.PersistentRepresentation;
import javaslang.collection.TreeMap;
import javaslang.control.Option;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.theoryinpractise.halbuilder.impl.api.Support.CURIES;
import static com.theoryinpractise.halbuilder.impl.api.Support.EMBEDDED;
import static com.theoryinpractise.halbuilder.impl.api.Support.HREF;
import static com.theoryinpractise.halbuilder.impl.api.Support.HREFLANG;
import static com.theoryinpractise.halbuilder.impl.api.Support.LINKS;
import static com.theoryinpractise.halbuilder.impl.api.Support.NAME;
import static com.theoryinpractise.halbuilder.impl.api.Support.PROFILE;
import static com.theoryinpractise.halbuilder.impl.api.Support.TITLE;

public class JsonRepresentationReader
    implements RepresentationReader {

  private final ObjectMapper mapper;
  private final AbstractRepresentationFactory representationFactory;

  public JsonRepresentationReader(AbstractRepresentationFactory representationFactory) {
    this.representationFactory = representationFactory;
    this.mapper = new ObjectMapper();
  }

  public PersistentRepresentation read(Reader reader) {
    try {
      String source = CharStreams.toString(reader);

      JsonNode rootNode = mapper.readValue(new StringReader(source), JsonNode.class);

      return readResource(rootNode).withContent(source);

    } catch (Exception e) {
      throw new RepresentationException(e.getMessage(), e);
    }

  }

  private PersistentRepresentation readResource(JsonNode rootNode) {

    Option<PersistentRepresentation> resource = Option.of(
        new PersistentRepresentation(representationFactory, null));

    return resource.map(r -> readNamespaces(rootNode, r))
                   .map(r -> readLinks(rootNode, r))
                   .map(r -> readProperties(rootNode, r))
                   .map(r -> readResources(rootNode, r))
                   .get();
  }

  private PersistentRepresentation readNamespaces(JsonNode rootNode, PersistentRepresentation resource) {
    PersistentRepresentation newRep = resource;
    if (rootNode.has(LINKS)) {
      JsonNode linksNode = rootNode.get(LINKS);
      if (linksNode.has(CURIES)) {
        JsonNode curieNode = linksNode.get(CURIES);

        if (curieNode.isArray()) {
          Iterator<JsonNode> values = curieNode.elements();
          while (values.hasNext()) {
            JsonNode valueNode = values.next();
            newRep = newRep.withNamespace(valueNode.get(NAME).asText(), valueNode.get(HREF).asText());
          }
        } else {
          newRep = newRep.withNamespace(curieNode.get(NAME).asText(), curieNode.get(HREF).asText());
        }
      }
    }
    return newRep;
  }

  private PersistentRepresentation readLinks(JsonNode rootNode, PersistentRepresentation resource) {
    PersistentRepresentation newRep = resource;
    if (rootNode.has(LINKS)) {
      Iterator<Map.Entry<String, JsonNode>> fields = rootNode.get(LINKS).fields();
      while (fields.hasNext()) {
        Map.Entry<String, JsonNode> keyNode = fields.next();
        if (!CURIES.equals((keyNode.getKey()))) {
          if (keyNode.getValue().isArray()) {
            Iterator<JsonNode> values = keyNode.getValue().elements();
            while (values.hasNext()) {
              JsonNode valueNode = values.next();
              newRep = withJsonLink(newRep, keyNode, valueNode);
            }
          } else {
            newRep = withJsonLink(newRep, keyNode, keyNode.getValue());
          }
        }
      }
    }
    return newRep;
  }

  private PersistentRepresentation withJsonLink(PersistentRepresentation resource, Map.Entry<String, JsonNode> keyNode,
                                                JsonNode valueNode) {
    String rel = keyNode.getKey();
    String href = valueNode.get(HREF).asText();
    String name = optionalNodeValueAsText(valueNode, NAME);
    String title = optionalNodeValueAsText(valueNode, TITLE);
    String hreflang = optionalNodeValueAsText(valueNode, HREFLANG);
    String profile = optionalNodeValueAsText(valueNode, PROFILE);

    return resource.withLink(rel, href, name, title, hreflang, profile);
  }

  String optionalNodeValueAsText(JsonNode node, String key) {
    JsonNode value = node.get(key);
    return value != null ? value.asText() : null;
  }

  private PersistentRepresentation readProperties(JsonNode rootNode, PersistentRepresentation resource) {
    try {
      PersistentRepresentation newRep = resource;
      Iterator<String> fieldNames = rootNode.fieldNames();
      while (fieldNames.hasNext()) {
        String fieldName = fieldNames.next();
        if (!Support.RESERVED_JSON_PROPERTIES.contains(fieldName)) {
          JsonNode field = rootNode.get(fieldName);
          if (field.isArray()) {
            List<Object> arrayValues = new ArrayList<Object>(field.size());
            for (JsonNode arrayValue : field) {
              arrayValues.add(valueFromNode(arrayValue));
            }
            newRep = newRep.withProperty(fieldName, arrayValues);
          } else {
            newRep = newRep.withProperty(fieldName, valueFromNode(field));
          }
        }
      }
      return newRep;
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }

  }

  private Object valueFromNode(JsonNode field) throws IOException {
    if (field.isNull()) {
      return null;
    } else {
      if (field.isContainerNode()) {
        return fromJavaMap(mapper.readValue(field.toString(), Map.class));
      } else {
        if (field.isBigDecimal()) {
          return field.decimalValue();
        } else if (field.isBigInteger()) {
          return field.bigIntegerValue();
        } else if (field.isInt()) {
          return field.intValue();
        } else if (field.isBoolean()) {
          return field.booleanValue();
        } else {
          return field.asText();
        }
      }
    }
  }

  private TreeMap<String, Object> fromJavaMap(java.util.Map<String, Object> map) {
    TreeMap<String, Object> returnMap = TreeMap.empty(Comparator.naturalOrder());
    for (java.util.Map.Entry<String, Object> entry : map.entrySet()) {
      returnMap = returnMap.put(entry.getKey(), entry.getValue());
    }
    return returnMap;
  }

  private PersistentRepresentation readResources(JsonNode rootNode, PersistentRepresentation resource) {
    if (rootNode.has(EMBEDDED)) {
      PersistentRepresentation newResource = resource;
      Iterator<Map.Entry<String, JsonNode>> fields = rootNode.get(EMBEDDED).fields();
      while (fields.hasNext()) {
        Map.Entry<String, JsonNode> keyNode = fields.next();
        if (keyNode.getValue().isArray()) {
          Iterator<JsonNode> values = keyNode.getValue().elements();
          while (values.hasNext()) {
            JsonNode valueNode = values.next();
            newResource = newResource.withRepresentation(keyNode.getKey(), readResource(valueNode));
          }
        } else {
          newResource = newResource.withRepresentation(keyNode.getKey(), readResource(keyNode.getValue()));
        }
      }
      return newResource;
    } else {
      return resource;
    }
  }
}
