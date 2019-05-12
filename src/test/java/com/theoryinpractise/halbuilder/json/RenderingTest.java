package com.theoryinpractise.halbuilder.json;

import com.damnhandy.uri.template.MalformedUriTemplateException;
import com.damnhandy.uri.template.UriTemplate;
import com.damnhandy.uri.template.VariableExpansionException;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.io.Resources;
import com.theoryinpractise.halbuilder.api.ReadableRepresentation;
import com.theoryinpractise.halbuilder.api.Representation;
import com.theoryinpractise.halbuilder.api.RepresentationException;
import com.theoryinpractise.halbuilder.api.RepresentationFactory;
import com.theoryinpractise.halbuilder.impl.representations.MutableRepresentation;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static org.fest.assertions.api.Assertions.assertThat;

public class RenderingTest {
  private static final String ROOT_URL = "https://example.com";
  private static final String BASE_URL = ROOT_URL + "/api/";

  private RepresentationFactory representationFactory =
      new JsonRepresentationFactory()
          .withNamespace("ns", ROOT_URL + "/apidocs/ns/{rel}")
          .withNamespace("role", ROOT_URL + "/apidocs/role/{rel}")
          .withFlag(RepresentationFactory.PRETTY_PRINT);

  private String exampleJson;
  private String exampleSingleElemArrayLinksJson;
  private String exampleJsonWithoutHref;
  private String exampleWithSubresourceJson;
  private String exampleWithSubresourceLinkingToItselfJson;
  private String exampleWithMultipleSubresourcesJson;
  private String exampleWithNullPropertyJson;
  private String exampleWithLiteralNullPropertyJson;
  private String exampleWithMultipleNestedSubresourcesJson;
  private String exampleWithTemplateJson;
  private String exampleWithArray;
  private String exampleWithSingleElemArray;

  @BeforeMethod
  public void setup() throws IOException {
    exampleJsonWithoutHref =
        Resources.toString(
                RenderingTest.class.getResource("/exampleWithoutHref.json"), Charsets.UTF_8)
            .trim();
    exampleJson =
        Resources.toString(RenderingTest.class.getResource("/example.json"), Charsets.UTF_8).trim();
    exampleSingleElemArrayLinksJson =
        Resources.toString(
                RenderingTest.class.getResource("/exampleSingleElemArrayLinks.json"),
                Charsets.UTF_8)
            .trim();
    exampleWithSubresourceJson =
        Resources.toString(
                RenderingTest.class.getResource("/exampleWithSubresource.json"), Charsets.UTF_8)
            .trim();
    exampleWithSubresourceLinkingToItselfJson =
        Resources.toString(
                RenderingTest.class.getResource("/exampleWithSubresourceLinkingToItself.json"),
                Charsets.UTF_8)
            .trim();
    exampleWithMultipleSubresourcesJson =
        Resources.toString(
                RenderingTest.class.getResource("/exampleWithMultipleSubresources.json"),
                Charsets.UTF_8)
            .trim();
    exampleWithNullPropertyJson =
        Resources.toString(
                RenderingTest.class.getResource("/exampleWithNullProperty.json"), Charsets.UTF_8)
            .trim();
    exampleWithLiteralNullPropertyJson =
        Resources.toString(
                RenderingTest.class.getResource("/exampleWithLiteralNullProperty.json"),
                Charsets.UTF_8)
            .trim();
    exampleWithMultipleNestedSubresourcesJson =
        Resources.toString(
                RenderingTest.class.getResource("/exampleWithMultipleNestedSubresources.json"),
                Charsets.UTF_8)
            .trim();
    exampleWithTemplateJson =
        Resources.toString(
                RenderingTest.class.getResource("/exampleWithTemplate.json"), Charsets.UTF_8)
            .trim();
    exampleWithArray =
        Resources.toString(
                RenderingTest.class.getResource("/exampleWithArray.json"), Charsets.UTF_8)
            .trim();
    exampleWithSingleElemArray =
        Resources.toString(
                RenderingTest.class.getResource("/exampleWithSingleElemArray.json"), Charsets.UTF_8)
            .trim();
  }

  private Representation newBaseResource(final Representation resource) {
    return resource.withLink(
        "ns:parent", BASE_URL + "customer/1234", "bob", "The Parent", "en", "");
  }

  private Representation newBaseResource(final URI uri) {
    return newBaseResource(representationFactory.newRepresentation(uri));
  }

  private Representation newBaseResource(final String href) {
    // https://example.com/api
    return newBaseResource(representationFactory.newRepresentation(BASE_URL + href));
  }

  @Test
  public void testFactoryWithLinks() {

    RepresentationFactory representationFactory =
        new JsonRepresentationFactory().withLink("home", "https://example.com/home");

    Representation resource = representationFactory.newRepresentation("/");

    assertThat(resource.getCanonicalLinks()).hasSize(2);
    assertThat(resource.getLinksByRel("home")).hasSize(1);
    assertThat(resource.getLinksByRel("home").iterator().next().toString())
        .isEqualTo("<link rel=\"home\" href=\"https://example.com/home\"/>");
  }

  @Test(expectedExceptions = RepresentationException.class)
  public void testFactoryWithDuplicateNamespaces() {
    new JsonRepresentationFactory()
        .withNamespace("home", "https://example.com/api/")
        .withNamespace("home", "https://example.com/api/");
  }

  @Test
  public void testResourcesWithoutHref() {

    ReadableRepresentation party =
        new JsonRepresentationFactory()
            .newRepresentation()
            .withProperty("name", "Example Resource");

    assertThat(party.getResourceLink()).isNull();
    assertThat(
            party.toString(
                RepresentationFactory.HAL_JSON,
                ImmutableSet.of(RepresentationFactory.PRETTY_PRINT)))
        .isEqualTo(exampleJsonWithoutHref);
  }

  @Test
  public void testCustomerHal() {

    String href = "customer/123456";
    ReadableRepresentation party =
        newBaseResource(href)
            .withLink("ns:users", BASE_URL + href + "?users")
            .withProperty("id", 123456)
            .withProperty("age", 33)
            .withProperty("name", "Example Resource")
            .withProperty("optional", Boolean.TRUE)
            .withProperty("expired", Boolean.FALSE);

    assertThat(party.getResourceLink().getHref())
        .isEqualTo("https://example.com/api/customer/123456");
    assertThat(
            party.toString(RepresentationFactory.HAL_JSON, RepresentationFactory.COALESCE_ARRAYS))
        .isEqualTo(exampleJson);
  }

  @Test
  public void testWithRepresentable() {

    String href = "customer/123456";
    ReadableRepresentation party =
        newBaseResource(href)
            .withLink("ns:users", BASE_URL + href + "?users")
            .withRepresentable(
                resource ->
                    resource
                        .withProperty("id", 123456)
                        .withProperty("age", 33)
                        .withProperty("name", "Example Resource")
                        .withProperty("optional", Boolean.TRUE)
                        .withProperty("expired", Boolean.FALSE));

    assertThat(
            party.toString(RepresentationFactory.HAL_JSON, RepresentationFactory.COALESCE_ARRAYS))
        .isEqualTo(exampleJson);
  }

  @Test
  public void testHalWithBean() {

    String href = "customer/123456";
    ReadableRepresentation party =
        newBaseResource(href)
            .withLink("ns:users", BASE_URL + href + "?users")
            .withBean(new Customer(123456, "Example Resource", 33));

    assertThat(
            party.toString(RepresentationFactory.HAL_JSON, RepresentationFactory.COALESCE_ARRAYS))
        .isEqualTo(exampleJson);
  }

  @Test
  public void testHalWithFields() {

    String href = "customer/123456";
    ReadableRepresentation party =
        newBaseResource(href)
            .withLink("ns:users", BASE_URL + href + "?users")
            .withFields(new OtherCustomer(123456, "Example Resource", 33));

    assertThat(
            party.toString(RepresentationFactory.HAL_JSON, RepresentationFactory.COALESCE_ARRAYS))
        .isEqualTo(exampleJson);
  }

  @Test
  public void testHalWithSubResources() {

    String href = "customer/123456";
    ReadableRepresentation party =
        newBaseResource(href)
            .withLink("ns:users", BASE_URL + href + "?users")
            .withRepresentation(
                "ns:user",
                representationFactory
                    .newRepresentation(ROOT_URL + "/user/11")
                    .withProperty("id", 11)
                    .withProperty("name", "Example User")
                    .withProperty("expired", false)
                    .withProperty("age", 32)
                    .withProperty("optional", true));

    assertThat(
            party.toString(RepresentationFactory.HAL_JSON, RepresentationFactory.COALESCE_ARRAYS))
        .isEqualTo(exampleWithSubresourceJson);
  }

  @Test
  public void testHalWithSubResourceLinkingToItself() {

    String href = "customer/123456";
    ReadableRepresentation party =
        newBaseResource(href)
            .withLink("ns:users", BASE_URL + href + "?users")
            .withRepresentation(
                "ns:user",
                representationFactory
                    .newRepresentation(ROOT_URL + "/user/11")
                    .withLink("role:admin", ROOT_URL + "/user/11")
                    .withProperty("id", 11)
                    .withProperty("name", "Example User")
                    .withProperty("expired", false)
                    .withProperty("age", 32)
                    .withProperty("optional", true));

    assertThat(
            party.toString(RepresentationFactory.HAL_JSON, RepresentationFactory.COALESCE_ARRAYS))
        .isEqualTo(exampleWithSubresourceLinkingToItselfJson);
  }

  @Test
  public void testHalWithBeanSubResource() {

    String href = "customer/123456";
    ReadableRepresentation party =
        newBaseResource(href)
            .withLink("ns:users", BASE_URL + href + "?users")
            .withBeanBasedRepresentation(
                "ns:user", ROOT_URL + "/user/11", new Customer(11, "Example User", 32));

    assertThat(
            party.toString(
                RepresentationFactory.HAL_JSON,
                ImmutableSet.of(RepresentationFactory.COALESCE_ARRAYS)))
        .isEqualTo(exampleWithSubresourceJson);
  }

  @Test
  public void testHalWithBeanMultipleSubResources() {

    String href = "customer/123456";
    ReadableRepresentation party =
        newBaseResource(href)
            .withLink("ns:users", BASE_URL + href + "?users")
            .withBeanBasedRepresentation(
                "ns:user", ROOT_URL + "/user/11", new Customer(11, "Example User", 32))
            .withBeanBasedRepresentation(
                "ns:user", ROOT_URL + "/user/12", new Customer(12, "Example User", 32));

    assertThat(
            party.toString(RepresentationFactory.HAL_JSON, RepresentationFactory.COALESCE_ARRAYS))
        .isEqualTo(exampleWithMultipleSubresourcesJson);
  }

  @Test
  public void testLinkWithDamnHandyUriTemplate()
      throws MalformedUriTemplateException, VariableExpansionException {

    Phone phone = new Phone(1234, "phone-123");

    String uri =
        UriTemplate.fromTemplate(ROOT_URL + "/customer/phone{?id,number}")
            .set("id", phone.getId())
            .set("number", phone.getNumber())
            .expand();

    ReadableRepresentation representation = newBaseResource("/test").withLink("phone", uri);

    assertThat(representation.getLinkByRel("phone").getHref())
        .isEqualTo("https://example.com/customer/phone?id=1234&number=phone-123");
  }

  @Test
  public void testNullPropertyHal()
      throws URISyntaxException, MalformedUriTemplateException, VariableExpansionException {

    String path =
        UriTemplate.fromTemplate(BASE_URL + "customer/{id}")
            .expand(ImmutableMap.<String, Object>of("id", "123456"));

    ReadableRepresentation party =
        newBaseResource(new URI(path))
            .withLink("ns:users", path + "?users")
            .withProperty("id", 123456)
            .withProperty("age", 33)
            .withProperty("name", "Example Resource")
            .withProperty("optional", Boolean.TRUE)
            .withProperty("expired", Boolean.FALSE)
            .withProperty("nullprop", null);

    assertThat(party.getResourceLink().getHref())
        .isEqualTo("https://example.com/api/customer/123456");
    assertThat(
            party.toString(RepresentationFactory.HAL_JSON, RepresentationFactory.COALESCE_ARRAYS))
        .isEqualTo(exampleWithNullPropertyJson);
  }

  @Test
  public void testLiteralNullPropertyHal()
      throws URISyntaxException, MalformedUriTemplateException, VariableExpansionException {
    String path =
        UriTemplate.fromTemplate(BASE_URL + "customer/{id}")
            .expand(ImmutableMap.<String, Object>of("id", "123456"));

    ReadableRepresentation party =
        newBaseResource(new URI(path))
            .withLink("ns:users", path + "?users")
            .withProperty("id", 123456)
            .withProperty("age", 33)
            .withProperty("name", "Example Resource")
            .withProperty("optional", Boolean.TRUE)
            .withProperty("expired", Boolean.FALSE)
            .withProperty("nullval", "null");

    assertThat(party.getResourceLink().getHref())
        .isEqualTo("https://example.com/api/customer/123456");
    assertThat(
            party.toString(RepresentationFactory.HAL_JSON, RepresentationFactory.COALESCE_ARRAYS))
        .isEqualTo(exampleWithLiteralNullPropertyJson);
  }

  @Test
  public void testHalWithUriTemplate() {
    ReadableRepresentation party =
        newBaseResource("customer")
            .withLink("ns:query", ROOT_URL + "/api/customer/search{?queryParam}");

    assertThat(
            party.toString(RepresentationFactory.HAL_JSON, RepresentationFactory.COALESCE_ARRAYS))
        .isEqualTo(exampleWithTemplateJson);
  }

  @Test
  public void testHalWithBeanMultipleNestedSubResources() {

    String href = "customer/123456";
    ReadableRepresentation party =
        newBaseResource(href)
            .withNamespace("phone", "https://example.com/apidocs/phone/{rel}")
            .withLink("ns:users", BASE_URL + href + "?users")
            .withBeanBasedRepresentation(
                "ns:user", ROOT_URL + "/user/11", new Customer(11, "Example User", 32))
            .withBeanBasedRepresentation(
                "ns:user", ROOT_URL + "/user/12", new Customer(12, "Example User", 32));

    MutableRepresentation mutableRepresentation =
        (MutableRepresentation) Iterables.getFirst(party.getResources(), null).getValue();
    mutableRepresentation.withBeanBasedRepresentation(
        "phone:cell", ROOT_URL + "/phone/1", new Phone(1, "555-666-7890"));

    assertThat(
            party.toString(RepresentationFactory.HAL_JSON, RepresentationFactory.COALESCE_ARRAYS))
        .isEqualTo(exampleWithMultipleNestedSubresourcesJson);
  }

  @Test
  public void testHalWithArray() {

    String representation =
        new JsonRepresentationFactory()
            .withFlag(RepresentationFactory.PRETTY_PRINT)
            .newRepresentation()
            .withProperty("name", "Example Resource")
            .withProperty("array", ImmutableList.of("one", "two", "three"))
            .toString(RepresentationFactory.HAL_JSON);

    assertThat(representation).isEqualTo(exampleWithArray);
  }

  @Test
  public void testHalWithSingleElemArrayLinks() {
    RepresentationFactory rf =
        new JsonRepresentationFactory()
            .withNamespace("ns", ROOT_URL + "/apidocs/ns/{rel}")
            .withFlag(RepresentationFactory.PRETTY_PRINT);

    String href = "customer/123456";
    ReadableRepresentation party =
        newBaseResource(rf.newRepresentation(BASE_URL + href))
            .withLink("ns:users", BASE_URL + href + "?users")
            .withBean(new Customer(123456, "Example Resource", 33));

    assertThat(party.toString(RepresentationFactory.HAL_JSON))
        .isEqualTo(exampleSingleElemArrayLinksJson);
  }

  @Test
  public void testHalWithSingleElemArray() {

    String representation =
        new JsonRepresentationFactory()
            .withFlag(RepresentationFactory.PRETTY_PRINT)
            .newRepresentation()
            .withProperty("name", "Example Resource")
            .withProperty("array", ImmutableList.of("one"))
            .toString(RepresentationFactory.HAL_JSON);

    assertThat(representation).isEqualTo(exampleWithSingleElemArray);
  }

  public static class Phone {
    private final Integer id;

    private final String number;

    public Phone(Integer id, String number) {
      this.id = id;
      this.number = number;
    }

    public Integer getId() {
      return id;
    }

    public String getNumber() {
      return number;
    }
  }

  public static class OtherCustomer {
    public final Integer id;
    public final String name;
    public final Integer age;
    public final Boolean expired = false;
    public final Boolean optional = true;

    public OtherCustomer(Integer id, String name, Integer age) {
      this.id = id;
      this.name = name;
      this.age = age;
    }
  }

  public static class Customer {
    private Integer id;
    private String name;
    private Integer age;
    private Boolean expired = false;
    private Boolean optional = true;

    public Customer(Integer id, String name, Integer age) {
      this.id = id;
      this.name = name;
      this.age = age;
    }

    public Integer getId() {
      return id;
    }

    public void setId(Integer id) {
      this.id = id;
    }

    public Boolean getExpired() {
      return expired;
    }

    public void setExpired(Boolean expired) {
      this.expired = expired;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public Integer getAge() {
      return age;
    }

    public void setAge(Integer age) {
      this.age = age;
    }

    public Boolean getOptional() {
      return optional;
    }

    public void setOptional(Boolean optional) {
      this.optional = optional;
    }
  }
}
