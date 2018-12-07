package net.jonh.mazeharvester;


import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;

/** A field of tiled hexagons. */
class HexFieldFactory extends AbstractFieldFactory {
  static final double tan30 = Math.tan(30 * Math.PI / 180);
  static final double rh = 0.5 / tan30; // row height

  static final double tan60 = Math.tan(60 * Math.PI / 180);
  static final double hh = 0.5 / tan60; // half a hex side. (1.0 is across the flats)
  static final double ph = rh - hh; // height to peak

  int w;
  int h;
  FieldMask fieldMask;

  static HexFieldFactory create(FieldMask fieldMask) {
    return new HexFieldFactory(
      fieldMask.getMaskSize().width,
      (int) (fieldMask.getMaskSize().height / rh),
      fieldMask);
  }

  private HexFieldFactory(int w, int h, FieldMask fieldMask) {
    this.w = w;
    this.h = h;
    this.fieldMask = fieldMask;
  }

  public Field build() {
    // Room addresses
    int middle = h / 2;
    for (int y = 0; y < h; y++) {
      int rowOffset = Math.abs(middle - y);
      int rowWidth = w - rowOffset;
      int xOffset = 0;
      int xMax = w;
      for (int x = xOffset; x < xMax; x++) {
        double xc = 0.5 * (y % 2) + x;
        double yc = y * rh;
        Point2D centerPoint = new Point2D.Double(xc, yc);
        if (fieldMask.admitRoom(centerPoint)) {
          addRoom(x, y, centerPoint);
        }
      }
    }

    // Doors
    for (Map.Entry<Pair<Integer, Integer>, Room> entry : map.entrySet()) {
      int x = entry.getKey().getLeft();
      int y = entry.getKey().getRight();
      Point2D center = entry.getValue().getCenterPoint();
      double xc = center.getX();
      double yc = center.getY();
      int xphase = y % 2;

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
          -1 + xphase,
          -1,
          new Line2D.Double(
              new Point2D.Double(xc - 0.5, yc - hh), new Point2D.Double(xc, yc - ph)));
      // Lower left
      placeDoor(
          x,
          y,
          -1 + xphase,
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
          0 + xphase,
          -1,
          new Line2D.Double(
              new Point2D.Double(xc + 0.5, yc - hh), new Point2D.Double(xc, yc - ph)));
      // Lower right
      placeDoor(
          x,
          y,
          0 + xphase,
          1,
          new Line2D.Double(
              new Point2D.Double(xc + 0.5, yc + hh), new Point2D.Double(xc, yc + ph)));
    }
    return bake();
  }
}
