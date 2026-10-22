package net.momirealms.craftengine.core.block.shelf;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ShelfLayoutTest {
 @Test void selectsFiveFloorsAndThreeColumns(){ assertEquals(14,ShelfLayout.slot(1.8,4.7,false));assertEquals(0,ShelfLayout.slot(-.8,.3,false)); }
 @Test void rejectsItemsOutsideShelf(){assertFalse(ShelfLayout.fits(0,2.5,false));assertFalse(ShelfLayout.fits(0,1.2,true));assertTrue(ShelfLayout.fits(1,2.5,false));}
 @Test void largeItemReservesAdjacentSpace(){assertTrue(ShelfLayout.overlaps(1,2.2,0,.8,false));assertFalse(ShelfLayout.overlaps(1,.8,0,.8,false));assertFalse(ShelfLayout.overlaps(1,3,4,3,false));}
 @Test void invalidDimensionsCannotPass(){assertThrows(IllegalArgumentException.class,()->ShelfLayout.fits(0,Float.NaN,false));assertThrows(IllegalArgumentException.class,()->ShelfLayout.fits(15,.5,false));}
}
