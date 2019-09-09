package com.logicgate.farm.util;

import com.logicgate.farm.domain.Animal;
import com.logicgate.farm.domain.Barn;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
@Scope("prototype")
public class BarnOrganizer {

  /**
   * <p>
   * Designed for individual or more incremental changes, where we don't want to completely
   * reorganize the barns or cause excessive DB writes.  After the change is made,
   * organizeAnimals is called and will rebalance for even distribution.  The intention
   * is to make minimal adjustments to the existing data.
   * This is called per color, so is limited to the one set of barns.
   * </p>
   * @param allBarns map animals keyed against barn, each barn has a list of animals
   * @return list of animals that have edits and will need an update operation
   */
  public List<Animal> organizeAnimals(Map<Barn, List<Animal>> allBarns) {

    // count of animals
    Double animalCount = allBarns.values().stream().mapToDouble(List::size).reduce(0, Double::sum);
    // store a list a overpopulate / dirty animals (animals that will require an update operation)
    List<Animal> overPopulatedAnimals = new ArrayList<>();

    // number of barns
    Integer barnCount = (int) Math.ceil(animalCount / FarmUtils.barnCapacity());
    // base (smaller) count for the barns
    Integer baseAnimalCount = (int) Math.floor(animalCount / barnCount);
    // number of remaining animals (where we'll need to have a slightly higher population)
    Integer remainingCount = (int) (animalCount - (barnCount * baseAnimalCount));

    // remove any animals over the base count from all of the barns
    // remove any animals from excess barns
    List<List<Animal>> validBarns = new ArrayList<>();
    // keep an index of the current barn
    int barnIndex = 0;
    for (List<Animal> currentBarn : allBarns.values()) {
      if (barnIndex < barnCount) {
        // save valid barn
        validBarns.add(currentBarn);
        // determine if this barn potentially has too many animals
        Integer barnOverPopulation = currentBarn.size() - baseAnimalCount;
        if (barnOverPopulation > 0) {
          // as we remove the animal, add it to the overpopulated list for use later
          for (int i = 0; i < barnOverPopulation; i++) {
            overPopulatedAnimals.add(currentBarn.remove(0));
          }
        }
      }
      else {
        // move all animals out of this barn, for later cleanup
        overPopulatedAnimals.addAll(currentBarn);
        currentBarn.clear();
      }
      barnIndex++;
    }

    // distribute animals as evenly as possible, with remaining barns
    distributeAnimals(validBarns, overPopulatedAnimals, baseAnimalCount, remainingCount);
    // ensure animals are referencing their correct barn
    allBarns.entrySet().stream().forEach(e -> e.getValue().forEach(animal -> animal.setBarn(e.getKey())));

    // return animals that were moved / require an update
    return overPopulatedAnimals;

  }

  /**
   * <p>
   * Initializer or optimized for larger change sets - ignores any existing
   * persisted Barns and organizes the Animals for even distribution.
   * This is called per color, so is limited to the one set of barns.
   * </p>
   * @param animals all of the animals that need to be organized into barns
   * @return the initialized barn distribution, as lists of animals
   */
  public List<List<Animal>> initializeAnimals(List<Animal> animals) {

    // count of animals
    Double animalCount = (double) animals.size();

    // number of barns
    Integer barnCount = (int) Math.ceil(animalCount / FarmUtils.barnCapacity());
    // base (smaller) count for the barns
    Integer baseAnimalCount = (int) Math.floor(animalCount / barnCount);
    // number of remaining animals (where we'll need to have a slightly higher population)
    Integer remainingCount = (int) (animalCount - (barnCount * baseAnimalCount));

    // create all of the 'barns' to distribute animals
    List<List<Animal>> initializedBarns = IntStream.range(0, barnCount).mapToObj(value -> new ArrayList<Animal>()).collect(Collectors.toList());

    // distribute animals as evenly as possible
    distributeAnimals(initializedBarns, animals, baseAnimalCount, remainingCount);

    return initializedBarns;

  }

  /**
   * <p>
   * Distribute the required number of animals to the given barns, using baseAnimalCount as the smaller population.
   * </p>
   * @param barns  list of barns, organized as lists of animals
   * @param animals those that are being added to the existing barns
   * @param baseAnimalCount the base or lower population count for the ensuring even distribution
   * @param largerBarnCount how many barn are larger, ie have 1 additional head count above the base population
   */
  private void distributeAnimals(List<List<Animal>> barns, List<Animal> animals, Integer baseAnimalCount, Integer largerBarnCount) {

    // need to keep a start index to copy animals from
    int animalStartIndex = 0;
    // iterate over barns and distribute the animals
    // to either baseAnimalCount or baseAnimalCount +1
    for (int i = 0; i < barns.size(); i++) {
      List<Animal> currentBarn = barns.get(i);
      Integer barnTargetPopulation = (i >= largerBarnCount) ? baseAnimalCount : (baseAnimalCount + 1);
      Integer barnAdditionalNeeded = barnTargetPopulation - currentBarn.size();
      if ((barnAdditionalNeeded > 0) && (animals.size() >= (animalStartIndex + barnAdditionalNeeded))) {
        currentBarn.addAll(animals.subList(animalStartIndex, animalStartIndex + barnAdditionalNeeded));
        animalStartIndex += barnAdditionalNeeded;
      }
    }
  }

}
