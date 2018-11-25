package net.jonh.mazeharvester;


import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.geom.Point2D;
import java.awt.Dimension;
import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import javax.imageio.ImageIO;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Defines a mask as the non-white points in a PNG.
 */
class ImageFieldMask implements FieldMask {
  static ImageFieldMask fromFilename(String filename) throws IOException {
    return new ImageFieldMask(ImageIO.read(new URL(filename)));
  }

  final BufferedImage image;

  ImageFieldMask(BufferedImage image) {
    this.image = image;
  }

  public Dimension getMaskSize() {
    return new Dimension(image.getWidth(), image.getHeight());
  }

  public boolean admitRoom(Point2D point) {
    int x = (int) point.getX();
    int y = (int) point.getY();

    if (0 <= x && x < image.getWidth() && 0 <= y && y < image.getHeight()) {
      return image.getRGB(x, y) != Color.WHITE.getRGB();
    }
    return false; // outside the image.
  }
}
