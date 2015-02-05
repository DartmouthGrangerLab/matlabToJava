import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Vector;

import javax.imageio.ImageIO;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.MatrixIO;
import org.ejml.ops.MatrixVisualization;
import org.ejml.simple.SimpleMatrix;


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
	int[] IMAGE_VT_Y_RANGE = null;
	int[] IMAGE_VT_X_RANGE = null;

	DenseMatrix64F subImage = null;
	
	int x_val, y_val, th_val;
	int[][] raw_image = null;
	int[] new_vt_history = null;
	
	Experience [] exps = new Experience [5];
	ArrayList <VT> vts = new ArrayList <VT> ();

	public Visual_Template(BufferedImage img, int x, int y, int th, int vidWidth, int vidHeight, Experience [] exps, ArrayList <VT> vt)
	{
		x_val = 0;
		y_val = 0;
		th_val = 0;
		int [][] intImg = Util.get2DarrayIntsFromImg(img);
		raw_image = intImg;
		IMAGE_X_SIZE = vidWidth;
		IMAGE_Y_SIZE = vidHeight;
		this.exps = exps;
		vts = vt;
		double[][] sub_image = null;
		
		IMAGE_VT_Y_RANGE = Util.setRange((IMAGE_Y_SIZE/2 - 80 - 40), (IMAGE_Y_SIZE/2 + 80 - 40)); // 80 and 40 ought to be settable too -bbt
		IMAGE_VT_X_RANGE = Util.setRange((IMAGE_X_SIZE/2 - 280 + 15), (IMAGE_X_SIZE/2 + 280 + 15)); // 280 and 15 ought to be settable too -bbt
		
		sub_image = new double [IMAGE_VT_Y_RANGE.length][IMAGE_VT_X_RANGE.length];
		
//		// getting matrix of raw_image here solely to visualize it for debugging ////////////////////////////
//		int ROWS = 480;
//		int COLS = 640;
//		SimpleMatrix rawImage = new SimpleMatrix (ROWS, COLS);
//		for (int col = 0; col < COLS; col++) {
//			for (int row = 0; row < ROWS; row++) {
//				rawImage.set(row, col, (double)raw_image[row][col]);
//			}
//		}
//		
////		MatrixVisualization.show(rawImage.getMatrix(), "rawImage");
//		MatrixIO.print(System.out, rawImage.getMatrix(), "%4.0f", 1, 2, 630, 640);
//		/////////////////////////////////////////////////////////////////////////////////////////////////////
//		//
//		// DEBUGGING ONLY: we'll load one known image here and in the MATLAB original and then we can compare byte translation to ints.
//		try {
//			File imgFile = new File("/Users/bentito/Downloads/stlucia_stills/image-1.jpg");
//		    img = ImageIO.read(imgFile);
//		} catch (Exception e) {
//			System.out.println("Maybe image file read error?: "+ e);
//		}
//		raw_image = get2DarrayIntsFromImg(img);
//		///////////////// DEBUG ONLY //////////////////////////////////
		for (int n = IMAGE_VT_Y_RANGE[0]; n < IMAGE_VT_Y_RANGE[IMAGE_VT_Y_RANGE.length-1]+1; n++) {
			for (int m = IMAGE_VT_X_RANGE[0]; m < IMAGE_VT_X_RANGE[IMAGE_VT_X_RANGE.length-1]+1; m++) {
				sub_image[n-IMAGE_VT_Y_RANGE[0]][m-IMAGE_VT_X_RANGE[0]] = (double) raw_image[n-1][m-1];
			}
		}
		subImage = new DenseMatrix64F(sub_image);
	}
	
	public void visual_template()
	{
		int vt_id = 0;

		// normalized intensity sums

		// Goes through each pixel of the image, sums up the rows
		// and stores the sum of each row into image_x_sums

		SimpleMatrix subImage_ = SimpleMatrix.wrap(subImage);
		DenseMatrix64F imageXSums = new DenseMatrix64F (subImage.numCols, 1);
		
		for (int i=0; i < subImage_.numCols(); i++) {
			imageXSums.add(i, 0, subImage_.extractVector(false, i).elementSum());
		}
		
		//////////////////////////// DEBUG ONLY /////////////////////////////////////////////
		// replace actual image based imageXSums with those from Matlab output to a CSV file
//		try {
//			imageXSums = MatrixIO.loadCSV("imageXSumMatlab.csv");
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		//////////////////////////   debug    ///////////////////////////////////////////////
		
		// adds all values in image_x_sums
		int sumOf_image_x_sums = (int) SimpleMatrix.wrap(imageXSums).elementSum();
		
		// divides each value in image_x_sums by total sum
		imageXSums =  SimpleMatrix.wrap(imageXSums).divide(sumOf_image_x_sums).getMatrix();
		
		// initialize minOffset and minDif
		// DO THIS!!!!
		//int[] minOffset = new int [numvts];
		Vector <Double> diff = new Vector <> ();
		numvts = vts.size();
		for (int k = 0; k < vts.size(); k++)
		{
			//vt.visTemp
			vts.get(k).template_decay = vts.get(k).template_decay - VT_GLOBAL_DECAY;
			if(vts.get(k).template_decay < 0)
			{
				vts.get(k).template_decay = 0;
			}
			if (vts.get(k).template.length == 0) {
				vts.get(k).template = new double [imageXSums.numRows];
			}
			// [min_offset[k], min_diff[k]] = rs_compare_segments(image_x_sums, vt[k].template, VT_SHIFT_MATCH, size(image_x_sums,2)
			// TODO: double to int array shifting is wasteful. Consider changing Segments to accept double array or even a DenseMatrix
			Segments segment = new Segments(imageXSums.data, vts.get(k).template, (int)VT_SHIFT_MATCH, imageXSums.numRows);
			double[] min_vals = segment.compare_segments();
			diff.add(min_vals[1]);
		}

		// NEED :: [diff, diff_id] = min(min_diff)
		
//		double[][] a_diff = new double[min_vals[k]][diff_id] ;
		double minDiff = Collections.min(diff);
		// for now
//		int diff = 1;
		int diff_id = 0;
		
		//if this intensity template doesn't match any of the existing templates,
		// then create a new template
		if ((minDiff * imageXSums.numRows) > VT_MATCH_THRESHOLD)
		{
			vts.add(new VT(numvts, imageXSums.data, VT_ACTIVE_DECAY, x_val, y_val, th_val, 1, 0, new Experience[1]));
			vt_id = numvts;
		}
		else
		{
			vt_id = diff_id;
			vts.get(vt_id).template_decay = VT_ACTIVE_DECAY;
			if (prev_vt_id != vt_id)
			{
				vts.get(vt_id).first = 0;
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
}
