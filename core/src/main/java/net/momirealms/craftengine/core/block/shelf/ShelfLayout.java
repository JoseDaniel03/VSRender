package net.momirealms.craftengine.core.block.shelf;
/** Physical occupancy of three columns over five shelves; stand uses a single cell. */
public final class ShelfLayout {
 private ShelfLayout() {}
 public static int slot(double x,double y,boolean invisible){
  if(!Double.isFinite(x)||!Double.isFinite(y))return -1;
  if(invisible)return x>=0&&x<=1&&y>=0&&y<1?0:-1;
  if(x< -1||x>=2||y<0||y>=5)return -1;
  return (int)Math.floor(y)*3+(int)Math.floor(x+1);
 }
 private static void validate(int slot,double width,boolean invisible){
  if(slot<0||slot>=(invisible?1:15)||!Double.isFinite(width)||width<.05||width>3)
   throw new IllegalArgumentException("Invalid shelf slot or width");
 }
 public static boolean fits(int slot,double width,boolean invisible){
  validate(slot,width,invisible);
  double center=invisible?.5:slot%3-.5;
  return center-width/2 >= (invisible?0:-1) && center+width/2 <= (invisible?1:2);
 }
 public static boolean overlaps(int a,double aw,int b,double bw,boolean invisible){
  validate(a,aw,invisible);validate(b,bw,invisible);
  if(a/3!=b/3)return false;
  double ac=invisible?.5:a%3-.5,bc=invisible?.5:b%3-.5;
  return Math.abs(ac-bc)<(aw+bw)/2;
 }
}
