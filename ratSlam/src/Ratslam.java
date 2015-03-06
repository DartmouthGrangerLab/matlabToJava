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

public class Ratslam {
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
	Double PC_W_E_DIM_HALF = Math.floor(PC_W_E_DIM / 2);
	Double PC_W_I_DIM_HALF = Math.floor(PC_W_I_DIM / 2);
	Double PC_C_SIZE_TH = (2 * PI) / PC_DIM_TH;
	
	
	// Lookups to wrap the pose cell excitation/inhibition weight steps

//	int [] PC_E_XY_WRAP = Util.setRange((PC_DIM_XY - PC_W_E_DIM_HALF.intValue() + 1), PC_W_E_DIM_HALF.intValue());//[(PC_DIM_XY - PC_W_E_DIM_HALF + 1):PC_DIM_XY 1:PC_DIM_XY 1:PC_W_E_DIM_HALF];
	static int [] PC_E_XY_WRAP = {59,60,0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,
			40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,0,1,2,3};
	
//	double PC_E_TH_WRAP;	// = [(PC_DIM_TH - PC_W_E_DIM_HALF + 1):PC_DIM_TH 1:PC_DIM_TH 1:PC_W_E_DIM_HALF];
	
	static int [] PC_E_TH_WRAP = {34,35,0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,0,1,2,3};
	
//	double PC_I_XY_WRAP;	// = [(PC_DIM_XY - PC_W_I_DIM_HALF + 1):PC_DIM_XY 1:PC_DIM_XY 1:PC_W_I_DIM_HALF];
	
	static int [] PC_I_XY_WRAP ={60,0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,0,1,2};
	
//	double PC_I_TH_WRAP;	// = [(PC_DIM_TH - PC_W_I_DIM_HALF + 1):PC_DIM_TH 1:PC_DIM_TH 1:PC_W_I_DIM_HALF];
	static int [] PC_I_TH_WRAP ={35,0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,0,1,2};
	// CONTINUE LATER

	// Lookups for finding the center of the posecell Posecells by rs_get_posecell_xyth()
	// = sin((1:PC_DIM_XY).*2*pi/PC_DIM_XY)
	static final double [] PC_XY_SUM_SIN_LOOKUP = {0.102820997137360,0.204552066126201,0.304114832327518,0.400453905651266,0.492548067953864,0.579421098204564,0.660152120671232,0.733885366432199,0.799839244739719,0.857314628076332,0.905702263080471,0.944489228783661,0.973264373700383,0.991722674136102,0.999668467514313,0.997017526448527,0.983797951573516,0.960149873671602,0.926323968251495,0.882678798325547,0.829677013552619,0.767880446036600,0.697944154766344,0.620609481827423,0.536696193991601,0.447093792985114,0.352752086549095,0.254671120241229,0.153890576704062,0.0514787547703467,-0.0514787547703465,-0.153890576704061,-0.254671120241229,-0.352752086549095,-0.447093792985114,-0.536696193991600,-0.620609481827423,-0.697944154766343,-0.767880446036600,-0.829677013552619,-0.882678798325547,-0.926323968251495,-0.960149873671602,-0.983797951573516,-0.997017526448527,-0.999668467514313,-0.991722674136102,-0.973264373700383,-0.944489228783661,-0.905702263080472,-0.857314628076332,-0.799839244739719,-0.733885366432199,-0.660152120671232,-0.579421098204564,-0.492548067953865,-0.400453905651267,-0.304114832327518,-0.204552066126201,-0.102820997137361,-2.44929359829471e-16};
	// = cos((1:PC_DIM_XY).*2*pi/PC_DIM_XY)
	static final double [] PC_XY_SUM_COS_LOOKUP = {0.994699875614589,0.978855685095358,0.952635380803383,0.916316904487005,0.870285241030155,0.815028337516811,0.751131930870520,0.679273338897293,0.600214280548368,0.514792801509831,0.423914390709861,0.328542381910835,0.229687742131796,0.128398355146551,0.0257479136549887,-0.0771754621266462,-0.179280758810736,-0.279485634851609,-0.376727893635185,-0.469976743027320,-0.558243722026865,-0.640593178698175,-0.716152188314393,-0.784119806576710,-0.843775559823186,-0.894487082228796,-0.935716819040494,-0.967027724791320,-0.988087896091077,-0.998674089884831,-0.998674089884831,-0.988087896091077,-0.967027724791320,-0.935716819040494,-0.894487082228796,-0.843775559823186,-0.784119806576710,-0.716152188314394,-0.640593178698175,-0.558243722026865,-0.469976743027321,-0.376727893635185,-0.279485634851610,-0.179280758810736,-0.0771754621266464,0.0257479136549877,0.128398355146551,0.229687742131795,0.328542381910834,0.423914390709861,0.514792801509831,0.600214280548368,0.679273338897293,0.751131930870520,0.815028337516811,0.870285241030155,0.916316904487004,0.952635380803383,0.978855685095358,0.994699875614589,1};
	// = sin((1:PC_DIM_TH).*2*pi/PC_DIM_TH);
	static final double [] PC_TH_SUM_SIN_LOOKUP = {0.173648177666930,0.342020143325669,0.500000000000000,0.642787609686539,0.766044443118978,0.866025403784439,0.939692620785908,0.984807753012208,1,0.984807753012208,0.939692620785908,0.866025403784439,0.766044443118978,0.642787609686540,0.500000000000000,0.342020143325669,0.173648177666930,1.22464679914735e-16,-0.173648177666930,-0.342020143325669,-0.500000000000000,-0.642787609686539,-0.766044443118978,-0.866025403784439,-0.939692620785908,-0.984807753012208,-1,-0.984807753012208,-0.939692620785908,-0.866025403784439,-0.766044443118978,-0.642787609686540,-0.500000000000000,-0.342020143325669,-0.173648177666930,-2.44929359829471e-16};
	// = cos((1:PC_DIM_TH).*2*pi/PC_DIM_TH);
	static final double [] PC_TH_SUM_COS_LOOKUP = {0.984807753012208,0.939692620785908,0.866025403784439,0.766044443118978,0.642787609686539,0.500000000000000,0.342020143325669,0.173648177666930,6.12323399573677e-17,-0.173648177666930,-0.342020143325669,-0.500000000000000,-0.642787609686539,-0.766044443118978,-0.866025403784439,-0.939692620785908,-0.984807753012208,-1,-0.984807753012208,-0.939692620785908,-0.866025403784439,-0.766044443118978,-0.642787609686540,-0.500000000000000,-0.342020143325669,-0.173648177666930,-1.83697019872103e-16,0.173648177666930,0.342020143325669,0.499999999999999,0.642787609686539,0.766044443118978,0.866025403784439,0.939692620785908,0.984807753012208,1};
	static final int PC_CELLS_TO_AVG = 3;
	// = [(PC_DIM_XY - PC_CELLS_TO_AVG + 1):PC_DIM_XY 1:PC_DIM_XY 1:PC_CELLS_TO_AVG];
	static final double [] PC_AVG_XY_WRAP = {59,60,0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,0,1,2,3};
	// = [(PC_DIM_TH - PC_CELLS_TO_AVG + 1):PC_DIM_TH 1:PC_DIM_TH 1:PC_CELLS_TO_AVG];
	static final double [] PC_AVG_TH_WRAP = {34,35,0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,0,1,2,3};


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
	static double[] xyth;
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
		frame.setSize(640, 480); 
		frame.setVisible(true);
		
//		showOnScreen(0,frame);
		
		//Setup to display results
		Display display = new Display ("RatSlam Output");
		display.pack();
		RefineryUtilities.centerFrameOnScreen(display);
//		showOnScreen(0,display);
		display.setVisible(true);
		DefaultXYDataset dataset = (DefaultXYDataset) display.dataset;
		
		ExpMapIteration expItr = new ExpMapIteration();
		ExpMapIteration.EXP_LOOPS = 100;
		ExpMapIteration.EXP_CORRECTION = 0.5;
		
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
				drawFrame(frame,img, img,frameIdx);
				System.out.println("debug: frame: "+ frameIdx);
				VisualTemplate viewTemplate = new VisualTemplate(img, x_pc, y_pc, th_pc, img.getWidth(), img.getHeight(), vts);
				viewTemplate.visual_template();
				VisualOdometry vo = new VisualOdometry ();
				vo.visual_odometry(img, odos);
				//XXX: use odoData to track odo data for comparison as per Matlab main
				Posecell_Iteration pci = new Posecell_Iteration(vts.size(), odos.get(odos.size()-1).vtrans, odos.get(odos.size()-1).vrot, pc, vts, PC_E_XY_WRAP, PC_E_TH_WRAP, PC_I_XY_WRAP, PC_I_TH_WRAP);
				pci.iteration();

				xyth = pc.getPosecellXYTH(PC_XY_SUM_SIN_LOOKUP, PC_XY_SUM_COS_LOOKUP, PC_TH_SUM_SIN_LOOKUP,
		                PC_TH_SUM_COS_LOOKUP, PC_CELLS_TO_AVG, PC_AVG_XY_WRAP, PC_AVG_TH_WRAP);
//				System.out.println("debug: odo.vtrans: "+ odos.get(odos.size()-1).vtrans);
				
				x_pc = xyth[0];
				y_pc = xyth[1];
				th_pc = xyth[2];

				expItr.iterate(vts.size(), odos.get(odos.size()-1).vtrans, odos.get(odos.size()-1).vrot,
						x_pc, y_pc, th_pc, vts , exps);

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
//		System.out.println("debug: x_m: "+exps.get(exps.size()-1).x_m);
//		System.out.println("debug: y_m: "+exps.get(exps.size()-1).y_m);
		
		retArr = new double [2][exps.size()];
		for (int x=0; x<xs.size(); x++){
			retArr [0][x] = (double) xs.get(x);
		}
		for (int y=0; y<ys.size(); y++){
			retArr [1][y] = (double) ys.get(y);
		}
		return retArr;
	}
	
	public static void drawFrame(Frame frame, BufferedImage image,  Image colorImg, int index) { 
		if (image!=null) { 
//			frame.setSize(image.getWidth(), image.getHeight()); 
			frame.getGraphics().drawImage(colorImg, 0, 0, null);
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
