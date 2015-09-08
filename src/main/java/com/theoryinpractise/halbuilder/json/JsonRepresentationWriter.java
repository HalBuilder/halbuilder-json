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
import com.google.common.collect.Ordering;
import com.theoryinpractise.halbuilder.api.*;
import fj.P2;
import fj.data.List;
import fj.data.Option;
import fj.data.Set;
import fj.data.TreeMap;

import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.util.Collection;
import java.util.Map;

import static com.theoryinpractise.halbuilder.impl.api.Support.*;


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
    if (flags.member(RepresentationFactory.PRETTY_PRINT)) {
      g.setPrettyPrinter(new DefaultPrettyPrinter());
    }
    return g;
  }

  protected JsonFactory getJsonFactory(Set<URI> flags) {
    JsonFactory f = new JsonFactory();
    ObjectMapper codec = new ObjectMapper();
    if (flags.member(RepresentationFactory.STRIP_NULLS)) {
      codec.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }
    codec.configure(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED, false);
    f.setCodec(codec);
    f.enable(JsonGenerator.Feature.QUOTE_FIELD_NAMES);
    return f;
  }

  private void renderJson(Set<URI> flags, JsonGenerator g, ReadableRepresentation representation, boolean embedded)
      throws IOException {

    if (!representation.getCanonicalLinks().isEmpty() || (!embedded && !representation.getNamespaces().isEmpty())) {
      g.writeObjectFieldStart(LINKS);

      List<Link> links = List.nil();

      // Include namespaces as links when not embedded
      if (!embedded) {
        for (P2<String, String> entry : representation.getNamespaces().toStream()) {
          links = links.cons(new Link(null, CURIES, entry._2(), entry._1(), null, null, null));
        }
      }

      // Add representation links
      links = links.append(representation.getLinks());

      // Partition representation links by rel
      Multimap<String, Link> linkMap = Multimaps.index(links, Link::getRel);

      for (Map.Entry<String, Collection<Link>> linkEntry : linkMap.asMap().entrySet()) {

        Rel rel = representation.getRels().get(linkEntry.getKey()).some();
        boolean coalesce = (linkEntry.getValue().size() == 1 && flags.member(RepresentationFactory.COALESCE_ARRAYS))
                           || rel.isSingleton();

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

    for (P2<String, Option<Object>> entry : representation.getProperties().toStream()) {
      if (entry._2().isSome()) {
        g.writeObjectField(entry._1(), entry._2().some());
      } else {
        if (!flags.member(RepresentationFactory.STRIP_NULLS)) {
          g.writeNullField(entry._1());
        }
      }
    }

    if (!representation.getResources().isEmpty()) {
      g.writeObjectFieldStart(EMBEDDED);

      TreeMap<String, Collection<? extends ReadableRepresentation>> resourceMap = representation.getResourceMap();

      for (P2<String, Collection<? extends ReadableRepresentation>> resourceEntry : resourceMap.toStream()) {

        Rel rel = representation.getRels().get(resourceEntry._1()).some();

        boolean coalesce = (resourceEntry._2().size() == 1 && flags.member(RepresentationFactory.COALESCE_ARRAYS))
                           || rel.isSingleton();


        if (coalesce) {
          g.writeObjectFieldStart(resourceEntry._1());
          ReadableRepresentation subRepresentation = resourceEntry._2().iterator().next();
          renderJson(flags, g, subRepresentation, true);
          g.writeEndObject();
        } else {

          final Collection<? extends ReadableRepresentation> values =
              rel.isSingleton()
              ? resourceEntry._2()
              : Ordering.from(rel.comparator()).sortedCopy(resourceEntry._2());

          final String collectionRel = rel.isSingleton() || flags.member(RepresentationFactory.SILENT_SORTING)
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
