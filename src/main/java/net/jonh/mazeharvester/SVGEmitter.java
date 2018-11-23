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

class SVGEmitter {
	final Point2D.Double paperSizeInches = new Point2D.Double(11, 8.5);
	final double pixelsPerInch = 90;
	final Point2D.Double paperSizePixels = new Point2D.Double(
		paperSizeInches.getX() * pixelsPerInch,
		paperSizeInches.getY() * pixelsPerInch);
	final double marginScale = 0.1;

	String outputFilename;

	// bounds computed by computeBounds()
	double scale;
	Dimension pageSize;
	Point2D pageOffset;

	SVGEmitter(String outputFilename) {
		this.outputFilename = outputFilename;
	}

	void computeBounds(Painter painter) {
		Rectangle2D boundingBox = null;
		for (Door door : painter.getDoorsForBounds()) {
			Rectangle2D shapeBox = door.segment.getBounds2D();
			if (boundingBox == null) {
				boundingBox = shapeBox;
			}
			boundingBox = boundingBox.createUnion(shapeBox);
		}

		// Scale the bounding box up to add borders.
		Rectangle2D scaledBBox = new Rectangle2D.Double(
			boundingBox.getX() - boundingBox.getWidth() * 0.5 * marginScale,
			boundingBox.getY() - boundingBox.getHeight() * 0.5 * marginScale,
			boundingBox.getWidth() * (1.0 + marginScale),
			boundingBox.getHeight() * (1.0 + marginScale));
		
		scale = Math.min(
			paperSizePixels.getX() / scaledBBox.getWidth(),
			paperSizePixels.getY() / scaledBBox.getHeight());
		pageSize = new Dimension((int) (scaledBBox.getWidth() * scale),
			(int) (scaledBBox.getHeight() * scale));
		pageOffset = new Point2D.Double(scaledBBox.getX(), scaledBBox.getY());
	}

	// https://xmlgraphics.apache.org/batik/using/svg-generator.html
	public void emit(Painter painter) throws IOException {
		DOMImplementation domImpl =
		  GenericDOMImplementation.getDOMImplementation();

		// Create an instance of org.w3c.dom.Document.
		String svgNS = "http://www.w3.org/2000/svg";
		Document document = domImpl.createDocument(svgNS, "svg", null);

		// Create an instance of the SVG Generator.
		SVGGraphics2D svgGenerator = new SVGGraphics2D(document);

		computeBounds(painter);
		svgGenerator.setSVGCanvasSize(pageSize);

		// Set up the page transform so painters can paint in their
		// Room/Door coordinate system.
		svgGenerator.scale(scale, scale);
		svgGenerator.translate(-pageOffset.getX(), -pageOffset.getY());

		// white background
		svgGenerator.setPaint(Color.WHITE);
		svgGenerator.fill(new Rectangle2D.Double(
			pageOffset.getX(), pageOffset.getY(),
			pageSize.getWidth(), pageSize.getHeight()));

		// Ask the test to render into the SVG Graphics2D implementation.
		painter.paint(svgGenerator);

		// Finally, stream out SVG to the standard output using
		// UTF-8 encoding.
		boolean useCSS = true; // we want to use CSS style attributes
		OutputStream outputStream = new FileOutputStream(outputFilename);
		Writer out = new OutputStreamWriter(outputStream, "UTF-8");
		svgGenerator.stream(out, useCSS);
	}
}

