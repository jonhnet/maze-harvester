package net.jonh.mazeharvester;


import org.apache.commons.lang3.tuple.Pair;
import java.awt.geom.Point2D;
import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.Random;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.ParseException;

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
  public static void main(String[] args) throws IOException {
    Options options = new Options();
    Option oMask = new Option("m", "mask-filename", true, "PNG file to use as mask to shape maze");
    options.addOption(oMask);
    Option oSize = new Option("s", "size", true, "Maze size <w,h>");
    options.addOption(oSize);
    Option oPattern = new Option("p", "pattern", true, "Room pattern <square|hexagon|triangle>");
    options.addOption(oPattern);
    Option oProportionalExits = new Option("x", "proportionalExits", true, "Specify exits on unit square <x1,y1,x2,y2>");
    options.addOption(oProportionalExits);
    Option oAbsoluteExits = new Option("y", "absoluteExits", true, "Specify exits in maze coordinates <x1,y1,x2,y2>");
    options.addOption(oAbsoluteExits);
    Option oRandomExits = new Option("z", "randomExits", false, "Select exits randomly");
    options.addOption(oRandomExits);

    CommandLineParser parser = new DefaultParser();
    HelpFormatter formatter = new HelpFormatter();
    CommandLine cmd;
    FieldFactory fieldFactory = null;
    ExitCutter exitCutter =
        new ProportionalExitsCutter(new Point2D.Double(0.0, 0.9), new Point2D.Double(1.0, 0.1));
    try {
      args = new String[] { "--size", "25,25", "--randomExits" };
      cmd = parser.parse(options, args);

      FieldMask fieldMask = new FieldMask.NoMask();
      String sMask = cmd.getOptionValue(oMask.getLongOpt());
      if (sMask != null) {
        fieldMask = ImageFieldMask.fromFilename("file:" + sMask);
      }
      
      String sSize = cmd.getOptionValue(oSize.getLongOpt());
      if (sSize != null) {
        fieldMask = fieldMask.scaleTo(parseDimension(sSize));
      }

      if (fieldMask.getMaskSize().equals(new Dimension(0, 0))) {
        throw new ParseException(
            String.format("Maze size must be specified with either --%s or --%s",
              oMask.getLongOpt(), oSize.getLongOpt()));
      }

      fieldFactory = SquareFieldFactory.create(fieldMask);
      String sPattern = cmd.getOptionValue(oPattern.getLongOpt());
      if (sPattern == null || sPattern.equals("square")) {
        fieldFactory = SquareFieldFactory.create(fieldMask);
      } else if (sPattern.equals("hexagon")) {
        fieldFactory = HexFieldFactory.create(fieldMask);
      } else if (sPattern.equals("triangle")) {
        fieldFactory = TriangleFieldFactory.create(fieldMask);
      } else {
        throw new ParseException(String.format("Unsupported %s %s", oPattern, sPattern));
      }

      String sProportionalExits = cmd.getOptionValue(oProportionalExits.getLongOpt());
      String sAbsoluteExits = cmd.getOptionValue(oAbsoluteExits.getLongOpt());
      boolean bRandomExits = cmd.hasOption(oRandomExits.getLongOpt());
      int exitOptionCount = (sProportionalExits != null ? 1 : 0)
          + (sAbsoluteExits != null ? 1 : 0) + (bRandomExits ? 1 : 0);
      if (exitOptionCount > 1) {
        throw new ParseException(String.format("Only one of {--%s, --%s, --%s} allowed.",
          oProportionalExits.getLongOpt(), oAbsoluteExits.getLongOpt(), oRandomExits.getLongOpt()));
      }
      if (sProportionalExits != null) {
        Pair<Point2D, Point2D> exits = parseExits(sProportionalExits);
        exitCutter = new ProportionalExitsCutter(exits.getLeft(), exits.getRight());
      }
      if (sAbsoluteExits != null) {
        Pair<Point2D, Point2D> exits = parseExits(sProportionalExits);
        exitCutter = new ProportionalExitsCutter(exits.getLeft(), exits.getRight());
      }
      if (bRandomExits) {
        exitCutter = new RandomExitsCutter();
      }
    } catch (ParseException e) {
      System.out.println(e.getMessage());
      formatter.printHelp("mazeharvester", options);
      System.exit(1);
    }

    Field field = fieldFactory.build();

    /*
    ExitCutter exitCutter =
        new ProportionalExitsCutter(new Point2D.Double(0.0, 0.1), new Point2D.Double(1.0, 0.9));
    */
    FieldWithExits fieldWithExits = exitCutter.cutExits(field);

    // Bump random seed to see different solution alternatives.
    Random random = new Random(27);

    // Don't print this maze! solveWithStretch mutates the walls.
    Maze firstMaze = Maze.create(random, fieldWithExits);

    SolvedMaze solvedMaze = SolvedMaze.solveWithStretch(firstMaze, 3.0);
    new SVGEmitter("maze.svg").emit(solvedMaze.getMaze());
    new SVGEmitter("solution.svg").emit(solvedMaze);
  }

  private static Dimension parseDimension(String s) throws ParseException {
    String[] parts = s.split(",");
    if (parts.length != 2) {
      throw new ParseException("Cannot parse dimension " + s);
    }
    return new Dimension(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
  }

  private static Pair<Point2D, Point2D> parseExits(String s) throws ParseException {
    String[] parts = s.split(",");
    if (parts.length != 4) {
      throw new ParseException("Cannot parse exit point pair " + s);
    }
    return Pair.of(
      new Point2D.Double(Double.parseDouble(parts[0]), Double.parseDouble(parts[1])),
      new Point2D.Double(Double.parseDouble(parts[2]), Double.parseDouble(parts[3])));
  }
}
