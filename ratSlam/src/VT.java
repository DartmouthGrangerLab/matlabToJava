import java.util.ArrayList;

// VT called in many parts of code (main and Visual_Template

public class VT 
{
	double template_decay = 1.0;
	int first = 1;
	int numexps = 1;
	double x_pc;
	double y_pc;
	double th_pc;
	ArrayList <Integer> exps;
	double [] template;
	//Experience exps = new Experience();
	// find out if 'id' is a keyword 
	int id = 0;
	
	public VT(int numvts, double [] img_sums, double decay, double xPc, double yPc, double thPc, int f, int numE)
	{
		id = numvts;
		template = img_sums;
		template_decay = decay;
		x_pc = xPc;
		y_pc = yPc;
		th_pc = thPc;
		first = f;
		numexps = numE;
		exps = new ArrayList <Integer> (); // this is just a list of exps id's that pair with this VT
	}
}
