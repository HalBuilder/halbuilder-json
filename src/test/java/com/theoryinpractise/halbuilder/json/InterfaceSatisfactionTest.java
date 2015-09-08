package com.theoryinpractise.halbuilder.json;

import com.theoryinpractise.halbuilder.api.*;
import com.theoryinpractise.halbuilder.impl.bytecode.InterfaceContract;
import fj.data.List;
import fj.data.TreeMap;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;

import static com.google.common.truth.Truth.assertThat;
import static com.theoryinpractise.halbuilder.api.RepresentationFactory.HAL_JSON;

public class InterfaceSatisfactionTest {

  private RepresentationFactory representationFactory = new JsonRepresentationFactory();

  @DataProvider
  public Object[][] providerSatisfactionData() {
    return new Object[][]{
        {IPerson.class, true},
        {INamed.class, true},
        {IJob.class, false},
        {ISimpleJob.class, false},
    };
  }

  @DataProvider
  public Object[][] provideSatisfactionResources() {
    return new Object[][]{
        {
            representationFactory.readRepresentation(
                HAL_JSON, new InputStreamReader(InterfaceSatisfactionTest.class.getResourceAsStream("/example.json"))),
            representationFactory.readRepresentation(
                HAL_JSON, new InputStreamReader(InterfaceSatisfactionTest.class.getResourceAsStream("/exampleWithNullProperty.json")))
        }
    };
  }

  @Test(dataProvider = "providerSatisfactionData")
  public void testSimpleInterfaceSatisfaction(Class<?> aClass, boolean shouldBeSatisfied) {

    ReadableRepresentation representation = representationFactory.readRepresentation(
        HAL_JSON, new InputStreamReader(InterfaceSatisfactionTest.class.getResourceAsStream("/example.json")));

    assertThat(representation.isSatisfiedBy(InterfaceContract.newInterfaceContract(aClass))).isEqualTo(shouldBeSatisfied);

  }

  @Test(dataProvider = "provideSatisfactionResources")
  public void testAnonymousInnerContractSatisfaction(ReadableRepresentation representation,
                                                     ReadableRepresentation nullPropertyRepresentation) {

    Contract contractHasName = resource -> resource.getProperties().contains("name");
    Contract contractHasOptional = resource -> resource.getProperties().contains("optional");
    Contract contractHasOptionalFalse = resource -> resource.getProperties().contains("optional")
                                                    && resource.getProperties().get("optional").equals("false");

    Contract contractHasNullProperty = new Contract() {
      public boolean isSatisfiedBy(ReadableRepresentation resource) {
        return resource.getProperties().get("nullprop")
                       .map(p -> true)
                       .orSome(() -> false);

      }
    };

    assertThat(representation.isSatisfiedBy(contractHasName)).isEqualTo(true);
    assertThat(representation.isSatisfiedBy(contractHasOptional)).isEqualTo(true);
    assertThat(representation.isSatisfiedBy(contractHasOptionalFalse)).isEqualTo(false);
    assertThat(representation.isSatisfiedBy(contractHasNullProperty)).isEqualTo(false);

    assertThat(nullPropertyRepresentation.isSatisfiedBy(contractHasNullProperty)).isEqualTo(true);
  }

  @Test
  public void testClassRendering() {
    ReadableRepresentation representation = representationFactory.readRepresentation(
        HAL_JSON, new InputStreamReader(InterfaceSatisfactionTest.class.getResourceAsStream("/example.json")));

    assertThat(representation.toClass(INamed.class).name()).isEqualTo("Example Resource");
    assertThat(representation.toClass(IPerson.class).getName()).isEqualTo("Example Resource");
    try {
      representation.toClass(ISimpleJob.class);
      throw new AssertionError("RepresentationException expected");
    } catch (RepresentationException e) {
      //
    }
    try {
      representation.toClass(IJob.class);
      throw new AssertionError("RepresentationException expected");
    } catch (RepresentationException e) {
      //
    }

  }

  @Test
  public void testNullPropertyClassRendering() {
    ReadableRepresentation representation = representationFactory.readRepresentation(
        HAL_JSON, new InputStreamReader(InterfaceSatisfactionTest.class.getResourceAsStream("/exampleWithNullProperty.json")));

    assertThat(representation.toClass(INullprop.class)).isNotNull();
    assertThat(representation.toClass(INullprop.class).nullprop() == null);
  }

  @Test
  public void testGetLinks() {
    ReadableRepresentation representation = representationFactory.readRepresentation(
        HAL_JSON, new InputStreamReader(InterfaceSatisfactionTest.class.getResourceAsStream("/example.json")));

    IPerson person = representation.toClass(IPerson.class);
    assertThat(person.getLinks().isEmpty()).isFalse();
  }

  @Test
  public void testGetEmbedded() {
    ReadableRepresentation representation = representationFactory.readRepresentation(
        HAL_JSON, new InputStreamReader(InterfaceSatisfactionTest.class.getResourceAsStream("/exampleWithSubresource.json")));

    INothing person = representation.toClass(INothing.class);
    assertThat(person.getEmbedded().isEmpty()).isFalse();
  }

  @Test
  public void testNestedObjectGraph() {
    ReadableRepresentation representation = representationFactory.readRepresentation(
        HAL_JSON, new InputStreamReader(InterfaceSatisfactionTest.class.getResourceAsStream("/exampleWithNestedObjects.json")));

    IPersonWithChild person = representation.toClass(IPersonWithChild.class);
    assertThat(person.getChildren()).isNotEmpty();
    assertThat(person.getChildren().get(0)).isNotNull();
  }

  public interface IPerson {
    Integer getAge();

    Boolean getExpired();

    Integer getId();

    String getName();

    List<Link> getLinks();
  }

  public interface INamed {
    String name();
  }

  public interface IJob {
    Integer getJobId();
  }

  public interface ISimpleJob {
    Integer jobId();
  }

  public interface INullprop {
    String nullprop();
  }

  public interface INothing {
    TreeMap<String, Collection<ReadableRepresentation>> getEmbedded();
  }

  public interface IPersonWithChild {
    Integer getAge();

    Boolean getExpired();

    Integer getId();

    String getName();

    List<Link> getLinks();

    ArrayList<IPerson> getChildren();
  }

}
