package net.jonh.mazeharvester;


import java.awt.geom.Point2D;
import java.awt.Dimension;

/** Filters rooms to impose an mask over some other underlying regular field structure. */
interface FieldMask {

  /** Returns the bounds of the mask. The field should be sized to have rooms covering the mask. */
  Dimension getMaskSize();

  /** Returns true if the room at {@code point} should be included in the field. */
  boolean admitRoom(Point2D point);

  /** Creates a new FieldMask scaled to the given dimension. */
  FieldMask scaleTo(Dimension dimension);

  /** Keeps the entire original field. */
  static class NoMask implements FieldMask {
    Dimension size;

    NoMask() {
      this.size = new Dimension(0, 0); // Dimensions expected from another source.
    }

    NoMask(Dimension size) {
      this.size = size;
    }

    public Dimension getMaskSize() {
      return size;
    }

    public boolean admitRoom(Point2D point) {
      return true;
    }

    public FieldMask scaleTo(Dimension dimension) {
      return new NoMask(dimension);
    }
  }
}
