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

class SolvedMaze implements Painter {
	static class ExitRoute {
		Room nextRoom;
		int distanceToExit;

		ExitRoute(Room nextRoom, int distanceToExit) {
			this.nextRoom = nextRoom;
			this.distanceToExit = distanceToExit;
		}

		@Override public String toString() {
			return Objects.toString(nextRoom) + ":" + distanceToExit;
		}
	}

	public static SolvedMaze solve(Maze maze) {
		HashMap<Room, ExitRoute> exitMap = new HashMap<>();
		Iterator<Door> exits = maze.fieldWithExits.getExitDoors().iterator();
		Door exitDoor = exits.next();
		Door entranceDoor = exits.next();
		Room exitRoom = exitDoor.onlyRoom();
		exitMap.put(exitRoom, new ExitRoute(null, 0));
		Queue<Room> frontier = new ArrayDeque<>();
		frontier.add(exitRoom);
		while (frontier.peek() != null) {
			Room room = frontier.remove();
			for (Room adjacentRoom : maze.getReachableAdjacentRooms(room)) {
				ExitRoute route = exitMap.get(room);
				if (!exitMap.containsKey(adjacentRoom)) {
					exitMap.put(adjacentRoom,
						new ExitRoute(room, route.distanceToExit + 1));
					frontier.add(adjacentRoom);
				}
			}
		}
		Room entranceRoom = entranceDoor.onlyRoom();
		ExitRoute route = exitMap.get(entranceRoom);
		// Enumerate the route.
		ImmutableList.Builder<Room> builder = ImmutableList.builder();
		Room room = entranceRoom;
		while (room != null) {
			builder.add(room);
			room = route.nextRoom;
			route = exitMap.get(room);
		}
		ImmutableList<Room> solution = builder.build();
		return new SolvedMaze(maze, solution, exitMap);
	}

	/**
	 * Add convolution to the maze until the path is {@code stretch} times
	 * longer than the crow-flies path between exits.
	 */
	public static SolvedMaze solveWithStretch(Maze maze, double stretch) {
		// Compute the goal path length.
		List<Room> exitRooms = maze.fieldWithExits.getExitDoors().stream()
			.map(d -> d.rooms.iterator().next())
			.collect(toList());
		double crowDistance = exitRooms.get(0).getCenterPoint()
			.distance(exitRooms.get(1).getCenterPoint());
		double goalPathLength = crowDistance * stretch;

		SolvedMaze solvedMaze = solve(maze);
		// Try to stretch the solution to be at least as long as 'stretch'.
		// Limit the number of tries to avoid an infinite loop.
		for (int tries = 0;
			solvedMaze.getSolutionPath().size() < goalPathLength && tries < 20;
			tries++) {
			maze = maze.convolute(solvedMaze);
			solvedMaze = SolvedMaze.solve(maze);
		}
		return solvedMaze;
	}

	private Maze maze;
	private ImmutableList<Room> solutionPath;
	private Map<Room, ExitRoute> exitMap;

	public Maze getMaze() { return maze; }

	// Accessors for convolute.
	ImmutableList<Room> getSolutionPath() { return solutionPath; }
	Map<Room, ExitRoute> getExitMap() { return exitMap; }

	public SolvedMaze(Maze maze, ImmutableList<Room> solutionPath,
		Map<Room, ExitRoute> exitMap) {
		this.maze = maze;
		this.solutionPath = solutionPath;
		this.exitMap = exitMap;
	}

	public ImmutableSet<Door> getDoorsForBounds() {
		return maze.getDoorsForBounds();
	}

	public void paint(Graphics2D g2d) {
		maze.paint(g2d);
		
		g2d.setStroke(new BasicStroke(0.16f,
		BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g2d.setPaint(Color.RED);
		for (int i = 1; i < solutionPath.size(); i++) {
			g2d.draw(new Line2D.Double(
				solutionPath.get(i-1).getCenterPoint(), 
				solutionPath.get(i).getCenterPoint()));
		}
	}
}

