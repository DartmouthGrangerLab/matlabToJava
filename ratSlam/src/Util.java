import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;


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
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static ArrayDeque layerRowWise(double twoDArray [][]) {
		//for circular shift down or up
		int xlen = twoDArray.length;
		int ylen = twoDArray[0].length;
		ArrayDeque rows = new ArrayDeque();
		ArrayDeque row = new ArrayDeque();
	    for (int y=0; y<ylen; y++) {
	    	for (int x=0; x<xlen; x++) {
	    		row.add(twoDArray[y][x]);
	    	}
	    	rows.add(row.clone());
	    	row.clear();
	    }
	    return rows;
	}
		
//	@SuppressWarnings({ "unchecked", "rawtypes" })
//	public static ArrayDeque layerColWise(double twoDArray [][]) {
//		//for circular shift left or right
//		int xlen = twoDArray.length;
//		int ylen = twoDArray[0].length;
//		ArrayDeque cols = new ArrayDeque();
//		ArrayDeque col = new ArrayDeque();
//	    for (int y=0; y<ylen; y++) {
//	    	for (int x=0; x<xlen; x++) {
//	    		col.add(twoDArray[x][y]);
//	    	}
//	    	cols.add(col.clone());
//	    	col.clear();
//	    }
//	    return cols;
//	}
	
	@SuppressWarnings({ "rawtypes" })
	public static double [][] arrayBack(ArrayDeque ad) {
		//for circular shift left or right
		int xlen = (int) ((ArrayDeque) ad.getFirst()).size();
		int ylen = ((ArrayDeque) ad).size();
		int x = 0;
		int y = 0;
		
		double [][] twoDArray = new double [xlen][ylen];
		ArrayDeque row = new ArrayDeque();
		while (!ad.isEmpty()){
			row = (ArrayDeque) ad.removeFirst();
			while (!row.isEmpty()){
				twoDArray[y][x] = (double) row.removeFirst();
				x++;
			}
			y++;
			x=0;
		}
	    return twoDArray;
	}
	
	@SuppressWarnings({ "rawtypes" })
	public static double [][][] arrayBack3D(ArrayDeque ad) {
		double [][] temp = (double[][]) ad.getFirst();
		int xlen = temp.length;
		int ylen = temp[0].length;
		int zlen = ad.size();
		int z=0;

		double [][][] threeDArray = new double [xlen][ylen][zlen];

		for(Iterator itr = ad.iterator();itr.hasNext();){
			temp = (double[][]) itr.next();
			for (int x = 0; x<xlen; x++){
				for (int y = 0; y<ylen; y++){
					threeDArray[x][y][z] = temp[x][y];
				}
			}
			z++;
		}
	    return threeDArray;
	}
	
	@SuppressWarnings({ "rawtypes", "unused", "unchecked" })
	public static ArrayDeque shiftLeftRight (ArrayDeque layer, int shift) {
		int ylen = layer.size();
		ArrayDeque row = new ArrayDeque();
		ArrayDeque newLayer = new ArrayDeque();
		while (!layer.isEmpty()) {
			row = (ArrayDeque) layer.removeFirst();
			if (shift >0 ){
				//shift right
				for (int n=0; n<shift; n++){
					double chop = (double) row.removeLast();
					row.addFirst(chop);
				}
			} else {
				//shift left
				for (int n=0; n>shift; n--){
					double chop = (double) row.removeFirst();
					row.addLast(chop);
				}					
			}
			newLayer.add(row.clone());
		}
		return newLayer;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static ArrayDeque shiftUpDown (ArrayDeque layerRows, int shift) {
		if (shift >0 ){
			//shift down
			for (int n=0; n<shift; n++){
				ArrayDeque chop = (ArrayDeque) layerRows.removeLast();
				layerRows.addFirst(chop);
			}
		} else {
			//shift up
			for (int n=0; n>shift; n--){
				ArrayDeque chop = (ArrayDeque) layerRows.removeFirst();
				layerRows.addLast(chop);
			}					
		}
		
		return layerRows;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static ArrayDeque shiftZ (ArrayDeque layers, int shift) {
		if (shift > 0 ){
			//shift down
			for (int n=0; n<shift; n++){
				double [][] chop = (double[][]) layers.removeLast();
				layers.addFirst(chop);
			}
		} else {
			//shift up
			for (int n=0; n>shift; n--){
				double [][] chop = (double[][]) layers.removeFirst();
				layers.addLast(chop);
			}					
		}
		return layers;
	}
	
	// actual circshift equivalent for 3 dimensional arrays
	@SuppressWarnings({ "rawtypes", "unused", "unchecked" })
	// dim shift amount is [rowShiftAmount, colShiftAmount, layerShiftAmount] like Matlab
	public static double[][][] circshift(double[][][] array, int[] dimAndShiftAmount) {	
		int xlen = array.length;
		int ylen = array[0].length;
		int zlen = array[0][0].length;
		
		if (dimAndShiftAmount[0] != 0) {
//			System.out.println("y dimension shift needed.");
			double [][] layer = new double [xlen][ylen];
			for (int z = 0; z<zlen; z++){
				for (int x = 0; x<xlen; x++){
					for (int y = 0; y<ylen; y++){
						layer[x][y] = array[x][y][z];
					}
				}
				ArrayDeque rotLayer = layerRowWise(layer);//layerColWise(layer);
				rotLayer = shiftUpDown(rotLayer, dimAndShiftAmount[0]);
				layer = arrayBack(rotLayer);
				for (int x = 0; x<xlen; x++){
					for (int y = 0; y<ylen; y++){
						array[x][y][z] = layer[x][y];
					}
				}
			}
		}
		
		if (dimAndShiftAmount[1] != 0) {
//			System.out.println("x dimension shift needed.");
			double [][] layer = new double [xlen][ylen];
			for (int z = 0; z<zlen; z++){
				for (int x = 0; x<xlen; x++){
					for (int y = 0; y<ylen; y++){
						layer[x][y] = array[x][y][z];
					}
				}
				ArrayDeque rotLayer = layerRowWise(layer);
				rotLayer = shiftLeftRight(rotLayer, dimAndShiftAmount[1]);
				layer = arrayBack(rotLayer);
				for (int x = 0; x<xlen; x++){
					for (int y = 0; y<ylen; y++){
						array[x][y][z] = layer[x][y];
					}
				}
			}
		}

		if (dimAndShiftAmount[2] != 0) {
//			System.out.println("z dimension shift needed.");
			double [][] layer = new double [xlen][ylen];
			ArrayDeque rotLayers = new ArrayDeque();
			for (int z = 0; z<zlen; z++){
				layer = new double [xlen][ylen];
				for (int x = 0; x<xlen; x++){
					for (int y = 0; y<ylen; y++){
						layer[x][y] = array[x][y][z];
					}
				}
				rotLayers.add(layer);
			}
			rotLayers = shiftZ(rotLayers, dimAndShiftAmount[2]);
			array = arrayBack3D(rotLayers);
		}
		return array;
	}
	
	// Think about having temp storage for poseCells.weights that use circshift, then have poseCells.weights equal temp storage
	public static double[][] circshift(double[][] check, int[] dir_Use) {
		if (dir_Use[0] == 1) {
			shiftDown(check);
			if (dir_Use[1] == 1) {	
				shiftLeft(check);
			}
		} else if (dir_Use[0] == 0) {
			if (dir_Use[1] == 1) {
				shiftLeft(check);
			}
		}
		return check;
	}

	// B = rot90(A,k) rotates matrix A counterclockwise by k*90 degrees, where k is an integer.
	public static double[][] rot90(double[][][] use, int constant, int numRotations) {
		double[][] toReturn = new double [use.length][use[0].length];
		int size = toReturn.length;
		int size2 = toReturn[0].length;
		for (int num = 0; num < numRotations; num++) {
			for(int i=0; i<size; i++) {
				for(int j=0; j<size2; j++) {
					toReturn[i][j] = use[j][size-i-1][constant];
				}
			}
		}
		return toReturn;
	}

	public static double[][] rot90(double[][] use, int numRotations) {
		double[][] toReturn = new double [use.length][use[0].length];
		int size = toReturn.length;
		int size2 = toReturn[0].length;
		for (int num = 0; num < numRotations; num++) {
			for(int i=0; i<size; i++) {
				for(int j=0; j<size2; j++) {
					toReturn[i][j] = use[j][size-i-1];
				}
			}
		}
		return toReturn;
	}
	
	public static double[][][] multiply_elements(double[][][] toChange, double toMultiply) {
		for (int i = 0; i < toChange.length - 1; i++) {
			for (int j = 0; j < toChange[0].length - 1; j++) {
				for (int k = 0; k < toChange[0][0].length - 1; k++) {
					toChange[i][j][k] *= toMultiply;
				}
			}
		}
			
		return toChange;
	}
	
	public static double[][][] addArrays(double[][][] arrA, double[][][] arrB) {
		for (int i = 0; i < arrA.length - 1; i++) {
			for (int j = 0; j < arrA[0].length - 1; j++) {
				for (int k = 0; k < arrA[0][0].length - 1; k++) {
					arrA[i][j][k] += arrB[i][j][k];
				}
			}
		}
			
		return arrA;
	}
	
	public static double[][] multiply_elements(double[][] toChange, double toMultiply) {
		for (int i = 0; i < toChange.length - 1; i++) {
			for (int j = 0; j < toChange[0].length - 1; j++) {
				toChange[i][j] *= toMultiply;
			}
		}
			
		return toChange;
	}

	public double[][][] shiftDown(double[][][] toReturn, int constant)
	{
		double[][][] temp = toReturn;
		for (int i = 0; i < toReturn.length - 1; i++)
		{
			for (int j = 0; j < toReturn[0].length - 1; j++)
			{
				if (++i == toReturn.length)
				{
					toReturn[1][j][constant] = temp[i][j][constant];
				}
				else
				{
					toReturn[i+1][j][constant] = temp[i][j][constant];
				}
			}
		}
		return toReturn;
	}

	public static double[][] shiftDown(double[][] toReturn) {
		double[][] temp = toReturn;
		for (int i = 0; i < toReturn.length - 1; i++) {
			for (int j = 0; j < toReturn[0].length - 1; j++) {
				if (++i == toReturn[0].length-1) {
					toReturn[0][j] = temp[i][j];
				} else {
					toReturn[i+1][j] = temp[i][j];
				}
			}
		}
		return toReturn;
	}
	
	public double[][][] shiftLeft(double[][][] toReturn, int constant)
	{
		double[][][] temp = toReturn;
		for (int i = 0; i < toReturn.length - 1; i++)
		{
			for (int j = 0; j < toReturn[0].length - 1; j++)
			{
				if (++j == toReturn.length)
				{
					toReturn[i][1][constant] = temp[i][j][constant];
				}
				else
				{
					toReturn[i][j+1][constant] = temp[i][j][constant];
				}
			}
		}
		return toReturn;
	}
	
	public static double[][] shiftLeft(double[][] toReturn) {
		double[][] temp = toReturn;
		for (int i = 0; i < toReturn.length - 1; i++) {
			for (int j = 0; j < toReturn[0].length - 1; j++) {
				if (++j == toReturn.length -1 ) {
					toReturn[i][1] = temp[i][j];
				} else {
					toReturn[i][j+1] = temp[i][j];
				}
			}
		}
		return toReturn;
	}
}
