package net.jonh.mazeharvester;


import com.google.common.collect.ImmutableSet;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.HashSet;
import org.apache.commons.lang3.tuple.Pair;

/** Handy methods that ease field construction. */
abstract class AbstractFieldFactory implements FieldFactory {
  HashMap<Pair<Integer, Integer>, Room> map = new HashMap<>();
  HashSet<Door> doors = new HashSet<>();

  protected void addRoom(int x, int y, Point2D.Double centerPoint) {
    map.put(Pair.<Integer, Integer>of(x, y), new Room(x, y, centerPoint));
  }

  protected void placeDoor(int x, int y, int dx, int dy, Line2D segment) {
    Room here = map.get(Pair.<Integer, Integer>of(x, y));
    Room other = map.get(Pair.<Integer, Integer>of(x + dx, y + dy));
    ImmutableSet<Room> adjoiningRooms =
        other == null ? ImmutableSet.of(here) : ImmutableSet.of(here, other);
    Room fakeDisambiguatorRoom = new Room(x + dx, y + dy, null);
    doors.add(new Door(segment, adjoiningRooms, fakeDisambiguatorRoom));
  }

  protected Field bake() {
    return new Field(ImmutableSet.copyOf(doors), ImmutableSet.copyOf(map.values()));
  }
}
