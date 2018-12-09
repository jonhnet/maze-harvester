package net.jonh.mazeharvester;


import java.awt.Color;
import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

class SVGEmitter {
  static final double pixelsPerInch = 90;

  PaperOptions paperOptions;
  String outputFilename;
  Point2D.Double paperSizePixels;

  // bounds computed by computeBounds()
  // "Rooms" are the Room units in a SquareField
  double scalePixelsPerRoom;
  Point2D pageOffsetRooms;

  SVGEmitter(String outputFilename, PaperOptions paperOptions) {
    this.outputFilename = outputFilename;
    this.paperOptions = paperOptions;
    Point2D paperSizeInches = paperOptions.getPaperSizeInches();
    this.paperSizePixels = new Point2D.Double(
          paperSizeInches.getX() * pixelsPerInch, paperSizeInches.getY() * pixelsPerInch);
  }

  void computeBounds(Rectangle2D bbMazeCoords) {
    // Scale the bounding box up to add borders.
    Rectangle2D scaledBBox = bbMazeCoords;

    // Rotate the paper if the maze doesn't have the same aspect ratio.
    boolean paperIsTall = paperSizePixels.getX() < paperSizePixels.getY();
    boolean mazeIsTall = scaledBBox.getWidth() < scaledBBox.getHeight();

    if (paperIsTall != mazeIsTall) {
      this.paperSizePixels = new Point2D.Double(paperSizePixels.getY(), paperSizePixels.getX());
    }

    // Account for the margins.
    double marginPixels = paperOptions.getMarginInches() * pixelsPerInch;
    Point2D availablePaperPixels = new Point2D.Double(
      paperSizePixels.getX() - marginPixels, paperSizePixels.getY() - marginPixels);

    scalePixelsPerRoom =
        Math.min(
            availablePaperPixels.getX() / scaledBBox.getWidth(),
            availablePaperPixels.getY() / scaledBBox.getHeight());

    double marginRooms = marginPixels / scalePixelsPerRoom;
    Point2D paperRooms = new Point2D.Double(
      paperSizePixels.getX() / scalePixelsPerRoom, paperSizePixels.getY() / scalePixelsPerRoom);
    System.out.println(String.format("sbbox %s marginRooms %s scaleroomsPerPixel %s",
      scaledBBox, marginRooms, scalePixelsPerRoom));
    pageOffsetRooms = new Point2D.Double(
      scaledBBox.getX() - 0.5*(paperRooms.getX()-scaledBBox.getWidth()),
      scaledBBox.getY() - 0.5*(paperRooms.getY()-scaledBBox.getHeight()));
  }

  // https://xmlgraphics.apache.org/batik/using/svg-generator.html
  public void emit(SegmentPainter painter) throws IOException {
    DOMImplementation domImpl = GenericDOMImplementation.getDOMImplementation();

    // Create an instance of org.w3c.dom.Document.
    String svgNS = "http://www.w3.org/2000/svg";
    Document document = domImpl.createDocument(svgNS, "svg", null);

    // Create an instance of the SVG Generator.
    SVGGraphics2D svgGenerator = new SVGGraphics2D(document);
    SegmentSorter segmentSorter = new SegmentSorter();
    segmentSorter.collect(painter);

    computeBounds(segmentSorter.getBounds());
    svgGenerator.setSVGCanvasSize(
      new Dimension((int) paperSizePixels.getX(), (int) paperSizePixels.getY()));

    // white background (before the maze-coordinates transform)
    svgGenerator.setPaint(Color.WHITE);
    svgGenerator.fill(
        new Rectangle2D.Double(
            0, 0, paperSizePixels.getX(), paperSizePixels.getY()));

    // Set up the page transform so painters can paint in their
    // Room/Door coordinate system.
    svgGenerator.scale(scalePixelsPerRoom, scalePixelsPerRoom);
    svgGenerator.translate(-pageOffsetRooms.getX(), -pageOffsetRooms.getY());

    // Paint segments into the SVG
    segmentSorter.paint(svgGenerator);

    // Finally, stream out SVG to the standard output using
    // UTF-8 encoding.
    boolean useCSS = true; // we want to use CSS style attributes
    OutputStream outputStream = new FileOutputStream(outputFilename);
    Writer out = new OutputStreamWriter(outputStream, "UTF-8");
    svgGenerator.stream(out, useCSS);
  }
}
