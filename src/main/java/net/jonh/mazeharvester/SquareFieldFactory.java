package net.jonh.mazeharvester;


import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import org.apache.commons.lang3.tuple.Pair;

/** Produces a rectangular field of squares: think graph paper. */
class SquareFieldFactory extends AbstractFieldFactory {
  int w;
  int h;

  public SquareFieldFactory(int w, int h) {
    this.w = w;
    this.h = h;
  }

  // Override to add voids, big-rooms.
  protected void buildRooms() {
    // Room addresses
    for (int y = 0; y < h; y++) {
      for (int x = 0; x < w; x++) {
        addRoom(x, y, new Point2D.Double(x, y));
      }
    }
  }

  public Field build() {
    buildRooms();

    // Doors
    for (Pair<Integer, Integer> address : map.keySet()) {
      int x = address.getLeft();
      int y = address.getRight();

      // Left
      placeDoor(
          x,
          y,
          -1,
          0,
          new Line2D.Double(
              new Point2D.Double(x - 0.5, y - 0.5), new Point2D.Double(x - 0.5, y + 0.5)));
      // Right
      placeDoor(
          x,
          y,
          1,
          0,
          new Line2D.Double(
              new Point2D.Double(x + 0.5, y - 0.5), new Point2D.Double(x + 0.5, y + 0.5)));
      // Above
      placeDoor(
          x,
          y,
          0,
          -1,
          new Line2D.Double(
              new Point2D.Double(x - 0.5, y - 0.5), new Point2D.Double(x + 0.5, y - 0.5)));
      // Below
      placeDoor(
          x,
          y,
          0,
          1,
          new Line2D.Double(
              new Point2D.Double(x - 0.5, y + 0.5), new Point2D.Double(x + 0.5, y + 0.5)));
    }
    return bake();
  }
}
