package isc.whu.defender.traffic;


import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Calendar;

import org.achartengine.ChartFactory;
import org.achartengine.chart.BarChart.Type;
import org.achartengine.model.CategorySeries;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.SimpleSeriesRenderer;
import org.achartengine.renderer.XYMultipleSeriesRenderer;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint.Align;

public class Barchart {

	private long[] uploadData;
	private long[] downloadData;
	
	private Context mContext;
	private int mUid;
	
	/**
	 * Barchart constructor
	 * @param ctx Context
	 * @param uid User ID of app
	 */
	public Barchart(Context ctx, int uid) {
		mContext = ctx;
		mUid = uid;
	}
	
	/**
	 * 生成统计图
	 */
	public void createChart() {
		uploadData = getUpload(mUid);
		downloadData = getDownload(mUid);
		
	    XYMultipleSeriesRenderer renderer = getBarRenderer();
	    setChartSettings(renderer);
	    Intent intent = ChartFactory.getBarChartIntent(mContext, getBarDataset(), renderer, Type.DEFAULT);
	    mContext.startActivity(intent);
	}
	
    public XYMultipleSeriesRenderer getBarRenderer() {
        XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
        
        SimpleSeriesRenderer r = new SimpleSeriesRenderer();
        
        r.setColor(Color.BLUE);
        r.setDisplayChartValues(true);
        r.setChartValuesTextAlign(Align.RIGHT);
        renderer.addSeriesRenderer(r);
  
        r = new SimpleSeriesRenderer();
        r.setColor(Color.GREEN);
        r.setDisplayChartValues(true);
        r.setChartValuesTextAlign(Align.RIGHT);
        renderer.addSeriesRenderer(r);
        
        return renderer;
      }
    
    private void setChartSettings(XYMultipleSeriesRenderer renderer) {
    	renderer.setXAxisMin(0.3);
        renderer.setXAxisMax(4);
        
        //TODO: Set Maxmium by the flow
        
        double max1 = Math.max(Math.max(uploadData[0], uploadData[1]), uploadData[2]);
        double max2 = Math.max(Math.max(downloadData[0], downloadData[1]), downloadData[2]);
        double max = Math.max(max1, max2);
        
        renderer.setYAxisMin(0);
        renderer.setYAxisMax(max * 1.1);
        renderer.setYLabels(0);
        
        renderer.setPanEnabled(false, false);
        renderer.setMargins(new int[] {40, 10, 10, 0});
        renderer.setMarginsColor(Color.WHITE);
        renderer.setBackgroundColor(Color.WHITE);
        renderer.setApplyBackgroundColor(true);
        renderer.setShowCustomTextGrid(true);
        renderer.setShowGridX(true);
        
        renderer.setLegendTextSize(16);
        
        renderer.setChartTitle("流量图");
        renderer.setChartTitleTextSize(24);
        
        
        renderer.setBarSpacing(0.3);
        renderer.setXLabels(0);
        
        //TODO: 时间设置
        renderer.addXTextLabel(1, "前日使用流量");
        renderer.addXTextLabel(2, "昨日使用流量");
        renderer.addXTextLabel(3, "今日使用流量");
        
        renderer.setLabelsTextSize(16);

      }
    
    private XYMultipleSeriesDataset getBarDataset() {
        XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();        
        CategorySeries uploadSeries = new CategorySeries("上传流量");
        CategorySeries downloadSeries = new CategorySeries("下载流量");
        
        
        //TODO:流量设置
        uploadSeries.add(uploadData[2]);
        uploadSeries.add(uploadData[1]);
        uploadSeries.add(uploadData[0]);
        
        dataset.addSeries(uploadSeries.toXYSeries());
        
        downloadSeries.add(downloadData[2]);
        downloadSeries.add(downloadData[1]);
        downloadSeries.add(downloadData[0]);
        
        dataset.addSeries(downloadSeries.toXYSeries());
        return dataset;
      }
    
    
    private long[] getUpload(int uid) {
    	DBAdapter adapter = new DBAdapter(mContext);
    	adapter.open();
    	Calendar c = Calendar.getInstance();
		int mYear = c.get(Calendar.YEAR);
		int mMonth = c.get(Calendar.MONTH) + 1;
		int mDay = c.get(Calendar.DAY_OF_MONTH);
		int mDay1 = mDay - 1;
		int mDay2 = mDay - 2;
		String today = mYear + "-" + mMonth + "-" + mDay;
		String yesterday = mYear + "-" + mMonth + "-" + mDay1;
		String beforeYesterday = mYear + "-" + mMonth + "-" + mDay2;
		
		long upload = getUidSnd(uid);
		long upload1 = adapter.getTitle(uid, today, 0);
		long upload2 = adapter.getTitle(uid, yesterday, 0);
		long upload3 = adapter.getTitle(uid, beforeYesterday, 0);
		
		System.out.println(upload + ", " + upload1 + ", " + upload2 + ", " + upload3);
		
		long fToday;
		long fYesterday;
		long fBeforeYesterday;
		
		fToday = (upload - upload1) / 1024;
		if(upload2 == -1) {
			fYesterday = 0;
		} else {
			fYesterday = (upload1 - upload2) /1024;
		}
		
		if(upload3 == -1) {
			fBeforeYesterday = 0;
		} else {
			fBeforeYesterday = (upload2 - upload3) /1024;
		}
		
		adapter.close();
		
		return new long[]{fToday, fYesterday, fBeforeYesterday};
    }

    private long[] getDownload(int uid) {
    	DBAdapter adapter = new DBAdapter(mContext);
    	adapter.open();
    	Calendar c = Calendar.getInstance();
		int mYear = c.get(Calendar.YEAR);
		int mMonth = c.get(Calendar.MONTH) + 1;
		int mDay = c.get(Calendar.DAY_OF_MONTH);
		int mDay1 = mDay - 1;
		int mDay2 = mDay - 2;
		String today = mYear + "-" + mMonth + "-" + mDay;
		String yesterday = mYear + "-" + mMonth + "-" + mDay1;
		String beforeYesterday = mYear + "-" + mMonth + "-" + mDay2;
		
		long download = getUidRcv(uid);
		long download1 = adapter.getTitle(uid, today, 1);
		long download2 = adapter.getTitle(uid, yesterday, 1);
		long download3 = adapter.getTitle(uid, beforeYesterday, 1);
		
		long fToday;
		long fYesterday;
		long fBeforeYesterday;
		
		fToday = (download - download1) / 1024;
		if(download2 == -1) {
			fYesterday = 0;
		} else {
			fYesterday = (download1 - download2) /1024;
		}
		
		if(download3 == -1) {
			fBeforeYesterday = 0;
		} else {
			fBeforeYesterday = (download2 - download3) /1024;
		}
		
		
		adapter.close();
		
		return new long[]{fToday, fYesterday, fBeforeYesterday};
    }
    
    private long getUidRcv(int uid) {
		String rcvFile = "/proc/uid_stat/" + uid + "/tcp_rcv";
		
		FileReader fReader = null;
		
		try {
			fReader = new FileReader(rcvFile);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return 0;
		}
		
		BufferedReader bReader = new BufferedReader(fReader);
		String line = null;
		try {
			line = bReader.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return Long.parseLong(line);
	}
	
	private long getUidSnd(int uid) {
		String rcvFile = "/proc/uid_stat/" + uid + "/tcp_snd";
		
		FileReader fReader = null;
		
		try {
			fReader = new FileReader(rcvFile);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return 0;
		}
		
		BufferedReader bReader = new BufferedReader(fReader);
		String line = null;
		try {
			line = bReader.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return Long.parseLong(line);
	}
}
