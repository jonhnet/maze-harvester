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
 * Produces a rectangular field of squares: think graph paper.
 */
class SquareFieldFactory extends AbstractFieldFactory {
	int w;
	int h;

	public SquareFieldFactory(int w, int h) {
		this.w = w;
		this.h = h;
	}

	// Override to add voids, big-rooms.
	protected void buildRooms() {
		// Room addresses
		for (int y=0; y<h; y++) {
			for (int x=0; x<w; x++) {
				addRoom(x, y, new Point2D.Double(x, y));
			}
		}
	}

	public Field build() {
		buildRooms();

		// Doors
		for (Pair<Integer, Integer> address : map.keySet()) {
			int x = address.getLeft();
			int y = address.getRight();

			// Left
			placeDoor(x, y, -1, 0, new Line2D.Double(
				new Point2D.Double(x-0.5, y-0.5), new Point2D.Double(x-0.5, y+0.5)));
			// Right
			placeDoor(x, y, 1, 0, new Line2D.Double(
				new Point2D.Double(x+0.5, y-0.5), new Point2D.Double(x+0.5, y+0.5)));
			// Above
			placeDoor(x, y, 0, -1, new Line2D.Double(
				new Point2D.Double(x-0.5, y-0.5), new Point2D.Double(x+0.5, y-0.5)));
			// Below
			placeDoor(x, y, 0, 1, new Line2D.Double(
				new Point2D.Double(x-0.5, y+0.5), new Point2D.Double(x+0.5, y+0.5)));
		}
		return bake();
	}
}
