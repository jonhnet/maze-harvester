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
 * A field of tiled hexagons.
 */
class HexFieldFactory extends AbstractFieldFactory {
	final double tan30 = Math.tan(30*Math.PI/180);
	final double rh = 0.5 / tan30;	// row height

	final double tan60 = Math.tan(60*Math.PI/180);
	final double hh = 0.5 / tan60;	 // half a hex side. (1.0 is across the flats)
	final double ph = rh - hh;	// height to peak

	int w;
	int h;

	public HexFieldFactory(int w, int h) {
		this.w = w;
		this.h = h;
	}

	public Field build() {
		// Room addresses
		int middle = h/2;
		for (int y=0; y<h; y++) {
			int rowOffset = Math.abs(middle - y);
			int rowWidth = w - rowOffset;
			int xOffset = y >= middle ? rowOffset : 0;
			int xMax = rowWidth + xOffset;
			for (int x=xOffset; x<xMax; x++) {
				double xc = 0.5 * (h - y) + x;
				double yc = y * rh;
				addRoom(x, y, new Point2D.Double(xc, yc));
			}
		}

		// Doors
		for (Map.Entry<Pair<Integer, Integer>, Room> entry : map.entrySet()) {
			int x = entry.getKey().getLeft();
			int y = entry.getKey().getRight();
			Point2D center = entry.getValue().getCenterPoint();
			double xc = center.getX();
			double yc = center.getY();

// Center point
/*
			placeDoor(x, y, 700, 700, new Line2D.Double(
				new Point2D.Double(xc, yc), new Point2D.Double(xc+0.01, yc+0.01)));
				*/
			// Left
			placeDoor(x, y, -1, 0, new Line2D.Double(
				new Point2D.Double(xc-0.5, yc-hh), new Point2D.Double(xc-0.5, yc+hh)));
			// Upper left
			placeDoor(x, y, -1, -1, new Line2D.Double(
				new Point2D.Double(xc-0.5, yc-hh), new Point2D.Double(xc, yc-ph)));
			// Lower left
			placeDoor(x, y, 0, 1, new Line2D.Double(
				new Point2D.Double(xc-0.5, yc+hh), new Point2D.Double(xc, yc+ph)));
			// Right
			placeDoor(x, y, 1, 0, new Line2D.Double(
				new Point2D.Double(xc+0.5, yc-hh), new Point2D.Double(xc+0.5, yc+hh)));
			// Upper right
			placeDoor(x, y, 0, -1, new Line2D.Double(
				new Point2D.Double(xc+0.5, yc-hh), new Point2D.Double(xc, yc-ph)));
			// Lower right
			placeDoor(x, y, 1, 1, new Line2D.Double(
				new Point2D.Double(xc+0.5, yc+hh), new Point2D.Double(xc, yc+ph)));
		}
		return bake();
	}
}
