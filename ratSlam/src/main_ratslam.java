// Copy of jlk_rs_main.m

//jlk_rs_main(viddata, ododata, 'jlk_sbsg_log', ...
//    'IMAGE_X_OFFSET', 0, ...
//    'BLOCK_READ', 100, ...
//    'RENDER_RATE', 10, ...
//    'VT_MATCH_THRESHOLD', 0.05, ...
//    'IMAGE_VT_Y_RANGE', 1:120, ...
//    'IMAGE_VT_X_RANGE', 1:160, ...
//    'EXP_DELTA_PC_THRESHOLD', 1.0, ...
//    'EXP_CORRECTION', 0.5, ...
//    'ODO_ROT_SCALING', 1, ... % to get the data into delta change in radians between frames
//    'POSECELL_VTRANS_SCALING', 2, ...
//    'VTRANS_SCALE', 1, ... % to get the data into delta change in radians between frames
//    'EXP_LOOPS', 200);
import javax.swing.GrayFilter;
import javax.swing.JFrame;

import org.jfree.chart.plot.FastScatterPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RefineryUtilities;

import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.ImageProducer;
import java.util.ArrayList;
import java.util.Random;

public class main_ratslam {
	// My Constants
	static final double PI = Math.PI;

	// Pose cell activity network constraints
	static int PC_DIM_XY = 61;
	static int PC_DIM_TH = 36;
	static int PC_W_E_DIM = 7;
	static int PC_W_I_DIM = 5;
	static double PC_GLOBAL_INHIB = 0.00002;
	static double PC_VT_INJECT_ENERGY = 0.1;
	static int PC_W_E_VAR = 1;
	static int PC_W_I_VAR = 2;

	// Set the initial position in the pose network
	static int x_pc = (PC_DIM_XY / 2) + 1;
	static int y_pc = (PC_DIM_XY / 2) + 1;
	static int th_pc = (PC_DIM_TH / 2) + 1;

	// Posecell excitation and inhibition 3D weight matrices
	static Posecells pc = new Posecells(PC_W_E_DIM, PC_W_E_VAR, PC_W_I_DIM, PC_W_I_VAR, 
			PC_DIM_XY, PC_DIM_TH, x_pc, y_pc, th_pc);
	
	double[][][] PC_W_EXCITE = pc.pc_w_excite;
	double[][][] PC_W_INHIB = pc.pc_w_inhib;
	double[][][] Posecells = pc.posecells;
	
	// Convenience constants
	double PC_W_E_DIM_HALF = Math.floor(PC_W_E_DIM / 2);
	double PC_W_I_DIM_HALF = Math.floor(PC_W_I_DIM / 2);
	double PC_C_SIZE_TH = (2 * PI) / PC_DIM_TH;

	// Lookups to wrap the pose cell excitation/inhibition weight steps
	double PC_E_XY_WRAP;	// = [(PC_DIM_XY - PC_W_E_DIM_HALF + 1):PC_DIM_XY 1:PC_DIM_XY 1:PC_W_E_DIM_HALF];
	double PC_E_TH_WRAP;	// = [(PC_DIM_TH - PC_W_E_DIM_HALF + 1):PC_DIM_TH 1:PC_DIM_TH 1:PC_W_E_DIM_HALF];
	double PC_I_XY_WRAP;	// = [(PC_DIM_XY - PC_W_I_DIM_HALF + 1):PC_DIM_XY 1:PC_DIM_XY 1:PC_W_I_DIM_HALF];
	double PC_I_TH_WRAP;	// = [(PC_DIM_TH - PC_W_I_DIM_HALF + 1):PC_DIM_TH 1:PC_DIM_TH 1:PC_W_I_DIM_HALF];
	// CONTINUE LATER

	// Lookups for finding the center of the posecell Posecells by rs_get_posecell_xyth()
	// HK EDIT START
	double PC_XY_SUM_SIN_LOOKUP;	// = sin((1:PC_DIM_XY).*2*pi/PC_DIM_XY);
	double PC_XY_SUM_COS_LOOKUP;	// = cos((1:PC_DIM_XY).*2*pi/PC_DIM_XY);
	double PC_TH_SUM_SIN_LOOKUP;	// = sin((1:PC_DIM_TH).*2*pi/PC_DIM_TH);
	double PC_TH_SUM_COS_LOOKUP;	// = cos((1:PC_DIM_TH).*2*pi/PC_DIM_TH);
	double PC_CELLS_TO_AVG = 3;
	double PC_AVG_XY_WRAP ;		// = [(PC_DIM_XY - PC_CELLS_TO_AVG + 1):PC_DIM_XY 1:PC_DIM_XY 1:PC_CELLS_TO_AVG];
	double PC_AVG_TH_WRAP;		// = [(PC_DIM_TH - PC_CELLS_TO_AVG + 1):PC_DIM_TH 1:PC_DIM_TH 1:PC_CELLS_TO_AVG];
	// HK EDIT END
	// CONTINUE LATER

	// Specify the movie and the frames to read
	static int START_FRAME = 1;
	static int END_FRAME = 100; // Should be size of viddata (photo, in our case)
	static int IMAGE_Y_SIZE = 100;		// Should be size of viddata
	static int IMAGE_X_SIZE = 100;		// Should be size of viddata

	// Used to process parameters
	static int RENDER_RATE = 1;
	static int BLOCK_READ = 10;

	// Other variables
	static int[] IMAGE_VT_Y_RANGE = Util.setRange(1, IMAGE_Y_SIZE);
	static int[] IMAGE_VT_X_RANGE =  Util.setRange(1, IMAGE_X_SIZE);
	static int[] IMAGE_ODO_X_RANGE =  Util.setRange((180+15), (460+15)); //st_lucia constants
	static int[] IMAGE_VTRANS_Y_RANGE =  Util.setRange(270, 430); //st_lucia constants
	static int[] IMAGE_VROT_Y_RANGE =  Util.setRange(75, 240); //st_lucia constants
	static double [] prev_vrot_image_x_sums;
	static double [] prev_vtrans_image_x_sums;

	static int POSECELL_VTRANS_SCALING = 100;

	int[] time_delta_s;
	// start stopwatch here

	static String MOV_FILE = "file:///Users/bentito/Downloads/stlucia_testloop.avi";
	// Main method of ratSLAM, not including constants (which are above, for the most part)
	public static void main(String[] args) {
		// Set initial position in the pose network
		double x_pc = (Math.floor(PC_DIM_XY / 2.0) + 1);
		double y_pc = (Math.floor(PC_DIM_XY / 2.0) + 1);
		double th_pc = (Math.floor(PC_DIM_TH / 2.0) + 1);

//		Posecells[x_pc][y_pc][th_pc] = 1;
		double[] max_act_xyth_path = {x_pc, y_pc, th_pc};

		// Set the initial position in the odo and experience map
		prev_vrot_image_x_sums = new double[IMAGE_ODO_X_RANGE.length];
		prev_vtrans_image_x_sums = new double[IMAGE_ODO_X_RANGE.length];

		Odo initOdo = new Odo (0, PI/2, prev_vtrans_image_x_sums, prev_vrot_image_x_sums, IMAGE_VTRANS_Y_RANGE,
				IMAGE_VROT_Y_RANGE, IMAGE_ODO_X_RANGE);

		// Specify movie and frames to read
		// In our case, specify image size, in x and y direction


		VideoSource vs = new VideoSource(MOV_FILE);
		vs.initialize();

		// store size in a variable
		// 5 used as random size
		ArrayList <VT> vts = new ArrayList <VT> ();
		ArrayList <Odo> odos = new ArrayList <Odo> ();
		ArrayList <Experience> exps = new ArrayList <Experience> ();
		ArrayList <Link> links = new ArrayList <Link> ();

		int numvts = 1;

		// Need to fix parameters; more specifically, array sizes
		vts.add( new VT(numvts, new double[]{},1.0,x_pc,y_pc,th_pc,1,1));
		odos.add(initOdo);

		//		vt[numvts].template_decay = 1.0;
		VT vtcurr = (VT) vts.get(0);
		vtcurr.template_decay = 1.0;

//		Experience[] exps = new Experience[5];
		// figure out where to get id
		exps.add(new Experience(0, x_pc, y_pc, th_pc, 0, 0, (PI/2), 1, 0, links));

		// Process the parameters
		//  nargin: number of arguments passed to main
		// varargin: input variable in a function definition statement that allows the function to accept any number of input arguments 
		//	1xN cell array, in which N is the number of inputs that the function receives after explicitly declared inputs;
		// 	use prompt.in
		for (int i = 0; i < (args.length - 3); i++) {
			// figure out varargin in Java
			// should be an array that is passed to the main(?) - double check
			switch(args[i]) {
			case "RENDER_RATE": RENDER_RATE = Integer.parseInt(args[i+1]);
			break;
			case "BLOCK_READ": BLOCK_READ = Integer.parseInt(args[i+1]);
			break;
			// HK EDIT START
			case "START_FRAME": START_FRAME = Integer.parseInt(args[i+1]);
			break;
			case "END_FRAME": END_FRAME = Integer.parseInt(args[i+1]);
			break;

			case "PC_VT_INJECT_ENERGY": PC_VT_INJECT_ENERGY = Integer.parseInt(args[i+1]);
			break;
			case "IMAGE_VT_Y_RANGE": IMAGE_VT_Y_RANGE = Util.setRange(Integer.parseInt(args[i+1]), Integer.parseInt(args[i+2]));
			break;
			case "IMAGE_VT_X_RANGE": IMAGE_VT_X_RANGE = Util.setRange(Integer.parseInt(args[i+1]), Integer.parseInt(args[i+2]));
			//					break;
			//				case "VT_SHIFT_MATCH": VT_SHIFT_MATCH = Integer.parseInt(args[i+1]);
			//					break;
			//				case "VT_MATCH_THRESHOLD": VT_MATCH_THRESHOLD = Integer.parseInt(args[i+1]);
			//					break;
			//					
			//				case "VTRANS_SCALE": VTRANS_SCALE = Integer.parseInt(args[i+1]);
			//					break;
			//				case "VISUAL_ODO_SHIFT_MATCH": VISUAL_ODO_SHIFT_MATCH = Integer.parseInt(args[i+1]);
			//					break;
			//				case "IMAGE_VTRANS_Y_RANGE": IMAGE_VTRANS_Y_RANGE = Integer.parseInt(args[i+1]);
			//					break;
			//				case "IMAGE_VROT_Y_RANGE": IMAGE_VROT_Y_RANGE = Integer.parseInt(args[i+1]);
			//					break;
			//				case "IMAGE_ODO_X_RANGE": IMAGE_ODO_X_RANGE = Integer.parseInt(args[i+1]);
			//					break;
			//				
			//				case "EXP_DELTA_PC_THRESHOLD": EXP_DELTA_PC_THRESHOLD = Integer.parseInt(args[i+1]);
			//					break;
			//				case "EXP_CORRECTION": EXP_CORRECTION = Integer.parseInt(args[i+1]);
			//					break;
			//				case "EXP_LOOPS": EXP_LOOPS = Integer.parseInt(args[i+1]);
			//					break;
			//				
			//				case "ODO_ROT_SCALING": ODO_ROT_SCALING = Integer.parseInt(args[i+1]);
			//					break;
			//				case "POSECELL_VTRANS_SCALING": POSECELL_VTRANS_SCALING = Integer.parseInt(args[i+1]);
			//					break;
			// HK EDIT END
			}
		}

		//		vtcurr.template = new double[IMAGE_VT_X_RANGE.length];


		int frameIdx = 0;
		while (vs.getState()==VideoSource.NOT_READY) { 
			try { Thread.sleep(100); } catch (Exception e) { } 
		} 
		if (vs.getState()==VideoSource.ERROR) { 
			System.out.println("Error while initing"+args[0]); 
			return; 
		}
		int frameCount = vs.getFrameCount();
		END_FRAME = frameCount;
		JFrame frame = new JFrame(); 
		frame.setVisible(true); 
		showOnScreen(0,frame);
		
		//Setup to display results
		Display display = new Display ("RatSlam Output");
		display.pack();
		RefineryUtilities.centerFrameOnScreen(display);
		showOnScreen(0,display);
		display.setVisible(true);
		DefaultXYDataset dataset = (DefaultXYDataset) display.dataset;
		
		for (frameIdx = 0; frameIdx < END_FRAME; frameIdx++) {
			// save the experience map information to the disk for later playback
			// read the avi file (in our case, the photo file) and record the delta time
			if (frameIdx % BLOCK_READ == 0)
			{
				// save
				//if (ODO_FILE != 0)
				//{

				//}
			} else {
				BufferedImage img = vs.getFrame(frameIdx);
				ImageFilter filter = new GrayFilter(true, 0);  
				ImageProducer producer = new FilteredImageSource(img.getSource(), filter);  
				Image grayImg = Toolkit.getDefaultToolkit().createImage(producer);  	
				drawFrame(frame,img, grayImg,frameIdx);
				Visual_Template viewTemplate = new Visual_Template(img, x_pc, y_pc, th_pc, img.getWidth(), img.getHeight(), vts);
				viewTemplate.visual_template();
				Visual_Odometry vo = new Visual_Odometry ();
				vo.visual_odometry(img, odos);
				//XXX: use odoData to track odo data for comparison as per Matlab main
				Posecell_Iteration pci = new Posecell_Iteration(vts.size(), odos.get(odos.size()-1).vtrans, odos.get(odos.size()-1).vrot, pc, vts);
//				pci.iteration();
//				double[] xyth = pc.getPosecellXYTH(pc_xy_sum_sin_lookup, pc_xy_sum_cos_lookup, pc_th_sum_sin_lookup,
//		                pc_th_sum_cos_lookup, pc_cells_to_avg, pc_avg_xy_wrap, pc_avg_th_wrap);
//				Exp_Map_Iteration(int vt_id, double vtrans, double vrot, double x_pc, double y_pc, double th_pc, ArrayList <VT> vt)
				th_pc = 25*Math.random(); // debugging Exp_Map_Iteration before working Posecell code available
				//TODO: x_pc, etc. are xyth[0-2]
				Exp_Map_Iteration exp = new Exp_Map_Iteration(vts.size(), odos.get(odos.size()-1).vtrans, odos.get(odos.size()-1).vrot,
						x_pc, y_pc, th_pc, vts , exps);
				exp.EXP_LOOPS = 200;
				exp.EXP_CORRECTION = 0.5;
				exp.iteration();
				dataset.addSeries("Experience Map", getExpsXY(exps));
			}
		}
	}

	public static void showOnScreen( int screen, JFrame frame ) {
	    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
	    GraphicsDevice[] gd = ge.getScreenDevices();
	    if( screen > -1 && screen < gd.length ) {
	        frame.setLocation(gd[screen].getDefaultConfiguration().getBounds().x, frame.getY());
	    } else if( gd.length > 0 ) {
	        frame.setLocation(gd[0].getDefaultConfiguration().getBounds().x, frame.getY());
	    } else {
	        throw new RuntimeException( "No Screens Found" );
	    }
	}
	
	public static double [][] getExpsXY(ArrayList <Experience> exps) {
		double [][] retArr = null;
		ArrayList <Double> xs = new ArrayList<Double>();
		ArrayList <Double> ys = new ArrayList<Double>();

		for (Experience exp : exps){
			xs.add(exp.x_m);
			ys.add(exp.y_m);
		}
		
		retArr = new double [2][exps.size()];
		for (int x=0; x<xs.size(); x++){
			retArr [0][x] = (double) xs.get(x);
		}
		for (int y=0; y<ys.size(); y++){
			retArr [1][y] = (double) ys.get(y);
		}
		return retArr;
	}
	
	public static void drawFrame(Frame frame, BufferedImage image,  Image grayImg, int index) { 
		if (image!=null) { 
			frame.setSize(image.getWidth(), image.getWidth()); 
			frame.getGraphics().drawImage(grayImg, 0, 0, null);
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} else { 
			System.out.println("null image"); 
		} 

	} 
}
