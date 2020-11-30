package net.jonh.mazeharvester;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

class OptionsPackage {
  Option oMask = new Option(null, "mask-filename", true, "PNG file to use as mask to shape maze");
  Option oSize = new Option(null, "size", true, "Maze size <w,h>");
  Option oPattern = new Option(null, "pattern", true, "Room pattern <square|hexagon|triangle>");
  Option oProportionalExits = new Option(null, "proportionalExits", true, "Specify exits on unit square <x1,y1,x2,y2>");
  Option oAbsoluteExits = new Option(null, "absoluteExits", true, "Specify exits in maze coordinates <x1,y1,x2,y2>");
  Option oRandomExits = new Option(null, "randomExits", false, "Select exits randomly");
  Option oStretch = new Option(null, "stretch", true, "Desired solution path length, as a ratio of maze diagonal");
  Option oDeviationPercentile = new Option(null, "deviationPercentile", true, "How much to stretch path towards a wall on each stretch. 1.0 means add the very longest path we can find; 0.8 means take an 80th-percentile path.");
  Option oSeed = new Option(null, "seed", true, "Integer random seed. Bump to create different solutions in otherwise equal configurations.");
  Option oPaperSize = new Option(null, "paper", true, "Paper size <letter|a4|WxH<in|cm|mm>>, such as 11x17in");
  Option oMargin = new Option(null, "margin", true, "Paper margin <X<in|cm|mm>>, such as 0.5in");
  Options options;

  OptionsPackage() {
    options = new Options();
    options.addOption(oMask);
    options.addOption(oSize);
    options.addOption(oPattern);
    options.addOption(oProportionalExits);
    options.addOption(oAbsoluteExits);
    options.addOption(oRandomExits);
    options.addOption(oStretch);
    options.addOption(oDeviationPercentile);
    options.addOption(oSeed);
    options.addOption(oPaperSize);
    options.addOption(oMargin);
  }
}
