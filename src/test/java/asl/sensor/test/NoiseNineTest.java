package asl.sensor.test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeriesCollection;
import org.junit.Test;

import asl.sensor.experiment.Experiment;
import asl.sensor.experiment.ExperimentEnum;
import asl.sensor.experiment.ExperimentFactory;
import asl.sensor.experiment.NoiseNineExperiment;
import asl.sensor.gui.ExperimentPanel;
import asl.sensor.gui.ExperimentPanelFactory;
import asl.sensor.gui.InputPanel;
import asl.sensor.gui.NoiseNinePanel;
import asl.sensor.gui.RandomizedPanel;
import asl.sensor.input.DataStore;
import asl.sensor.utils.ReportingUtils;
import asl.sensor.utils.TimeSeriesUtils;

public class NoiseNineTest {

  String currentDir = System.getProperty("user.dir");

  String folder = currentDir + "/data/noisenine/";
  String[] types = new String[]{"00","10","60"};
  String freqName = "_BH";
  String[] components = new String[]{"1","2","Z"};
  String ending = ".512.seed";

  String respName = "responses/RESP.XX.NS088..BHZ.STS1.360.2400";

  @Test
  public void canRunAndPlotTest(){

    DataStore ds = new DataStore();

    for (int i = 0; i < types.length; ++i) {
      for (int j = 0; j < components.length; ++j) {
        int indexInStore = i * types.length + j;

        String fName = folder + types[i] + freqName + components[j] + ending;

        System.out.println(fName);

        List<String> dataNames = new ArrayList<String>();
        try {
          dataNames = new ArrayList<String>( 
              TimeSeriesUtils.getMplexNameSet(fName) );
        } catch (FileNotFoundException e) {
          fail();
          e.printStackTrace();
        }
        ds.setData(indexInStore, fName, dataNames.get(0) );
        ds.setResponse(indexInStore, respName);
      }
    }

    SimpleDateFormat sdf = InputPanel.SDF;
    sdf.setTimeZone( TimeZone.getTimeZone("UTC") );
    // sdf.setLenient(false);

    Calendar cCal = Calendar.getInstance( sdf.getTimeZone() );
    cCal.setTimeInMillis( ds.getBlock(0).getStartTime() / 1000 );
    cCal.set(Calendar.HOUR, 7);
    System.out.println( "start: " + sdf.format( cCal.getTime() ) );
    long start = cCal.getTime().getTime() * 1000L;
    cCal.set(Calendar.HOUR, 8);
    cCal.set(Calendar.MINUTE, 30);
    System.out.println( "end: " + sdf.format( cCal.getTime() ) );
    long end = cCal.getTime().getTime() * 1000L;

    ds.trimAll(start, end);

    NoiseNineExperiment nne = new NoiseNineExperiment();
    assertTrue( nne.hasEnoughData(ds) );
    nne.setData(ds);

    List<XYSeriesCollection> xysc = nne.getData();
    JFreeChart[] jfcl = new JFreeChart[xysc.size()];

    String xAxisTitle = "Frequency (Hz)";
    NumberAxis xAxis = new LogarithmicAxis(xAxisTitle);
    Font bold = xAxis.getLabelFont().deriveFont(Font.BOLD);
    xAxis.setLabelFont(bold);
    String yAxisTitle = "Power (rel. 1 (m/s^2)^2/Hz)";
    String[] orientations = new String[]{" (North)", " (East)", " (Vertical)"};

    for (int i = 0; i < xysc.size(); ++i) {
      jfcl[i] = ChartFactory.createXYLineChart(
          ExperimentEnum.RANDM.getName() + orientations[i],
          xAxisTitle,
          yAxisTitle,
          xysc.get(i),
          PlotOrientation.VERTICAL,
          true,
          false,
          false);

      XYPlot xyp = jfcl[i].getXYPlot();

      //xyp.clearAnnotations();
      //xyp.addAnnotation(xyt);

      xyp.setDomainAxis( xAxis );
    }

    StringBuilder sb = new StringBuilder();
    // sb.append( NoiseNinePanel.getInsetString(nne) );
    // sb.append('\n');
    sb.append( NoiseNinePanel.getTimeStampString(nne) );
    sb.append('\n');
    sb.append("RESPONSES USED:");
    sb.append('\n');
    sb.append( nne.getResponseNames() );

    int width = 1280; int height = 960;

    PDDocument pdf = new PDDocument();
    ReportingUtils.chartsToPDFPage(width, height, pdf, jfcl);
    ReportingUtils.textToPDFPage( sb.toString(), pdf );

    String testResultFolder = currentDir + "/testResultImages/";
    File dir = new File(testResultFolder);
    if ( !dir.exists() ) {
      dir.mkdir();
    }

    String testResult = testResultFolder + "Random-Calib-Test.pdf";
    try {
      pdf.save( new File(testResult) );
      pdf.close();
    } catch (IOException e) {
      e.printStackTrace();
      fail();
    }
    
    System.out.println("Output result has been written");

  }

}
