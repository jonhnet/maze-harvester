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

class Maze4
{
	static class Room {
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

	static class Door {
		public Line2D segment;
		public ImmutableSet<Room> rooms;
		Room disambiguator;	// one room may have many walls to nowhere, so walls need disambiguation to avoid collapsing.

		Door(Line2D segment, ImmutableSet<Room> rooms, Room disambiguator) {
			this.segment = segment;
			this.rooms = rooms;
			this.disambiguator = rooms.size()==1 ? disambiguator : null;
			for (Room room : rooms) {
				room.addDoor(this);
			}
		}

		@Override public String toString() {
			return rooms.toString() + disambiguator;
		}

		boolean isWall() {
			return rooms.size() == 1;
		}

		Room opposite(Room a) {
			Iterator<Room> opposites = rooms.stream()
				.filter(r -> r != a).collect(toList()).iterator();
			return opposites.hasNext() ? opposites.next() : null;
		}

		// asserts this is a Wall.
		Room onlyRoom() {
			if (!isWall()) {
				throw new IllegalArgumentException("Not a wall.");
			}
			return rooms.iterator().next();
		}

		@Override public boolean equals(Object other) {
			if (other instanceof Door) {
				Door otherDoor = (Door) other;
				//System.out.println("rooms"+rooms+" dis" + disambiguator);
				return rooms.equals(otherDoor.rooms)
					&& Objects.equals(disambiguator, otherDoor.disambiguator);
			}
			return false;
		}

		@Override public int hashCode() {
			return Objects.hash(rooms, disambiguator);
		}
	}

	interface Painter {
		ImmutableSet<Door> getDoorsForBounds();
		void paint(Graphics2D g2d);
	}

	static class Field {
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

	interface FieldFactory {
		Field build();
	}

	static abstract class AbstractFieldFactory implements FieldFactory {
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

	static class SquareFieldFactory extends AbstractFieldFactory {
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

	static BufferedImage imageFromFilename(String filename) throws IOException {
		return ImageIO.read(new URL(filename));
	}

	static class ImageFieldFactory extends SquareFieldFactory {
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
			//System.out.println("Seen rgbs " + seenRGBs);
		}
	}

	static class HexFieldFactory extends AbstractFieldFactory {
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

	static class TriangleFieldFactory extends AbstractFieldFactory {
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

	static class FieldWithExits {
		private Field field;
		private ImmutableSet<Door> exitDoors = ImmutableSet.of();

		public FieldWithExits(Field field, ImmutableSet<Door> exitDoors) {
			this.field = field;
			this.exitDoors = exitDoors;
		}

		public Field getField() { return field; }
		public ImmutableSet<Door> getExitDoors() { return exitDoors; }
	}

	interface ExitCutter {
		FieldWithExits cutExits(Field field);
	}

	static class RandomExitsCutter {
		public FieldWithExits cutExits(Field field) {
			ArrayList<Door> list = new ArrayList<>(field.getWalls());
			Collections.shuffle(list);
			return new FieldWithExits(field,
				ImmutableSet.of(list.get(0), list.get(1)));
		}
	}

	static class ProportionalExitsCutter implements ExitCutter {
		private Point2D entrance;
		private Point2D exit;

		/**
		 * Selects entrance and exit by interpolating rooms along a cardinal
		 * edge and knocking out a wall there.
		 */
		ProportionalExitsCutter(Point2D entrance, Point2D exit) {
			this.entrance = entrance;
			this.exit = exit;
		}

		class ProximityComparator implements Comparator<Room> {
			private Point2D ideal;

			public ProximityComparator(Point2D ideal) {
				this.ideal = ideal;
			}

			public int compare(Room a, Room b) {
				return Double.compare(
					ideal.distance(a.getCenterPoint()),
					ideal.distance(b.getCenterPoint()));
			}
		}

		private double interpolateAxis(
				double minv, double maxv, double prop) {
			return prop * (maxv - minv) + minv;
		}

		public Point2D.Double interpolate(Rectangle2D bounds,
			Point2D proportion) {
			return new Point2D.Double(
				interpolateAxis(bounds.getMinX(), bounds.getMaxX(),
					proportion.getX()),
				interpolateAxis(bounds.getMinY(), bounds.getMaxY(),
					proportion.getY()));
		}

		public FieldWithExits cutExits(Field field) {
			List<Room> exteriorRooms = field.getRooms().stream()
				.filter(r -> r.isExterior()).collect(toList());

			Rectangle2D bounds = null;
			for (Room room : exteriorRooms) {
				Rectangle2D point = new Rectangle2D.Double(
					room.getCenterPoint().getX(),
					room.getCenterPoint().getY(),
					0,
					0);
				if (bounds == null) { bounds = point; }
				bounds = bounds.createUnion(point);
			}
			Point2D.Double scaledEntrance = interpolate(bounds, entrance);
			Point2D.Double scaledExit = interpolate(bounds, exit);

			Room entranceRoom = Collections.min(exteriorRooms, new ProximityComparator(scaledEntrance));
			Room exitRoom = Collections.min(exteriorRooms, new ProximityComparator(scaledExit));

			// Select exterior walls arbitrarily.
			Door entranceDoor = entranceRoom.getWalls().iterator().next();
			Door exitDoor = exitRoom.getWalls().iterator().next();
			return new FieldWithExits(field,
				ImmutableSet.of(entranceDoor, exitDoor));
		}
	}

	static class Maze implements Painter {
		FieldWithExits fieldWithExits;
		public ImmutableSet<Door> pathDoors = ImmutableSet.of();

		private Maze(FieldWithExits fieldWithExits) {
			this.fieldWithExits = fieldWithExits;
		}

		static public Maze create(Random random, FieldWithExits fieldWithExits) {
			Maze maze = new Maze(fieldWithExits);
			maze.plumbMaze(random);
			return maze;
		}

		public ImmutableSet<Door> getClosedDoors() {
			return fieldWithExits.getField().getDoors().stream()
				.filter(d -> !fieldWithExits.getExitDoors().contains(d) &&
					!pathDoors.contains(d))
				.collect(collectingAndThen(toSet(), ImmutableSet::copyOf));
		}

		public ImmutableSet<Door> getClosedInteriorDoors() {
			return fieldWithExits.getField().getDoors().stream()
				.filter(d ->
					   !d.isWall()
					&& !fieldWithExits.getExitDoors().contains(d)
					&& !pathDoors.contains(d))
				.collect(collectingAndThen(toSet(), ImmutableSet::copyOf));
		}

		// Like Room.getAdjacentRooms, but only considers rooms to which
		// the door has an open path.
		public ImmutableSet<Room> getReachableAdjacentRooms(Room room) {
			return room.getDoors().stream()
				.filter(d -> pathDoors.contains(d))
				.map(d -> d.opposite(room))
				.collect(collectingAndThen(toSet(), ImmutableSet::copyOf));
		}

		/**
		 * Generate the maze by opening doors.
		 */
		class Plumber {
			Random random;

			Plumber(Random random) {
				this.random = random;
			}

			class Frontier {
				ArrayList<Room> frontier = new ArrayList<>();
				HashSet<Room> visited = new HashSet<>();

				boolean isEmpty() {
					return frontier.isEmpty();
				}

				void add(Room room) {
					if (!visited.contains(room)) {
						visited.add(room);
						frontier.add(room);
					}
				}

				Room removeRandomRoom() {
					int index = random.nextInt(frontier.size());
					return frontier.remove(index);
				}
			}

			// Rooms that can reach one another via opened doors.
			HashSet<Room> reachableRooms = new HashSet<>();

			// Unreachable rooms that adjoin a reachable room.
			Frontier frontier = new Frontier();

			// The doors we decided to open.
			ImmutableSet.Builder<Door> openedDoors = new ImmutableSet.Builder<>();

			private void visit(Room room) {
				reachableRooms.add(room);

				// Mark its neighbors as ready to explore.
				for (Room adjacentRoom : room.getAdjacentRooms()) {
					frontier.add(adjacentRoom);
				}
			}
			
			ImmutableSet<Door> plumb() {
				Room firstRoom = fieldWithExits.getField().getRooms().iterator().next();
				visit(firstRoom);

				// Explore until we're done.
				while (!frontier.isEmpty()) {
					Room room = frontier.removeRandomRoom();
					if (room == null) {
						break;
					}

					List<Door> eligibleDoors = room.doors.stream()
						.filter(d -> reachableRooms.contains(d.opposite(room)))
						.collect(toList());
					int index = random.nextInt(eligibleDoors.size());
					Door randomDoor = eligibleDoors.get(index);
					openedDoors.add(randomDoor);
					visit(room);
				}
				return openedDoors.build();
			}
		}

		private void plumbMaze(Random random) {
			pathDoors = new Plumber(random).plumb();
		}

		public ImmutableSet<Door> getDoorsForBounds() {
			return fieldWithExits.getField().doors;
		}

		public void paint(Graphics2D g2d) {
			g2d.setPaint(Color.BLACK);
			g2d.setStroke(new BasicStroke(0.16f,
				BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			for (Door door : getClosedDoors()) {
				g2d.draw(door.segment);
			}
		}

		private class Convoluter {
			/*static*/ class DetourRoute {
				Room solutionRoom;
				Room nextRoom;
				int distanceToSolution;

				DetourRoute(
						Room solutionRoom, Room nextRoom, int distanceToSolution) {
					this.solutionRoom = solutionRoom;
					this.nextRoom = nextRoom;
					this.distanceToSolution = distanceToSolution;
				}

				@Override public String toString() {
					return Objects.toString(nextRoom) + ":" + distanceToSolution;
				}
			}

			class DoorScoreComparator
					implements Comparator<Map.Entry<Door, Integer>> {
				public int compare(
						Map.Entry<Door, Integer> a, Map.Entry<Door, Integer> b) {
					return Integer.compare(
						a.getValue(), b.getValue());
				}
			}

			Queue<Room> queue = new ArrayDeque<>();
			Map<Room, DetourRoute> detourMap = new HashMap<>();

			Maze convolute(SolvedMaze solution) {
				// Label all the rooms in the maze with their DetourRoute.
				for (Room room : solution.getSolutionPath()) {
					detourMap.put(room, new DetourRoute(room, null, 0));
					queue.add(room);
				}

				while (queue.peek() != null) {
					Room room = queue.remove();
					DetourRoute route = detourMap.get(room);
					for (Room adjacentRoom : getReachableAdjacentRooms(room)) {
						if (!detourMap.containsKey(adjacentRoom)) {
							detourMap.put(adjacentRoom, new DetourRoute(
								route.solutionRoom, room, route.distanceToSolution + 1));
							queue.add(adjacentRoom);
						}
					}
				}

				// Now find adjacent rooms that form long detours.
				Map<Room, SolvedMaze.ExitRoute> exitMap = solution.getExitMap();
				Map<Door, Integer> doorScore = new HashMap<>();
				for (Door door : getClosedInteriorDoors()) {
					Iterator<Room> iterator = door.rooms.iterator();
					Room roomA = iterator.next();
					Room roomB = iterator.next();
					DetourRoute routeA = detourMap.get(roomA);
					DetourRoute routeB = detourMap.get(roomB);

					if (routeA.solutionRoom == routeB.solutionRoom) {
						// These rooms share a path to the solution.
						continue;
					}

					// Compute how much the path will grow with this door swap.
					int newDistance =
						routeA.distanceToSolution + routeB.distanceToSolution;
					int removedDistance = Math.abs(
						exitMap.get(routeA.solutionRoom).distanceToExit
						- exitMap.get(routeB.solutionRoom).distanceToExit);
					int changedDistance = newDistance - removedDistance;
					doorScore.put(door, changedDistance);
				}

				// Now find the longest convolution.
				Map.Entry<Door, Integer> bestDoorEntry = Collections.max(
					doorScore.entrySet(), new DoorScoreComparator());
				// We'll be opening this door.
				Door bestDoor = bestDoorEntry.getKey();

				// Which door do we close? A door in one the solutionRooms.
				// Which one? The one closer to the other solutionRoom.
				Iterator<Room> iterator = bestDoor.rooms.iterator();
				Room roomA = iterator.next();
				Room roomB = iterator.next();
				DetourRoute routeA = detourMap.get(roomA);
				DetourRoute routeB = detourMap.get(roomB);
				// Remove a useless symmetry, such that A's solution distance
				// is less than B's.
				Room exitSideRoom =
					(exitMap.get(routeA.solutionRoom).distanceToExit <
					exitMap.get(routeB.solutionRoom).distanceToExit)
						? roomA : roomB;
				DetourRoute exitSideDetour = detourMap.get(exitSideRoom);
				Room victim = exitSideDetour.solutionRoom;
				// Now the door we want is the door from victim that leads to a
				// room on the solution path with a greater solution distance
				// (and hence is along the solution path towards the other
				// room).
				int victimDistance = exitMap.get(victim).distanceToExit;

				List<Door> victimDoors = victim.getDoors().stream().filter(
					d -> {
						Room r = d.opposite(victim);
						return solution.getSolutionPath().contains(r)
							&& pathDoors.contains(d)
							&& exitMap.get(r).distanceToExit > victimDistance;
					}).collect(toList());
				if (victimDoors.size() != 1) {
					System.out.println("victimDoors " + victimDoors);
					throw new IllegalArgumentException("assertion failed");
				}
				Door victimDoor = victimDoors.get(0);

				// Copy and modify previous solution.
				HashSet<Door> newPathDoors = new HashSet<>(pathDoors);
				newPathDoors.remove(victimDoor);
				newPathDoors.add(bestDoor);

				Maze maze = new Maze(fieldWithExits);
				maze.pathDoors = ImmutableSet.copyOf(newPathDoors);
				return maze;
			}
		}

		public Maze convolute(SolvedMaze solution) {
			return new Convoluter().convolute(solution);
		}
	}

	static class SolvedMaze implements Painter {
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

	static class SVGEmitter {
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
	 *
	 *
	 * Future features:
	 * - add voids and big-rooms
	 * - generalize image mask support to work for all field tilings.
	 * - command line flags, I guess!
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
		new Maze4().main();
	}
}
