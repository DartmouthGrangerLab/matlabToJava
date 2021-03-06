import java.awt.image.BufferedImage;
import java.util.ArrayList;

import org.ejml.data.DenseMatrix64F;
import org.ejml.simple.SimpleMatrix;

public class VisualOdometry 
{
	static final double PI = Math.PI;

	// Set up the visual odometry
	double vrot = 0;
	double vtrans = 0;

	int VTRANS_SCALE = 100;
	int VISUAL_ODO_SHIFT_MATCH = 140;

	public void visual_odometry(BufferedImage img, ArrayList <Odo> odos) {

		int[] IMAGE_VTRANS_Y_RANGE = odos.get(0).IMAGE_ODO_X_RANGE;
		int[] IMAGE_VROT_Y_RANGE = odos.get(0).IMAGE_VROT_Y_RANGE;
		int[] IMAGE_ODO_X_RANGE = odos.get(0).IMAGE_ODO_X_RANGE;
		
		// this block represents marginal tuning improvements for Kemeny_Loop videos
//		int[] IMAGE_VTRANS_Y_RANGE = Util.setRange(240, 475);
//		int[] IMAGE_VROT_Y_RANGE = Util.setRange(10, 275);
//		int[] IMAGE_ODO_X_RANGE = odos.get(0).IMAGE_ODO_X_RANGE;

		int [][] raw_image = Util.get2DarrayIntsFromImg(img);
		double FOV_DEG = 65; //65 is BBT phone camera //94.4;// one of the GoPro modes //60 work for 2nd Life default FOV //90; 90 works for ARDrone captures //50; // 50 works for St Lucia dataset
		double width = raw_image[1].length;
		double degreesPerPixel = FOV_DEG / width;
		
		
		int currOdo = odos.size();
		double [] prev_vtrans_image_x_sums = odos.get(currOdo - 1).prev_vtrans_image_x_sums;
		double [] prev_vrot_image_x_sums = odos.get(currOdo -1).prev_vrot_image_x_sums;

		Odo newOdo = new Odo (0, PI/2, prev_vtrans_image_x_sums, prev_vrot_image_x_sums, IMAGE_VTRANS_Y_RANGE,
				IMAGE_VROT_Y_RANGE, IMAGE_ODO_X_RANGE);
		odos.add(newOdo);
		
		// vtrans
		double [][]sub_image = new double [IMAGE_VTRANS_Y_RANGE.length][IMAGE_ODO_X_RANGE.length];
		
		for (int n = IMAGE_VTRANS_Y_RANGE[0]; n < IMAGE_VTRANS_Y_RANGE[IMAGE_VTRANS_Y_RANGE.length-1]+1; n++) {
			for (int m = IMAGE_ODO_X_RANGE[0]; m < IMAGE_ODO_X_RANGE[IMAGE_ODO_X_RANGE.length-1]+1; m++) {
				sub_image[n-IMAGE_VTRANS_Y_RANGE[0]][m-IMAGE_ODO_X_RANGE[0]] = (double) raw_image[n-1][m-1];
			}
		}
		DenseMatrix64F subImage = new DenseMatrix64F(sub_image);

		SimpleMatrix subImage_ = SimpleMatrix.wrap(subImage);
		DenseMatrix64F imageXSums = new DenseMatrix64F (subImage.numCols, 1);
		
		for (int i=0; i < subImage_.numCols(); i++) {
			imageXSums.add(i, 0, subImage_.extractVector(false, i).elementSum());
		}

		int sumOf_image_x_sums = (int) SimpleMatrix.wrap(imageXSums).elementSum();
		double avint = sumOf_image_x_sums / imageXSums.numRows;
		imageXSums = SimpleMatrix.wrap(imageXSums).divide(avint).getMatrix();

		Segments segment = new Segments(imageXSums.data, prev_vtrans_image_x_sums, (int)VISUAL_ODO_SHIFT_MATCH, imageXSums.numRows);
		double[] min_vals = segment.compare_segments();
		
		odos.get(currOdo).prev_vtrans_image_x_sums = imageXSums.data;
		
		double mindiff = min_vals[1];
		double vtrans = mindiff * VTRANS_SCALE;

		// a hack to detect excessively large vtrans
		if (vtrans > 10)
		{
			vtrans = 0;
		}
		odos.get(currOdo).vtrans = vtrans;

		// now do rotation
		
		double [][]sub_image2 = new double [IMAGE_VROT_Y_RANGE.length][IMAGE_ODO_X_RANGE.length];

		for (int n = IMAGE_VROT_Y_RANGE[0]; n < IMAGE_VROT_Y_RANGE[IMAGE_VROT_Y_RANGE.length-1]+1; n++) {
			for (int m = IMAGE_ODO_X_RANGE[0]; m < IMAGE_ODO_X_RANGE[IMAGE_ODO_X_RANGE.length-1]+1; m++) {
				sub_image2[n-IMAGE_VROT_Y_RANGE[0]][m-IMAGE_ODO_X_RANGE[0]] = (double) raw_image[n-1][m-1];
			}
		}
		DenseMatrix64F subImage2 = new DenseMatrix64F(sub_image);

		SimpleMatrix subImage2_ = SimpleMatrix.wrap(subImage2);
		DenseMatrix64F imageXSums2 = new DenseMatrix64F (subImage2.numCols, 1);
		
		for (int i=0; i < subImage2_.numCols(); i++) {
			imageXSums2.add(i, 0, subImage2_.extractVector(false, i).elementSum());
		}

		int sumOf_image_x_sums2 = (int) SimpleMatrix.wrap(imageXSums2).elementSum();
		double avint2 = sumOf_image_x_sums2 / imageXSums2.numRows;
		imageXSums2 = SimpleMatrix.wrap(imageXSums2).divide(avint2).getMatrix();

		Segments segment2 = new Segments(imageXSums2.data, prev_vrot_image_x_sums, (int)VISUAL_ODO_SHIFT_MATCH, imageXSums2.numRows);
		double[] min_vals2 = segment2.compare_segments();
		
		odos.get(currOdo).prev_vrot_image_x_sums = imageXSums2.data;
		
		double minoffset = min_vals2[0];

		vrot = minoffset * degreesPerPixel * PI / 180;
		
		odos.get(currOdo).prev_vrot_image_x_sums = imageXSums2.data;
		odos.get(currOdo).vtrans = vtrans;
		odos.get(currOdo).vrot = vrot;
	}
}
