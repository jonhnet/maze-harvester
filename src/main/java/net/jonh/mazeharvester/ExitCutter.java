package net.jonh.mazeharvester;

/** Chops a couple doors out of a field to provide an entrance and exit. */
interface ExitCutter {
  FieldWithExits cutExits(Field field);
}
