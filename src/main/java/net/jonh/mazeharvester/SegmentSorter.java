package net.jonh.mazeharvester;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import com.google.common.collect.ImmutableList;
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
  // Two points are the same point if they're within a millionth of a room-width.
  // (Avoid missing an equality due to double precision limitations.)
  static final double COLLAPSE_THRESH = 1e-6;

  // Comically-slow sort, because mazes big enough for O(n^2) are too big for my kids to solve,
  // and I don't have a QuadTree lying around to make this efficient.
  private static List<Line2D> quadraticSort(ImmutableList<Line2D> input) {
    ArrayList<Line2D> segments = new ArrayList<>(input);
    ArrayList<Line2D> output = new ArrayList<>();

    // Pick the first segment arbitrarily.
    Line2D nextSegment = segments.remove(0);
    output.add(nextSegment);

    // Now find nearby segments with scans of the segments array. O(n^2)!
    while (segments.size() > 0) {
      Point2D currentPen = nextSegment.getP2();

      int bestIndex = 0;
      int bestDirection = 0;
      double bestDistance = Double.MAX_VALUE;
      
      for (int i=0; i<segments.size(); i++) {
        Line2D thisLine = segments.get(i);
        for (int direction = 0; direction < 2; direction++) {
          Point2D thisPoint = direction==0 ? thisLine.getP1() : thisLine.getP2();
          double thisDistance = currentPen.distance(thisPoint);
          if (thisDistance < bestDistance) {
            bestIndex = i;
            bestDirection = direction;
            bestDistance = thisDistance;
          }
        }
      }

      nextSegment = segments.remove(bestIndex);
      if (bestDirection == 1) {
        nextSegment = new Line2D.Double(nextSegment.getP2(), nextSegment.getP1());
      }
      output.add(nextSegment);
    }
    return output;
  }

  private static class SegmentCollector implements SegmentGraphics {
    Map<Color, List<Line2D>> segsByColor = new HashMap<>();
    List<Line2D> curColorList;

    void add(Line2D seg) {
      curColorList.add(seg);
    }

    public Iterable<Color> getColors() {
      return segsByColor.keySet();
    }

    public ImmutableList<Line2D> getSegments(Color color) {
      return ImmutableList.copyOf(segsByColor.get(color));
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

  public void collect(SegmentPainter painter) {
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

    // Greedily gather segments into polyline paths.
    List<Line2D> polyLine = new ArrayList<>();
    Point2D lastPoint = null;
    for (Color color : collector.getColors()) {
      g2d.setPaint(color);
      List<Line2D> sortedSegments = quadraticSort(collector.getSegments(color));
      for (Line2D seg : sortedSegments) {
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
