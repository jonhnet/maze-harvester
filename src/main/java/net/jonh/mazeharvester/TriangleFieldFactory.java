package net.jonh.mazeharvester;


import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;

/** A field of tiled equilateral triangles. */
class TriangleFieldFactory extends AbstractFieldFactory {
  static final double tan30 = Math.tan(30 * Math.PI / 180);
  static final double rh = 0.5 / tan30; // row height

  int w;
  int h;
  FieldMask fieldMask;

  static TriangleFieldFactory createGrid(int w, int h) {
    return new TriangleFieldFactory(w, h, new FieldMask.NoMask());
  }

  static TriangleFieldFactory createFromMask(FieldMask fieldMask) {
    return new TriangleFieldFactory(
      (int) (fieldMask.getMaskSize().getWidth() * 2.0),
      (int) (fieldMask.getMaskSize().getHeight() / rh),
      fieldMask);
  }

  private TriangleFieldFactory(int w, int h, FieldMask fieldMask) {
    this.w = w;
    this.h = h;
    this.fieldMask = fieldMask;
  }

  public Field build() {
    // Room addresses
    for (int y = 0; y < h; y++) {
      double yc = y * rh;
      for (int x = 0; x < w; x++) {
        double xc = x * 0.5;
        Point2D centerPoint = new Point2D.Double(xc, yc);
        if (fieldMask.admitRoom(centerPoint)) {
          addRoom(x, y, centerPoint);
        }
      }
    }

    final double xo = 0.5;
    // Doors
    for (Map.Entry<Pair<Integer, Integer>, Room> entry : map.entrySet()) {
      int x = entry.getKey().getLeft();
      int y = entry.getKey().getRight();
      Point2D center = entry.getValue().getCenterPoint();
      double xc = center.getX();
      double yc = center.getY();

      int parity = ((x + y) % 2 == 0) ? -1 : 1;

      // "top" (bottom)
      placeDoor(
          x,
          y,
          0,
          -1 * parity,
          new Line2D.Double(
              new Point2D.Double(xc - xo, yc - 0.5 * rh * parity),
              new Point2D.Double(xc + xo, yc - 0.5 * rh * parity)));
      // "lower left" (left)
      placeDoor(
          x,
          y,
          -1,
          0,
          new Line2D.Double(
              new Point2D.Double(xc - xo, yc - 0.5 * rh * parity),
              new Point2D.Double(xc, yc + 0.5 * rh * parity)));
      // "lower right" (right)
      placeDoor(
          x,
          y,
          1,
          0,
          new Line2D.Double(
              new Point2D.Double(xc + xo, yc - 0.5 * rh * parity),
              new Point2D.Double(xc, yc + 0.5 * rh * parity)));
    }

    return bake();
  }
}
