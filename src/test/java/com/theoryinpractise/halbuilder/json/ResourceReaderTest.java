package com.theoryinpractise.halbuilder.json;

import com.theoryinpractise.halbuilder.api.Link;
import com.theoryinpractise.halbuilder.api.ReadableRepresentation;
import com.theoryinpractise.halbuilder.api.RepresentationException;
import com.theoryinpractise.halbuilder.api.RepresentationFactory;
import javaslang.control.Option;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.google.common.truth.Truth.assertThat;
import static org.apache.commons.jxpath.JXPathContext.newContext;
import static org.boon.Lists.idx;
import static org.boon.Maps.idxStr;
import static org.boon.json.JsonFactory.fromJson;

public class ResourceReaderTest implements ResourceReader {

  @Override
  public RepresentationFactory representationFactory() {
    return new JsonRepresentationFactory();
  }

  @DataProvider
  public Object[][] provideResources() {
    return new Object[][]{
        {readJson("/example.json")},
    };
  }

  @DataProvider
  public Object[][] provideResourcesWithNulls() {
    return new Object[][]{
        {readJson("/exampleWithNullProperty.json")},
    };
  }

  @DataProvider
  public Object[][] provideSubResources() {
    return new Object[][]{
        {readJson("/exampleWithSubresource.json")},
    };
  }

  @DataProvider
  public Object[][] provideResourcesWithouHref() {
    return new Object[][]{
        {readJson("/exampleWithoutHref.json")},
    };
  }

  @DataProvider
  public Object[][] provideResourceWithUnderscoredProperty() {
    return new Object[][]{
        {readJson("/exampleWithUnderscoredProperty.json")},
    };
  }

  @DataProvider
  public Object[][] provideResourceWithSimpleArrays() {
    return new Object[][]{
        {readJson("/exampleWithArray.json")},
    };
  }

  @Test(dataProvider = "provideResources")
  public void testReader(ReadableRepresentation representation) {
    assertThat(representation.getResourceLink().get().getHref()).isEqualTo("https://example.com/api/customer/123456");
    assertThat(representation.getNamespaces()).hasSize(2);
    assertThat(representation.getProperties().get("name")).isEqualTo(Option.of(Option.of("Example Resource")));
    assertThat(representation.getValue("name")).isEqualTo(Option.of("Example Resource"));
    assertThat(representation.getValue("name")).isEqualTo(Option.of("Example Resource"));
    assertThat(representation.getCanonicalLinks()).hasSize(3);
    assertThat(representation.getResources()).hasSize(0);
    assertThat(representation.getResourcesByRel("role:admin")).hasSize(0);
    assertThat(representation.getLinksByRel("ns:users")).hasSize(1);
    assertThat(representation.getLinkByRel("ns:users")).isNotNull();
  }

  @Test(dataProvider = "provideResourcesWithNulls")
  public void testReaderWithNulls(ReadableRepresentation representation) {
    assertThat(representation.getValue("nullprop").isEmpty()).isTrue();
    assertThat(representation.getProperties().get("nullprop").get().isEmpty()).isTrue();
  }

  @Test(dataProvider = "provideResourceWithSimpleArrays")
  public void testReaderWithArray(ReadableRepresentation representation) {
    final List array = (List) representation.getValue("array")
                                            .orElse(new ArrayList());
    assertThat(array).hasSize(3);
  }

  @Test(dataProvider = "provideResources")
  public void testLinkAttributes(ReadableRepresentation representation) {
    Link parent = representation.getLinkByRel("ns:parent").get();
    assertThat(parent.getHref()).isEqualTo("https://example.com/api/customer/1234");
    assertThat(parent.getRel()).isEqualTo("ns:parent");
    assertThat(parent.getName()).isEqualTo("bob");
    assertThat(parent.getTitle()).isEqualTo("The Parent");
    assertThat(parent.getHreflang()).isEqualTo("en");
  }

  @Test(dataProvider = "provideSubResources")
  public void testSubReader(ReadableRepresentation representation) {
    assertThat(representation.getResourceLink().get().getHref()).isEqualTo("https://example.com/api/customer/123456");
    assertThat(representation.getNamespaces()).hasSize(2);
    assertThat(representation.getCanonicalLinks()).hasSize(3);
    assertThat(representation.getResources()).hasSize(1);
    assertThat(representation.getResources().head()._2.getValue("name")).isEqualTo(Option.of(("Example User")));
    assertThat(representation.getResourcesByRel("ns:user")).hasSize(1);
  }

  @Test(dataProvider = "provideResourcesWithouHref")
  public void testResourcesWithoutHref(ReadableRepresentation representation) {
    assertThat(representation.getResourceLink().isDefined()).isFalse();
    assertThat(representation.getNamespaces()).hasSize(0);
    assertThat(representation.getCanonicalLinks()).hasSize(0);
    assertThat(representation.getValue("name")).isEqualTo(Option.of("Example Resource"));
  }

  @Test(dataProvider = "provideResourceWithUnderscoredProperty")
  public void testResourceWithUnderscoredProperty(ReadableRepresentation representation) {
    assertThat(representation.getValue("_name")).isEqualTo(Option.of("Example Resource"));
  }

  @Test(expectedExceptions = RepresentationException.class)
  public void testUnknownFormat() {
    readJson(new StringReader("!!!"));
  }

  @Test(expectedExceptions = RepresentationException.class)
  public void testNullReader() {
    readJson((Reader) null);
  }

  @Test
  public void testContentExtraction() {

    ReadableRepresentation rep = readJson("/example.json");
    assertThat(rep.getContent()).isNotEmpty();

    final String content = rep.getContent().get();

    assertThat(idxStr(fromJson(content, Map.class), "name")).isEqualTo("Example Resource");
    assertThat(newContext(fromJson(content)).getValue("name")).isEqualTo("Example Resource");
    assertThat(newContext(fromJson(content)).getValue("_links/curies/name")).isEqualTo("ns");
    assertThat(newContext(fromJson(content)).getValue("_links/curies/href"))
        .isEqualTo("https://example.com/apidocs/ns/{rel}");

    assertThat(rep.getResourceLink().get().getHref()).isEqualTo("https://example.com/api/customer/123456");
  }

  @Test
  public void testNestedObject() {
    ReadableRepresentation rep = readJson("/exampleWithNestedObjects.json");

    Map map = (Map) rep.getValue("child").get();
    assertThat(map).isNotNull();
    assertThat(map.get("age")).isEqualTo(12);

    List<Map> list = (List) rep.getValue("children").get();
    assertThat(list).hasSize(2);
    assertThat(idx(list, 0).get("age")).isEqualTo(12);

    assertThat(rep.getContent()).isNotEmpty();

    // These tests should actually be in the core
    Map childMap = rep.toClass(Family.class).child();
    assertThat(childMap.get("age")).isEqualTo(12);

    List<Map> childList = rep.toClass(Family.class).children();
    assertThat(idx(childList, 0).get("age")).isEqualTo(12);

    Child child = rep.toClass(Family2.class).child();
    assertThat(child.age()).isEqualTo(12);

    List<Child> childList2 = rep.toClass(Family2.class).children();
    assertThat(idx(childList2, 0).age()).isEqualTo(12);

  }

  public interface Family {
    Map child();

    List<Map> children();
  }

  public interface Family2 {
    Child child();

    List<Child> children();
  }

  public interface Child {
    Integer age();

    Integer name();
  }

}
