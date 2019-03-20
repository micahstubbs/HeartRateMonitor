package com.example.heartratemonitor;

import android.graphics.Color;
import android.view.View;
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

    private LineChart mChart;
    private LineData mData;
    private ILineDataSet mDataSet1, mDataSet2;
    private  YAxis leftAxis;

    //default values of Y-axis
    private float y_max=260f, y_min=-10f;

    //default values of X-axis
    private int CHART_MAX_VIEW_POINTS = 1000;


    public Charting(LineChart chart){
        this.mChart = chart;

        createDataSet();
        mData = mChart.getData();
        mDataSet1 = mData.getDataSetByIndex(0);
        mDataSet2 = mData.getDataSetByIndex(1);
        leftAxis = mChart.getAxisLeft();
    }


    //return amount of Entry points in dataset at index "index"
    public int getEntryCount(int index){
        return mChart.getData().getDataSetByIndex(index).getEntryCount();
    }

    public int getChartMaxViewPoints(){
        return CHART_MAX_VIEW_POINTS;
    }

    public void setMaxViewPoints(int maxPoints){

        if ((maxPoints < 5000) & (maxPoints > 99)){
            CHART_MAX_VIEW_POINTS = maxPoints;
        } else {
            CHART_MAX_VIEW_POINTS = 100;
        }
        XAxis xl = mChart.getXAxis();
        xl.setAxisMaximum((float)CHART_MAX_VIEW_POINTS);
        /*
        if (mData != null){
            int PointCount = mDataSet1.getEntryCount();
            if (PointCount < CHART_MAX_VIEW_POINTS){
                for (int i = 0; i<(CHART_MAX_VIEW_POINTS-PointCount); i++){
                    mData.addEntry(new Entry(mDataSet1.getEntryCount(),0),0);
                }
            } else {
                for (int i = 0; i<(PointCount - CHART_MAX_VIEW_POINTS); i++){
                    mDataSet1.removeLast();
                }
            }
            mChart.getXAxis().setAxisMinimum(0f);
            mChart.getXAxis().setAxisMaximum((float)CHART_MAX_VIEW_POINTS);
        }
*/
    }


    private void createDataSet() {
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

        mChart.getDescription().setText("fgwef");




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

    public void putDataChart(byte[] data){
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