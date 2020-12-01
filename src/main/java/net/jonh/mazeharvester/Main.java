package net.jonh.mazeharvester;


import org.apache.commons.lang3.tuple.Pair;
import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.util.Vector;
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
  static class Configuration {
    public FieldFactory fieldFactory = null;
    public ExitCutter exitCutter = null;
    public StretchOptions stretchOptions = null;
    public Random random = new Random(1);
    public PaperOptions paperOptions = null;
  }

  public static void main(String[] args) throws IOException {
    Configuration configuration;
    try {
      configuration = parseArgs(args);
      renderOneMaze(configuration);
    } catch (ParseException e) {
      System.out.println(e.getMessage());
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp("mazeharvester", new OptionsPackage().options);
      System.exit(1);
    }
  }

  static private Configuration parseArgs(String[] args) throws IOException, ParseException {
    CommandLineParser parser = new DefaultParser();
    CommandLine cmd;
    PaperOptions.Builder paperOptionsBuilder = PaperOptions.builder();
    StretchOptions.Builder stretchOptionsBuilder = StretchOptions.builder();
    Configuration configuration = new Configuration();

      // ./gradlew run --args='--size=60,30 --pattern=hexagon'

    OptionsPackage op = new OptionsPackage();
    cmd = parser.parse(op.options, args);

    FieldMask fieldMask = new FieldMask.NoMask();
    String sMask = cmd.getOptionValue(op.oMask.getLongOpt());
    if (sMask != null) {
      fieldMask = ImageFieldMask.fromFilename("file:" + sMask);
    }
    
    String sSize = cmd.getOptionValue(op.oSize.getLongOpt());
    if (sSize != null) {
      fieldMask = fieldMask.scaleTo(parseDimension(sSize));
    }

    String sSeed = cmd.getOptionValue(op.oSeed.getLongOpt());
    if (sSeed != null) {
      configuration.random = new Random(Integer.parseInt(sSeed));
    }

    if (fieldMask.getMaskSize().equals(new Dimension(0, 0))) {
      throw new ParseException(
          String.format("Maze size must be specified with either --%s or --%s",
            op.oMask.getLongOpt(), op.oSize.getLongOpt()));
    }

    configuration.fieldFactory = SquareFieldFactory.create(fieldMask);
    String sPattern = cmd.getOptionValue(op.oPattern.getLongOpt());
    if (sPattern == null || sPattern.equals("square")) {
      configuration.fieldFactory = SquareFieldFactory.create(fieldMask);
    } else if (sPattern.equals("hexagon")) {
      configuration.fieldFactory = HexFieldFactory.create(fieldMask);
    } else if (sPattern.equals("triangle")) {
      configuration.fieldFactory = TriangleFieldFactory.create(fieldMask);
    } else {
      throw new ParseException(String.format("Unsupported %s %s", op.oPattern, sPattern));
    }

    String sProportionalExits = cmd.getOptionValue(op.oProportionalExits.getLongOpt());
    String sAbsoluteExits = cmd.getOptionValue(op.oAbsoluteExits.getLongOpt());
    boolean bRandomExits = cmd.hasOption(op.oRandomExits.getLongOpt());
    int exitOptionCount = (sProportionalExits != null ? 1 : 0)
        + (sAbsoluteExits != null ? 1 : 0) + (bRandomExits ? 1 : 0);
    if (exitOptionCount > 1) {
      throw new ParseException(String.format("Only one of {--%s, --%s, --%s} allowed.",
        op.oProportionalExits.getLongOpt(), op.oAbsoluteExits.getLongOpt(), op.oRandomExits.getLongOpt()));
    }
    if (sProportionalExits != null) {
      Pair<Point2D, Point2D> exits = parseExits(sProportionalExits);
      configuration.exitCutter = NearestExitsCutter.fromProportional(
          fieldMask.getMaskSize(), exits.getLeft(), exits.getRight());
    }
    if (sAbsoluteExits != null) {
      Pair<Point2D, Point2D> exits = parseExits(sAbsoluteExits);
      Point2D left = configuration.fieldFactory.cellToPaper(exits.getLeft());
      Point2D right = configuration.fieldFactory.cellToPaper(exits.getRight());
      configuration.exitCutter = NearestExitsCutter.fromAbsolute(left, right);
    }
    if (bRandomExits) {
      configuration.exitCutter = new RandomExitsCutter(configuration.random);
    }
    if (configuration.exitCutter == null) {
      configuration.exitCutter = NearestExitsCutter.fromProportional(fieldMask.getMaskSize(),
          new Point2D.Double(0.0, 0.9), new Point2D.Double(1.0, 0.1));
    }

    String sStretch = cmd.getOptionValue(op.oStretch.getLongOpt());
    if (sStretch != null) {
      stretchOptionsBuilder.setStretch(Double.parseDouble(sStretch));
    }

    String sDeviationPercentile = cmd.getOptionValue(op.oDeviationPercentile.getLongOpt());
    if (sDeviationPercentile != null) {
      stretchOptionsBuilder.setDeviationPercentile(Double.parseDouble(sDeviationPercentile));
    }

    String sPaperSize = cmd.getOptionValue(op.oPaperSize.getLongOpt());
    if (sPaperSize != null) {
      paperOptionsBuilder.setPaperSize(sPaperSize);
    }

    String sMargin = cmd.getOptionValue(op.oMargin.getLongOpt());
    if (sMargin != null) {
      paperOptionsBuilder.setMargin(sMargin);
    }
    configuration.paperOptions = paperOptionsBuilder.build();
    configuration.stretchOptions = stretchOptionsBuilder.build();
    return configuration;
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
   */
  static void renderOneMaze(Configuration configuration) throws IOException {
    Field field = configuration.fieldFactory.build();

    FieldWithExits fieldWithExits = configuration.exitCutter.cutExits(field);

    // Don't print this maze! solveWithStretch mutates the walls.
    Maze firstMaze = Maze.create(configuration.random, fieldWithExits);

    SolvedMaze solvedMaze = SolvedMaze.solveWithStretch(firstMaze, configuration.stretchOptions);
    new SVGEmitter("maze.svg", configuration.paperOptions).emit(solvedMaze.getMaze());
    new SVGEmitter("solution.svg", configuration.paperOptions).emit(solvedMaze);
  }

  static class StackedSolutions implements SegmentPainter {
    public Vector<SolvedMaze> solvedMazes = new Vector();

    public void paint(SegmentGraphics segmentGraphics) {
      int i=0;
      for (SolvedMaze solvedMaze : solvedMazes) {
        //int red = i*255 / (solvedMazes.size()-1);
        int red = 255 - i;
        int green = 255 - red;
        Color color = new Color(red, green, 0);
        System.out.println("i " + i + " color " + color);
        solvedMaze.paintSolution(segmentGraphics, color);
        i++;
      }
    }
  }

  static void renderManyMazesForDebug(String[] args) throws IOException, ParseException {
    // Reusing the same configuration with different random objects is a bit of cheating,
    // because configuration.exitCutter tucks a copy of random if you use the randomExits flag.
    // But this is a one-off for debugging.

    Configuration configuration = null;
    StackedSolutions stackedSolutions = new StackedSolutions();
    for (int seed=0; seed<32; seed++) {
      configuration = parseArgs(args);
      System.out.println("Seed " + seed + "\n");
      configuration.random = new Random(seed);

      Field field = configuration.fieldFactory.build();
      FieldWithExits fieldWithExits = configuration.exitCutter.cutExits(field);
      Maze firstMaze = Maze.create(configuration.random, fieldWithExits);
      // without stretch:
//      SolvedMaze solvedMaze = SolvedMaze.solve(firstMaze);
      // with stretch:
      SolvedMaze solvedMaze = SolvedMaze.solveWithStretch(firstMaze, configuration.stretchOptions);

      // extract the solution svg...
      stackedSolutions.solvedMazes.add(solvedMaze);
    }
    // then paint.
    new SVGEmitter("stackedSolutions.svg", configuration.paperOptions).emit(stackedSolutions);
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
