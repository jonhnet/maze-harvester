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
![alt text](https://github.com/jonhnet/maze-harvester/raw/master/doc/star-masked.png "Maze in a star")
![alt text](https://github.com/jonhnet/maze-harvester/raw/master/doc/smiley.png "Maze in a smiley face")

You can specify the locations of the exits.

```
./gradlew run --args='--size=60,60 --paper 4x4in --mask samples/fat-star.png --proportionalExits 0,1,1,1'
inkscape solution.svg -z -e doc/star-exits.png -w400 -h400
```
![alt text](https://github.com/jonhnet/maze-harvester/raw/master/doc/star-exits.png "Exits at star points")

You can select how convoluted the solution path is, from "pretty easy"
through "labyrinth".

```
./gradlew run --args='--size=50,50 --paper 4x4in --proportionalExits 0,0,1,0 --stretch 0'
inkscape solution.svg -z -e doc/easy.png -w400 -h400
./gradlew run --args='--size=50,50 --paper 4x4in --proportionalExits 0,0,1,0 --stretch 7'
inkscape solution.svg -z -e doc/hard.png -w400 -h400
```
![alt text](https://github.com/jonhnet/maze-harvester/raw/master/doc/easy.png "Easy solution")
![alt text](https://github.com/jonhnet/maze-harvester/raw/master/doc/hard.png "Hard solution")

Don't like the solution? Bump the random seed to explore others.

```
./gradlew run --args='--size=50,50 --paper 4x4in --proportionalExits 0,0.3,1,0.7 --seed 2'
inkscape solution.svg -z -e doc/seed2.png -w250 -h250
./gradlew run --args='--size=50,50 --paper 4x4in --proportionalExits 0,0.3,1,0.7 --seed 3'
inkscape solution.svg -z -e doc/seed3.png -w250 -h250
./gradlew run --args='--size=50,50 --paper 4x4in --proportionalExits 0,0.3,1,0.7 --seed 4'
inkscape solution.svg -z -e doc/seed4.png -w250 -h250
```
![alt text](https://github.com/jonhnet/maze-harvester/raw/master/doc/seed2.png "Seed 2")
![alt text](https://github.com/jonhnet/maze-harvester/raw/master/doc/seed3.png "Seed 3")
![alt text](https://github.com/jonhnet/maze-harvester/raw/master/doc/seed4.png "Seed 4")

maze-harvester knows about paper sizes `--paper a4 --margin 3cm`,
and (of course) it can emit E-sized paper for your HP 7585B:
`--paper 36x48in --margin 1.5in`

TODO
- PDF, PNG output modes?
	https://xmlgraphics.apache.org/batik/javadoc/org/apache/batik/apps/rasterizer/SVGConverter.html
- How about some tests?
- add big-rooms
