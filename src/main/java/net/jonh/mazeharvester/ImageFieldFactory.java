package net.jonh.mazeharvester;


import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import javax.imageio.ImageIO;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Creates a field of squares, but uses a black-and-white PNG mask to remove squares to specify
 * hand-crafted geometries.
 */
class ImageFieldFactory extends SquareFieldFactory {
  static BufferedImage imageFromFilename(String filename) throws IOException {
    return ImageIO.read(new URL(filename));
  }

  final BufferedImage image;

  ImageFieldFactory(BufferedImage image) {
    super(image.getWidth(), image.getHeight());
    this.image = image;
  }

  public void buildRooms() {
    super.buildRooms();

    HashSet<Integer> seenRGBs = new HashSet<>();
    for (int y = 0; y < h; y++) {
      for (int x = 0; x < w; x++) {
        seenRGBs.add(image.getRGB(x, y));
        if (image.getRGB(x, y) == 0xffffffff) {
          map.remove(Pair.<Integer, Integer>of(x, y));
        }
      }
    }
  }
}
