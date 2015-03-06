import java.awt.Color;
import java.awt.Paint;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.util.Random;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.DefaultDrawingSupplier;
import org.jfree.chart.plot.FastScatterPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.ApplicationFrame;
import org.jfree.util.ShapeUtilities;

/**
 * @author bentito
 *
 * Display experience map and other data
 */
public class Display extends ApplicationFrame{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public JFreeChart chart;
	public DefaultXYDataset dataset = new DefaultXYDataset();

	public Display(final String title) {
		super(title);
//		dataset.addSeries("Experience Map",new double[][]{{1}, {1}});
		chart = ChartFactory.createScatterPlot(
				null,
				"X", "Y", 
				dataset, 
				PlotOrientation.VERTICAL,
				true, 
				true, 
				false
				);
		NumberAxis domainAxis = (NumberAxis) chart.getXYPlot().getDomainAxis();
		domainAxis.setAutoRangeIncludesZero(false);
		ChartPanel chartPanel = new ChartPanel(chart);
		chartPanel.setPreferredSize(new java.awt.Dimension(500, 270));
		chartPanel.setVerticalAxisTrace(true);
		chartPanel.setHorizontalAxisTrace(true);

		Shape cross = ShapeUtilities.createDiagonalCross(3, 1);

		XYPlot xyPlot = (XYPlot) chart.getPlot();
		XYItemRenderer renderer = xyPlot.getRenderer();
        renderer.setBaseShape(cross);
        renderer.setSeriesShape(0, cross);
        renderer.setSeriesPaint(0, Color.blue);
        xyPlot.setRenderer(renderer);
        
		setContentPane(chartPanel);
	}
}
