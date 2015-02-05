import java.awt.image.BufferedImage;
import java.util.ArrayList;

import org.ejml.data.DenseMatrix64F;
import org.ejml.simple.SimpleMatrix;

public class Visual_Odometry 
{
	static final double PI = Math.PI;

	int IMAGE_Y_SIZE = 100;		
	int IMAGE_X_SIZE = 100;	
	
	// Set up the visual odometry
	double vrot = 0;
	double vtrans = 0;
//	int[] IMAGE_ODO_X_RANGE = Util.setRange(IMAGE_X_SIZE);
//	int[] IMAGE_VTRANS_Y_RANGE = Util.setRange(IMAGE_Y_SIZE);
//	int[] IMAGE_VROT_Y_RANGE = Util.setRange(IMAGE_Y_SIZE);
	int VTRANS_SCALE = 100;
	int VISUAL_ODO_SHIFT_MATCH = 140;

	int[] prev_vrot_image_X_sums;
	int[] prev_vtrans_image_x_sums;

	int accum_delta_x = 0;
	int accum_delta_y = 0;
	double accum_delta_facing = PI / 2;

	int numexps = 1;
	int curr_exp_id = 1;
	int exp_history = 1;
	double EXP_CORRECTION = 0.5;
	double EXP_LOOPS = 100;
	double EXP_DELTA_PC_THRESHOLD = 1.0;

	int ODO_ROT_SCALING = 1;
	int ODO_VTRANS_SCALING = 1;
	int POSECELL_VTRANS_SCALING = 1;
	int ODO_FILE = 1;

	public void visual_odometry(BufferedImage img, ArrayList <Odo> odos) {

		int[] IMAGE_VTRANS_Y_RANGE = odos.get(0).IMAGE_ODO_X_RANGE;
		int[] IMAGE_VROT_Y_RANGE = odos.get(0).IMAGE_VROT_Y_RANGE;
		int[] IMAGE_ODO_X_RANGE = odos.get(0).IMAGE_ODO_X_RANGE;

		int [][] raw_image = Util.get2DarrayIntsFromImg(img);
		int FOV_DEG = 50;
		int dpp = FOV_DEG / raw_image[1].length;
		
		
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

		vrot = minoffset * dpp * PI / 180;
		
		odos.get(currOdo).prev_vrot_image_x_sums = imageXSums2.data;
		odos.get(currOdo).vtrans = vtrans;
		odos.get(currOdo).vrot = vrot;
	}
}
