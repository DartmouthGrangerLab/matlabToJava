public class Odo {
	double vtrans = 0;
	double vrot = 0;

	double [] prev_vtrans_image_x_sums;
	double [] prev_vrot_image_x_sums;
	
	int[] IMAGE_VTRANS_Y_RANGE;
	int[] IMAGE_VROT_Y_RANGE;
	int[] IMAGE_ODO_X_RANGE;
	
	public Odo(double vtrans, double vrot, double[] prev_vtrans_image_x_sums,
			double[] prev_vrot_image_x_sums, int[] IMAGE_VTRANS_Y_RANGE,
			int[] IMAGE_VROT_Y_RANGE, int[] IMAGE_ODO_X_RANGE) {
		super();
		this.vtrans = vtrans;
		this.vrot = vrot;
		this.prev_vtrans_image_x_sums = prev_vtrans_image_x_sums;
		this.prev_vrot_image_x_sums = prev_vrot_image_x_sums;
		this.IMAGE_VTRANS_Y_RANGE = IMAGE_VTRANS_Y_RANGE;
		this.IMAGE_VROT_Y_RANGE = IMAGE_VROT_Y_RANGE;
		this.IMAGE_ODO_X_RANGE = IMAGE_ODO_X_RANGE;
	}
	
	
}
