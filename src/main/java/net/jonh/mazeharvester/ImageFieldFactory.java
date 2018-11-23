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
 * Creates a field of squares, but uses a black-and-white PNG mask to remove
 * squares to specify hand-crafted geometries.
 */
class ImageFieldFactory extends SquareFieldFactory {
	static BufferedImage imageFromFilename(String filename) throws IOException {
		return ImageIO.read(new URL(filename));
	}

	final BufferedImage image;

	ImageFieldFactory(BufferedImage image) {
		super(image.getWidth(), image.getHeight());
		this.image = image;
	}

	public void buildRooms() {
		super.buildRooms();

		HashSet<Integer> seenRGBs = new HashSet<>();
		for (int y=0; y<h; y++) {
			for (int x=0; x<w; x++) {
				seenRGBs.add(image.getRGB(x, y));
				if (image.getRGB(x, y) == 0xffffffff) {
					map.remove(Pair.<Integer,Integer>of(x,y));
				}
			}
		}
	}
}
