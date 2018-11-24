package net.jonh.mazeharvester;

import static java.util.stream.Collectors.toList;

import com.google.common.collect.ImmutableSet;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

class ProportionalExitsCutter implements ExitCutter {
  private Point2D entrance;
  private Point2D exit;

  /**
   * Selects entrance and exit by interpolating rooms along a cardinal edge and knocking out a wall
   * there.
   */
  ProportionalExitsCutter(Point2D entrance, Point2D exit) {
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

  private double interpolateAxis(double minv, double maxv, double prop) {
    return prop * (maxv - minv) + minv;
  }

  public Point2D.Double interpolate(Rectangle2D bounds, Point2D proportion) {
    return new Point2D.Double(
        interpolateAxis(bounds.getMinX(), bounds.getMaxX(), proportion.getX()),
        interpolateAxis(bounds.getMinY(), bounds.getMaxY(), proportion.getY()));
  }

  public FieldWithExits cutExits(Field field) {
    List<Room> exteriorRooms =
        field.getRooms().stream().filter(r -> r.isExterior()).collect(toList());

    Rectangle2D bounds = null;
    for (Room room : exteriorRooms) {
      Rectangle2D point =
          new Rectangle2D.Double(room.getCenterPoint().getX(), room.getCenterPoint().getY(), 0, 0);
      if (bounds == null) {
        bounds = point;
      }
      bounds = bounds.createUnion(point);
    }
    Point2D.Double scaledEntrance = interpolate(bounds, entrance);
    Point2D.Double scaledExit = interpolate(bounds, exit);

    Room entranceRoom = Collections.min(exteriorRooms, new ProximityComparator(scaledEntrance));
    Room exitRoom = Collections.min(exteriorRooms, new ProximityComparator(scaledExit));

    // Select exterior walls arbitrarily.
    Door entranceDoor = entranceRoom.getWalls().iterator().next();
    Door exitDoor = exitRoom.getWalls().iterator().next();
    return new FieldWithExits(field, ImmutableSet.of(entranceDoor, exitDoor));
  }
}
