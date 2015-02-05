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

import com.opencsv.CSVReader;

import java.awt.Frame;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.ImageProducer;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Scanner;

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
	Posecells pc = new Posecells(PC_W_E_DIM, PC_W_E_VAR, PC_W_I_DIM, PC_W_I_VAR, 
			PC_DIM_XY, PC_DIM_TH, x_pc, y_pc, y_pc);
	
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
	double[] PC_XY_SUM_SIN_LOOKUP;	// = sin((1:PC_DIM_XY).*2*pi/PC_DIM_XY);
	double[] PC_XY_SUM_COS_LOOKUP;	// = cos((1:PC_DIM_XY).*2*pi/PC_DIM_XY);
	double[] PC_TH_SUM_SIN_LOOKUP;	// = sin((1:PC_DIM_TH).*2*pi/PC_DIM_TH);
	double[] PC_TH_SUM_COS_LOOKUP;	// = cos((1:PC_DIM_TH).*2*pi/PC_DIM_TH);
	int PC_CELLS_TO_AVG = 3;
	double[] PC_AVG_XY_WRAP ;		// = [(PC_DIM_XY - PC_CELLS_TO_AVG + 1):PC_DIM_XY 1:PC_DIM_XY 1:PC_CELLS_TO_AVG];
	double[] PC_AVG_TH_WRAP;		// = [(PC_DIM_TH - PC_CELLS_TO_AVG + 1):PC_DIM_TH 1:PC_DIM_TH 1:PC_CELLS_TO_AVG];

	
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
	static int[] IMAGE_VT_Y_RANGE = setRange(1, IMAGE_Y_SIZE);
	static int[] IMAGE_VT_X_RANGE = setRange(1, IMAGE_X_SIZE);
	static int[] IMAGE_ODO_X_RANGE = setRange(1, IMAGE_X_SIZE);
	static int[] IMAGE_VTRANS_Y_RANGE = setRange(1, IMAGE_Y_SIZE);
	static int[] IMAGE_VROT_Y_RANGE = setRange(1, IMAGE_Y_SIZE);
	static int VT_SHIFT_MATCH = 20;
	static int VTRANS_SCALE = 100;
	static int VISUAL_ODO_SHIFT_MATCH = 140;
	static double VT_MATCH_THRESHOLD = 0.09;
	static int[][] prev_vrot_image_x_sums;
	static int[][] prev_vtrans_image_x_sums;
	
	static double EXP_DELTA_PC_THRESHOLD = 1.0;
	static double EXP_CORRECTION = 0.5;
	static int EXP_LOOPS = 100;
	static double ODO_ROT_SCALING = PI / 180 / 7;
	static double POSECELL_VTRANS_SCALING = 1.0;

	int[] time_delta_s;
	// start stopwatch here
	
//	static String MOV_FILE = "file:///Users/bentito/Downloads/stlucia_testloop.avi";
	static String MOV_FILE = "/Users/hkim/Desktop/test1.mp4";
	// Main method of ratSLAM, not including constants (which are above, for the most part)
	public static void main(String[] args) {
		int[] max_act_xyth_path = {x_pc, y_pc, th_pc};

		// Set the initial position in the odo and experience map
		double[] odo = {0, 0, (PI / 2)};
	
		// Specify movie and frames to read
		// In our case, specify image size, in x and y direction

		VideoSource vs = new VideoSource(MOV_FILE);
		vs.initialize();

		// store size in a variable
		// 5 used as random size
		VT[] vt = new VT[5];
		int numvts = 1;
		
		// Need to fix parameters; more specifically, array sizes
		vt[1] = new VT(numvts, new int[][]{},1.0,x_pc,y_pc,th_pc,1,1, new Experience[5]);
		vt[numvts].template_decay = 1.0;
		Experience[] exps = new Experience[5];
		// figure out where to get id
		exps[1] = new Experience(0, x_pc, y_pc, th_pc, 0, 0, (PI/2), 1, 0, new Link[5]);

		
		
		// Process the parameters
		//  nargin: number of arguments passed to main
		// varargin: input variable in a function definition statement that allows the function to accept any number of input arguments 
		//	1xN cell array, in which N is the number of inputs that the function receives after explicitly declared inputs;
		// 	use prompt.in
		for (int i = 0; i < (args.length - 3); i++) {
			// figure out varargin in Java
			// should be an array that is passed to the main(?) - double check
			System.out.println("current arg is " + args[i]);
			switch(args[i]) {
				case "RENDER_RATE": RENDER_RATE = Integer.parseInt(args[i+1]);
					System.out.println("case RENDER_RATE");
					break;
				case "BLOCK_READ": BLOCK_READ = Integer.parseInt(args[i+1]);
					System.out.println("case BLOCK_READ");
					break;
				// HK EDIT START
				case "START_FRAME": START_FRAME = Integer.parseInt(args[i+1]);
					System.out.println("case START_FRAME");
					break;
				case "END_FRAME": END_FRAME = Integer.parseInt(args[i+1]);
					System.out.println("case END_FRAME");
					break;
				case "PC_VT_INJECT_ENERGY": PC_VT_INJECT_ENERGY = Integer.parseInt(args[i+1]);
					System.out.println("case PC_VT_INJECT_ENERGY");
					break;
				case "IMAGE_VT_Y_RANGE": IMAGE_VT_Y_RANGE = create1DArray(args[i+1]);
					System.out.println("case IMAGE_VT_Y_RANGE");
					break;
				case "IMAGE_VT_X_RANGE": IMAGE_VT_X_RANGE = create1DArray(args[i+1]);
					System.out.println("case IMAGE_VT_X_RANGE");
					break;
				case "VT_SHIFT_MATCH": VT_SHIFT_MATCH = Integer.parseInt(args[i+1]);
					System.out.println("case VT_SHIFT_MATCH");
					break;
				case "VT_MATCH_THRESHOLD": VT_MATCH_THRESHOLD = Double.parseDouble(args[i+1]);
					System.out.println("case VT_MATCH_THRESHOLD");
					break;
					
				case "VTRANS_SCALE": VTRANS_SCALE = Integer.parseInt(args[i+1]);
					System.out.println("case VTRANS_SCALE");
					break;
				case "VISUAL_ODO_SHIFT_MATCH": VISUAL_ODO_SHIFT_MATCH = Integer.parseInt(args[i+1]);
					System.out.println("case VISUAL_ODO_SHIFT_MATCH");
					break;
				case "IMAGE_VTRANS_Y_RANGE": IMAGE_VTRANS_Y_RANGE = create1DArray(args[i+1]);
					System.out.println("case IMAGE_VTRANS_Y_RANGE");
					break;
				case "IMAGE_VROT_Y_RANGE": IMAGE_VROT_Y_RANGE = create1DArray(args[i+1]);
					System.out.println("case IMAGE_VROT_Y_RANGE");
					break;
				case "IMAGE_ODO_X_RANGE": IMAGE_ODO_X_RANGE = create1DArray(args[i+1]);
					System.out.println("case IMAGE_ODO_X_RANGE");
					break;
				
				case "EXP_DELTA_PC_THRESHOLD": EXP_DELTA_PC_THRESHOLD = Double.parseDouble(args[i+1]);
					System.out.println("case EXP_DELTA_PC_THRESHOLD");
					break;
				case "EXP_CORRECTION": EXP_CORRECTION = Double.parseDouble(args[i+1]);
					System.out.println("case EXP_CORRECTION");
					break;
				case "EXP_LOOPS": EXP_LOOPS = Integer.parseInt(args[i+1]);
					System.out.println("case EXP_LOOPS");
					break;
				
				case "ODO_ROT_SCALING": ODO_ROT_SCALING = Double.parseDouble(args[i+1]);
					System.out.println("case ODO_ROT_SCALING");
					break;
				case "POSECELL_VTRANS_SCALING": POSECELL_VTRANS_SCALING = Double.parseDouble(args[i+1]);
					System.out.println("case POSECELL_VTRANS_SCALING");
					break;
				// HK EDIT END
			}

			// VISUAL TEMPLATE STUFF- EXAMINE LATER
			// ============
			vt[1].template = new int[1][IMAGE_VT_X_RANGE.length];
			// ============
			prev_vrot_image_x_sums = new int[1][IMAGE_ODO_X_RANGE.length];
			prev_vtrans_image_x_sums = new int[1][IMAGE_ODO_X_RANGE.length];
		}
		
		// TESTING PURPOSES - START
		System.out.println("RENDER_RATE = " + RENDER_RATE);
		System.out.println("BLOCK_READ = " + BLOCK_READ);
		System.out.println("START_FRAME = " + START_FRAME);
		System.out.println("END_FRAME = " + END_FRAME);
		System.out.println("PC_VT_INJECT_ENERGY = " + PC_VT_INJECT_ENERGY);
		System.out.println("IMAGE_VT_Y_RANGE = " + IMAGE_VT_Y_RANGE[0] + " - " + IMAGE_VT_Y_RANGE[IMAGE_VT_Y_RANGE.length-1]);
		System.out.println("IMAGE_VT_X_RANGE = " + IMAGE_VT_X_RANGE[0] + " - " + IMAGE_VT_X_RANGE[IMAGE_VT_X_RANGE.length-1]);
		System.out.println("VT_SHIFT_MATCH = " + VT_SHIFT_MATCH);
		System.out.println("VT_MATCH_THRESHOLD = " + VT_MATCH_THRESHOLD);
		System.out.println("VTRANS_SCALE = " + VTRANS_SCALE);
		System.out.println("VISUAL_ODO_SHIFT_MATCH = " + VISUAL_ODO_SHIFT_MATCH);
		System.out.println("IMAGE_VTRANS_Y_RANGE = " + IMAGE_VTRANS_Y_RANGE[0] + " - " + IMAGE_VTRANS_Y_RANGE[IMAGE_VTRANS_Y_RANGE.length-1]);
		System.out.println("IMAGE_VROT_Y_RANGE = " + IMAGE_VROT_Y_RANGE[0] + " - " + IMAGE_VROT_Y_RANGE[IMAGE_VROT_Y_RANGE.length-1]);
		System.out.println("IMAGE_ODO_X_RANGE = " + IMAGE_ODO_X_RANGE[0] + " - " + IMAGE_ODO_X_RANGE[IMAGE_ODO_X_RANGE.length-1]);
		System.out.println("EXP_DELTA_PC_THRESHOLD = " + EXP_DELTA_PC_THRESHOLD);
		System.out.println("EXP_CORRECTION = " + EXP_CORRECTION);
		System.out.println("EXP_LOOPS = " + EXP_LOOPS);
		System.out.println("ODO_ROT_SCALING = " + ODO_ROT_SCALING);
		System.out.println("POSECELL_VTRANS_SCALING = " + POSECELL_VTRANS_SCALING);
		
		System.exit(0);
		// TESTING PURPOSES - END
		
		
		
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
		Frame frame = new Frame(); 
	    frame.setVisible(true); 
	    int vt_id;
	    
		for (frameIdx = 0; frameIdx < END_FRAME; frameIdx++)
		{
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
				Visual_Template viewTemplate = new Visual_Template(img, x_pc, y_pc, th_pc, vs.vidWidth, vs.vidHeight, exps);
				viewTemplate.visual_template();
			}
		}

		// call functions
	}

	// Creates 1-dimensional arrays that mimic MATLAB colon operator
	// HK EDIT (to allow for range that doesn't start with 1)
	private static int[] setRange(int lower, int upper) {
		int[] toReturn = new int[upper - lower + 1];
		for (int i = 0, j = lower; i < toReturn.length && j <= upper; i++, j++) {
			toReturn[i] = j;
		}
		return toReturn;
	}
	
	private static int[] create1DArray(String rangeString) {
		String[] range = rangeString.split(":");
		int lower = Integer.parseInt(range[0]);
		int upper = Integer.parseInt(range[1]);
		return setRange(lower, upper);
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
	        System.out.println("Image at index: "+index); 
	    } else { 
	        System.out.println("null image"); 
	    } 
	     
	} 
}
