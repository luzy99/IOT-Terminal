package com.m5go.plugin;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.StandardChartTheme;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.renderer.xy.XYSplineRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.general.DatasetUtils;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;

//JFreeChart Line Chart（折线图）
public class JChart {
    /**
     * 创建JFreeChart Line Chart（折线图）
     */
    public static void main(String[] args) {
        createPic("D:/");
    }

    public static String createPic(String path) {
        StandardChartTheme mChartTheme = new StandardChartTheme("CN");
        mChartTheme.setLargeFont(new Font("Microsoft YaHei", Font.PLAIN, 15));
        mChartTheme.setExtraLargeFont(new Font("Microsoft YaHei", Font.BOLD, 18));
        mChartTheme.setRegularFont(new Font("Microsoft YaHei", Font.PLAIN, 15));
        ChartFactory.setChartTheme(mChartTheme);
        JFreeChart xychart = createXY();
        long timestamp = System.currentTimeMillis()/1000;
        String realpath=path+String.valueOf(timestamp)+".jpg";
        saveAsFile(xychart, realpath, 1000, 600);
        return realpath;
    }

    // 保存为文件
    public static void saveAsFile(JFreeChart chart, String outputPath,
                                  int weight, int height) {
        FileOutputStream out = null;
        try {
            File outFile = new File(outputPath);
            if (!outFile.getParentFile().exists()) {
                outFile.getParentFile().mkdirs();
            }
            out = new FileOutputStream(outputPath);
            // 保存为PNG
            // ChartUtilities.writeChartAsPNG(out, chart, 600, 400);
            // 保存为JPEG
            ChartUtils.writeChartAsJPEG(out, chart, weight, height);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

// 绘制折线图
    private static JFreeChart createXY() {
        //加载数据
        if (MqttServer.conn == null) {
            MqttServer.connect();
        }
        HashMap<String, ArrayList<Double>> rawdata = MqttServer.getData();
        ArrayList<Double> tmp = rawdata.get("tmp");
        ArrayList<Double> hum = rawdata.get("hum");
        ArrayList<Double> time = rawdata.get("time");

        //create the series - add some dummy data
        XYSeries series1 = new XYSeries("温度");
        XYSeries series2 = new XYSeries("湿度");

        for (int i = 0; i < tmp.size(); i++) {
            series1.add(time.get(i), tmp.get(i));
            series2.add(time.get(i), hum.get(i));
        }

        //create the datasets
        XYSeriesCollection dataset1 = new XYSeriesCollection();
        XYSeriesCollection dataset2 = new XYSeriesCollection();
        dataset1.addSeries(series1);
        dataset2.addSeries(series2);

        //construct the plot
        XYPlot plot = new XYPlot();
        plot.setDataset(0, dataset1);
        plot.setDataset(1, dataset2);

        //线条颜色
        plot.setRenderer(0, new XYSplineRenderer());//use default fill paint for first series
        XYLineAndShapeRenderer splinerenderer = new XYLineAndShapeRenderer();
//        XYSplineRenderer splinerenderer = new XYSplineRenderer();
        splinerenderer.setSeriesPaint(0, Color.BLUE);
        plot.setRenderer(1, splinerenderer);

        //坐标轴
        NumberAxis tmpAxis = new NumberAxis("温度/℃");
        tmpAxis.setAutoRange(true);
        tmpAxis.setNumberFormatOverride(new DecimalFormat("0"));
        tmpAxis.setAutoRangeIncludesZero(false);
        plot.setRangeAxis(0, tmpAxis);

        NumberAxis humAxis = new NumberAxis("湿度/%");
        humAxis.setAutoRange(true);
        humAxis.setNumberFormatOverride(new DecimalFormat("0.0"));
        humAxis.setAutoRangeIncludesZero(false);
        plot.setRangeAxis(1, humAxis);

        //X轴
        DateAxis dateAxis = new DateAxis();
        dateAxis.setDateFormatOverride(new SimpleDateFormat("HH:mm"));
        plot.setDomainAxis(dateAxis);

        //Map the data to the appropriate axis
        plot.mapDatasetToRangeAxis(0, 0);
        plot.mapDatasetToRangeAxis(1, 1);

        //generate the chart
        String title = "24小时温湿度走势图";
        SimpleDateFormat fmt = new SimpleDateFormat("(MM-dd HH:mm)");
        String timestr = fmt.format(System.currentTimeMillis());
        JFreeChart chart = new JFreeChart(title+timestr, null, plot, true);

        return chart;
    }
}
