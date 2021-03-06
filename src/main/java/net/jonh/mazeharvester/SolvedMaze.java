package net.jonh.mazeharvester;

import static java.util.stream.Collectors.toList;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;

class SolvedMaze implements SegmentPainter {
  static class ExitRoute {
    Room nextRoom;
    int distanceToExit;

    ExitRoute(Room nextRoom, int distanceToExit) {
      this.nextRoom = nextRoom;
      this.distanceToExit = distanceToExit;
    }

    @Override
    public String toString() {
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
          exitMap.put(adjacentRoom, new ExitRoute(room, route.distanceToExit + 1));
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
   * Add convolution to the maze until the path is {@code stretch} times longer than the crow-flies
   * path between exits.
   */
  public static SolvedMaze solveWithStretch(Maze maze, StretchOptions options) {
    // Compute the goal path length.
    Rectangle2D bounds = maze.fieldWithExits.getField().getRoomBounds();
    double diagonalLength =
        new Point2D.Double(0, 0).distance(bounds.getWidth(), bounds.getHeight());
    double goalPathLength = diagonalLength  * options.getStretch();

    SolvedMaze solvedMaze = solve(maze);
    // Try to stretch the solution to be at least as long as 'stretch'.
    // Limit the number of tries to avoid an infinite loop.
    final int maxTries = 200;
    int tries;
    for (tries = 0;
        solvedMaze.getSolutionPath().size() < goalPathLength && tries < maxTries;
        tries++) {
      maze = maze.convolute(solvedMaze, options);
      solvedMaze = SolvedMaze.solve(maze);
    }
    if (tries == maxTries) {
      System.out.println(String.format(
        "I should warn you that solveWithStretch gave up after %d tries", maxTries));
    }
    return solvedMaze;
  }

  private Maze maze;
  private ImmutableList<Room> solutionPath;
  private Map<Room, ExitRoute> exitMap;

  public Maze getMaze() {
    return maze;
  }

  // Accessors for convolute.
  ImmutableList<Room> getSolutionPath() {
    return solutionPath;
  }

  Map<Room, ExitRoute> getExitMap() {
    return exitMap;
  }

  public SolvedMaze(Maze maze, ImmutableList<Room> solutionPath, Map<Room, ExitRoute> exitMap) {
    this.maze = maze;
    this.solutionPath = solutionPath;
    this.exitMap = exitMap;
  }

  public ImmutableSet<Door> getDoorsForBounds() {
    return maze.getDoorsForBounds();
  }

  public void paintSolution(SegmentGraphics segmentGraphics, Color color) {
    segmentGraphics.setColor(color);
    for (int i = 1; i < solutionPath.size(); i++) {
      segmentGraphics.draw(
          new Line2D.Double(
              solutionPath.get(i - 1).getCenterPoint(), solutionPath.get(i).getCenterPoint()));
    }
  }

  public void paint(SegmentGraphics segmentGraphics) {
    maze.paint(segmentGraphics);
    paintSolution(segmentGraphics, Color.RED);
  }
}
