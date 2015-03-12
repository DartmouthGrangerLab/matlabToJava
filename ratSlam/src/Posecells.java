// Corresponds with MATLAB script called "create_posecell_weights.m" and "rs_get_posecell_xyth.m"

public class Posecells {
	final double PI = Math.PI;
	int PC_DIM_XY;
	int PC_DIM_TH;
	int [] PC_AVG_XY_WRAP;
	double[][][] pc_w_excite = null;
	double[][][] pc_w_inhib = null;
	double[][][] posecells = null;	// for lack of a better word (corresponds to global variable Posecells)
		
	// Constructor to initialize posecell weight array 
	// and the center of the dimension
	public Posecells(int pc_w_e_dim, int pc_w_e_var, int pc_w_i_dim, int pc_w_i_var, 
			int pc_dim_xy, int pc_dim_th, int x_pc, int y_pc, int th_pc) {
		PC_DIM_XY = pc_dim_xy;
		PC_DIM_TH = pc_dim_th;
		
		posecells = new double[PC_DIM_XY][PC_DIM_XY][PC_DIM_TH];
		posecells[x_pc][y_pc][th_pc] = 1;
		
		double dim_center_e = Math.floor(pc_w_e_dim / 2.0);
		double dim_center_i = Math.floor(pc_w_i_dim / 2.0);

		pc_w_excite = createPosecellWeights(pc_w_e_dim, pc_w_e_var, dim_center_e);	// creates the posecell weights so no need to call outside of constructor
		pc_w_inhib = createPosecellWeights(pc_w_i_dim, pc_w_i_var, dim_center_i);	// creates the posecell weights so no need to call outside of constructor
	}
	
	
	// Create array of posecell weights
	// from matlab file: "Creates a 3D normalized distribution of size dim^3 with a variance of var."
	private double[][][] createPosecellWeights(int dim, int var, double dim_center) {		
		// Create 3-dimensional array of posecell weights
		double[][][] weight = new double[dim][dim][dim];
		
		// Create constant a
		double a = Math.sqrt(2 * PI);
		
		// Set up weights for posecells
		// Sum up elements of array for total weight
		double total = 0;
		for (int x = 0; x < dim; x++) {
			for (int y = 0; y < dim; y++) {
				for (int z = 0; z < dim; z++) {
					double b = -1 * (x - dim_center)*(x -dim_center);
					double c = (y - dim_center) * (y - dim_center);
					double d = (z - dim_center) * (z - dim_center);

					weight[x][y][z] = 1/(var * a) * Math.exp((b - c - d)/(2 * var * var));
					total += weight[x][y][z];
				}
			}
		}
		
		// Divide each weight by the total weight
		for (int x = 0; x < dim; x++) {
			for (int y = 0; y < dim; y++) {
				for (int z = 0; z < dim; z++) {
					weight[x][y][z] = weight[x][y][z]/total;
				}
			}
		}	
		return weight;
	}
	
	/**
	 * Output: double[] xyth
	 * xyth[0] = X
	 * xyth[1] = Y
	 * xyth[2] = TH
	 */
	public double[] getPosecellXYTH( double[] PC_XY_SUM_SIN_LOOKUP, 
			double[] PC_XY_SUM_COS_LOOKUP, double[] PC_TH_SUM_SIN_LOOKUP, double[] PC_TH_SUM_COS_LOOKUP,
			int PC_CELLS_TO_AVG, int[] PC_AVG_XY_WRAP, int[] PC_AVG_TH_WRAP) {
		
		// find the max activated cell by getting (x, y, z) coordinates of the max element
		double max = Double.MIN_VALUE;
		int x = 0;
		int y = 0;
		int z = 0;
		for (int currX = 0; currX < PC_DIM_XY; currX++) {
			for (int currY = 0; currY < PC_DIM_XY; currY++) {
				for (int currZ = 0; currZ < PC_DIM_TH; currZ++) {
//					//debug
//					if (posecells[currX][currY][currZ] ==0){
//						System.out.println("debug: x,y,z = " + currX+","+currY+","+currZ);
//						System.out.println("debug: posecells[currX][currY][currZ]: "+ posecells[currX][currY][currZ]);
//					}
//					//debug
					if (posecells[currX][currY][currZ] > max) {
						max = posecells[currX][currY][currZ];
						x = currX;
						y = currY;
						z = currZ;
					}
				}
			}
		}
		
		// "take the max activated cell +- AVG_CELL in 3d space"
		// copy over sub-matrix of posecells
		double[][][] z_Posecells = new double[PC_DIM_XY][PC_DIM_XY][PC_DIM_TH];
		for (int row = PC_AVG_XY_WRAP[x-1]; row < PC_AVG_XY_WRAP[x + PC_CELLS_TO_AVG * 2]; row++) {
			for (int col = PC_AVG_XY_WRAP[y-1]; col < PC_AVG_XY_WRAP[y + PC_CELLS_TO_AVG * 2]; col++) {
				for (int layer = PC_AVG_TH_WRAP[z-1]; layer < PC_AVG_TH_WRAP[z + PC_CELLS_TO_AVG * 2]; layer++) {
					z_Posecells[row][col][layer] = posecells[row][col][layer];
				}
			}
		}
		
		// "get the sums for each axis"
		double[] x_sums = new double[PC_DIM_XY];
		double[] y_sums = new double[PC_DIM_XY];
		double[] th_sums = new double[PC_DIM_TH];
		for (int layer = 0; layer < PC_DIM_TH; layer++) {
			for (int row = 0; row < PC_DIM_XY; row++) {
				for (int col = 0; col < PC_DIM_XY; col++) {
					double currVal = z_Posecells[row][col][layer];
					x_sums[row] += currVal;
					y_sums[col] += currVal;
					th_sums[layer] += currVal;
				}
			}
		}
		
		// "now find the (x, y, th) using population vector decoding to handle the wrap around"
		double sum_x_xy_sin_lookup = 0;
		double sum_y_xy_sin_lookup = 0;
		double sum_x_xy_cos_lookup = 0;
		double sum_y_xy_cos_lookup = 0;
		for (int i = 0; i < PC_XY_SUM_SIN_LOOKUP.length; i++) {	// since pc_xy_sum_sin_lookup.length == pc_xy_sum_cos_lookup.length
			double val_x_sin = PC_XY_SUM_SIN_LOOKUP[i] * x_sums[i];
			double val_y_sin = PC_XY_SUM_SIN_LOOKUP[i] * y_sums[i];
			double val_x_cos = PC_XY_SUM_COS_LOOKUP[i] * x_sums[i];
			double val_y_cos = PC_XY_SUM_COS_LOOKUP[i] * y_sums[i];
			sum_x_xy_sin_lookup += val_x_sin;
			sum_y_xy_sin_lookup += val_y_sin;
			sum_x_xy_cos_lookup += val_x_cos;
			sum_y_xy_cos_lookup += val_y_cos;
		}
		
		double sum_th_sin_lookup = 0;
		double sum_th_cos_lookup = 0;
		for (int i = 0; i < PC_TH_SUM_SIN_LOOKUP.length; i++) {  // pc_th_sum_sin_lookup.length == pc_th_sum_cos_lookup.length
			double val_sin = PC_TH_SUM_SIN_LOOKUP[i] * th_sums[i];
			double val_cos = PC_TH_SUM_COS_LOOKUP[i] * th_sums[i];
			sum_th_sin_lookup += val_sin;
			sum_th_cos_lookup += val_cos;
		}
		
		double x_atan2_result = Math.atan2(sum_x_xy_sin_lookup, sum_x_xy_cos_lookup);
		double y_atan2_result = Math.atan2(sum_y_xy_sin_lookup, sum_y_xy_cos_lookup);
		double th_atan2_result = Math.atan2(sum_th_sin_lookup, sum_th_cos_lookup);
		
		double x_mod_first_term = x_atan2_result * PC_DIM_XY / (2 * PI);
		double y_mod_first_term = y_atan2_result * PC_DIM_XY / (2 * PI);
		double th_mod_first_term = th_atan2_result * PC_DIM_TH / (2 * PI);

		double X = x_mod_first_term - Math.floor(x_mod_first_term / PC_DIM_XY) * PC_DIM_XY; //OMG this is what MATLAB is actually frigging doing, how do they call this MOD!!!
		double Y = y_mod_first_term - Math.floor(y_mod_first_term / PC_DIM_XY) * PC_DIM_XY;
		double TH = th_mod_first_term - Math.floor(th_mod_first_term / PC_DIM_TH) * PC_DIM_TH;

		double[] xyth = {X, Y, TH};
		return xyth;
	}
}
