package com.theoryinpractise.halbuilder.json;

import static com.theoryinpractise.halbuilder.impl.api.Support.CURIES;
import static com.theoryinpractise.halbuilder.impl.api.Support.EMBEDDED;
import static com.theoryinpractise.halbuilder.impl.api.Support.HREF;
import static com.theoryinpractise.halbuilder.impl.api.Support.HREFLANG;
import static com.theoryinpractise.halbuilder.impl.api.Support.LINKS;
import static com.theoryinpractise.halbuilder.impl.api.Support.NAME;
import static com.theoryinpractise.halbuilder.impl.api.Support.PROFILE;
import static com.theoryinpractise.halbuilder.impl.api.Support.TEMPLATED;
import static com.theoryinpractise.halbuilder.impl.api.Support.TITLE;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.theoryinpractise.halbuilder.api.Link;
import com.theoryinpractise.halbuilder.api.ReadableRepresentation;
import com.theoryinpractise.halbuilder.api.RepresentationException;
import com.theoryinpractise.halbuilder.api.RepresentationFactory;
import com.theoryinpractise.halbuilder.api.RepresentationWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class JsonRepresentationWriter implements RepresentationWriter<String> {

  private final ObjectMapper mapper;

  public JsonRepresentationWriter() {
    this(new ObjectMapper());
  }

  public JsonRepresentationWriter(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public void write(ReadableRepresentation representation, Set<URI> flags, Writer writer) {

    try {
      JsonGenerator g = getJsonGenerator(flags, writer);
      g.writeStartObject();
      renderJson(flags, g, representation, false);
      g.writeEndObject();
      g.close();
    } catch (IOException e) {
      throw new RepresentationException(e);
    }
  }

  protected JsonGenerator getJsonGenerator(Set<URI> flags, Writer writer) throws IOException {
    JsonGenerator g = getJsonFactory(flags).createJsonGenerator(writer);
    if (flags.contains(RepresentationFactory.PRETTY_PRINT)) {
      g.setPrettyPrinter(new DefaultPrettyPrinter());
    }
    return g;
  }

  protected JsonFactory getJsonFactory(Set<URI> flags) {
    JsonFactory f = new JsonFactory();
    ObjectMapper codec = this.mapper.copy();
    if (flags.contains(RepresentationFactory.STRIP_NULLS)) {
      codec.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }
    codec.configure(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED, false);
    f.setCodec(codec);
    f.enable(JsonGenerator.Feature.QUOTE_FIELD_NAMES);
    return f;
  }

  private void renderJson(
      Set<URI> flags, JsonGenerator g, ReadableRepresentation representation, boolean embedded)
      throws IOException {

    if (!representation.getCanonicalLinks().isEmpty()
        || (!embedded && !representation.getNamespaces().isEmpty())) {
      g.writeObjectFieldStart(LINKS);

      List<Link> links = Lists.newArrayList();

      // Include namespaces as links when not embedded
      if (!embedded) {
        for (Map.Entry<String, String> entry : representation.getNamespaces().entrySet()) {
          links.add(new Link(CURIES, entry.getValue(), entry.getKey(), "", "", ""));
        }
      }

      // Add representation links
      links.addAll(representation.getLinks());

      // Partition representation links by rel
      Multimap<String, Link> linkMap = Multimaps.index(links, link -> link.getRel());

      for (Map.Entry<String, Collection<Link>> linkEntry : linkMap.asMap().entrySet()) {
        if (linkEntry.getValue().size() == 1
                && flags.contains(RepresentationFactory.COALESCE_ARRAYS)
            || linkEntry.getKey().equals("self")) {
          Link link = linkEntry.getValue().iterator().next();
          g.writeObjectFieldStart(linkEntry.getKey());
          writeJsonLinkContent(g, link);
          g.writeEndObject();
        } else {
          g.writeArrayFieldStart(linkEntry.getKey());
          for (Link link : linkEntry.getValue()) {
            g.writeStartObject();
            writeJsonLinkContent(g, link);
            g.writeEndObject();
          }
          g.writeEndArray();
        }
      }
      g.writeEndObject();
    }

    for (Map.Entry<String, Object> entry : representation.getProperties().entrySet()) {
      if (entry.getValue() != null) {
        g.writeObjectField(entry.getKey(), entry.getValue());
      } else {
        if (!flags.contains(RepresentationFactory.STRIP_NULLS)) {
          g.writeNullField(entry.getKey());
        }
      }
    }

    if (!representation.getResources().isEmpty()) {
      g.writeObjectFieldStart(EMBEDDED);

      Map<String, Collection<ReadableRepresentation>> resourceMap = representation.getResourceMap();

      for (Map.Entry<String, Collection<ReadableRepresentation>> resourceEntry :
          resourceMap.entrySet()) {
        if (resourceEntry.getValue().size() == 1
            && flags.contains(RepresentationFactory.COALESCE_ARRAYS)) {
          g.writeObjectFieldStart(resourceEntry.getKey());
          ReadableRepresentation subRepresentation = resourceEntry.getValue().iterator().next();
          renderJson(flags, g, subRepresentation, true);
          g.writeEndObject();
        } else {
          g.writeArrayFieldStart(resourceEntry.getKey());
          for (ReadableRepresentation subRepresentation : resourceEntry.getValue()) {
            g.writeStartObject();
            renderJson(flags, g, subRepresentation, true);
            g.writeEndObject();
          }
          g.writeEndArray();
        }
      }
      g.writeEndObject();
    }
  }

  private void writeJsonLinkContent(JsonGenerator g, Link link) throws IOException {
    g.writeStringField(HREF, link.getHref());
    if (!Strings.isNullOrEmpty(link.getName())) {
      g.writeStringField(NAME, link.getName());
    }
    if (!Strings.isNullOrEmpty(link.getTitle())) {
      g.writeStringField(TITLE, link.getTitle());
    }
    if (!Strings.isNullOrEmpty(link.getHreflang())) {
      g.writeStringField(HREFLANG, link.getHreflang());
    }
    if (!Strings.isNullOrEmpty(link.getProfile())) {
      g.writeStringField(PROFILE, link.getProfile());
    }
    if (link.hasTemplate()) {
      g.writeBooleanField(TEMPLATED, true);
    }
  }
}
