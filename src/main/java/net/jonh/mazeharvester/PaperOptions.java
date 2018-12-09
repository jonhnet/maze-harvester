package net.jonh.mazeharvester;


import org.apache.commons.lang3.tuple.Pair;
import java.awt.geom.Point2D;
import org.apache.commons.cli.ParseException;

public class PaperOptions {
  private static Pair<Double, String> parseUnits(String specifier) throws ParseException {
    double scale;
    if (specifier.endsWith("in")) {
      scale = 1.0;
    } else if (specifier.endsWith("cm")) {
      scale = 1.0/2.54;
    } else if (specifier.endsWith("mm")) {
      scale = 0.1/2.54;
    } else {
      throw new ParseException("Specifier must include units <in|cm|mm>");
    }
    return Pair.of(scale, specifier.substring(0, specifier.length() - 2));
  }

  private static Point2D.Double parsePaper(String specifier) throws ParseException {
    if (specifier.toLowerCase().equals("a4")) {
      return parsePaper("210x297mm");
    }
    if (specifier.toLowerCase().equals("letter")) {
      return parsePaper("8.5x11in");
    }
    Pair<Double, String> parts = parseUnits(specifier);
    double scale = parts.getLeft();
    String[] dims = parts.getRight().split("x");
    double w = Double.parseDouble(dims[0]);
    double h = Double.parseDouble(dims[1]);
    return new Point2D.Double(scale*w, scale*h);
  }

  private static double parseMargin(String specifier) throws ParseException {
    Pair<Double, String> parts = parseUnits(specifier);
    double scale = parts.getLeft();
    double measure = Double.parseDouble(parts.getRight());
    return measure * scale;
  }

  private Point2D.Double paperSizeInches;
  private double marginInches = 0.5;

  private PaperOptions() {
    try {
      paperSizeInches = parsePaper("letter");
    } catch (ParseException ex) {
      // can't happen.
    }
  }

  public Point2D getPaperSizeInches() { return paperSizeInches; }
  public double getMarginInches() { return marginInches; }

  public static Builder builder() { return new Builder(); }
  public static class Builder {
    private PaperOptions options = new PaperOptions();
    private Builder() {
    }
    public void setPaperSize(String specifier) throws ParseException {
      options.paperSizeInches = parsePaper(specifier);
    }
    public void setMargin(String specifier) throws ParseException {
      options.marginInches = parseMargin(specifier);
    }
    public PaperOptions build() {
      return options;
    }
  }
}
