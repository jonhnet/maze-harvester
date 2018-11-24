package net.jonh.mazeharvester;


import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;

/** A field of tiled hexagons. */
class HexFieldFactory extends AbstractFieldFactory {
  final double tan30 = Math.tan(30 * Math.PI / 180);
  final double rh = 0.5 / tan30; // row height

  final double tan60 = Math.tan(60 * Math.PI / 180);
  final double hh = 0.5 / tan60; // half a hex side. (1.0 is across the flats)
  final double ph = rh - hh; // height to peak

  int w;
  int h;

  public HexFieldFactory(int w, int h) {
    this.w = w;
    this.h = h;
  }

  public Field build() {
    // Room addresses
    int middle = h / 2;
    for (int y = 0; y < h; y++) {
      int rowOffset = Math.abs(middle - y);
      int rowWidth = w - rowOffset;
      int xOffset = y >= middle ? rowOffset : 0;
      int xMax = rowWidth + xOffset;
      for (int x = xOffset; x < xMax; x++) {
        double xc = 0.5 * (h - y) + x;
        double yc = y * rh;
        addRoom(x, y, new Point2D.Double(xc, yc));
      }
    }

    // Doors
    for (Map.Entry<Pair<Integer, Integer>, Room> entry : map.entrySet()) {
      int x = entry.getKey().getLeft();
      int y = entry.getKey().getRight();
      Point2D center = entry.getValue().getCenterPoint();
      double xc = center.getX();
      double yc = center.getY();

      // Center point
      /*
      placeDoor(x, y, 700, 700, new Line2D.Double(
      	new Point2D.Double(xc, yc), new Point2D.Double(xc+0.01, yc+0.01)));
      	*/
      // Left
      placeDoor(
          x,
          y,
          -1,
          0,
          new Line2D.Double(
              new Point2D.Double(xc - 0.5, yc - hh), new Point2D.Double(xc - 0.5, yc + hh)));
      // Upper left
      placeDoor(
          x,
          y,
          -1,
          -1,
          new Line2D.Double(
              new Point2D.Double(xc - 0.5, yc - hh), new Point2D.Double(xc, yc - ph)));
      // Lower left
      placeDoor(
          x,
          y,
          0,
          1,
          new Line2D.Double(
              new Point2D.Double(xc - 0.5, yc + hh), new Point2D.Double(xc, yc + ph)));
      // Right
      placeDoor(
          x,
          y,
          1,
          0,
          new Line2D.Double(
              new Point2D.Double(xc + 0.5, yc - hh), new Point2D.Double(xc + 0.5, yc + hh)));
      // Upper right
      placeDoor(
          x,
          y,
          0,
          -1,
          new Line2D.Double(
              new Point2D.Double(xc + 0.5, yc - hh), new Point2D.Double(xc, yc - ph)));
      // Lower right
      placeDoor(
          x,
          y,
          1,
          1,
          new Line2D.Double(
              new Point2D.Double(xc + 0.5, yc + hh), new Point2D.Double(xc, yc + ph)));
    }
    return bake();
  }
}
