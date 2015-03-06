import java.awt.Component;
import java.awt.List;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// rs_posecell_iteration (vt_id, vtrans, vrot)
public class Posecell_Iteration 
{
	static final double PI = Math.PI;

	// Constants Needed
	static int PC_DIM_XY = 61;
	static int PC_DIM_TH = 36;
	static int PC_W_E_DIM = 7;
	static int PC_W_I_DIM = 5;
	static int PC_W_E_VAR = 1;
	static int PC_W_I_VAR = 2;
	static double PC_GLOBAL_INHIB = 0.00002;

	// Variables Needed
	double PC_C_SIZE_TH = (2 * PI) / PC_DIM_TH;
	int[] PC_E_XY_WRAP;
	int[] PC_E_TH_WRAP;
	int[] PC_I_XY_WRAP;
	int[] PC_I_TH_WRAP;
	double[][][] PC_W_EXCITE;
	double[][][] PC_W_INHIB;
	double PC_VT_INJECT_ENERGY;
	int act_x;
	int act_y;
	int act_th;
	double energy;
	double[][][] pca_new;
	double dir;
	double[][][] posecells;
	int circshiftX = 0;
	int circshiftY = 0;
	int circshiftZ1 = 0;
	int circshiftZ2 = 0;

	// My variables
	double[][] pca90;
	double dir90;

	// Arguments in Constructor
	int vt_id;
	double vtrans;
	double vrot;
	Posecells pc;
	ArrayList <VT> vts;

	public Posecell_Iteration(int vtId, double trans, double rot, Posecells p, ArrayList <VT> vts, int [] PC_E_XY_WRAP, int [] PC_E_TH_WRAP, int [] PC_I_XY_WRAP, int [] PC_I_TH_WRAP)
	{
		vt_id = vtId;
		vtrans = trans;
		vrot = rot;
		pc = p;
		this.vts = vts;
		posecells = p.posecells;
		PC_W_EXCITE = pc.pc_w_excite;
		PC_W_INHIB = pc.pc_w_inhib;
		this.PC_E_XY_WRAP = PC_E_XY_WRAP;
		this.PC_E_TH_WRAP = PC_E_TH_WRAP;
		this.PC_I_XY_WRAP = PC_I_XY_WRAP;
		this.PC_I_TH_WRAP = PC_I_TH_WRAP;
	}

	public void iteration()
	{
		// if this isn't a new vt, then add the energy at its associated posecell location
		if (vts.get(vt_id-1).first != 1)
		{
			act_x = (int) Math.min(Math.max(Math.round(vts.get(vt_id-1).x_pc), 1), PC_DIM_XY);
			act_y = (int) Math.min(Math.max(Math.round(vts.get(vt_id-1).y_pc), 1), PC_DIM_XY);
			act_th = (int) Math.min(Math.max(Math.round(vts.get(vt_id-1).th_pc), 1), PC_DIM_TH);

			// This decays the amount of energy that's injected at the vt's posecell location
			// This is important as the Posecells poseCells will erroneously snap
			// for bad vt matches that occur over long periods (ex. a bad match that occurs while agent is stationary)
			// This means that multiple vt's need to be recognized for a snap to happen
			energy = PC_VT_INJECT_ENERGY * (1/30) * (30 - Math.exp(1.2 * vts.get(vt_id-1).template_decay));
			if (energy > 0)
			{
				posecells[act_x][act_y][act_th] += energy;
			}
		}

		// local excitation - PC_le = PC elements + PC weights
		pca_new = new double[PC_DIM_XY][PC_DIM_XY][PC_DIM_TH];
		for (int i = 0; i < PC_DIM_XY; i++) {
			for (int j = 0; j < PC_DIM_XY; j++) {
				for (int k = 0; k < PC_DIM_TH; k++) {
					if (posecells[i][j][k] != 0) {
						for (int m = i; m < (i+ PC_W_E_DIM - 1); m++)
							for (int n = j; n < (j + PC_W_E_DIM - 1); n++)
								for (int o = k; o < (k + PC_W_E_DIM - 1); o++)
									pca_new[PC_E_XY_WRAP[m]][PC_E_XY_WRAP[n]][PC_E_TH_WRAP[o]] = 
									pca_new[PC_E_XY_WRAP[m]][PC_E_XY_WRAP[n]][PC_E_TH_WRAP[o]] + posecells[i][j][k] * PC_W_EXCITE[m-i][n-j][o-k];					
					}
				}
			}
		}
		posecells = pca_new;

		// local inhibition - PC_li = PC_;e - PC_le elements * PC weights

		// re-initialize pca_new
		pca_new = new double[PC_DIM_XY][PC_DIM_XY][PC_DIM_TH];
		for (int i = 0; i < PC_DIM_XY; i++) {
			for (int j = 0; j < PC_DIM_XY; j++) {
				for (int k = 0; k < PC_DIM_TH; k++) {
					if (posecells[i][j][k] != 0) {
						for (int m = i; m < (i + PC_W_I_DIM - 1); m++)
							for (int n = j; n < (j + PC_W_I_DIM - 1); n++)
								for (int o = k; o < (k + PC_W_I_DIM - 1); o++)
									pca_new[PC_I_XY_WRAP[m]][PC_I_XY_WRAP[n]][PC_I_TH_WRAP[o]] =
									pca_new[PC_I_XY_WRAP[m]][PC_I_XY_WRAP[n]][PC_I_TH_WRAP[o]] + posecells[i][j][k] * PC_W_INHIB[m-i][n-j][o-k];
					}
				}
			}
		}


		// Subtract every posecell by pca_new
		for (int i = 0; i < PC_DIM_XY; i++) {
			for (int j = 0; j < PC_DIM_XY; j++) {
				for (int k = 0; k < PC_DIM_TH; k++) {
					posecells[i][j][k] -= pca_new[i][j][k];
				}
			}
		}

		// Set poseCells again taking inhibition into account
		// local global inhibition - PC_gi = PC_li elements - inhibition
		// Also, find total after inhibition is taken into account and divide each poseCells element by total
		double total = 0;
		for (int i = 0; i < PC_DIM_XY; i++) {
			for (int j = 0; j < PC_DIM_XY; j++) {
				for (int k = 0; k < PC_DIM_TH; k++) {
					double x = posecells[i][j][k];
					if (x >= PC_GLOBAL_INHIB) {
						posecells[i][j][k] *= (x - PC_GLOBAL_INHIB);
						total += posecells[i][j][k];
					}
				}
			}
		}

		// Divide every element of poseCells by total
		for (int i = 0; i < PC_DIM_XY; i++) {
			for (int j = 0; j < PC_DIM_XY; j++) {
				for (int k = 0; k < PC_DIM_TH; k++) {
					double x = posecells[i][j][k];					
					posecells[i][j][k] = x / total;
				}
			}
		}

		// Path Integration
		// vtrans affects xy directions
		// shift in each th given by the th
		for (int dir_pc = 0; dir_pc < PC_C_SIZE_TH; dir_pc++) {
			// radians
			dir = (dir_pc - 1) * PC_C_SIZE_TH;

			// north, east, south, west are straightforward
			if (dir == 0) {
				int[] dirArray = {0, 1};
				goThrough(dir_pc, dirArray);
			} else if (dir == PI / 2) {
				int[] dirArray = {1, 0};
				goThrough(dir_pc, dirArray);
			} else if (dir == PI) {
				int[] dirArray = {0, -1};
				goThrough(dir_pc, dirArray);
			} else if (dir == (3 * PI) / 2) {
				int[] dirArray = {-1, 0};
				goThrough(dir_pc, dirArray);
			} else {
				// rotate  poseCells instead of implementing for four quadrants
				pca90 = Util.rot90(posecells, 0, (int)Math.floor(dir * 2 / PI));
				dir90 = dir - Math.floor(dir * 2 / PI) * (PI / 2);

				double [][] pca_new = new double[PC_DIM_XY + 1][PC_DIM_XY + 1];

				for(int i = 1; i < pca_new.length-1; i++) {
					for(int j = 2; j < pca_new.length-1; j++) {
						// is pca_new a 2-d or 3-d array
						pca_new[i][j] = pca90[i][j];
					}
				}
				double weight_sw = Math.pow(vtrans, 2) * Math.cos(dir90) * Math.sin(dir90);
				double weight_se = vtrans * Math.sin(dir90) 
						- (vtrans*vtrans*Math.cos(dir90)*Math.sin(dir90));
				double weight_nw = vtrans * Math.cos(dir90) 
						- (vtrans*vtrans*Math.cos(dir90)*Math.sin(dir90));
				double weight_ne = 1.0 - weight_sw - weight_se - weight_nw;	

				// circular shift and multiple by the contributing weight
				// copy those shifted elements for the wrap around
				for (int i = 0; i < pca_new.length; i++) {
					for (int j = 0; j < pca_new[0].length; j++) {
						double[][] addend1 = Util.circshift(pca_new, new int[]{0, 1});
						double[][] addend2 = Util.circshift(pca_new, new int[]{1, 0});
						double[][] addend3 = Util.circshift(pca_new, new int[]{1, 1});
						pca_new[i][j] = pca_new[i][j]*weight_ne + 
								Util.multiply_elements(addend1, weight_nw)[i][j] +
								Util.multiply_elements(addend2, weight_se)[i][j] +
								Util.multiply_elements(addend3, weight_sw)[i][j];
					}
				}

				// pca90 = pca_new(2:end-1, 2:end-1);
				for (int i = 1; i < pca_new[1].length - 1; i++){
					for (int j = 1; j < pca_new[2].length - 1; j++) {
						pca90[i-1][j-1] = pca_new[i][j];
					}
				}

				// pca90(2:end, 1) = pca90(2:end, 1) + pca_new(3:end-1, end);
				for (int i = 1; i < pca90.length; i++) {
					for (int j = 0; j < pca90[2].length; j++) {
						pca90[i][j] = pca90[i][j] + pca_new[i+1][j];
					}
				}

				// pca90(1, 2:end) = pca90(1, 2:end) + pca_new(end, 3:end-1);
				for (int i = 1; i < pca90.length; i++) {
					for (int j = pca90[2].length-1; j >= 0; j--) {
						pca90[i][j] = pca90[i][j] + pca_new[i][pca90[2].length - j];
					}
				}

				// pca(1,1) = pca90(1,1) + pca_new(end, end);
				pca90[1][1] = pca90[1][1] + pca_new[pca_new.length - 1][pca_new.length - 1];

				// unrotate the pose cell xy layer
				for (int i = 0; i < posecells[1].length; i++) {
					for (int j = 0; j < posecells[2].length; j++) {
						posecells[i][j][dir_pc] = Util.rot90( pca90, (int)(4 - Math.floor(dir * 2/PI)) )[i][j];
					}
				}
			}
		}

		// Path Integration - Theta
		// Shift the pose cells +/- theta given by vrot
		if (vrot != 0) {
//			System.out.println("Debug: Doing path integration.");
			// mod to work out the partial shift amount
			double weight = (Math.abs(vrot) / PC_C_SIZE_TH) % 1;
			if (weight == 0) {
				weight = 1.0;
			}
			circshiftX = 0;
			circshiftY = 0;
			circshiftZ1 = 0;
			double tempZ1 = Math.signum(vrot) * Math.floor(Math.abs(vrot)/PC_C_SIZE_TH);
			double tempZ2 = Math.signum(vrot) * Math.ceil(Math.abs(vrot)/PC_C_SIZE_TH);
			circshiftZ1 = (int) tempZ1;
			circshiftZ2 = (int) tempZ1;
			double [][][] leftHalf = Util.circshift(posecells, new int[]{circshiftX, circshiftY, circshiftZ1});
			double [][][] rightHalf = Util.circshift(posecells, new int[]{circshiftX, circshiftY, circshiftZ2});
			posecells = Util.addArrays(Util.multiply_elements(leftHalf, (1.0 - weight)), 
					Util.multiply_elements(rightHalf, weight)); 
			}
		}

		// Use when doing N, E, S, W directions
		private void goThrough (int constThird, int[] direct) {
			for (int i = 0; i < PC_DIM_XY; i++) {
				for (int j = 0; j < PC_DIM_XY; j++) {
					double toUse = posecells[i][j][constThird];			
					posecells[i][j][constThird] = (toUse * (1 - vtrans)) + (
							Util.multiply_elements(Util.circshift(posecells, direct), vtrans)[i][j][constThird]);
				}
			}
		}
		
		public static void main (String args []){
			double testArrLR [][][] = new double [3][4][3];
			double testArrUD [][][] = new double [3][3][3];
			double thisArr [][][] = new double [3][3][3];
// this version is good for shift up/down testing
			testArrUD [0][0][0] = 1;
			testArrUD [1][0][0] = 2;
			testArrUD [2][0][0] = 3;
			
			testArrUD [0][1][0] = 1;
			testArrUD [1][1][0] = 2;
			testArrUD [2][1][0] = 3;
			
			testArrUD [0][2][0] = 1;
			testArrUD [1][2][0] = 2;
			testArrUD [2][2][0] = 3;
			
			testArrUD [0][0][1] = 4;
			testArrUD [1][0][1] = 5;
			testArrUD [2][0][1] = 6;
			
			testArrUD [0][1][1] = 4;
			testArrUD [1][1][1] = 5;
			testArrUD [2][1][1] = 6;
			
			testArrUD [0][2][1] = 4;
			testArrUD [1][2][1] = 5;
			testArrUD [2][2][1] = 6;
			
			testArrUD [0][0][2] = 7;
			testArrUD [1][0][2] = 8;
			testArrUD [2][0][2] = 9;
			
			testArrUD [0][1][2] = 7;
			testArrUD [1][1][2] = 8;
			testArrUD [2][1][2] = 9;
			
			testArrUD [0][2][2] = 7;
			testArrUD [1][2][2] = 8;
			testArrUD [2][2][2] = 9;
			
// this version is good for shift left/right testing
			testArrLR [0][0][0] = 1;
			testArrLR [1][0][0] = 1;
			testArrLR [2][0][0] = 1;
			
			testArrLR [0][1][0] = 2;
			testArrLR [1][1][0] = 2;
			testArrLR [2][1][0] = 2;
			
			testArrLR [0][2][0] = 3;
			testArrLR [1][2][0] = 3;
			testArrLR [2][2][0] = 3;
			
			testArrLR [0][3][0] = 3.5;
			testArrLR [1][3][0] = 3.5;
			testArrLR [2][3][0] = 3.5;
			
			testArrLR [0][0][1] = 4;
			testArrLR [1][0][1] = 4;
			testArrLR [2][0][1] = 4;
			
			testArrLR [0][1][1] = 5;
			testArrLR [1][1][1] = 5;
			testArrLR [2][1][1] = 5;
			
			testArrLR [0][2][1] = 6;
			testArrLR [1][2][1] = 6;
			testArrLR [2][2][1] = 6;
			
			testArrLR [0][3][1] = 6.5;
			testArrLR [1][3][1] = 6.5;
			testArrLR [2][3][1] = 6.5;
			
			testArrLR [0][0][2] = 7;
			testArrLR [1][0][2] = 7;
			testArrLR [2][0][2] = 7;
			
			testArrLR [0][1][2] = 8;
			testArrLR [1][1][2] = 8;
			testArrLR [2][1][2] = 8;
			
			testArrLR [0][2][2] = 9;
			testArrLR [1][2][2] = 9;
			testArrLR [2][2][2] = 9;
			
			testArrLR [0][3][2] = 9.5;
			testArrLR [1][3][2] = 9.5;
			testArrLR [2][3][2] = 9.5;
			
			thisArr = testArrUD;
			for(int k=0; k<3; k++) {
				System.out.println("depth:"+k);
				for(int i=0; i<3; i++) {
					for(int j=0; j<4; j++)
						System.out.print(thisArr[i][j][k] + " ");
					System.out.println();
				}
			}
			thisArr = Util.circshift (thisArr, new int [] {1,0,0}); // int [] {row or y shift, col or x shift, depth or z shift}
			System.out.println("After circshift--------------------------------");
			for(int k=0; k<3; k++) {
				System.out.println("depth:"+k);
				for(int i=0; i<3; i++) {
					for(int j=0; j<4; j++)
						System.out.print(thisArr[i][j][k] + " ");
					System.out.println();
				}
			}
		}
	}