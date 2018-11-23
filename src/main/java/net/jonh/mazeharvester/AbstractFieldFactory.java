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
 * Handy methods that ease field construction.
 */
abstract class AbstractFieldFactory implements FieldFactory {
	HashMap<Pair<Integer,Integer>, Room> map = new HashMap<>();
	HashSet<Door> doors = new HashSet<>();

	protected void addRoom(int x, int y, Point2D.Double centerPoint) {
		map.put(Pair.<Integer,Integer>of(x,y), new Room(x, y, centerPoint));
	}

	protected void placeDoor(int x, int y, int dx, int dy, Line2D segment) {
		Room here = map.get(Pair.<Integer,Integer>of(x,y));
		Room other = map.get(Pair.<Integer,Integer>of(x+dx,y+dy));
		ImmutableSet<Room> adjoiningRooms = other==null
			? ImmutableSet.of(here)
			: ImmutableSet.of(here, other);
		Room fakeDisambiguatorRoom = new Room(x+dx, y+dy, null);
		doors.add(new Door(segment, adjoiningRooms, fakeDisambiguatorRoom));
	}

	protected Field bake() {
		return new Field(ImmutableSet.copyOf(doors), ImmutableSet.copyOf(map.values()));
	}
}

