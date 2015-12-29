package com.theoryinpractise.halbuilder.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.base.Strings;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.theoryinpractise.halbuilder.api.Link;
import com.theoryinpractise.halbuilder.api.ReadableRepresentation;
import com.theoryinpractise.halbuilder.api.Rel;
import com.theoryinpractise.halbuilder.api.Rels;
import com.theoryinpractise.halbuilder.api.RepresentationException;
import com.theoryinpractise.halbuilder.api.RepresentationFactory;
import com.theoryinpractise.halbuilder.api.RepresentationWriter;
import javaslang.Tuple2;
import javaslang.collection.List;
import javaslang.collection.Set;
import javaslang.control.Option;

import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;

import static com.theoryinpractise.halbuilder.impl.api.Support.CURIES;
import static com.theoryinpractise.halbuilder.impl.api.Support.EMBEDDED;
import static com.theoryinpractise.halbuilder.impl.api.Support.HREF;
import static com.theoryinpractise.halbuilder.impl.api.Support.HREFLANG;
import static com.theoryinpractise.halbuilder.impl.api.Support.LINKS;
import static com.theoryinpractise.halbuilder.impl.api.Support.NAME;
import static com.theoryinpractise.halbuilder.impl.api.Support.PROFILE;
import static com.theoryinpractise.halbuilder.impl.api.Support.TEMPLATED;
import static com.theoryinpractise.halbuilder.impl.api.Support.TITLE;

public class JsonRepresentationWriter
    implements RepresentationWriter<String> {

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

  protected JsonGenerator getJsonGenerator(Set<URI> flags, Writer writer)
      throws IOException {
    JsonGenerator g = getJsonFactory(flags).createJsonGenerator(writer);
    if (flags.contains(RepresentationFactory.PRETTY_PRINT)) {
      g.setPrettyPrinter(new DefaultPrettyPrinter());
    }
    return g;
  }

  protected JsonFactory getJsonFactory(Set<URI> flags) {
    JsonFactory f = new JsonFactory();
    ObjectMapper codec = new ObjectMapper();
    if (flags.contains(RepresentationFactory.STRIP_NULLS)) {
      codec.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }
    codec.configure(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED, false);
    f.setCodec(codec);
    f.enable(JsonGenerator.Feature.QUOTE_FIELD_NAMES);
    return f;
  }

  private boolean isSingleton(Rel matcher) {
    return matcher.match(Rels.cases(
        (rel) -> true,
        (rel) -> false,
        (rel) -> false,
        (rel, key, comparator) -> false));
  }

  private boolean isCollection(Rel matcher) {
    return matcher.match(Rels.cases(
        (rel) -> false,
        (rel) -> false,
        (rel) -> true,
        (rel, key, comparator) -> false));
  }

  private void renderJson(Set<URI> flags, JsonGenerator g, ReadableRepresentation representation, boolean embedded)
      throws IOException {

    if (!representation.getCanonicalLinks().isEmpty() || (!embedded && !representation.getNamespaces().isEmpty())) {
      g.writeObjectFieldStart(LINKS);

      List<Link> links = List.empty();

      // Include namespaces as links when not embedded
      if (!embedded) {
        links = links.appendAll(representation.getNamespaces()
                                              .map(ns -> new Link(CURIES, ns._2, ns._1, null, null, null)));
      }

      // Add representation links
      links = links.appendAll(representation.getLinks());

      // Partition representation links by rel
      Multimap<String, Link> linkMap = Multimaps.index(links, Link::getRel);

      for (Map.Entry<String, Collection<Link>> linkEntry : linkMap.asMap().entrySet()) {

        Rel rel = representation.getRels().get(linkEntry.getKey()).get();
        boolean coalesce = !isCollection(rel) && (isSingleton(rel) || linkEntry.getValue().size() == 1);

        if (coalesce) {
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

    for (Tuple2<String, Option<Object>> entry : representation.getProperties()) {
      if (entry._2.isDefined()) {
        g.writeObjectField(entry._1, entry._2.get());
      } else {
        if (!flags.contains(RepresentationFactory.STRIP_NULLS)) {
          g.writeNullField(entry._1);
        }
      }
    }

    if (!representation.getResources().isEmpty()) {
      g.writeObjectFieldStart(EMBEDDED);

      javaslang.collection.Map<String, List<? extends ReadableRepresentation>> resourceMap = representation.getResourceMap();

      for (Tuple2<String, List<? extends ReadableRepresentation>> resourceEntry : resourceMap) {

        Rel rel = representation.getRels().get(resourceEntry._1).get();

        boolean coalesce = !isCollection(rel) && (isSingleton(rel) || resourceEntry._2().length() == 1);

        if (coalesce) {
          g.writeObjectFieldStart(resourceEntry._1());
          ReadableRepresentation subRepresentation = resourceEntry._2().iterator().next();
          renderJson(flags, g, subRepresentation, true);
          g.writeEndObject();
        } else {

          final Comparator<ReadableRepresentation> repComparator = Rels.getComparator(rel)
                                                                       .orElse(Rel.naturalComparator);

          final List<? extends ReadableRepresentation> values = isSingleton(rel)
                                                                ? resourceEntry._2()
                                                                : resourceEntry._2().sort(repComparator);

          final String collectionRel = isSingleton(rel) || flags.contains(RepresentationFactory.SILENT_SORTING)
                                       ? rel.rel()
                                       : rel.fullRel();

          g.writeArrayFieldStart(collectionRel);

          for (ReadableRepresentation subRepresentation : values) {
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

  private void writeJsonLinkContent(JsonGenerator g, Link link)
      throws IOException {
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
