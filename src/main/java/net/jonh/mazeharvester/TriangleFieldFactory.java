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
 * A field of tiled equilateral triangles.
 */
class TriangleFieldFactory extends AbstractFieldFactory {
	final double tan30 = Math.tan(30*Math.PI/180);
	final double rh = 0.5 / tan30;	// row height

	int w;
	int h;

	public TriangleFieldFactory(int w, int h) {
		this.w = w;
		this.h = h;
	}

	public Field build() {
		// Room addresses
		for (int y=0; y<h; y++) {
			double yc = y * rh;
			for (int x=0; x<w; x++) {
				double xc = x * 0.5;
				addRoom(x, y, new Point2D.Double(xc, yc));
			}
		}

		final double xo = 0.5;
		// Doors
		for (int y=0; y<h; y++) {
			double yc = y * rh;
			for (int x=0; x<w; x++) {
				double xc = x * 0.5;
				int parity = ((x + y) % 2 == 0) ? -1 : 1;

				// "top" (bottom)
				placeDoor(x, y, 0, -1*parity, new Line2D.Double(
					new Point2D.Double(xc-xo, yc-0.5*rh*parity), new Point2D.Double(xc+xo, yc-0.5*rh*parity)));
				// "lower left" (left)
				placeDoor(x, y, -1, 0, new Line2D.Double(
					new Point2D.Double(xc-xo, yc-0.5*rh*parity), new Point2D.Double(xc, yc+0.5*rh*parity)));
				// "lower right" (right)
				placeDoor(x, y, 1, 0, new Line2D.Double(
					new Point2D.Double(xc+xo, yc-0.5*rh*parity), new Point2D.Double(xc, yc+0.5*rh*parity)));
			}
		}
		
		return bake();
	}
}
