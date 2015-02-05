import org.ejml.data.DenseMatrix64F;
import org.ejml.equation.Equation;
import org.ejml.simple.SimpleMatrix;

// Translation of rs_compare_segments

public class Segments 
{
	double[] seg1, seg2, cdiff;
	int cwl;
	int slen;
	double[] segment;
	
	public Segments(double[] s1, double[] s2, int len, int cwl)
	{
		seg1 = s1;
		seg2 = s2;
		slen = len;
//		cdiff = new double [seg1.length];
		this.cwl = cwl;
	}
	
	public double[] compare_segments()
	{
		// assume a large difference
		double mindiff = 1000000;
		double minoffset = 0;
		double ddiff = 0;
		
//		double[] diffs = new double[slen];
		SimpleMatrix diffs = new SimpleMatrix (slen, slen); // What the heck is this matrix here for, visualization? Has no impact either here or the Matlab original
		DenseMatrix64F seg1 = DenseMatrix64F.wrap(cwl, 1, this.seg1);
		DenseMatrix64F seg2 = DenseMatrix64F.wrap(cwl, 1, this.seg2);
		SimpleMatrix sseg1 = SimpleMatrix.wrap(seg1);
		SimpleMatrix sseg2 = SimpleMatrix.wrap(seg2);
		SimpleMatrix cdiff = new SimpleMatrix ();
		Equation eq = new Equation ();
		
		// for each offset sum the abs difference between the two segments
		// for offset = 0 : slen
//		for (int offset = 0, index = 0; offset < slen; offset = offset + 5, index++){
//			cdiff[index] = 0;
//			for (int i = offset + 1, j = 0; i < cwl && j < (cwl - offset); i++, j++) {
////				cdiff[index] += Math.abs(seg1[i] - seg2[j]);
//			}
//			cdiff[index] = cdiff[index] / (cwl - offset);
//			diffs.set((slen - offset - 1), cdiff[index]);
//			if (cdiff[index] < mindiff) {
//				mindiff = cdiff[index];
//				minoffset = offset;
//			}
//		}
		
		
//		for each offset sum the abs difference between the two segments
		for (int offset = 0; offset <this.slen; offset++) {
//		    cdiff = abs(seg1(1 + offset:cwl) - seg2(1:cwl - offset));
		    cdiff = sseg1.extractMatrix(offset, cwl, 0, 1).minus(sseg2.extractMatrix(0, cwl-offset, 0, 1));
		    eq.alias(cdiff, "cdiff");
		    eq.process ("cdiff = abs(cdiff)");
//		    cdiff = sum(cdiff) / (cwl - offset); 
		    ddiff = cdiff.elementSum() / (cwl - offset);
//		    diffs(slen - offset + 1) = cdiff;
		    if (ddiff < mindiff) {
		        mindiff = ddiff;
		        minoffset = offset;
		    }
		}
		
		// for offset = 1 : slen
//		for (int offset = 0, index2 = 0; offset < slen; offset += 5, index2++) {
//			for (int i = 0, j = 1 + offset; i < (cwl - offset) && j < cwl; i++, j++) {
////				cdiff[index2] += Math.abs(seg1[i] - seg2[j]);
//			}
//			cdiff[index2] = cdiff[index2] / (cwl - offset);
//			diffs.set((slen + offset - 1), cdiff[index2]);
//			if (cdiff[index2] < mindiff) {
//				mindiff = cdiff[index2];
//				minoffset = -1 * offset;
//			}
//		}
		
		for (int offset = 0; offset <this.slen; offset++) {
//		    cdiff = abs(seg1(1 + offset:cwl) - seg2(1:cwl - offset));
		    cdiff = sseg1.extractMatrix(offset, cwl, 0, 1).minus(sseg2.extractMatrix(0, cwl-offset, 0, 1));
		    eq.alias(cdiff, "cdiff");
		    eq.process ("cdiff = abs(cdiff)");
//		    cdiff = sum(cdiff) / (cwl - offset); 
		    ddiff = cdiff.elementSum() / (cwl - offset);
//		    diffs(slen - offset + 1) = cdiff;
		    if (ddiff < mindiff) {
		        mindiff = ddiff;
		        minoffset = offset;
		    }
		}
		
		return new double[]{minoffset, mindiff};
	}
}
