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

/**
 * A Room is an open region centered on some point in space, bordered by
 * a set of Doors (line segments) that are shared with adjacent Rooms. If
 * all of the doors are drawn, then the room is unreachable; the maze plumber
 * knocks down doors to make paths through the field.
 */
class Room {
	public String name;
	private HashSet<Door> doors = new HashSet<>();
	private Point2D centerPoint;

	public Point2D getCenterPoint() { return centerPoint; }

	Room(int x, int y, Point2D centerPoint) {
		this.name = String.format("%d,%d", x, y);
		this.centerPoint = centerPoint;
	}

	public ImmutableSet<Door> getDoors() {
		return ImmutableSet.copyOf(doors);
	}

	void addDoor(Door door) {
		doors.add(door);
	}

	@Override public String toString() {
		return this.name;
	}

	ImmutableSet<Room> getAdjacentRooms() {
		return doors.stream().map(d -> d.opposite(this))
			.filter(r -> r != null)
			.collect(collectingAndThen(toSet(), ImmutableSet::copyOf));
	}

	ImmutableSet<Door> getWalls() {
		return doors.stream().filter(d -> d.isWall())
			.collect(collectingAndThen(toSet(), ImmutableSet::copyOf));
	}

	boolean isExterior() {
		return doors.stream().filter(d -> d.isWall()).count() > 0;
	}
}
