package net.jonh.mazeharvester;

import java.awt.geom.Point2D;

interface FieldFactory {
  // Convert a point from cell coordinates to paper coordinates.
  Point2D cellToPaper(Point2D point);

  // Build the configured field.
  Field build();
}
