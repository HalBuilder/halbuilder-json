package com.theoryinpractise.halbuilder.json;

import com.damnhandy.uri.template.MalformedUriTemplateException;
import com.damnhandy.uri.template.UriTemplate;
import com.damnhandy.uri.template.VariableExpansionException;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import com.theoryinpractise.halbuilder.api.ReadableRepresentation;
import com.theoryinpractise.halbuilder.api.Rels;
import com.theoryinpractise.halbuilder.api.Representation;
import com.theoryinpractise.halbuilder.api.RepresentationException;
import com.theoryinpractise.halbuilder.api.RepresentationFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static com.google.common.truth.Truth.assertThat;

public class RenderingTest {
  private static final String ROOT_URL = "https://example.com";
  private static final String BASE_URL = ROOT_URL + "/api/";

  private RepresentationFactory representationFactory = new JsonRepresentationFactory()
                                                            .withNamespace("ns", ROOT_URL + "/apidocs/ns/{rel}")
                                                            .withNamespace("role", ROOT_URL + "/apidocs/role/{rel}")
                                                            .withFlag(RepresentationFactory.PRETTY_PRINT);

  private String exampleJson;
  private String exampleSingleElemArrayLinksJson;
  private String exampleJsonWithoutHref;
  private String exampleWithSubresourceJson;
  private String exampleWithSubresourceLinkingToItselfJson;
  private String exampleWithMultipleSubresourcesJson;
  private String exampleWithSortedSubresourcesJson;
  private String exampleWithNullPropertyJson;
  private String exampleWithLiteralNullPropertyJson;
  private String exampleWithMultipleNestedSubresourcesJson;
  private String exampleWithTemplateJson;
  private String exampleWithArray;
  private String exampleWithSingleElemArray;
  private String exampleWithEmptySubRepresentation;

  private static String trimmedResource(String path) throws IOException {
    return Resources.toString(RenderingTest.class.getResource(path), Charsets.UTF_8).trim();
  }

  @BeforeMethod
  public void setup()
      throws IOException {

    exampleJsonWithoutHref = trimmedResource("/exampleWithoutHref.json");
    exampleJson = trimmedResource("/example.json");
    exampleSingleElemArrayLinksJson = trimmedResource("/exampleSingleElemArrayLinks.json");
    exampleWithSubresourceJson = trimmedResource("/exampleWithSubresource.json");
    exampleWithSubresourceLinkingToItselfJson = trimmedResource("/exampleWithSubresourceLinkingToItself.json");
    exampleWithMultipleSubresourcesJson = trimmedResource("/exampleWithMultipleSubresources.json");
    exampleWithSortedSubresourcesJson = trimmedResource("/exampleWithSortedSubresources.json");
    exampleWithNullPropertyJson = trimmedResource("/exampleWithNullProperty.json");
    exampleWithLiteralNullPropertyJson = trimmedResource("/exampleWithLiteralNullProperty.json");
    exampleWithMultipleNestedSubresourcesJson = trimmedResource("/exampleWithMultipleNestedSubresources.json");
    exampleWithTemplateJson = trimmedResource("/exampleWithTemplate.json");
    exampleWithArray = trimmedResource("/exampleWithArray.json");
    exampleWithSingleElemArray = trimmedResource("/exampleWithSingleElemArray.json");
    exampleWithEmptySubRepresentation = trimmedResource("/exampleWithEmptySubresource.json");
  }

  @Test
  public void testFactoryWithLinks() {

    RepresentationFactory representationFactory = new JsonRepresentationFactory()
                                                      .withLink("home", "https://example.com/home");

    Representation resource = representationFactory.newRepresentation("/");

    assertThat(resource.getCanonicalLinks()).hasSize(2);
    assertThat(resource.getLinksByRel("home")).hasSize(1);
    assertThat(resource.getLinksByRel("home").iterator().next().toString()).isEqualTo("<link rel=\"home\" href=\"https://example"
                                                                                      + ".com/home\"/>");

  }

  @Test(expectedExceptions = RepresentationException.class)
  public void testFactoryWithDuplicateNamespaces() {
    new JsonRepresentationFactory()
        .withNamespace("home", "https://example.com/api/")
        .withNamespace("home", "https://example.com/api/");
  }

  @Test
  public void testResourcesWithoutHref() {

    ReadableRepresentation party = new JsonRepresentationFactory().newRepresentation()
                                                                  .withProperty("name", "Example Resource");

    assertThat(party.getResourceLink().isDefined()).isFalse();
    assertThat(party.toString(RepresentationFactory.HAL_JSON, RepresentationFactory.PRETTY_PRINT))
        .isEqualTo(exampleJsonWithoutHref);

  }

  @Test
  public void testCustomerHal() {

    String href = "customer/123456";
    ReadableRepresentation party = newBaseResource(href)
                                       .withLink("ns:users", BASE_URL + href + "?users")
                                       .withProperty("id", 123456)
                                       .withProperty("age", 33)
                                       .withProperty("name", "Example Resource")
                                       .withProperty("optional", Boolean.TRUE)
                                       .withProperty("expired", Boolean.FALSE);

    assertThat(party.getResourceLink().get().getHref()).isEqualTo("https://example.com/api/customer/123456");
    assertThat(party.toString(RepresentationFactory.HAL_JSON))
        .isEqualTo(exampleJson);

  }

  private Representation newBaseResource(final String href) {
    // https://example.com/api
    return newBaseResource(representationFactory.newRepresentation(BASE_URL + href));
  }

  private Representation newBaseResource(final Representation resource) {
    return resource.withRel(Rels.singleton("ns:parent"))
                   .withLink("ns:parent", BASE_URL + "customer/1234", "bob", "The Parent", "en", "");
  }

  @Test
  public void testWithRepresentable() {

    String href = "customer/123456";
    ReadableRepresentation party = newBaseResource(href)
                                       .withLink("ns:users", BASE_URL + href + "?users")
                                       .withRepresentable(resource -> resource.withProperty("id", 123456)
                                                                              .withProperty("age", 33)
                                                                              .withProperty("name", "Example Resource")
                                                                              .withProperty("optional", Boolean.TRUE)
                                                                              .withProperty("expired", Boolean.FALSE));

    assertThat(party.toString(RepresentationFactory.HAL_JSON))
        .isEqualTo(exampleJson);

  }

  @Test
  public void testHalWithBean() {

    String href = "customer/123456";
    ReadableRepresentation party = newBaseResource(href)
                                       .withLink("ns:users", BASE_URL + href + "?users")
                                       .withBean(new Customer(123456, "Example Resource", 33));

    assertThat(party.toString(RepresentationFactory.HAL_JSON))
        .isEqualTo(exampleJson);

  }

  @Test
  public void testHalWithSingletonRel() {

    String href = "customer/123456";
    ReadableRepresentation party = newBaseResource(href)
                                       .withRel(Rels.singleton("ns:users"))
                                       .withLink("ns:users", BASE_URL + href + "?users")
                                       .withBean(new Customer(123456, "Example Resource", 33));

    assertThat(party.toString(RepresentationFactory.HAL_JSON))
        .isEqualTo(exampleJson);

  }

  @Test
  public void testHalWithFields() {

    String href = "customer/123456";
    ReadableRepresentation party = newBaseResource(href)
                                       .withLink("ns:users", BASE_URL + href + "?users")
                                       .withFields(new OtherCustomer(123456, "Example Resource", 33));

    assertThat(party.toString(RepresentationFactory.HAL_JSON))
        .isEqualTo(exampleJson);

  }

  @Test
  public void testHalWithSubResources() {

    String href = "customer/123456";
    ReadableRepresentation party = newBaseResource(href)
                                       .withLink("ns:users", BASE_URL + href + "?users")
                                       .withRepresentation("ns:user", representationFactory
                                                                          .newRepresentation(ROOT_URL + "/user/11")
                                                                          .withProperty("id", 11)
                                                                          .withProperty("name", "Example User")
                                                                          .withProperty("expired", false)
                                                                          .withProperty("age", 32)
                                                                          .withProperty("optional", true));

    assertThat(party.toString(RepresentationFactory.HAL_JSON))
        .isEqualTo(exampleWithSubresourceJson);

  }

  @Test
  public void testHalWithEmptySubResource() {
    String href = "customer/123456";
    ReadableRepresentation party = newBaseResource(href)
                                       .withLink("ns:users", BASE_URL + href + "?users")
                                       .withRepresentation("ns:users",  representationFactory
                                           .newRepresentation("1")
                                           .setEmptySubRepresentation(true));

    assertThat(party.toString(RepresentationFactory.HAL_JSON))
        .isEqualTo(exampleWithEmptySubRepresentation);
  }

  @Test
  public void testHalWithSubResourceLinkingToItself() {

    String href = "customer/123456";
    ReadableRepresentation party = newBaseResource(href)
                                       .withLink("ns:users", BASE_URL + href + "?users")
                                       .withRepresentation("ns:user", representationFactory
                                                                          .newRepresentation(ROOT_URL + "/user/11")
                                                                          .withLink("role:admin", ROOT_URL + "/user/11")
                                                                          .withProperty("id", 11)
                                                                          .withProperty("name", "Example User")
                                                                          .withProperty("expired", false)
                                                                          .withProperty("age", 32)
                                                                          .withProperty("optional", true));

    assertThat(party.toString(RepresentationFactory.HAL_JSON))
        .isEqualTo(exampleWithSubresourceLinkingToItselfJson);

  }

  @Test
  public void testHalWithBeanSubResource() {

    String href = "customer/123456";
    ReadableRepresentation party = newBaseResource(href)
                                       .withLink("ns:users", BASE_URL + href + "?users")
                                       .withBeanBasedRepresentation("ns:user", ROOT_URL + "/user/11", new Customer(11, "Example "
                                                                                                                       + "User",
                                                                                                                      32));

    assertThat(party.toString(RepresentationFactory.HAL_JSON))
        .isEqualTo(exampleWithSubresourceJson);

  }

  @Test
  public void testHalWithBeanMultipleSubResources() {

    String href = "customer/123456";
    ReadableRepresentation party = newBaseResource(href)
                                       .withLink("ns:users", BASE_URL + href + "?users")
                                       .withBeanBasedRepresentation("ns:user", ROOT_URL + "/user/11", new Customer(11, "Example "
                                                                                                                       + "User",
                                                                                                                      32))
                                       .withBeanBasedRepresentation("ns:user", ROOT_URL + "/user/12", new Customer(12, "Example "
                                                                                                                       + "User",
                                                                                                                      32));

    assertThat(party.toString(RepresentationFactory.HAL_JSON))
        .isEqualTo(exampleWithMultipleSubresourcesJson);

  }

  @Test
  public void testHalWithBeanMultipleOrderedSubResources() {

    String href = "customer/123456";
    ReadableRepresentation party = newBaseResource(href)
                                       .withRel(Rels.sorted("ns:user", "id",
                                           (r1, r2) -> r2.getValue("id", 0).compareTo(r1.getValue("id", 0))))
                                       .withLink("ns:users", BASE_URL + href + "?users")
                                       .withBeanBasedRepresentation("ns:user", ROOT_URL + "/user/11",
                                           new Customer(11, "Example User", 32))
                                       .withBeanBasedRepresentation("ns:user", ROOT_URL + "/user/12",
                                           new Customer(12, "Example User", 32));

    assertThat(party.toString(RepresentationFactory.HAL_JSON))
        .isEqualTo(exampleWithSortedSubresourcesJson);

  }

  @Test
  public void testLinkWithDamnHandyUriTemplate()
      throws MalformedUriTemplateException, VariableExpansionException {

    Phone phone = new Phone(1234, "phone-123");

    String uri = UriTemplate.fromTemplate(ROOT_URL + "/customer/phone{?id,number}")
                            .set("id", phone.getId())
                            .set("number", phone.getNumber())
                            .expand();

    ReadableRepresentation representation = newBaseResource("/test").withLink("phone", uri);

    assertThat(representation.getLinkByRel("phone").get().getHref())
        .isEqualTo("https://example.com/customer/phone?id=1234&number=phone-123");

  }

  @Test
  public void testNullPropertyHal()
      throws URISyntaxException, MalformedUriTemplateException, VariableExpansionException {

    String path = UriTemplate.fromTemplate(BASE_URL + "customer/{id}").expand(ImmutableMap.<String, Object>of("id", "123456"));

    ReadableRepresentation party = newBaseResource(new URI(path))
                                       .withLink("ns:users", path + "?users")
                                       .withProperty("id", 123456)
                                       .withProperty("age", 33)
                                       .withProperty("name", "Example Resource")
                                       .withProperty("optional", Boolean.TRUE)
                                       .withProperty("expired", Boolean.FALSE)
                                       .withProperty("nullprop", null);

    assertThat(party.getResourceLink().get().getHref()).isEqualTo("https://example.com/api/customer/123456");
    assertThat(party.toString(RepresentationFactory.HAL_JSON))
        .isEqualTo(exampleWithNullPropertyJson);
  }

  private Representation newBaseResource(final URI uri) {
    return newBaseResource(representationFactory.newRepresentation(uri));

  }

  @Test
  public void testLiteralNullPropertyHal()
      throws URISyntaxException, MalformedUriTemplateException, VariableExpansionException {
    String path = UriTemplate.fromTemplate(BASE_URL + "customer/{id}").expand(ImmutableMap.<String, Object>of("id", "123456"));

    ReadableRepresentation party = newBaseResource(new URI(path))
                                       .withLink("ns:users", path + "?users")
                                       .withProperty("id", 123456)
                                       .withProperty("age", 33)
                                       .withProperty("name", "Example Resource")
                                       .withProperty("optional", Boolean.TRUE)
                                       .withProperty("expired", Boolean.FALSE)
                                       .withProperty("nullval", "null");

    assertThat(party.getResourceLink().get().getHref()).isEqualTo("https://example.com/api/customer/123456");
    assertThat(party.toString(RepresentationFactory.HAL_JSON))
        .isEqualTo(exampleWithLiteralNullPropertyJson);
  }

  @Test
  public void testHalWithUriTemplate() {
    ReadableRepresentation party = newBaseResource("customer")
                                       .withLink("ns:query", ROOT_URL + "/api/customer/search{?queryParam}");

    assertThat(party.toString(RepresentationFactory.HAL_JSON))
        .isEqualTo(exampleWithTemplateJson);
  }

  @Test
  public void testHalWithBeanMultipleNestedSubResources() {

    Representation embedded = representationFactory.newRepresentation(ROOT_URL + "/user/11")
        .withBean(new Customer(11, "Example " + "User", 32))
        .withBeanBasedRepresentation("phone:cell", ROOT_URL + "/phone/1", new Phone(1, "555-666-7890"));

    String href = "customer/123456";
    ReadableRepresentation party = newBaseResource(href)
                                       .withNamespace("phone", "https://example.com/apidocs/phone/{rel}")
                                       .withLink("ns:users", BASE_URL + href + "?users")
                                       .withRepresentation("ns:user", embedded)
                                       .withBeanBasedRepresentation("ns:user", ROOT_URL + "/user/12",
                                           new Customer(12, "Example User", 32));

    assertThat(party.toString(RepresentationFactory.HAL_JSON))
        .isEqualTo(exampleWithMultipleNestedSubresourcesJson);
  }

  @Test
  public void testHalWithArray() {

    String representation = new JsonRepresentationFactory()
                                .withFlag(RepresentationFactory.PRETTY_PRINT)
                                .newRepresentation()
                                .withProperty("name", "Example Resource")
                                .withProperty("array", ImmutableList.of("one", "two", "three"))
                                .toString(RepresentationFactory.HAL_JSON);

    assertThat(representation).isEqualTo(exampleWithArray);

  }

  @Test
  public void testHalWithSingleElemArrayLinks() {
    RepresentationFactory rf = new JsonRepresentationFactory()
                                   .withNamespace("ns", ROOT_URL + "/apidocs/ns/{rel}")
                                   .withFlag(RepresentationFactory.PRETTY_PRINT)
                                   .withRel(Rels.collection("ns:users"));

    String href = "customer/123456";
    ReadableRepresentation party = newBaseResource(rf.newRepresentation(BASE_URL + href))
                                       .withLink("ns:users", BASE_URL + href + "?users")
                                       .withBean(new Customer(123456, "Example Resource", 33));

    assertThat(party.toString(RepresentationFactory.HAL_JSON)).isEqualTo(exampleSingleElemArrayLinksJson);

  }

  @Test
  public void testHalWithSingleElemArray() {

    String representation = new JsonRepresentationFactory()
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
