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
import javax.media.*;
import javax.swing.GrayFilter;

import java.awt.Frame;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.ImageProducer;
import java.io.File;

public class main_ratslam 
{
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

	// Posecell excitation and inhibition 3D weight matrices
	Posecells PC_W_EXCITE = new Posecells(PC_W_E_DIM, PC_W_E_VAR);
	Posecells PC_W_INHIB = new Posecells(PC_W_I_DIM, PC_W_I_VAR);

	// Convenience constants
	double PC_W_E_DIM_HALF = Math.floor(PC_W_E_DIM / 2);
	double PC_W_I_DIM_HALF = Math.floor(PC_W_I_DIM / 2);
	double PC_C_SIZE_TH = (2 * PI) / PC_DIM_TH;

	// Lookups to wrap the pose cell excitation/inhibition weight steps
	double PC_E_XY_WRAP;
	// continue later

	// Lookups for finding the center of the posecell Posecells by rs_get_posecell_xyth()
	double PC_XY_SUM_SIN_LOOKUP;
	// continue later 

	// Specify the movie and the frames to read
	static int START_FRAME = 1;
	static int END_FRAME = 100; // Should be size of viddata (photo, in our case)
	static int IMAGE_Y_SIZE = 100;		// Should be size of viddata
	static int IMAGE_X_SIZE = 100;		// Should be size of viddata

	// Used to process parameters
	static int RENDER_RATE = 1;
	static int BLOCK_READ = 10;


	static int[][][] Posecells =  new int[PC_DIM_XY][PC_DIM_XY][PC_DIM_TH];

	// Other variables
	static int[] IMAGE_VT_Y_RANGE = setRange(IMAGE_Y_SIZE);
	static int[] IMAGE_VT_X_RANGE = setRange(IMAGE_X_SIZE);
	static int[] IMAGE_ODO_X_RANGE = setRange(IMAGE_X_SIZE);
	static int[] IMAGE_VTRANS_Y_RANGE = setRange(IMAGE_Y_SIZE);
	static int[] IMAGE_VROT_Y_RANGE = setRange(IMAGE_Y_SIZE);
	static int[][] prev_vrot_image_x_sums;
	static int[][] prev_vtrans_image_x_sums;

	int[] time_delta_s;
	// start stopwatch here
	
	static String MOV_FILE = "file:///Users/bentito/Downloads/stlucia_testloop.avi";
	// Main method of ratSLAM, not including constants (which are above, for the most part)
	public static void main(String[] args) 
	{
		// Set initial position in the pose network
		int x_pc = (int) (Math.floor(PC_DIM_XY / 2.0) + 1);
		int y_pc = (int) (Math.floor(PC_DIM_XY / 2.0) + 1);
		int th_pc = (int) (Math.floor(PC_DIM_TH / 2.0) + 1);

		Posecells[x_pc][y_pc][th_pc] = 1;
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
		vt[1] = new VT(numvts, new int[][]{},1.0,x_pc,y_pc,th_pc,1,1,new Experience[5]);
		vt[numvts].template_decay = 1.0;
		Experience[] exps = new Experience[5];
		// figure out where to get id
		exps[1] = new Experience(0, x_pc, y_pc, th_pc, 0, 0, (PI/2), 1, 0, new Link[5]);

		// Process the parameters
		//  nargin: number of arguments passed to main
		// varargin: input variable in a function definition statement that allows the function to accept any number of input arguments 
		//	1xN cell array, in which N is the number of inputs that the function receives after explicitly declared inputs;
		// 	use prompt.in
		for (int i = 0; i < (args.length - 3); i++)
		{
			// figure out varargin in Java
			// should be an array that is passed to the main(?) - double check
			switch(args[i])
			{
			case "RENDER_RATE": RENDER_RATE = Integer.parseInt(args[i+1]);
			break;
			case "BLOCK_READ": BLOCK_READ = Integer.parseInt(args[i+1]);
			break;
			// ...
			}

			vt[1].template = new int[1][IMAGE_VT_X_RANGE.length];
			prev_vrot_image_x_sums = new int[1][IMAGE_ODO_X_RANGE.length];
			prev_vtrans_image_x_sums = new int[1][IMAGE_ODO_X_RANGE.length];
		}
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
	private static int[] setRange(int range)
	{
		int[] toReturn = new int[range];
		for (int i = 0; i < range; i++)
		{
			toReturn[i] = i;
		}
		return toReturn;
	}
	
	public static void drawFrame(Frame frame, BufferedImage image,  Image grayImg, int index) { 
	    if (image!=null) { 
	        frame.setSize(image.getWidth(), image.getWidth()); 
	        frame.getGraphics().drawImage(grayImg, 0, 0, null);
	        try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	        System.out.println("Image at index: "+index); 
	    } else { 
	        System.out.println("null image"); 
	    } 
	     
	} 
}
