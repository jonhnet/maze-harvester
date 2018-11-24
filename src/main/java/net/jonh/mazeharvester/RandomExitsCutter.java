package net.jonh.mazeharvester;


import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Cut two doors entirely at random. Can produce silly situations like adjacent exits, but those
 * might be okay if you use convolution later.
 */
class RandomExitsCutter {
  public FieldWithExits cutExits(Field field) {
    ArrayList<Door> list = new ArrayList<>(field.getWalls());
    Collections.shuffle(list);
    return new FieldWithExits(field, ImmutableSet.of(list.get(0), list.get(1)));
  }
}
