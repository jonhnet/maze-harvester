package net.jonh.mazeharvester;

import static java.util.stream.Collectors.toList;

import java.awt.Dimension;
import com.google.common.collect.ImmutableSet;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

class NearestExitsCutter implements ExitCutter {
  // Points near where we should look for an exit, in maze coordinates.
  private Point2D entrance;
  private Point2D exit;

  /**
   * Selects entrance and exit by interpolating rooms along a cardinal edge and knocking out a wall
   * there.
   */
  static NearestExitsCutter fromAbsolute(Point2D entrance, Point2D exit) {
    return new NearestExitsCutter(entrance, exit);
  }

  /** entrance and exit are on unit square. */
  static NearestExitsCutter fromProportional(Dimension size, Point2D entrance, Point2D exit) {
    return new NearestExitsCutter(interpolate(size, entrance), interpolate(size, exit));
  }

  private static Point2D.Double interpolate(Dimension size, Point2D proportion) {
    return new Point2D.Double(size.getWidth() * proportion.getX(),
      size.getHeight() * proportion.getY());
  }

  private NearestExitsCutter(Point2D entrance, Point2D exit) {
    this.entrance = entrance;
    this.exit = exit;
  }

  class ProximityComparator implements Comparator<Room> {
    private Point2D ideal;

    public ProximityComparator(Point2D ideal) {
      this.ideal = ideal;
    }

    public int compare(Room a, Room b) {
      return Double.compare(ideal.distance(a.getCenterPoint()), ideal.distance(b.getCenterPoint()));
    }
  }

  public FieldWithExits cutExits(Field field) {
    List<Room> exteriorRooms =
        field.getRooms().stream().filter(r -> r.isExterior()).collect(toList());

    Room entranceRoom = Collections.min(exteriorRooms, new ProximityComparator(entrance));
    Room exitRoom = Collections.min(exteriorRooms, new ProximityComparator(exit));

    // Select an exterior wall from each room arbitrarily (if there are multiple choices).
    Door entranceDoor = entranceRoom.getWalls().iterator().next();
    Door exitDoor = exitRoom.getWalls().iterator().next();
    return new FieldWithExits(field, ImmutableSet.of(entranceDoor, exitDoor));
  }
}
