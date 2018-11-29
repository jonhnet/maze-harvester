# maze-harvester
Grow a custom vector maze with a command line; print as SVG.

I use this program to plot huge 3 x 4 foot mazes on my 1985 pen plotter.

Mazes can live on a rectangular grid, a hexagonal grid, or a triangular grid.
(The architecture makes it easy to even extend to an irregular pattern, like
a random Voronoi diagram.)

The maze can have an arbitrary boundary and holes
in the middle, which you can specify by drawing a black-and-white PNG file.

You can specify the locations of the exits, and you can select how
convoluted the solution path is, from "pretty easy" through "labyrinth".


TODO
- Path convoluter spends all its budget on one long excursion.
- How about some tests?
- add voids and big-rooms
- command line flags, I guess!
