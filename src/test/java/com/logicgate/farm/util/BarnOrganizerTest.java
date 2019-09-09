package com.logicgate.farm.util;

import com.logicgate.farm.domain.Animal;
import com.logicgate.farm.domain.Barn;
import com.logicgate.farm.domain.Color;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;

@SpringBootTest
@RunWith(SpringRunner.class)
public class BarnOrganizerTest {

  @Autowired
  private ApplicationContext applicationContext;

  @Test
  public void organizeWithBarnDecrease() {

    // retrieve new BarnOrganizer from context
    BarnOrganizer barnOrganizer = applicationContext.getBean(BarnOrganizer.class);

    // initialize sample data
    List<Animal> sampleAnimals = IntStream.range(0, 60)
      .mapToObj(value -> new Animal(FarmUtils.animalName(value), Color.BLUE))
      .collect(Collectors.toList());
    List<Barn> sampleBarns = IntStream.range(0, 3)
      .mapToObj(value -> new Barn("Barn-" + value, Color.BLUE))
      .collect(Collectors.toList());

    // organize animals into ID'd barn map
    Map<Barn, List<Animal>> mappedBarns = new HashMap<>();
    mappedBarns.put(sampleBarns.get(0), new ArrayList<>(sampleAnimals.subList(0, 20)));
    mappedBarns.put(sampleBarns.get(1), new ArrayList<>(sampleAnimals.subList(20, 40)));
    mappedBarns.put(sampleBarns.get(2), new ArrayList<>(sampleAnimals.subList(40, 41)));

    // calculate animal count
    Double animalCount = mappedBarns.values().stream().mapToDouble(List::size).reduce(0, Double::sum);
    assertThat("Should have the correct number of Animals", animalCount, comparesEqualTo(41.0));

    // remove an animal
    mappedBarns.get(sampleBarns.get(0)).remove(0);
    // reorganize the barns
    List<Animal> updatedAnimals = barnOrganizer.organizeAnimals(mappedBarns);

    // recalculate animal count
    animalCount = mappedBarns.values().stream().mapToDouble(List::size).reduce(0, Double::sum);
    assertThat("Should still have the correct number of Animals", animalCount, comparesEqualTo(40.0));

    // get 20 and 0 count barn lists, should be 2 and 1 respectively
    long twentyCount = mappedBarns.values().stream().filter( barn -> barn.size() == 20).count();
    long zeroCount = mappedBarns.values().stream().filter( barn -> barn.size() == 0).count();

    assertThat("Should have two barns of population 20", twentyCount, comparesEqualTo(2l));
    assertThat("Should have one barn of population 0", zeroCount, comparesEqualTo(1l));

  }

  @Test
  public void organizeWithBarnIncrease() {

    // retrieve new BarnOrganizer from context
    BarnOrganizer barnOrganizer = applicationContext.getBean(BarnOrganizer.class);

    // initialize sample data
    List<Animal> sampleAnimals = IntStream.range(0, 60)
      .mapToObj(value -> new Animal(FarmUtils.animalName(value), Color.GOLD))
      .collect(Collectors.toList());
    List<Barn> sampleBarns = IntStream.range(0, 3)
      .mapToObj(value -> new Barn("Barn-" + value, Color.GOLD))
      .collect(Collectors.toList());

    // organize animals into ID'd barn map
    Map<Barn, List<Animal>> mappedBarns = new HashMap<>();
    mappedBarns.put(sampleBarns.get(0), new ArrayList<>(sampleAnimals.subList(0, 20)));
    mappedBarns.put(sampleBarns.get(1), new ArrayList<>(sampleAnimals.subList(20, 40)));

    // calculate animal count
    Double animalCount = mappedBarns.values().stream().mapToDouble(List::size).reduce(0, Double::sum);
    assertThat("Should have the correct number of Animals", animalCount, comparesEqualTo(40.0));

    // add an animal
    mappedBarns.put(sampleBarns.get(2), new ArrayList<>(sampleAnimals.subList(40, 41)));

    // reorganize the barns
    List<Animal> updatedAnimals = barnOrganizer.organizeAnimals(mappedBarns);

    // recalculate animal count
    animalCount = mappedBarns.values().stream().mapToDouble(List::size).reduce(0, Double::sum);
    assertThat("Should still have the correct number of Animals", animalCount, comparesEqualTo(41.0));

    // get 13 and 14 count barn lists, should be 2 and 1 respectively
    long thirteenCount = mappedBarns.values().stream().filter( barn -> barn.size() == 13).count();
    long fourteenCount = mappedBarns.values().stream().filter( barn -> barn.size() == 14).count();

    assertThat("Should have two barns of population 14", fourteenCount, comparesEqualTo(2l));
    assertThat("Should have one barn of population 13", thirteenCount, comparesEqualTo(1l));

  }

  @Test
  public void organizeWithSingleBarn() {

    // retrieve new BarnOrganizer from context
    BarnOrganizer barnOrganizer = applicationContext.getBean(BarnOrganizer.class);

    // initialize sample data
    List<Animal> sampleAnimals = IntStream.range(0, 20)
      .mapToObj(value -> new Animal(FarmUtils.animalName(value), Color.GREEN))
      .collect(Collectors.toList());
    List<Barn> sampleBarns = IntStream.range(0, 3)
      .mapToObj(value -> new Barn("Barn-" + value, Color.GREEN))
      .collect(Collectors.toList());

    // organize animals into ID'd barn map
    Map<Barn, List<Animal>> mappedBarns = new HashMap<>();
    mappedBarns.put(sampleBarns.get(0), new ArrayList<>(sampleAnimals.subList(0, 13)));

    // calculate animal count
    Double animalCount = mappedBarns.values().stream().mapToDouble(List::size).reduce(0, Double::sum);
    assertThat("Should have the correct number of Animals", animalCount, comparesEqualTo(13.0));

    // remove animals
    mappedBarns.get(sampleBarns.get(0)).remove(0);
    mappedBarns.get(sampleBarns.get(0)).remove(0);

    // reorganize the barns
    List<Animal> updatedAnimals = barnOrganizer.organizeAnimals(mappedBarns);

    // recalculate animal count
    animalCount = mappedBarns.values().stream().mapToDouble(List::size).reduce(0, Double::sum);
    assertThat("Should still have the correct number of Animals", animalCount, comparesEqualTo(11.0));

    // get 11 count barn lists, should be 1 only
    long elevenCount = mappedBarns.values().stream().filter( barn -> barn.size() == 11).count();

    assertThat("Should have one barn of population 11", elevenCount, comparesEqualTo(1l));

  }

  @Test
  public void initializeFullBarns() {

    // retrieve new BarnOrganizer from context
    BarnOrganizer barnOrganizer = applicationContext.getBean(BarnOrganizer.class);

    // initialize sample data
    List<Animal> sampleAnimals = IntStream.range(0, 100)
      .mapToObj(value -> new Animal(FarmUtils.animalName(value), Color.BLACK))
      .collect(Collectors.toList());

    // organize list of animals into barns
    List<List<Animal>> barnedAnimals = barnOrganizer.initializeAnimals(sampleAnimals);

    // calculate animal count
    Double animalCount = barnedAnimals.stream().mapToDouble(List::size).reduce(0, Double::sum);
    // expected barn count
    Integer barnCount = (int) Math.ceil(animalCount / FarmUtils.barnCapacity());

    assertThat("Should have initialized the correct number of Animals", animalCount, comparesEqualTo(100.0));
    assertThat("Barn count should match expected", barnedAnimals.size(), comparesEqualTo(barnCount));

  }

  @Test
  public void initializePartialBarns() {

    // retrieve new BarnOrganizer from context
    BarnOrganizer barnOrganizer = applicationContext.getBean(BarnOrganizer.class);

    // initialize sample data
    List<Animal> sampleAnimals = IntStream.range(0, 44)
      .mapToObj(value -> new Animal(FarmUtils.animalName(value), Color.DARKER_THAN_BLACK))
      .collect(Collectors.toList());

    // organize list of animals into barns
    List<List<Animal>> barnedAnimals = barnOrganizer.initializeAnimals(sampleAnimals);

    // calculate animal count
    Double animalCount = barnedAnimals.stream().mapToDouble(List::size).reduce(0, Double::sum);
    // expected barn count
    Integer barnCount = (int) Math.ceil(animalCount / FarmUtils.barnCapacity());

    assertThat("Should have initialized the correct number of Animals", animalCount, comparesEqualTo(44.0));
    assertThat("Barn count should match expected", barnedAnimals.size(), comparesEqualTo(barnCount));

  }

  @Test
  public void initializeSingleBarn() {

    // retrieve new BarnOrganizer from context
    BarnOrganizer barnOrganizer = applicationContext.getBean(BarnOrganizer.class);

    // initialize sample data
    List<Animal> sampleAnimals = IntStream.range(0, 3)
      .mapToObj(value -> new Animal(FarmUtils.animalName(value), Color.PLATINUM))
      .collect(Collectors.toList());

    // organize list of animals into barns
    List<List<Animal>> barnedAnimals = barnOrganizer.initializeAnimals(sampleAnimals);

    // calculate animal count
    Double animalCount = barnedAnimals.stream().mapToDouble(List::size).reduce(0, Double::sum);
    // expected barn count
    Integer barnCount = (int) Math.ceil(animalCount / FarmUtils.barnCapacity());

    assertThat("Should have initialized the correct number of Animals", animalCount, comparesEqualTo(3.0));
    assertThat("Barn count should match expected", barnedAnimals.size(), comparesEqualTo(barnCount));

  }

}
