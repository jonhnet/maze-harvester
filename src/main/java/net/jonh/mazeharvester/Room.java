package net.jonh.mazeharvester;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toSet;

import com.google.common.collect.ImmutableSet;
import java.awt.geom.Point2D;
import java.util.HashSet;

/**
 * A Room is an open region centered on some point in space, bordered by a set of Doors (line
 * segments) that are shared with adjacent Rooms. If all of the doors are drawn, then the room is
 * unreachable; the maze plumber knocks down doors to make paths through the field.
 */
class Room {
  public String name;
  private HashSet<Door> doors = new HashSet<>();
  private Point2D centerPoint;

  public Point2D getCenterPoint() {
    return centerPoint;
  }

  Room(int x, int y, Point2D centerPoint) {
    this.name = String.format("%d,%d", x, y);
    this.centerPoint = centerPoint;
  }

  public ImmutableSet<Door> getDoors() {
    return ImmutableSet.copyOf(doors);
  }

  void addDoor(Door door) {
    doors.add(door);
  }

  @Override
  public String toString() {
    return this.name;
  }

  ImmutableSet<Room> getAdjacentRooms() {
    return doors
        .stream()
        .map(d -> d.opposite(this))
        .filter(r -> r != null)
        .collect(collectingAndThen(toSet(), ImmutableSet::copyOf));
  }

  ImmutableSet<Door> getWalls() {
    return doors
        .stream()
        .filter(d -> d.isWall())
        .collect(collectingAndThen(toSet(), ImmutableSet::copyOf));
  }

  boolean isExterior() {
    return doors.stream().filter(d -> d.isWall()).count() > 0;
  }
}
