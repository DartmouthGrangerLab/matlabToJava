import java.util.ArrayList;
import java.util.Collections;
import java.util.Vector;

// From rs_experience_map_iteration.m
public class ExpMapIteration {
	// Variables Passed to Class
	int vt_id, prev_vt_id; 
	double vtrans, vrot, x_pc, y_pc, th_pc;
	ArrayList <VT> vts;
	boolean link_exists;

	// Constants
	static final double PI = Math.PI;

	// Variables
	double delta_pc;
	Vector <Double> delta_pc_vec = new Vector <Double> ();
	Vector <Double> search = new Vector <Double> ();
	int newExpId, prevExpId, prev_exp_id, curr_exp_id;
	ArrayList <Experience> exps;

	int matched_exp_id, matched_exp_count;
	int[] exp_history = {};

	// Persistent variables
	double accumDeltaX = 0;
	double accumDeltaY = 0;
	double accum_delta_facing = PI/2;

	// Set from Ratslam.main
	static double EXP_DELTA_PC_THRESHOLD = 0;
	static int EXP_LOOPS;
	static double EXP_CORRECTION;
	static int PC_DIM_XY;
	static int PC_DIM_TH;

	public ExpMapIteration() {

	}

	public void iterate(int vt_id, double vtrans, double vrot, double x_pc, double y_pc, double th_pc,
			ArrayList <VT> vts, ArrayList <Experience> exps) {

		this.vt_id = vt_id;
		this.vtrans = vtrans;
		this.vrot = vrot;
		this.x_pc = x_pc;
		this.y_pc = y_pc;
		this.th_pc = th_pc;
		this.vts = vts;
		this.exps = exps;

		int currExpId = exps.size()-1;

		Experience expCurrExpId = exps.get(currExpId);

		if (exps.size() >1) {
			accum_delta_facing = clip_rad_180(accum_delta_facing + vrot);
			accumDeltaX = accumDeltaX + vtrans * 
					Math.cos(accum_delta_facing);
			accumDeltaY = accumDeltaY + vtrans * 
					Math.sin(accum_delta_facing);

		} else {
			accum_delta_facing = clip_rad_180(
					accum_delta_facing + vrot); // assumes very first exps has stored initializer constants, PI/2 in this case.
			accumDeltaX = accumDeltaX + vtrans * Math.cos(accum_delta_facing);
			accumDeltaY = accumDeltaY + vtrans * Math.sin(accum_delta_facing);
		}

		delta_pc = Math.sqrt(Math.pow(get_min_delta(expCurrExpId.x_pc, x_pc, PC_DIM_XY),2) + 
				Math.pow(get_min_delta(expCurrExpId.y_pc, y_pc, PC_DIM_XY), 2) +
				Math.pow(get_min_delta(expCurrExpId.th_pc, th_pc, PC_DIM_TH), 2));

//		System.out.println("debug: expCurrExpId.x_pc: "+ expCurrExpId.x_pc);
//		System.out.println("debug: x_pc: "+ x_pc);
//		System.out.println("debug: expCurrExpId.y_pc: "+ expCurrExpId.y_pc);
//		System.out.println("debug: y_pc: "+ y_pc);
//		System.out.println("debug: expCurrExpId.th_pc: "+ expCurrExpId.th_pc);
//		System.out.println("debug: th_pc: "+ th_pc);
//		System.out.println("debug: delta_pc: "+ delta_pc);

		// if the vt is new or the pc x,y,th has changed enough, create a new experience
		prev_vt_id = vts.get(vts.size()-1).id; // see if this works replacing functionality of global in MATLAB
		if (vts.get(vts.size()-1).numexps == 0 || delta_pc > EXP_DELTA_PC_THRESHOLD) {
			create_new_exp(currExpId, exps.size());

			prevExpId = currExpId;
			currExpId = exps.size()-1;

			expCurrExpId = exps.get(currExpId);

			accumDeltaX = 0;
			accumDeltaY = 0;
			accum_delta_facing = exps.get(currExpId).facing_rad;
		} else if (prev_vt_id != vt_id) {
			matched_exp_count = 0;
			matched_exp_id = 0;
			
			delta_pc_vec.clear();
			for (int search_id = 0; search_id < vts.get(vt_id).numexps; search_id++) {
				delta_pc = Math.sqrt(Math.pow(get_min_delta(exps.get(search_id).x_pc, x_pc, PC_DIM_XY),2) + 
						Math.pow(get_min_delta(exps.get(search_id).y_pc, y_pc, PC_DIM_XY), 2) +
						Math.pow(get_min_delta(exps.get(search_id).th_pc, th_pc, PC_DIM_TH), 2));
				delta_pc_vec.add(delta_pc);

				if (delta_pc < EXP_DELTA_PC_THRESHOLD) {
					matched_exp_count = matched_exp_count + 1; 
				}
			}

			if (matched_exp_count > 1) {
				//				this means we aren't sure about which experience is a match due
				//				to hash table collision
				//				instead of a false positive which may create blunder links in
				//				the experience map keep the previous experience
				//				matched_exp_count
			} else {
				double min_delta = Collections.min(delta_pc_vec);
				search.clear();
				search.add(min_delta);
				int min_delta_id = Collections.indexOfSubList(delta_pc_vec, search);

				if (min_delta < EXP_DELTA_PC_THRESHOLD) {
					matched_exp_id = vts.get(vt_id).exps.get(min_delta_id);

					// see if the prev exp already has a link to the current exp
					link_exists = false;
					for (int link_id = 0; link_id < expCurrExpId.numLinks(); link_id++) {
						if (expCurrExpId.links.get(link_id).exp_id == matched_exp_id) {
							link_exists = true;
							break;
						}
					}

					if (!link_exists) {
						int expID = matched_exp_id;
						double d = Math.sqrt(Math.pow(accumDeltaX, 2) + Math.pow(accumDeltaY, 2));
						double heading_rad = get_signed_delta_rad(expCurrExpId.facing_rad, 
								Math.atan2(accumDeltaY, accumDeltaX));
						double facing_rad = get_signed_delta_rad(expCurrExpId.facing_rad, accum_delta_facing);
						expCurrExpId.links.add(new Link(expID, d, heading_rad, facing_rad));
					}
				}

				// if there wasn't an experience with the current vt and the posecell x y th
				// then create a new experience
				if (matched_exp_id == 0) {
					//					numExps++;
					create_new_exp(currExpId, exps.size());
					matched_exp_id = exps.size();
				}

				prev_exp_id = currExpId;
				curr_exp_id = matched_exp_id;

				expCurrExpId = exps.get(currExpId);

				accumDeltaX = 0;
				accumDeltaY = 0;
				accum_delta_facing = exps.get(curr_exp_id-1).facing_rad;
			}
		}
		for (int i = 0; i < EXP_LOOPS; i++) {
			for(int exp_id = 0; exp_id < exps.size(); exp_id++) {
				for (int link_id = 0; link_id < exps.get(exp_id).numLinks(); link_id++) {
					int e0 = exp_id;
					int e1 = exps.get(exp_id).links.get(link_id).exp_id;

					double lx = exps.get(e0).x_m + exps.get(e0).links.get(link_id).d * 
							Math.cos(exps.get(e0).facing_rad + 
									exps.get(e0).links.get(link_id).heading_rad);
					double ly = exps.get(e0).y_m + exps.get(e0).links.get(link_id).d * 
							Math.sin(exps.get(e0).facing_rad + 
									exps.get(e0).links.get(link_id).heading_rad);

					exps.get(e0).x_m = exps.get(e0).x_m + (exps.get(e1).x_m - lx) * EXP_CORRECTION;
					exps.get(e0).y_m = exps.get(e0).y_m + (exps.get(e1).y_m - ly) * EXP_CORRECTION;
					exps.get(e1).x_m = exps.get(e1).x_m - (exps.get(e1).x_m - lx) * EXP_CORRECTION;
					exps.get(e1).y_m = exps.get(e1).y_m - (exps.get(e1).y_m - ly) * EXP_CORRECTION;

					//						System.out.println("debug: e0: "+ e0);

					double df = get_signed_delta_rad((exps.get(e0).facing_rad + 
							exps.get(e0).links.get(link_id).facing_rad), exps.get(e1).facing_rad);

					exps.get(e0).facing_rad = clip_rad_180(exps.get(e0).facing_rad + 
							(df * EXP_CORRECTION));
					exps.get(e1).facing_rad = clip_rad_180(exps.get(e1).facing_rad - 
							(df * EXP_CORRECTION));
				}
			}
		}

		//		int newLength = exp_history.length + 1;
		//		int[] new_exp_history = new int[newLength];
		//		for (int i = 0; i < newLength; i++) {
		//			new_exp_history[i] = exp_history[i];
		//		}
		//		new_exp_history[newLength + 1] = currExpId;
		//		exp_history = new_exp_history;
	}

	// Create a new experience map and add the current experience map onto it
	private void create_new_exp(int curr_exp_id, int new_exp_id) {
		int currExpIdLinkIdx = 0;
		Experience expCurrExpId = exps.get(curr_exp_id);

		expCurrExpId.links.add(new Link(new_exp_id, 0, 0, 0));

		currExpIdLinkIdx = expCurrExpId.numLinks()-1;
		Link currExpIdLink = expCurrExpId.links.get(currExpIdLinkIdx);

		currExpIdLink.exp_id = new_exp_id;
		currExpIdLink.d = Math.sqrt( Math.pow(accumDeltaX,2) + Math.pow(accumDeltaY,2));
		currExpIdLink.heading_rad = get_signed_delta_rad(expCurrExpId.facing_rad, Math.atan2(accumDeltaY, accumDeltaX));
		currExpIdLink.facing_rad = get_signed_delta_rad(expCurrExpId.facing_rad, accum_delta_facing);

		exps.add(new Experience(currExpIdLinkIdx+1, x_pc, y_pc, th_pc, (expCurrExpId.x_m + accumDeltaX),
				(expCurrExpId.y_m + accumDeltaY), clip_rad_180(accum_delta_facing), vt_id, new ArrayList <Link> ()));

//		System.out.println("debug: exps count: "+ exps.size());

		// add this experience id to the vt for efficient lookup <---NO, THIS IS REQUIRED FOR PROPER FUNCTIONING
		//TODO needs fixing
		vts.get(vt_id).numexps++;
		vts.get(vt_id).exps.add(new_exp_id);
	}

	// Clip the input angle to between 0 and 2pi radians 
	private double clip_rad_360(double angle) {
		if (angle < 0) {
			angle += (2 * PI);
		} else if (angle >= (2 * PI)) {
			angle = angle - (2 * PI);
		}
		return angle;
	}

	// Clip the input angle to between -pi and pi
	private double clip_rad_180(double angle) {
		if (angle > PI) {
			angle -= (2 * PI);
		} else if (angle <= -PI) {
			angle += (2 * PI);
		}
		return angle;
	}

	// Get the minimum delta distance between two values assuming a wrap to zero at max
	private double get_min_delta(double d1, double d2, double max) {
		return Math.min(Math.abs(d1 - d2), max - Math.abs(d1 - d2));
	}

	// Get signed delta angle from angle1 and angle2 handling the wrap from 2pi to 0
	private double get_signed_delta_rad(double angle1, double angle2) {
		double angle = 0;
		double dir = clip_rad_180(angle2 - angle1);
		double delta_angle = Math.abs(clip_rad_360(angle1) - clip_rad_360(angle2));
		if (delta_angle < (2 * PI - delta_angle)) {
			if (dir > 0) {
				angle = delta_angle;
			} else {
				angle = -1 * delta_angle;
			}
		} else {
			if (dir > 0) {
				angle = 2 * PI - delta_angle;
			} else {
				angle = -1 * (2 * PI - delta_angle);
			}
		}
		return angle;
	}
}
