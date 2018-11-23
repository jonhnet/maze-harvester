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

class Main
{
	/*
	 * Construct a collection of "rooms" and "doors."
	 * Each door joins a pair of rooms.
	 * The geometry can be arbitrary; there can be voids in the middle
	 * for decoration (whose perimeter aren't doors), or a room can contain
	 * decorations inside them (that are passable via doors).
	 * A door that adjoins only one room is a wall. (Exterior or void.)
	 *
	 * Construct a path through the maze. Seeding at multiple locations
	 * may encourage richer paths. (For the Texas maze, I had a way to
	 * enforce a rich path. I wonder what that was.)
	 *
	 * Bust open some edge walls as entrance / exit. (Decorate?)
	 *
	 * Convert the non-open doors into line segments.
	 * Greedily glue the line segments to half-assedly TSP them.
	 *
	 * Scale the whole mess.
	 * Emit an SVG. (HPGL is for the birds ...and the plotter.)
	 *
	 * Plot the solution.
	 */
	
	public void main() throws IOException {
		//Field field = new SquareFieldFactory(30, 15).build();
		Field field = new HexFieldFactory(20, 15).build();
		//Field field = new TriangleFieldFactory(45, 30).build();
		//Field field = new ImageFieldFactory(imageFromFilename("file:ian-small.png")).build();
		//Field field = new ImageFieldFactory(imageFromFilename("file:ian-big.png")).build();

		//ExitCutter exitCutter = new RandomExitsCutter();
		ExitCutter exitCutter = new ProportionalExitsCutter(
			new Point2D.Double(0.0, 0.1),
			new Point2D.Double(1.0, 0.9));
		exitCutter = new ProportionalExitsCutter(
			new Point2D.Double(0.0, 0.9),
			new Point2D.Double(1.0, 0.1));
		FieldWithExits fieldWithExits = exitCutter.cutExits(field);

		// Bump random seed to see different solution alternatives.
		Random random = new Random(27);

		// Don't print this maze! solveWithStretch mutates the walls.
		Maze firstMaze = Maze.create(random, fieldWithExits);

		SolvedMaze solvedMaze = SolvedMaze.solveWithStretch(firstMaze, 5.0);
		new SVGEmitter("maze.svg").emit(solvedMaze.getMaze());
		new SVGEmitter("solution.svg").emit(solvedMaze);
	}
	
	public static void main(String[] args) throws IOException {
		new Main().main();
	}
}
