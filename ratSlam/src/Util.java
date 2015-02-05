import java.awt.image.BufferedImage;
import java.awt.image.Raster;


public class Util {

	// Creates 1-dimensional arrays that mimic MATLAB colon operator
	// HK EDIT (to allow for range that doesn't start with 1)
	public static int[] setRange(int lower, int upper) {
		int[] toReturn = new int[upper - lower + 1];
		for (int i = 0, j = lower; i < toReturn.length && j <= upper; i++, j++) {
			toReturn[i] = j;
		}
		return toReturn;
	}
	
	public static int[] setRange(int range)
	{
		int[] toReturn = new int[range];
		for (int i = 0; i < range; i++)
		{
			toReturn[i] = i;
		}
		return toReturn;
	}
	
	public static int [] doubleToIntArray (double [] arr) {
		int [] retIntArray = new int [arr.length];
		for (int i = 0; i<arr.length;i++) {
			retIntArray [i] = (int) arr [i];
		}
		return retIntArray;
	}
	
	public static int [][] get2DarrayIntsFromImg(BufferedImage img) {
		// convert a BufferedImage to a 2D array of int like in Matlab
		try {
			Raster raster=img.getData();
			int w=raster.getWidth(),h=raster.getHeight();
			int pixels[][]=new int[h][w];
			for (int x=0;x<w;x++) {
				for(int y=0;y<h;y++) {
					pixels[y][x]=raster.getSample(x,y,0);
				}
			}
			return pixels;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
