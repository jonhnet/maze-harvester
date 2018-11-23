package net.jonh.mazeharvester;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.Rectangle;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Random;
import javax.imageio.ImageIO;
import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.commons.lang3.tuple.Pair;
import org.w3c.dom.Document;
import org.w3c.dom.DOMImplementation;

class ProportionalExitsCutter implements ExitCutter {
	private Point2D entrance;
	private Point2D exit;

	/**
	 * Selects entrance and exit by interpolating rooms along a cardinal
	 * edge and knocking out a wall there.
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
			return Double.compare(
				ideal.distance(a.getCenterPoint()),
				ideal.distance(b.getCenterPoint()));
		}
	}

	private double interpolateAxis(
			double minv, double maxv, double prop) {
		return prop * (maxv - minv) + minv;
	}

	public Point2D.Double interpolate(Rectangle2D bounds,
		Point2D proportion) {
		return new Point2D.Double(
			interpolateAxis(bounds.getMinX(), bounds.getMaxX(),
				proportion.getX()),
			interpolateAxis(bounds.getMinY(), bounds.getMaxY(),
				proportion.getY()));
	}

	public FieldWithExits cutExits(Field field) {
		List<Room> exteriorRooms = field.getRooms().stream()
			.filter(r -> r.isExterior()).collect(toList());

		Rectangle2D bounds = null;
		for (Room room : exteriorRooms) {
			Rectangle2D point = new Rectangle2D.Double(
				room.getCenterPoint().getX(),
				room.getCenterPoint().getY(),
				0,
				0);
			if (bounds == null) { bounds = point; }
			bounds = bounds.createUnion(point);
		}
		Point2D.Double scaledEntrance = interpolate(bounds, entrance);
		Point2D.Double scaledExit = interpolate(bounds, exit);

		Room entranceRoom = Collections.min(exteriorRooms, new ProximityComparator(scaledEntrance));
		Room exitRoom = Collections.min(exteriorRooms, new ProximityComparator(scaledExit));

		// Select exterior walls arbitrarily.
		Door entranceDoor = entranceRoom.getWalls().iterator().next();
		Door exitDoor = exitRoom.getWalls().iterator().next();
		return new FieldWithExits(field,
			ImmutableSet.of(entranceDoor, exitDoor));
	}
}
