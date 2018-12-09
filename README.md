# maze-harvester
Grow a custom vector maze with a command line; print as SVG.

```
./gradlew run --args='--size=30,30 --paper 4x4in'
inkscape solution.svg -z -e doc/basic.png -w400 -h400
```
![alt text](https://github.com/jonhnet/maze-harvester/raw/master/doc/basic.png "Basic square maze")

I use this program to plot huge 3 x 4 foot mazes on my 1985 pen plotter.

Mazes can live on a rectangular grid, a hexagonal grid, or a triangular grid.
(The architecture makes it easy to even extend to an irregular pattern, like
a random Voronoi diagram.)

```
./gradlew run --args='--size=30,30 --paper 4x4in --pattern hexagon'
inkscape solution.svg -z -e doc/hexagon.png -w400 -h400
./gradlew run --args='--size=30,30 --paper 4x4in --pattern triangle --stretch 5.0'
inkscape solution.svg -z -e doc/triangle.png -w400 -h400
```
![alt text](https://github.com/jonhnet/maze-harvester/raw/master/doc/hexagon.png "Maze of hexagons")
![alt text](https://github.com/jonhnet/maze-harvester/raw/master/doc/triangle.png "Maze of triangles")

The maze can have an arbitrary boundary and holes
in the middle, which you can specify by drawing a black-and-white PNG file.

```
./gradlew run --args='--size=60,60 --paper 4x4in --mask samples/fat-star.png  -stretch 1.2'
inkscape solution.svg -z -e doc/star-masked.png -w400 -h400
./gradlew run --args='--size=60,60 --paper 4x4in --mask samples/smiley.png'
inkscape solution.svg -z -e doc/smiley.png -w400 -h400
```
![alt text](https://github.com/jonhnet/maze-harvester/raw/master/doc/hexagon.png "Maze in a star")
![alt text](https://github.com/jonhnet/maze-harvester/raw/master/doc/smiley.png "Maze in a smiley face")

You can specify the locations of the exits, and you can select how
convoluted the solution path is, from "pretty easy" through "labyrinth".


TODO
- PDF, PNG output modes?
	https://xmlgraphics.apache.org/batik/javadoc/org/apache/batik/apps/rasterizer/SVGConverter.html
- How about some tests?
- add big-rooms
