package net.jonh.mazeharvester;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toSet;

import com.google.common.collect.ImmutableSet;
import java.awt.geom.Rectangle2D;

/**
 * A Field is a connected graph of Rooms and Doors. A square field looks like a sheet of graph paper
 * with no routes through it; a Maze later decides which Doors to knock down to create paths.
 *
 * <p>Fields don't have to be regular patterns (but we haven't yet implemented any that aren't).
 */
class Field {
  private ImmutableSet<Door> doors;
  private ImmutableSet<Room> rooms;

  public ImmutableSet<Door> getDoors() {
    return doors;
  }

  public ImmutableSet<Room> getRooms() {
    return rooms;
  }

  /** Returns the dimensions of the field in Room (maze) coordinates. */
  public Rectangle2D getRoomBounds() {
    Rectangle2D rect = new Rectangle2D.Double();
    for (Room room : rooms) {
      rect.add(room.getCenterPoint());
    }
    return rect;
  }

  Field(ImmutableSet<Door> doors, ImmutableSet<Room> rooms) {
    this.doors = doors;
    this.rooms = rooms;
  }

  public ImmutableSet<Door> getWalls() {
    return doors
        .stream()
        .filter(d -> d.isWall())
        .collect(collectingAndThen(toSet(), ImmutableSet::copyOf));
  }

  public ImmutableSet<Door> getInteriorDoors() {
    return doors
        .stream()
        .filter(d -> !d.isWall())
        .collect(collectingAndThen(toSet(), ImmutableSet::copyOf));
  }

  public ImmutableSet<Door> getDoorsForBounds() {
    return doors;
  }
}
