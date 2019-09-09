package com.logicgate.farm.service;

import com.logicgate.farm.domain.Animal;
import com.logicgate.farm.domain.Barn;
import com.logicgate.farm.domain.Color;
import com.logicgate.farm.repository.AnimalRepository;
import com.logicgate.farm.repository.BarnRepository;
import com.logicgate.farm.util.BarnOrganizer;
import com.logicgate.farm.util.FarmUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class AnimalServiceImpl implements AnimalService {

  @Autowired
  private ApplicationContext applicationContext;

  private Random random = new Random();

  private final AnimalRepository animalRepository;

  private final BarnRepository barnRepository;

  // find or create a barn with availability to insert an additional animal
  private Barn findOrCreateBarnWithAvailability(Map<Barn, List<Animal>> barns, Color color) {
    // search for a barn that isn't already at capacity
    Optional<Barn> availableBarn = barns.entrySet().stream()
            .filter(entry -> entry.getValue().size() < FarmUtils.barnCapacity())
            .map(Map.Entry::getKey).findFirst();

    // if we find one return it, otherwise create / persist a new one,
    // add it to the map and return it
    if (availableBarn.isPresent()) {
      return availableBarn.get();
    }
    else {
      Barn newBarn = new Barn(FarmUtils.barnName(color, random.nextInt()), color);
      newBarn = barnRepository.save(newBarn);
      barns.put(newBarn, new ArrayList<>());
      return newBarn;
    }
  }

  // create a new barn
  private Barn createNewBarn(Color color) {
    Barn newBarn = new Barn(FarmUtils.barnName(color, random.nextInt()), color);
    newBarn = barnRepository.save(newBarn);
    return newBarn;
  }

  // remove any empty barns from map and DB
  private void cleanupEmptyBarns(Map<Barn, List<Animal>> barns) {
    // search for empty barns
    List<Barn> emptyBarns = barns.entrySet().stream()
            .filter(entry -> entry.getValue().size() == 0)
            .map(Map.Entry::getKey).collect(Collectors.toList());

    for (Barn emptyBarn : emptyBarns) {
      barns.remove(emptyBarn);
      barnRepository.delete(emptyBarn);
    }
  }

  @Autowired
  public AnimalServiceImpl(AnimalRepository animalRepository, BarnRepository barnRepository) {
    this.animalRepository = animalRepository;
    this.barnRepository = barnRepository;
  }

  @Override
  @Transactional(readOnly = true)
  public List<Animal> findAll() {
    return animalRepository.findAll();
  }

  @Override
  public void deleteAll() {
    animalRepository.deleteAll();
  }

  @Override
  public Animal addToFarm(Animal animal) {

    // retrieve new BarnOrganizer from context
    BarnOrganizer barnOrganizer = applicationContext.getBean(BarnOrganizer.class);

    // get all animals with the same color / barn
    List<Animal> animalsMatchingColor = animalRepository.findAnimalsByFavoriteColor(animal.getFavoriteColor());
    // organize animals by barn ID
    Map<Barn, List<Animal>> barnedAnimals = animalsMatchingColor.stream().collect(Collectors.groupingBy(Animal::getBarn));
    // find a barn with availability
    Barn availableBarn = findOrCreateBarnWithAvailability(barnedAnimals, animal.getFavoriteColor());
    // add new animal to the barn
    barnedAnimals.getOrDefault(availableBarn, new ArrayList<>()).add(animal);
    animal.setBarn(availableBarn);
    animalRepository.save(animal);
    // organize / rebalance the barns for this color, since we've modified it
    List<Animal> updatedAnimals = barnOrganizer.organizeAnimals(barnedAnimals);
    // update any animals that were modified during the reorganization
    animalRepository.saveAll(updatedAnimals);

    return animal;

  }

  @Override
  public void addToFarm(List<Animal> animals) {

    // retrieve new BarnOrganizer from context
    BarnOrganizer barnOrganizer = applicationContext.getBean(BarnOrganizer.class);

    // group given animals by color
    Map<Color, List<Animal>> animalsByColor = animals.stream().collect(Collectors.groupingBy(Animal::getFavoriteColor));

    for (Map.Entry<Color, List<Animal>> colorSet : animalsByColor.entrySet()) {
      // get all animals with the same color
      List<Animal> animalsMatchingColor = animalRepository.findAnimalsByFavoriteColor(colorSet.getKey());
      // store the barns in a set for later reuse
      Set<Barn> existingBarns = animalsMatchingColor.stream().map(Animal::getBarn).collect(Collectors.toSet());
      // add the new animals to the existing list
      animalsMatchingColor.addAll(colorSet.getValue());
      // organize the animals so that they're ready to be added to barns and persisted
      List<List<Animal>> organizedAnimals = barnOrganizer.initializeAnimals(animalsMatchingColor);

      Iterator<Barn> barnIterator = existingBarns.iterator();
      for (List<Animal> barnOfAnimals : organizedAnimals) {
        // get existing or create new barn
        Barn addToBarn = barnIterator.hasNext() ? barnIterator.next() : createNewBarn(colorSet.getKey());
        // add animals to the barn
        barnOfAnimals.forEach(animal -> animal.setBarn(addToBarn));
        // persist the animals
        animalRepository.saveAll(barnOfAnimals);
      }
      // delete any remaining barns
      while (barnIterator.hasNext()) {
        barnRepository.delete(barnIterator.next());
      }

    }
  }

  @Override
  public void removeFromFarm(Animal animal) {

    // retrieve new BarnOrganizer from context
    BarnOrganizer barnOrganizer = applicationContext.getBean(BarnOrganizer.class);

    // remove animal from repository
    animalRepository.delete(animal);
    // get all animals with the same color / barn
    List<Animal> animalsMatchingColor = animalRepository.findAnimalsByFavoriteColor(animal.getFavoriteColor());
    // organize animals by barn ID
    Map<Barn, List<Animal>> barnedAnimals = animalsMatchingColor.stream().collect(Collectors.groupingBy(Animal::getBarn));
    // organize / rebalance the barns for this color, since we've modified it
    List<Animal> updatedAnimals = barnOrganizer.organizeAnimals(barnedAnimals);
    // update any animals that were modified during the reorganization
    animalRepository.saveAll(updatedAnimals);
    // delete any empty barns
    cleanupEmptyBarns(barnedAnimals);

  }

  @Override
  public void removeFromFarm(List<Animal> animals) {
    animals.forEach(animal -> removeFromFarm(animalRepository.getOne(animal.getId())));
  }
}
