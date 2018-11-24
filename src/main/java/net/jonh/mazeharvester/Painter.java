package net.jonh.mazeharvester;


import com.google.common.collect.ImmutableSet;
import java.awt.Graphics2D;

interface Painter {
  ImmutableSet<Door> getDoorsForBounds();

  void paint(Graphics2D g2d);
}
