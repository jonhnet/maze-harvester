package net.jonh.mazeharvester;


import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.Random;

class Main {
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
//      Field field = HexFieldFactory.createGrid(60, 45).build();
    // Field field = new HexFieldFactory(20, 20, new FieldMask.NoMask()).build();

    //Field field = HexFieldFactory.createFromMask(fieldMask).build();
//    Field field = SquareFieldFactory.createFromMask(fieldMask).build();

    ImageFieldMask fieldMask = ImageFieldMask.fromFilename("file:samples/ian-big.png").scaleTo(90, 45);
    Field field = HexFieldFactory.createFromMask(fieldMask).build();

//    Field field = TriangleFieldFactory.createGrid(45, 30).build();

//    Field field = new SquareFieldFactory(
//        (int) fieldMask.getImageSize().getWidth(),
//        (int) fieldMask.getImageSize().getHeight(), fieldMask).build();
    // Field field = new ImageFieldFactory(imageFromFilename("file:ian-big.png")).build();

    // ExitCutter exitCutter = new RandomExitsCutter();
    ExitCutter exitCutter =
        new ProportionalExitsCutter(new Point2D.Double(0.0, 0.1), new Point2D.Double(1.0, 0.9));
    exitCutter =
        new ProportionalExitsCutter(new Point2D.Double(0.0, 0.9), new Point2D.Double(1.0, 0.1));
    FieldWithExits fieldWithExits = exitCutter.cutExits(field);

    // Bump random seed to see different solution alternatives.
    Random random = new Random(27);

    // Don't print this maze! solveWithStretch mutates the walls.
    Maze firstMaze = Maze.create(random, fieldWithExits);

    SolvedMaze solvedMaze = SolvedMaze.solveWithStretch(firstMaze, 3.0);
    new SVGEmitter("maze.svg").emit(solvedMaze.getMaze());
    new SVGEmitter("solution.svg").emit(solvedMaze);
  }

  public static void main(String[] args) throws IOException {
    new Main().main();
  }
}
