package com.example.heartratemonitor;

import android.graphics.Color;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.util.ArrayList;



public class Charting {

    public enum ChartType{
        ECG, HR
    }
    private ChartType chartType;

    private LineChart mChart;
    private LineData mData;
    private ILineDataSet mDataSet1, mDataSet2;
    private  YAxis leftAxis;

    //default values of Y-axis
    private float y_max=260f, y_min=-10f;

    //default values of X-axis
    private int CHART_MAX_VIEW_POINTS = 1000;


    public Charting(LineChart chart, ChartType type){
        this.mChart = chart;
        if (type == ChartType.ECG) {
            createDataSet2();
            this.chartType = type;

            mData = mChart.getData();
            mDataSet1 = mData.getDataSetByIndex(0);
            mDataSet2 = mData.getDataSetByIndex(1);
            leftAxis = mChart.getAxisLeft();
        }
        if (type == ChartType.HR){
            createDataSet1();
            this.chartType = type;
            mData = mChart.getData();
            mDataSet1 = mData.getDataSetByIndex(0);
            leftAxis = mChart.getAxisLeft();
        }

    }

    private void createDataSet1() {
        ArrayList<Entry> yValues = new ArrayList<>();
        LineDataSet set1 = new LineDataSet(yValues, "DataSet1");
        set1.setColor(Color.RED);
        set1.setLineWidth(2f);
        set1.setDrawCircles(false);
        set1.setDrawValues(false);

        ArrayList<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(set1);

        LineData data = new LineData(dataSets);

        mChart.setData(data);

        mChart.getDescription().setText("Heart Rate");

        // get the legend (only possible after setting data)
        Legend l = mChart.getLegend();

        // modify the legend ...
        l.setEnabled(false);
        //l.setForm(Legend.LegendForm.LINE);
        //l.setTextColor(Color.DKGRAY);

        XAxis xl = mChart.getXAxis();
        xl.setTextColor(Color.WHITE);
        xl.setEnabled(false);


        leftAxis = mChart.getAxisLeft();
        leftAxis.setTextColor(Color.LTGRAY);
        leftAxis.setDrawGridLines(true);
        autoScale(true);
        //leftAxis.setAxisMaximum(256f);
        //leftAxis.setAxisMinimum(-10f);
        leftAxis.setDrawGridLines(true);

        YAxis rightAxis = mChart.getAxisRight();
        rightAxis.setEnabled(false);

        mChart.getAxisLeft().setDrawGridLines(false);
        mChart.getXAxis().setDrawGridLines(false);
        mChart.setDrawBorders(false);

    }


    //return amount of Entry points in dataset at index "index"
    public int getEntryCount(int index){
        return mChart.getData().getDataSetByIndex(index).getEntryCount();
    }

    public int getChartMaxViewPoints(){
        return CHART_MAX_VIEW_POINTS;
    }

    public void setMaxViewPoints(int maxPoints){
        if (chartType == ChartType.ECG){
            if ((maxPoints < 5000) & (maxPoints > 99)){
                CHART_MAX_VIEW_POINTS = maxPoints;
            } else {
                CHART_MAX_VIEW_POINTS = 100;
            }
            XAxis xl = mChart.getXAxis();
            xl.setAxisMaximum((float)CHART_MAX_VIEW_POINTS);
        }
        if (chartType == ChartType.HR){
            CHART_MAX_VIEW_POINTS = maxPoints;
            mChart.setVisibleXRange(0, CHART_MAX_VIEW_POINTS);
        }

    }


    private void createDataSet2() {
        ArrayList<Entry> yValues = new ArrayList<>();
        LineDataSet set1 = new LineDataSet(yValues, "DataSet1");
        set1.setColor(Color.RED);
        set1.setLineWidth(2f);
        set1.setDrawCircles(false);
        set1.setDrawValues(false);
        LineDataSet set2 = new LineDataSet(new ArrayList<Entry>(),"DataSet2");
        set2.setColor(Color.GREEN);
        set2.setLineWidth(2f);
        set2.setDrawCircles(false);
        set2.setDrawValues(false);
        // сглаживание
        //set1.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        //set1.setCubicIntensity(0.2f);

        ArrayList<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(set1);
        dataSets.add(set2);
        LineData data = new LineData(dataSets);

        mChart.setData(data);

        mChart.getDescription().setText("ECG");




        // get the legend (only possible after setting data)
        Legend l = mChart.getLegend();

        // modify the legend ...
        l.setForm(Legend.LegendForm.LINE);
        l.setTextColor(Color.WHITE);

        XAxis xl = mChart.getXAxis();
        xl.setTextColor(Color.WHITE);
        xl.setDrawGridLines(true);
        xl.setAvoidFirstLastClipping(true);
        xl.setAxisMinimum(0f);
        xl.setAxisMaximum((float)CHART_MAX_VIEW_POINTS);
        xl.setEnabled(true);


        leftAxis = mChart.getAxisLeft();
        //leftAxis.setTextColor(Color.WHITE);
        leftAxis.setDrawGridLines(false);
        autoScale(true);
        //leftAxis.setAxisMaximum(256f);
        leftAxis.setAxisMinimum(-10f);
        leftAxis.setDrawGridLines(true);
        YAxis rightAxis = mChart.getAxisRight();
        rightAxis.setEnabled(false);

        mChart.getAxisLeft().setDrawGridLines(false);
        mChart.getXAxis().setDrawGridLines(false);
        mChart.setDrawBorders(false);

    }



    public void autoScale(boolean enable){
        if (enable){
            leftAxis.resetAxisMaximum();
            leftAxis.resetAxisMinimum();
        }
        else{
            leftAxis.setAxisMaximum(y_max);
            leftAxis.setAxisMinimum(y_min);
        }
    }

    public void setYAxisMaxMin(float y_max, float y_min){
        this.y_max = y_max;
        this.y_min = y_min;
        leftAxis.setAxisMaximum(this.y_max);
        leftAxis.setAxisMinimum(this.y_min);
    }

    private int X1;
    private boolean toggler;

    public void drawChart(byte[] arraydata, float beats){
        if (chartType == ChartType.ECG) putDataChart2(arraydata);
        if (chartType == ChartType.HR) putDataChart1(beats);
    }

    // Для одного графика просто добавляем данные и отрисовываем
    private void putDataChart1(float beats) {
        if (beats == Float.POSITIVE_INFINITY)beats=0;
        mData.addEntry(new Entry(mDataSet1.getEntryCount(), beats),0);
        mChart.notifyDataSetChanged();
        mChart.setVisibleXRange(0, CHART_MAX_VIEW_POINTS);
        mChart.moveViewToX(mDataSet1.getEntryCount());
        //ILineDataSet set = mData.getDataSetByIndex(0);

        //mData.addEntry(new Entry(set.getEntryCount(), beats),0);
        //mChart.notifyDataSetChanged();
        //mChart.setVisibleXRange(1,200);
        //mChart.moveViewToX(set.getEntryCount());

    }

    // Для кардиограммы рисуем в 2 датасета
    private void putDataChart2(byte[] data){
        if (data != null){
/*
            //ползущий график
            for (byte xValue : data)
            {
                int anUnsignedByte = (0x000000FF & (int)xValue);
                mData.addEntry(new Entry(mDataSet1.getEntryCount(),anUnsignedByte),0);
                mDataSet1.removeFirst();
            }


            for (int i=0; i<mDataSet1.getEntryCount();i++){
                mDataSet1.getEntryForIndex(i).setX(i);// на первый взгляд тупая конструкция берет первый элемент, хоть индекс первого элемента начинается здесь, к примеру, с 20
            }

*//*
            //рисует график в один датасет, слева на право постоянно
            for (byte xValue : data)
            {
                X1++;
                if (X1 >=CHART_MAX_VIEW_POINTS) X1 =0;
                int anUnsignedByte = (0x000000FF & (int)xValue);
                mDataSet1.getEntryForIndex(X1).setY(anUnsignedByte);
            }


*/
            // в 2 датасета, слева на право
            for (byte xValue : data)
            {

                int anUnsignedByte = (0x000000FF & (int)xValue);
                X1++;
                if (X1 >=CHART_MAX_VIEW_POINTS) {
                    X1 = 0;
                    toggler = !toggler;
                }
                if (!toggler){
                    mData.addEntry(new Entry(X1,anUnsignedByte),0);
                    mDataSet2.removeFirst();
                }else{
                    mData.addEntry(new Entry(X1,anUnsignedByte),1);
                    mDataSet1.removeFirst();
                }

            }

            mData.notifyDataChanged();
            mChart.notifyDataSetChanged();
            mChart.invalidate();
            //mChart.animateX(300, Easing.EaseOutQuad);// анимация не заработала, анимипует график весь полностью, а не только добавленые точки
        }
    }

    public void removePoints(int pointCount) {

        for (int xv = 0; xv<pointCount; xv++) {
            mDataSet1.removeFirst();
        }
        mData.notifyDataChanged();
        mChart.notifyDataSetChanged();
        mChart.invalidate();

    }
}