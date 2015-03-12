import java.util.ArrayList;

// Used to setup experience map

public class Experience 
{
	static final double PI = Math.PI;
	
	// Variables
	double x_pc;
	double y_pc;
	double th_pc;
	double x_m;
	double y_m;
	double facing_rad;
	double accum_delta_facing = PI/2;
	double accum_delta_x = 0;
	double accum_delta_y = 0;
	int vt_id;
	ArrayList <Link> links;
	int numLinks;
	int id;
	public Experience()
	{
		
	}
	
	public Experience(int id, double xPc, double yPc, double thPc, double xM, double yM, double facRad, int vtId, 
			ArrayList <Link> links)
	{
		this.id = id;
		x_pc = xPc;
		y_pc = yPc;
		th_pc = thPc;
		x_m = xM;
		y_m = yM;
		facing_rad = facRad;
		vt_id = vtId;
		this.links = links;
		
	}
	
	public int numLinks() {
		return this.links.size(); 
	}
}
