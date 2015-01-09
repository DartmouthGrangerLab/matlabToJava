import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.util.Arrays;


// This class creates the visual template, taking the image 
// and breaking it down into a two-dimensional array

public class Visual_Template 
{
	int IMAGE_Y_SIZE;		
	int IMAGE_X_SIZE;		

	int numvts = 1;
	int prev_vt_id = 1;
	int[] vt_history = {0};

	double VT_GLOBAL_DECAY = 0.1;
	double VT_ACTIVE_DECAY = 1.0;
	double VT_SHIFT_MATCH = 20;
	double VT_MATCH_THRESHOLD = 0.09;
	int[] IMAGE_VT_Y_RANGE = setRange(IMAGE_Y_SIZE);
	int[] IMAGE_VT_X_RANGE = setRange(IMAGE_X_SIZE);

//	int[][] sub_image = {IMAGE_VT_Y_RANGE,IMAGE_VT_X_RANGE};
	int[][] sub_image = new int[IMAGE_Y_SIZE][];
	
	int x_val, y_val, th_val;
	int[][] raw_image = null;
	int[] new_vt_history = null;
	
	Experience [] exps = new Experience [5];
	
	public Visual_Template(int raw_img[][], int x, int y, int th)
	{
		x_val = 0;
		y_val = 0;
		th_val = 0;
		raw_image = raw_img;
	}

	public Visual_Template(BufferedImage img, int x, int y, int th, int vidWidth, int vidHeight, Experience [] exps)
	{
		x_val = 0;
		y_val = 0;
		th_val = 0;
		int [][] intImg = get2DarrayIntsFromImg(img);
		raw_image = intImg;
		IMAGE_X_SIZE = vidWidth;
		IMAGE_Y_SIZE = vidHeight;
		exps = exps;
		
		for (int i = 0; i < IMAGE_Y_SIZE; i++) {
			sub_image[i] = Arrays.copyOfRange(raw_image[i], 0, IMAGE_X_SIZE);
		}
	}
	
	private int [][] get2DarrayIntsFromImg(BufferedImage img) {
		// convert a BufferedImage to a 2D array of int like in Matlab
		try {
			Raster raster=img.getData();
			int w=raster.getWidth(),h=raster.getHeight();
			int pixels[][]=new int[w][h];
			for (int x=0;x<w;x++) {
				for(int y=0;y<h;y++) {
					pixels[x][y]=raster.getSample(x,y,0);
				}
			}
			return pixels;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	

	public void visual_template()
	{
		int vt_id = 0;

		// normalized intensity sums
		int[][] image_x_sums = sub_image.clone();

		// Goes through each pixel of the image, sums up the rows
		// and stores the sum of each row into image_x_sums
//		for (int i = 0; i < sub_image.length; i++)
//		{
//			for(int j = 0; j < sub_image[1].length; i++)
//			{
//				image_x_sums[i][j] = sub_image[i][j];
//			}
//		}

		// adds all values in image_x_sums
		int sumOf_image_x_sums = 0;
		for (int i = 0; i < image_x_sums.length; i++)
		{
			for (int j = 0; j < image_x_sums[i].length; j++)
				sumOf_image_x_sums = image_x_sums[i][j];
		}

		// divides each value in image_x_sums by total sum
		for (int i = 0; i < image_x_sums.length; i++)
		{
			for(int j = 0; j < image_x_sums[i].length; j++)
				image_x_sums[i][j]  = image_x_sums[i][j] / sumOf_image_x_sums;
		}

		// initialize minOffset and minDif
		// DO THIS!!!!
		//int[] minOffset = new int [numvts];
		//int[] minDif = new int [numvts];

		// change parameters
		VT[] vt = new VT[]{};

		//vt[0] = new VT(1,image_x_sums,1.0,31, 31, 19, 1, 1, exp);
		// public VT(int numvts, int[][] img_sums, double decay, int xPc, int yPc, int thPc, int f, int numE, Experience[] exp)
		// get vt down before doing this loop
		for (int k = 0; k < numvts; k++)
		{
			//vt.visTemp
			vt[k].template_decay = vt[k].template_decay - VT_GLOBAL_DECAY;
			if(vt[k].template_decay < 0)
			{
				vt[k].template_decay = 0;
			}
			// [min_offset[k], min_diff[k]] = rs_compare_segments(image_x_sums, vt[k].template, VT_SHIFT_MATCH, size(image_x_sums,2)
			Segments segment = new Segments(image_x_sums[1], vt[k].template[1], (int)VT_SHIFT_MATCH, image_x_sums[2].length);
			double[] min_vals = segment.compare_segments();
		}

		// NEED :: [diff, diff_id] = min(min_diff)
		
//		double[][] a_diff = new double[min_vals[k]][diff_id] ;
//		a_diff = min[min_diff];

		// for now
		int diff = 1;
		int diff_id = 1;
		
		//if this intensity template doesn't match any of the existing templates,
		// then create a new template
		if ((diff * image_x_sums[2].length) > VT_MATCH_THRESHOLD)
		{
			numvts++;
			vt[numvts] = new VT(numvts, image_x_sums, VT_ACTIVE_DECAY, x_val, y_val, th_val, 1, 0, new Experience[1]);
			vt_id = numvts;
		}
		else
		{
			vt_id = diff_id;
			vt[vt_id].template_decay = VT_ACTIVE_DECAY;
			if (prev_vt_id != vt_id)
			{
				vt[vt_id].first = 0;
			}
		}
		
		// copy vt_history and add vt_id to it
		int[] new_vt_hist = new int[vt_history.length + 1];
		for (int i = 0; i < (new_vt_hist.length + 1); i++)
		{
			
			if (i == new_vt_hist.length - 1)
			{
				new_vt_hist[i] = vt_id;
			}
		}
		vt_history = new_vt_hist;
	}

	// Creates 1-dimensional arrays that mimics MATLAB colon operator
	private int[] setRange(int range)
	{
		int[] toReturn = new int[range];
		for (int i = 0; i < range; i++)
		{
			toReturn[i] = i;
		}
		return toReturn;
	}
}
