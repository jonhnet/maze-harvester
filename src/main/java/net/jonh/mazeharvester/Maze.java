package net.jonh.mazeharvester;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import com.google.common.collect.ImmutableSet;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.AbstractCollection;
import java.util.ArrayDeque;
import java.util.ArrayList;
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

class Maze implements SegmentPainter {
  FieldWithExits fieldWithExits;
  public ImmutableSet<Door> pathDoors = ImmutableSet.of();

  private Maze(FieldWithExits fieldWithExits) {
    this.fieldWithExits = fieldWithExits;
  }

  public static Maze create(Random random, FieldWithExits fieldWithExits) {
    Maze maze = new Maze(fieldWithExits);
    maze.plumbMaze(random);
    return maze;
  }

  public ImmutableSet<Door> getClosedDoors() {
    return fieldWithExits
        .getField()
        .getDoors()
        .stream()
        .filter(d -> !fieldWithExits.getExitDoors().contains(d) && !pathDoors.contains(d))
        .collect(collectingAndThen(toSet(), ImmutableSet::copyOf));
  }

  public ImmutableSet<Door> getClosedInteriorDoors() {
    return fieldWithExits
        .getField()
        .getDoors()
        .stream()
        .filter(
            d ->
                !d.isWall() && !fieldWithExits.getExitDoors().contains(d) && !pathDoors.contains(d))
        .collect(collectingAndThen(toSet(), ImmutableSet::copyOf));
  }

  // Like Room.getAdjacentRooms, but only considers rooms to which
  // the door has an open path.
  public ImmutableSet<Room> getReachableAdjacentRooms(Room room) {
    return room.getDoors()
        .stream()
        .filter(d -> pathDoors.contains(d))
        .map(d -> d.opposite(room))
        .collect(collectingAndThen(toSet(), ImmutableSet::copyOf));
  }

  /** Generate the maze by opening doors. */
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
      // First room we plumb is disproportionately likely to end up in the solution,
      // so select it randomly. (When I used to just take the first room arbitrarily,
      // solutions were biased to the upper-left corner of every maze.)
      //Room firstRoom = fieldWithExits.getField().getRooms().iterator().next();
      AbstractCollection rooms = fieldWithExits.getField().getRooms();
      Room firstRoom = (Room) rooms.stream().skip(random.nextInt(rooms.size())).findFirst().get();
      visit(firstRoom);

      // Explore until we're done.
      while (!frontier.isEmpty()) {
        Room room = frontier.removeRandomRoom();
        if (room == null) {
          break;
        }

        List<Door> eligibleDoors =
            room.getDoors()
                .stream()
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
    return fieldWithExits.getField().getDoors();
  }

  public void paint(SegmentGraphics segmentGraphics) {
    segmentGraphics.setColor(Color.BLACK);
    for (Door door : getClosedDoors()) {
      segmentGraphics.draw(door.segment);
    }
  }

  private class Convoluter {
    /*static*/ class DetourRoute {
      Room solutionRoom;
      Room nextRoom;
      int distanceToSolution;

      DetourRoute(Room solutionRoom, Room nextRoom, int distanceToSolution) {
        this.solutionRoom = solutionRoom;
        this.nextRoom = nextRoom;
        this.distanceToSolution = distanceToSolution;
      }

      @Override
      public String toString() {
        return Objects.toString(nextRoom) + ":" + distanceToSolution;
      }
    }

    class DoorScoreComparator implements Comparator<Map.Entry<Door, Integer>> {
      public int compare(Map.Entry<Door, Integer> a, Map.Entry<Door, Integer> b) {
        return Integer.compare(a.getValue(), b.getValue());
      }
    }

    Queue<Room> queue = new ArrayDeque<>();
    Map<Room, DetourRoute> detourMap = new HashMap<>();

    Maze convolute(SolvedMaze solution, StretchOptions options) {
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
            detourMap.put(
                adjacentRoom,
                new DetourRoute(route.solutionRoom, room, route.distanceToSolution + 1));
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
        int newDistance = routeA.distanceToSolution + routeB.distanceToSolution;
        int removedDistance =
            Math.abs(
                exitMap.get(routeA.solutionRoom).distanceToExit
                    - exitMap.get(routeB.solutionRoom).distanceToExit);
        int changedDistance = newDistance - removedDistance;
        doorScore.put(door, changedDistance);
      }

      List<Map.Entry<Door, Integer>> doorScores = new ArrayList<>(doorScore.entrySet());
      Collections.sort(doorScores, new DoorScoreComparator());
      int percentileIndex = Math.min((int) (doorScores.size() * options.getDeviationPercentile()), doorScores.size() - 1);
      Map.Entry<Door, Integer> bestDoorEntry = doorScores.get(percentileIndex);

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
          (exitMap.get(routeA.solutionRoom).distanceToExit
                  < exitMap.get(routeB.solutionRoom).distanceToExit)
              ? roomA
              : roomB;
      DetourRoute exitSideDetour = detourMap.get(exitSideRoom);
      Room victim = exitSideDetour.solutionRoom;
      // Now the door we want is the door from victim that leads to a
      // room on the solution path with a greater solution distance
      // (and hence is along the solution path towards the other
      // room).
      int victimDistance = exitMap.get(victim).distanceToExit;

      List<Door> victimDoors =
          victim
              .getDoors()
              .stream()
              .filter(
                  d -> {
                    Room r = d.opposite(victim);
                    return solution.getSolutionPath().contains(r)
                        && pathDoors.contains(d)
                        && exitMap.get(r).distanceToExit > victimDistance;
                  })
              .collect(toList());
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

  public Maze convolute(SolvedMaze solution, StretchOptions options) {
    return new Convoluter().convolute(solution, options);
  }
}
