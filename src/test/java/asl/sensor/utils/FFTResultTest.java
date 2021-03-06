package asl.sensor.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import asl.sensor.input.DataBlock;
import asl.sensor.input.InstrumentResponse;
import asl.sensor.test.TestUtils;
import edu.iris.dmc.seedcodec.CodecException;
import edu.sc.seis.seisFile.mseed.SeedFormatException;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.imageio.ImageIO;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;
import org.apache.commons.math3.util.Pair;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.junit.Test;

public class FFTResultTest {

  private static final String folder = TestUtils.TEST_DATA_LOCATION + TestUtils.SUBPAGE;

  @Test
  public void testGetFreqIndex_freqInList() {
    double[] freqs = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};
    assertEquals(5, FFTResult.getIndexOfFrequency(freqs, 5));
  }

  @Test
  public void testGetFreqIndex_freqBetweenValues() {
    double[] freqs = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};
    assertEquals(5, FFTResult.getIndexOfFrequency(freqs, 5.25));
  }

  @Test
  public void testGetFreqIndex_freqLessThanBounds() {
    double[] freqs = {5, 6, 7, 8, 9, 10, 11, 12};
    assertEquals(0, FFTResult.getIndexOfFrequency(freqs, 2));
  }

  @Test
  public void testGetFreqIndex_freqGreaterThanBounds() {
    double[] freqs = {5, 6, 7, 8, 9, 10, 11, 12};
    assertEquals(freqs.length - 1, FFTResult.getIndexOfFrequency(freqs, 50));
  }

  @Test
  public void calcPSDTest() {
    String dataName = folder + "psd-check/" + "00_LHZ.512.seed";
    try {
      DataBlock db = TimeSeriesUtils.getFirstTimeSeries(dataName);
      assertEquals(86400, db.getData().length);
      // InstrumentResponse ir = new InstrumentResponse(respName);
      FFTResult psd = FFTResult.spectralCalc(db, db);
      Complex[] spect = psd.getFFT();
      System.out.println(spect.length);
      for (int i = 0; i < 10; ++i) {
        System.out.println(spect[i]);
      }
      assertEquals(spect.length, 16385);
      double deltaFreq = psd.getFreq(1);
      int lowIdx = (int) Math.ceil(1. / (deltaFreq * 5.));
      int highIdx = (int) Math.floor(1. / (deltaFreq * 3.));
      assertEquals(0.200012207031, psd.getFreq(lowIdx), 1E-12);
      assertEquals(0.333312988281, psd.getFreq(highIdx), 1E-12);
      Complex[] spectTrim = Arrays.copyOfRange(spect, lowIdx, highIdx);
      double[] psdAmp = new double[spectTrim.length];
      for (int i = 0; i < spectTrim.length; ++i) {
        psdAmp[i] = 10 * Math.log10(spectTrim[i].abs());
      }
      double mean = TimeSeriesUtils.getMean(psdAmp);
      System.out.println(mean);
      assertEquals(55.314, mean, 5E-3);
    } catch (SeedFormatException | CodecException | IOException e) {
      e.printStackTrace();
      fail();
    }
  }

  @Test
  public void calcPSDTestBHZ() {
    String dataName = folder + "psd-check/" + "00_BHZ.512.seed";
    try {
      DataBlock db = TimeSeriesUtils.getFirstTimeSeries(dataName);
      assertEquals(1728000, db.getData().length);
      // InstrumentResponse ir = new InstrumentResponse(respName);
      FFTResult psd = FFTResult.spectralCalc(db, db);
      Complex[] spect = psd.getFFT();
      System.out.println(spect.length);
      for (int i = 0; i < 10; ++i) {
        System.out.println(spect[i]);
      }
      assertEquals(262145, spect.length);
      double deltaFreq = psd.getFreq(1);
      int lowIdx = (int) Math.ceil(1. / (deltaFreq * 5.));
      int highIdx = (int) Math.floor(1. / (deltaFreq * 3.));
      assertEquals(0.20, psd.getFreq(lowIdx), 1E-5);
      assertEquals(0.33333, psd.getFreq(highIdx), 1E-5);
      Complex[] spectTrim = Arrays.copyOfRange(spect, lowIdx, highIdx);
      double[] psdAmp = new double[spectTrim.length];
      for (int i = 0; i < spectTrim.length; ++i) {
        psdAmp[i] = 10 * Math.log10(spectTrim[i].abs());
      }
      double mean = TimeSeriesUtils.getMean(psdAmp);
      System.out.println(mean);
      assertEquals(55.477, mean, 5E-3);
    } catch (SeedFormatException | CodecException | IOException e) {
      e.printStackTrace();
      fail();
    }
  }

  @Test
  public void cosineTaperTest() {
    double[] x = {5, 5, 5, 5, 5, 5};
    double[] toTaper = x.clone();

    double power = FFTResult.cosineTaper(toTaper, 0.05);
    assertEquals(toTaper.length, power, 1E-3);
  }

  @Test
  public void fftInversionTest() {
    double[] timeSeries = {10, 11, 12, 11, 10, 11, 12, 11, 10, 11, 12};

    int padSize = 2;
    while (padSize < timeSeries.length) {
      padSize *= 2;
    }

    double[] paddedTS = new double[padSize];
    System.arraycopy(timeSeries, 0, paddedTS, 0, timeSeries.length);

    // System.out.println(paddedTS.length);

    FastFourierTransformer fft =
        new FastFourierTransformer(DftNormalization.UNITARY);

    Complex[] frqDomn = fft.transform(paddedTS, TransformType.FORWARD);

    padSize = frqDomn.length / 2 + 1;
    // System.out.println(padSize);

    Complex[] trim = new Complex[padSize];

    System.arraycopy(frqDomn, 0, trim, 0, trim.length);

    padSize = (trim.length - 1) * 2;

    // System.out.println(padSize);

    Complex[] frqDomn2 = new Complex[padSize];

    for (int i = 0; i < padSize; ++i) {
      if (i < trim.length) {
        frqDomn2[i] = trim[i];
      } else {
        int idx = padSize - i;
        frqDomn2[i] = trim[idx].conjugate();
      }

      // System.out.println(frqDomn[i]+"|"+frqDomn2[i]);

    }

    Complex[] inverseFrqDomn = fft.transform(frqDomn2, TransformType.INVERSE);
    double[] result = new double[timeSeries.length];

    for (int i = 0; i < timeSeries.length; ++i) {
      result[i] = Math.round(inverseFrqDomn[i].getReal());
      // System.out.println( result[i] + "," + inverseFrqDomn[i].getReal() );
      assertEquals(timeSeries[i], result[i], 0.1);
    }

  }

  @Test
  public void fftZerosTestWelch() {
    long interval = TimeSeriesUtils.ONE_HZ_INTERVAL;
    double[] data = new double[1000];
    FFTResult fftr = FFTResult.spectralCalc(data, data, interval);
    Complex[] values = fftr.getFFT();
    for (Complex c : values) {
      assertEquals(c, Complex.ZERO);
    }
  }

  @Test
  public void lowPassFilterTest() {
    double[] timeSeries = new double[400];

    for (int i = 0; i < timeSeries.length; ++i) {
      if (i % 2 == 0) {
        timeSeries[i] = -10;
      } else {
        timeSeries[i] = 10;
      }
    }

    double sps = 40.;

    double[] lowPassed = FFTResult.bandFilter(timeSeries, sps, 0.5, 1.5);
    //System.out.println(Arrays.toString(lowPassed));
    for (int i = 1; i < (lowPassed.length - 1); ++i) {
      assertTrue(Math.abs(lowPassed[i]) < 1.);
    }

  }

  @Test
  public void PSDWindowTest() {
    String dataName = folder + "psd-check/" + "00_LHZ.512.seed";
    try {
      DataBlock db = TimeSeriesUtils.getFirstTimeSeries(dataName);
      double[] list1 = db.getData();
      assertEquals(86400, list1.length);
      int range = list1.length / 4;
      assertEquals(21600, range);
      int slider = range / 4;
      assertEquals(5400, slider);
      int padding = 2;
      while (padding < range) {
        padding *= 2;
      }
      assertEquals(32768, padding);
      // double period = 1.0 / TimeSeriesUtils.ONE_HZ_INTERVAL;
      // period *= db.getInterval();
      // int singleSide = padding / 2 + 1;
      // double deltaFreq = 1. / (padding * period);
      int segsProcessed = 0;
      int rangeStart = 0;
      int rangeEnd = range;
      while (rangeEnd <= list1.length) {
        // give us a new list we can modify to get the data of
        double[] toFFT =
            Arrays.copyOfRange(list1, rangeStart, rangeEnd);
        // FFTResult.cosineTaper(toFFT, 0.05);
        assertEquals(range, toFFT.length);
        Pair<Complex[], Double> tempResult = FFTResult.getSpectralWindow(toFFT, padding);
        double cos = tempResult.getSecond();
        assertEquals(20925.25, cos, 1E-10);
        Complex[] result = tempResult.getFirst();
        if (segsProcessed == 0) {
          double[] inputCompare = {
              0., 0.00829733, 0.04025516, 0.07436855, 0.11318463,
              0.20593302, 0.39650397, 0.56754482, 0.53962978, 0.2964054,
          };
          Complex[] compareFFTAgainst = {
              new Complex(-527418.58107920),
              new Complex(2776737.84811514, 3857608.90528254),
              new Complex(3026409.04852451, -5380632.86921071),
              new Complex(674295.05515449, -16987.54811898),
              new Complex(-935438.31540157, -1996975.50482683),
              new Complex(906760.19785340, -1335406.39712464),
              new Complex(-362291.20689872, -152366.81080565),
              new Complex(-608945.67973085, -1276477.12005977),
              new Complex(386499.87292928, -567698.68950399),
              new Complex(-542277.47724910, -8439.67480907)
          };
          for (int i = 0; i < 10; ++i) {
            assertEquals(toFFT[i], inputCompare[i], 1E-4);
            String msg = "Got " + result[i] + " -- expected " + compareFFTAgainst[i];
            assertTrue(msg, Complex.equals(result[i], compareFFTAgainst[i], 1E-5));
          }
        }
        if (segsProcessed == 12) {
          System.out.println(Arrays.toString(Arrays.copyOfRange(toFFT, 0, 10)));
          System.out.println(Arrays.toString(Arrays.copyOfRange(result, 0, 10)));
        }

        ++segsProcessed;
        rangeStart += slider;
        rangeEnd += slider;

      }

      assertEquals(segsProcessed, 13);

    } catch (SeedFormatException | CodecException | IOException e) {
      e.printStackTrace();
      fail();
    }
  }

  @Test
  public void PSDWindowTestBHZ() {
    String dataName = folder + "psd-check/" + "00_BHZ.512.seed";
    try {
      DataBlock db = TimeSeriesUtils.getFirstTimeSeries(dataName);
      double[] list1 = db.getData();
      assertEquals(1728000, list1.length);
      int range = list1.length / 4;
      assertEquals(432000, range);
      int slider = range / 4;
      assertEquals(108000, slider);
      int padding = 2;
      while (padding < range) {
        padding *= 2;
      }
      assertEquals(524288, padding);
      // double period = 1.0 / TimeSeriesUtils.ONE_HZ_INTERVAL;
      // period *= db.getInterval();
      // int singleSide = padding / 2 + 1;
      // double deltaFreq = 1. / (padding * period);
      int segsProcessed = 0;
      int rangeStart = 0;
      int rangeEnd = range;
      while (rangeEnd <= list1.length) {
        // give us a new list we can modify to get the data of
        double[] toFFT =
            Arrays.copyOfRange(list1, rangeStart, rangeEnd);
        // FFTResult.cosineTaper(toFFT, 0.05);
        assertEquals(range, toFFT.length);
        Pair<Complex[], Double> tempResult = FFTResult.getSpectralWindow(toFFT, padding);
        double cos = tempResult.getSecond();
        assertEquals(418500.25, cos, 1E-10);
        Complex[] result = tempResult.getFirst();
        if (segsProcessed == 0) {
          double[] inputCompare = {
              0., 2.27521894e-06, 4.10761004e-06, -3.13525163e-06, -3.43485286e-05,
              -9.96880087e-05, -2.00677052e-04, -3.61266385e-04, -5.91019355e-04, -8.83398142e-04,
          };
          Complex[] compareFFTAgainst = {
              new Complex(-2250979.39477224),
              new Complex(-49396771.06922297, 1.32135458e+08),
              new Complex(-35615464.04919781, 3.60986729e+06),
              new Complex(12934786.68812495, -1.49785128e+07),
              new Complex(22086209.13211321, 1.02074979e+07),
              new Complex(9482703.26443943, 2.12119761e+07),
              new Complex(-4460398.13506965, 1.75932949e+07),
              new Complex(-7329192.90210010, 1.99426219e+06),
              new Complex(626621.12752514, -5.87733026e+06),
              new Complex(10710941.30159938, 4.15431178e+05)
          };
          for (int i = 0; i < 10; ++i) {
            assertEquals(toFFT[i], inputCompare[i], 1E-4);
            String msg = "Got " + result[i] + " -- expected " + compareFFTAgainst[i];
            assertTrue(msg, Complex.equals(result[i], compareFFTAgainst[i], 1E4));
          }
        }
        if (segsProcessed == 12) {
          System.out.println(Arrays.toString(Arrays.copyOfRange(toFFT, 0, 10)));
          System.out.println(Arrays.toString(Arrays.copyOfRange(result, 0, 10)));
        }

        ++segsProcessed;
        rangeStart += slider;
        rangeEnd += slider;

      }

      assertEquals(segsProcessed, 13);

    } catch (SeedFormatException | CodecException | IOException e) {
      e.printStackTrace();
      fail();
    }
  }

  @Test
  public void rangeCopyTest() {

    Number[] numbers = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};

    int low = 5;
    int high = 9;

    List<Number> numList = Arrays.asList(numbers);
    List<Number> subseq = new ArrayList<>(numList.subList(low, high));

    for (int i = 0; i < subseq.size(); ++i) {
      int fullListIdx = i + low;
      assertEquals(numList.get(fullListIdx), subseq.get(i));
    }

    for (int i = 0; i < subseq.size(); ++i) {
      Number temp;
      temp = 2000;
      assertNotEquals(subseq.get(i), temp); // can't try to change "in-place"
      subseq.set(i, 100);
    }

    for (int i = 0; i < subseq.size(); ++i) {
      int fullListIdx = i + low;
      assertNotEquals(numList.get(fullListIdx), subseq.get(i));
    }

  }

  @Test
  public void simpleFFTTest() {
    String dataName = folder + "psd-check/" + "00_LHZ.512.seed";
    try {
      DataBlock db = TimeSeriesUtils.getFirstTimeSeries(dataName);
      assertEquals(86400, db.getData().length);
      double[] data = db.getData();
      // double[] data = Arrays.copyOfRange(data, 0, 100);
      double sps = db.getSampleRate();
      FFTResult psd = FFTResult.singleSidedFFT(data, sps, false);
      Complex[] spect = psd.getFFT();
      assertEquals(spect.length, 131072 / 2 + 1);

      Complex[] reference = {
          new Complex(722987.975235, 0.),
          new Complex(-37414805.72779498, -5822012.70678756),
          new Complex(-45662469.49645267, 24281858.75109717),
          new Complex(20729519.11753263, 64465056.80586579),
          new Complex(17930348.83313549, -8668246.91616714),
          new Complex(9736247.76648419, 6334566.51901217),
          new Complex(909246.03876311, 2069056.83883289),
          new Complex(4312111.25618865, 77417.00854856),
          new Complex(3528384.60323541, 3103798.7647818),
          new Complex(884142.76006608, 857091.04696191)
      };

      for (int i = 0; i < 10; ++i) {
        String msg = "Got " + spect[i] + " -- expected " + reference[i];
        // System.out.println(msg);
        assertTrue(msg, Complex.equals(spect[i], reference[i], 1E-3));

      }
    } catch (SeedFormatException | CodecException | IOException e) {
      e.printStackTrace();
      fail();
    }
  }


  @Test
  public void spectrumTest() {

    long interval = TimeSeriesUtils.ONE_HZ_INTERVAL;
    long timeStart = 0L;
    String name1 = "XX_FAKE_LH1_00";
    String name2 = "XX_FAKE_LH2_00";
    int len = 10000;
    double[] timeSeries = new double[len];
    double[] secondSeries = new double[len];

    for (int i = 0; i < len; ++i) {
      timeSeries[i] = Math.sin(i);
      secondSeries[i] = Math.sin(i) + Math.sin(2 * i);
    }

    DataBlock db = new DataBlock(timeSeries, interval, name1, timeStart);
    DataBlock db2 = new DataBlock(secondSeries, interval, name2, timeStart);
    FFTResult fft = FFTResult.spectralCalc(db, db2);

    XYSeriesCollection xysc = new XYSeriesCollection();
    String name = name1 + "_" + name2;
    XYSeries xysr = new XYSeries(name + " spectrum (Real part)");
    XYSeries xysi = new XYSeries(name + " spectrum (Imag part)");
    for (int i = 0; i < fft.size(); ++i) {

      double freq = fft.getFreq(i);
      if (freq <= 0.) {
        continue;
      }

      xysr.add(freq, fft.getFFT(i).getReal());
      xysi.add(freq, fft.getFFT(i).getImaginary());
    }
    // xysc.addSeries(xysr);
    xysc.addSeries(xysi);

    JFreeChart jfc = ChartFactory.createXYLineChart(
        "SPECTRUM TEST CHART",
        "frequency",
        "value of spectrum",
        xysc);

    ValueAxis x = new LogarithmicAxis("frequency");
    jfc.getXYPlot().setDomainAxis(x);

    BufferedImage bi = ReportingUtils.chartsToImage(640, 480, jfc);
    String currentDir = System.getProperty("user.dir");
    String testResultFolder = currentDir + "/testResultImages/";
    File dir = new File(testResultFolder);
    if (!dir.exists()) {
      dir.mkdir();
    }

    String testResult =
        testResultFolder + "spectrum.png";
    File file = new File(testResult);
    try {
      ImageIO.write(bi, "png", file);
    } catch (IOException e) {
      fail();
      e.printStackTrace();
    }

  }

  @Test
  public void testBandFilter() throws Exception {
    String name = folder + "bandfilter-test/00_LHZ.512.seed";
    double[] testAgainst = new double[]{-50394.9143358, -111785.107014, -18613.4142884,
        143117.116357, 141452.164593, 6453.3516971, -79041.0146413, -58317.1285426, -8621.19465151,
        12272.6705308};
    DataBlock db = TimeSeriesUtils.getFirstTimeSeries(name);
    double sps = db.getSampleRate();
    assertEquals(sps, 1.0, 1E-10);
    double[] data = db.getData();
    double[] taper = new double[data.length];
    for (int i = 0; i < taper.length; ++i) {
      taper[i] = 1.;
    }
    FFTResult.cosineTaper(taper, 1.);
    assertEquals(86400, data.length);
    double[] testThis = FFTResult.bandFilter(data, sps, 1. / 8., 1. / 4.);
    for (int i = 0; i < testAgainst.length; ++i) {
      assertEquals(testThis[i], testAgainst[i], 1E-6);
    }
  }

  @Test
  public void testCosineTaper() {
    double[] taper = FFTResult.getCosTaperCurveSingleSide(100, 0.05);
    assertEquals(taper.length, 2);
    assertEquals(0., taper[0], 1E-9);
    assertEquals(0.5, taper[1], 1E-9);
  }

  @Test
  public void testFFTInputAllZero() throws Exception {
    double[] data;
    String dataName = folder + "psd-check/" + "ALLzero.mseed";
    DataBlock db = TimeSeriesUtils.getFirstTimeSeries(dataName);
    double sps = db.getSampleRate();
    data = db.getData();
    FFTResult psd = FFTResult.singleSidedFFT(data, sps, false);
    Complex[] spect = psd.getFFT();
    assertEquals(spect.length, 131072 / 2 + 1);
    for (Complex c : spect) {
      String msg = "Got " + c + " but expected ZERO";
      assertTrue(msg, Complex.equals(c, Complex.ZERO, 1E-20));
    }
  }

  @Test
  public void testFFTInputAllZeroButOne() throws Exception {
    double[] data;
    String dataName = folder + "psd-check/" + "ALLButOnezero.mseed";

    DataBlock db = TimeSeriesUtils.getFirstTimeSeries(dataName);
    double sps = db.getSampleRate();
    data = db.getData();
    FFTResult psd = FFTResult.singleSidedFFT(data, sps, false);
    Complex[] spect = psd.getFFT();
    assertEquals(spect.length, 131072 / 2 + 1);
    Complex[] testAgainst = {
        new Complex(-0.97500000),
        new Complex(0.20859933, 0.38178972),
        new Complex(-0.10179138, 0.15856818),
        new Complex(0.03587773, 0.00253845),
        new Complex(0.04882482, 0.10662358),
        new Complex(-0.03568545, 0.04779705),
        new Complex(0.03446735, 0.00490186),
        new Complex(0.02392292, 0.06379192),
        new Complex(-0.01669606, 0.01934787),
        new Complex(0.03221568, 0.00693072)
    };
    for (int i = 0; i < testAgainst.length; ++i) {
      Complex c = spect[i];
      Complex compare = testAgainst[i];
      String msg = "Got " + c + " -- expected " + compare;
      assertTrue(msg, Complex.equals(c, compare, 1E-4));
    }
  }

  @Test
  public void testMoreComplexCosTaper() {
    int len = 1000;
    double width = 0.05;
    int taperLen = (int) (((len * width) + 1) / 2.) - 1;
    assertEquals(24, taperLen);
    double[] taper = FFTResult.getCosTaperCurveSingleSide(len, 0.05);
    assertEquals(taper.length, 24);
    double[] expected = {
        0., 0.00427757, 0.01703709, 0.03806023, 0.0669873, 0.10332333,
        0.14644661, 0.19561929, 0.25, 0.30865828, 0.37059048, 0.4347369,
        0.5, 0.5652631, 0.62940952, 0.69134172, 0.75, 0.80438071,
        0.85355339, 0.89667667, 0.9330127, 0.96193977, 0.98296291, 0.99572243
    };
    for (int i = 0; i < expected.length; ++i) {
      assertEquals(taper[i], expected[i], 1E-4);
    }

  }


  @Test
  public void testMultitaper() {
    int size = 2000;
    List<Double> timeSeries = new ArrayList<>();
    for (int i = 0; i < size; ++i) {
      if (i % 2 == 0) {
        timeSeries.add(-500.);
      } else {
        timeSeries.add(500.);
      }
    }

    final int TAPERS = 12;
    double[][] taper = FFTResult.getMultitaperSeries(size, TAPERS);
    for (double[] aTaper : taper) {
      double[] toFFT = new double[size];
      int l = toFFT.length - 1; // last point
      //double taperSum = 0.;
      //System.out.println(j + "-th taper curve first point: " + taperCurve[0]);
      //System.out.println(j + "-th taper curve last point: " + taperCurve[l]);
      for (int i = 0; i < timeSeries.size(); ++i) {
        // taperSum += Math.abs(taperCurve[i]);
        double point = timeSeries.get(i);
        toFFT[i] = point * aTaper[i];
      }
      //System.out.println(j + "-th tapered-data first point: " + toFFT[0]);
      //System.out.println(j + "-th tapered-data last point: " + toFFT[l]);

      assertEquals(0., toFFT[0], 1E-10);
      assertEquals(0., toFFT[l], 1E-10);
    }
  }

  @Test
  public void PSDCalcTestWithResp() {
    String dataName = folder + "psd-check/" + "00_LHZ.512.seed";
    String respName = folder + "psd-check/" + "RESP.IU.ANMO.00.LHZ";
    try {
      DataBlock db = TimeSeriesUtils.getFirstTimeSeries(dataName);
      InstrumentResponse ir = new InstrumentResponse(respName);
      FFTResult psd = FFTResult.crossPower(db, db, ir, ir);
      Complex[] spect = psd.getFFT();
      double deltaFreq = psd.getFreq(1);
      int lowIdx = (int) Math.floor(1. / (deltaFreq * 5.));
      int highIdx = (int) Math.ceil(1. / (deltaFreq * 3.));
      Complex[] spectTrim = Arrays.copyOfRange(spect, lowIdx, highIdx);
      double[] psdAmp = new double[spectTrim.length];
      for (int i = 0; i < spectTrim.length; ++i) {
        psdAmp[i] = 10 * Math.log10(spectTrim[i].abs());
      }
      double mean = TimeSeriesUtils.getMean(psdAmp);
      assertEquals(-132.2, mean, 1E-1);
    } catch (SeedFormatException | CodecException | IOException e) {
      e.printStackTrace();
      fail();
    }
  }

  @Test
  public void PSDCalcTestWithRespBHZ() {
    String dataName = folder + "psd-check/" + "00_BHZ.512.seed";
    String respName = folder + "psd-check/" + "RESP.IU.ANMO.00.BHZ";
    try {
      DataBlock db = TimeSeriesUtils.getFirstTimeSeries(dataName);
      InstrumentResponse ir = new InstrumentResponse(respName);
      FFTResult psd = FFTResult.crossPower(db, db, ir, ir);
      Complex[] spect = psd.getFFT();
      double deltaFreq = psd.getFreq(1);
      int lowIdx = (int) Math.floor(1. / (deltaFreq * 5.));
      int highIdx = (int) Math.ceil(1. / (deltaFreq * 3.));
      Complex[] spectTrim = Arrays.copyOfRange(spect, lowIdx, highIdx);
      double[] psdAmp = new double[spectTrim.length];
      for (int i = 0; i < spectTrim.length; ++i) {
        psdAmp[i] = 10 * Math.log10(spectTrim[i].abs());
      }
      double mean = TimeSeriesUtils.getMean(psdAmp);
      assertEquals(-132.1, mean, 1E-1);
    } catch (SeedFormatException | CodecException | IOException e) {
      e.printStackTrace();
      fail();
    }
  }

  @Test
  public void testTaperCurve() {
    int length = 21600;
    double width = 0.05;
    double[] taper = FFTResult.getCosTaperCurveSingleSide(length, width);
    double[] testAgainst = new double[]{
        0.0, 8.49299745992e-06, 3.39717013156e-05, 7.64352460048e-05, 0.000135882188956,
        0.00021231051064, 0.000305717614632, 0.000416100327709, 0.000543454899949,
        0.000687777004865, 0.000849061739547, 0.00102730362483, 0.00122249660549
    };
    for (int i = 0; i < testAgainst.length; ++i) {
      assertEquals(testAgainst[i], taper[i], 5E-6);
    }
  }

  @Test
  public void findFFTPaddingLength_standardValues() {
    assertEquals(65536, FFTResult.findFFTPaddingLength(45000));
    assertEquals(2, FFTResult.findFFTPaddingLength(2));
    assertEquals(65536, FFTResult.findFFTPaddingLength(65535));
    assertEquals(65536, FFTResult.findFFTPaddingLength(65536));
  }
}
