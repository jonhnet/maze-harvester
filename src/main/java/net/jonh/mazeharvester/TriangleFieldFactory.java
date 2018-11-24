package net.jonh.mazeharvester;


import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

/** A field of tiled equilateral triangles. */
class TriangleFieldFactory extends AbstractFieldFactory {
  final double tan30 = Math.tan(30 * Math.PI / 180);
  final double rh = 0.5 / tan30; // row height

  int w;
  int h;

  public TriangleFieldFactory(int w, int h) {
    this.w = w;
    this.h = h;
  }

  public Field build() {
    // Room addresses
    for (int y = 0; y < h; y++) {
      double yc = y * rh;
      for (int x = 0; x < w; x++) {
        double xc = x * 0.5;
        addRoom(x, y, new Point2D.Double(xc, yc));
      }
    }

    final double xo = 0.5;
    // Doors
    for (int y = 0; y < h; y++) {
      double yc = y * rh;
      for (int x = 0; x < w; x++) {
        double xc = x * 0.5;
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
    }

    return bake();
  }
}
