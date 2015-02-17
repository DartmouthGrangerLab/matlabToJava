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
	
	/**
	 * [offset, sdif] = rs_compare_segments(seg1, seg2, slen, cwl)
	 * determine the best match between seg1 and seg2 of size cw1 allowing for shifts of up to slen
	 */
	public double[] compare_segments() {
		// assume a large difference
		double mindiff = 1000000;
		double minoffset = 0;
		double ddiff = 0;
		
//		double[] diffs = new double[slen];
//		SimpleMatrix diffs = new SimpleMatrix (slen, slen); // What the heck is this matrix here for, visualization? Has no impact either here or the Matlab original
		DenseMatrix64F seg1 = DenseMatrix64F.wrap(cwl, 1, this.seg1);
		DenseMatrix64F seg2 = DenseMatrix64F.wrap(cwl, 1, this.seg2);
		SimpleMatrix sseg1 = SimpleMatrix.wrap(seg1);
		SimpleMatrix sseg2 = SimpleMatrix.wrap(seg2);
		SimpleMatrix cdiff = new SimpleMatrix ();
		Equation eq = new Equation ();
		
		// for each offset sum the abs difference between the two segments
		
		for (int offset = 0; offset <this.slen; offset++) {
		    cdiff = sseg1.extractMatrix(offset, cwl, 0, 1).minus(sseg2.extractMatrix(0, cwl-offset, 0, 1));
		    eq.alias(cdiff, "cdiff");
		    eq.process ("cdiff = abs(cdiff)");
		    ddiff = cdiff.elementSum() / (cwl - offset);
		    if (ddiff < mindiff) {
		        mindiff = ddiff;
		        minoffset = offset;
		    }
		}
		
		for (int offset = 0; offset <this.slen; offset++) {
		    cdiff = sseg1.extractMatrix(0, cwl-offset, 0, 1).minus(sseg2.extractMatrix(offset, cwl, 0, 1));
		    eq.alias(cdiff, "cdiff");
		    eq.process ("cdiff = abs(cdiff)");
		    ddiff = cdiff.elementSum() / (cwl - offset);

		    if (ddiff < mindiff) {
		        mindiff = ddiff;
		        minoffset = -offset;
		    }

		}
		
		return new double[]{minoffset, mindiff};
	}
}
