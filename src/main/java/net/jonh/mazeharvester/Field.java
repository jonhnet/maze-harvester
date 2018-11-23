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
 * A Field is a connected graph of Rooms and Doors. A square field looks like a
 * sheet of graph paper with no routes through it; a Maze later decides which
 * Doors to knock down to create paths.
 *
 * <p>Fields don't have to be regular patterns (but we haven't yet implemented
 * any that aren't).
 */
class Field {
	private ImmutableSet<Door> doors;
	private ImmutableSet<Room> rooms;

	public ImmutableSet<Door> getDoors() { return doors; }
	public ImmutableSet<Room> getRooms() { return rooms; }


	Field(ImmutableSet<Door> doors, ImmutableSet<Room> rooms) {
		this.doors = doors;
		this.rooms = rooms;
	}

	public ImmutableSet<Door> getWalls() {
		return doors.stream().filter(d -> d.isWall())
			.collect(collectingAndThen(toSet(), ImmutableSet::copyOf));
	}

	public ImmutableSet<Door> getInteriorDoors() {
		return doors.stream().filter(d -> !d.isWall())
			.collect(collectingAndThen(toSet(), ImmutableSet::copyOf));
	}

	public ImmutableSet<Door> getDoorsForBounds() {
		return doors;
	}
}
