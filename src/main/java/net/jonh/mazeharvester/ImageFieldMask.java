package net.jonh.mazeharvester;


import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.ColorConvertOp;
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
    this.image = toGray(image);
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

  public static BufferedImage toGray(BufferedImage input) {
    BufferedImage grayImage = new BufferedImage(input.getWidth(), input.getHeight(),
       BufferedImage.TYPE_BYTE_GRAY);
    ColorConvertOp op = new ColorConvertOp(input.getColorModel().getColorSpace(),
       grayImage.getColorModel().getColorSpace(), null);
    op.filter(input, grayImage);
    return grayImage;
  }

  /** Returns a mask with the same image, resized to (w,h). */
  public ImageFieldMask scaleTo(int w, int h) {
    BufferedImage grayImage = this.image;
    BufferedImage scaledImage = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
    AffineTransform at = new AffineTransform();
    at.scale(((double)w) / grayImage.getWidth(), ((double) h) / grayImage.getHeight());
    AffineTransformOp scaleOp = new AffineTransformOp(at, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
    scaleOp.filter(grayImage, scaledImage);
    return new ImageFieldMask(scaledImage);
  }
}
