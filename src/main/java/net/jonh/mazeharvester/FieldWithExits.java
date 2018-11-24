package net.jonh.mazeharvester;


import com.google.common.collect.ImmutableSet;

class FieldWithExits {
  private Field field;
  private ImmutableSet<Door> exitDoors = ImmutableSet.of();

  public FieldWithExits(Field field, ImmutableSet<Door> exitDoors) {
    this.field = field;
    this.exitDoors = exitDoors;
  }

  public Field getField() {
    return field;
  }

  public ImmutableSet<Door> getExitDoors() {
    return exitDoors;
  }
}
