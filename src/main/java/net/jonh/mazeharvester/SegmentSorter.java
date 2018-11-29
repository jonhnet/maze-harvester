package net.jonh.mazeharvester;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import com.google.common.collect.ImmutableSet;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.Dimension;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.Queue;
import java.util.Random;
import java.awt.geom.Line2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;

/**
 * Collects line segments, sorts by color, and then sorts by proximity to reduce plot time.
 */
class SegmentSorter {
  static final double FIDELITY = 1e-6;
  static final double COLLAPSE_THRESH = FIDELITY;

  TreeSet<Double> internSet = new TreeSet<>();

  private Double intern(double v) {
    Double low = internSet.floor(v);
    if (low != null && v - low < FIDELITY) {
      return low;
    }
    Double high = internSet.ceiling(v);
    if (high != null && high - v < FIDELITY) {
      return high;
    }
    Double dv = new Double(v);
    internSet.add(dv);
    return dv;
  }

  static class Segment {
    Line2D line2d;

    Segment(Line2D line2d) {
      line2d = line2d;
    }

    Line2D getLine() {
      return line2d;
    }

    Line2D getReversed() {
      return new Line2D.Double(line2d.getP2(), line2d.getP1());
    }
  }

  static class SegmentCollector implements SegmentGraphics {
    Map<Color, List<Line2D>> segsByColor = new HashMap<>();
    List<Line2D> curColorList;

    void add(Line2D seg) {
      curColorList.add(seg);
    }

    public Iterable<Color> getColors() {
      return segsByColor.keySet();
    }

    public Iterable<Line2D> getSegments(Color color) {
      return segsByColor.get(color);
    }

    // Implements SegmentGraphics
    public void setColor(Color color) {
      List<Line2D> list = segsByColor.get(color);
      if (list == null) {
        list = new ArrayList<>();
        segsByColor.put(color, list);
      }
      curColorList = list;
    }

    public void draw(Line2D line2d) {
      if (curColorList == null) {
        setColor(Color.BLACK);
      }
      curColorList.add(line2d);
    }
  }

  SegmentCollector collector = new SegmentCollector();

  void collect(SegmentPainter painter) {
    painter.paint(collector);
  }

  private List<Line2D> emitCurrentPolyLine(Graphics2D g2d, List<Line2D> segments) {
    if (segments.size() == 0) {
      return segments;
    }

    GeneralPath path = new GeneralPath();
    Point2D p = segments.get(0).getP1();
    path.moveTo(p.getX(), p.getY());
    for (int i=0; i<segments.size(); i++) {
      p = segments.get(i).getP2();
      path.lineTo(p.getX(), p.getY());
    }
    g2d.draw(path);
    return new ArrayList<>();
  }

  public Rectangle2D getBounds() {
    Rectangle2D boundingBox = null;
    for (Color color : collector.getColors()) {
      for (Line2D seg : collector.getSegments(color)) {
        Rectangle2D shapeBox = seg.getBounds2D();
        if (boundingBox == null) {
          boundingBox = shapeBox;
        }
        boundingBox = boundingBox.createUnion(shapeBox);
      }
    }
    return boundingBox;
  }

  public void paint(Graphics2D g2d) {
    g2d.setStroke(new BasicStroke(0.16f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

    // Gather segments into polyline paths.
    List<Line2D> polyLine = new ArrayList<>();
    Point2D lastPoint = null;
    for (Color color : collector.getColors()) {
      g2d.setPaint(color);
      for (Line2D seg : collector.getSegments(color)) {
        if (lastPoint == null || lastPoint.distance(seg.getP1()) > COLLAPSE_THRESH) {
          polyLine = emitCurrentPolyLine(g2d, polyLine);
        }
        polyLine.add(seg);
        lastPoint = seg.getP2();
      }
      polyLine = emitCurrentPolyLine(g2d, polyLine);
    }
  }
}
