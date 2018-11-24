package net.jonh.mazeharvester;

import static java.util.stream.Collectors.toList;

import com.google.common.collect.ImmutableSet;
import java.awt.geom.Line2D;
import java.util.Iterator;
import java.util.Objects;

/**
 * A Door is a boundary between two Rooms, or possibly an edge of a single Room (where it is on the
 * perimeter of the maze, or next to an interior void; then we call it a "wall").
 *
 * <p>A Door has a line segment that represents it when present, although later layers will mask
 * away doors to make paths through the maze and exits through the wall.
 */
class Door {
  public Line2D segment;
  public ImmutableSet<Room> rooms;

  // A single Room may have multiple walls to nowhere (imagine a corner
  // square), so walls need disambiguation to avoid collapsing into a
  // single equivalence class in set membership.
  Room disambiguator;

  Door(Line2D segment, ImmutableSet<Room> rooms, Room disambiguator) {
    this.segment = segment;
    this.rooms = rooms;
    this.disambiguator = rooms.size() == 1 ? disambiguator : null;
    for (Room room : rooms) {
      room.addDoor(this);
    }
  }

  @Override
  public String toString() {
    return rooms.toString() + disambiguator;
  }

  boolean isWall() {
    return rooms.size() == 1;
  }

  Room opposite(Room a) {
    Iterator<Room> opposites = rooms.stream().filter(r -> r != a).collect(toList()).iterator();
    return opposites.hasNext() ? opposites.next() : null;
  }

  // asserts this is a Wall.
  Room onlyRoom() {
    if (!isWall()) {
      throw new IllegalArgumentException("Not a wall.");
    }
    return rooms.iterator().next();
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof Door) {
      Door otherDoor = (Door) other;
      // System.out.println("rooms"+rooms+" dis" + disambiguator);
      return rooms.equals(otherDoor.rooms)
          && Objects.equals(disambiguator, otherDoor.disambiguator);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(rooms, disambiguator);
  }
}
