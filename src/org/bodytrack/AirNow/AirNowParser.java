package org.bodytrack.AirNow;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * @author Justin Loutsenhizer <nano351@gmail.com>
 */
public class AirNowParser {
	private static final int DATA_DATE_INDEX = 0;
	private static final int DATA_TIME_INDEX = 1;
	private static final int DATA_SITE_ID_INDEX = 2;
	private static final int DATA_SITE_NAME_INDEX = 3;
	private static final int DATA_PARAMTER_ID_INDEX = 5;
	private static final int DATA_VALUE_INDEX = 7;
	
	private static final int SITE_SITE_ID_INDEX = 0;
	private static final int SITE_PARAMETER_ID_INDEX = 1;
	private static final int SITE_SITE_NAME_INDEX = 3;
	private static final int SITE_LATITUDE_INDEX = 8;
	private static final int SITE_LONGITUDE_INDEX = 9;
	private static final int SITE_ELEVATION = 10;
	
	private String workingDir;
	
	public AirNowParser(String workingDir){
		this.workingDir = workingDir;
	}
	
	private long dateToTimeStamp(String date, String time){
		int year = Integer.parseInt("20" + date.substring(6,8));
		int month = Integer.parseInt(date.substring(0,2));
		int day = Integer.parseInt(date.substring(3,5));
		int hour = Integer.parseInt(time.substring(0,2));
		int minute = Integer.parseInt(time.substring(3,5));
		TimeZone tz = TimeZone.getTimeZone("GMT");
		Calendar cal = new GregorianCalendar(tz);
		cal.set(year,month-1,day,hour,minute,0);
		return (cal.getTimeInMillis() / 1000 * 1000);
	}
	
	private AirNowData parseData(InputStream is, String encoding) throws Exception{
		AirNowData data = new AirNowData();
		BufferedReader reader = new BufferedReader(new InputStreamReader(is,encoding));
		String curLine;
		while ((curLine = reader.readLine()) != null){
			String[] lineParts = curLine.split("\\|");
			if (lineParts.length == 9){
				long timestamp = dateToTimeStamp(lineParts[DATA_DATE_INDEX],lineParts[DATA_TIME_INDEX]);
				String siteId = lineParts[DATA_SITE_ID_INDEX];
				String siteName = lineParts[DATA_SITE_NAME_INDEX];
				String parameterId = lineParts[DATA_PARAMTER_ID_INDEX];
				double value = Double.parseDouble(lineParts[DATA_VALUE_INDEX]);
				
				AirNowDataPoint.verifyDataTypeSupport(workingDir, parameterId);
				
				AirNowDataPoint curPoint = new AirNowDataPoint(timestamp, parameterId, value);
				data.addDataPoint(siteId, siteName, curPoint);
			}
		}
		reader.close();
		return data;
	}
	
	private String[] possibleEncodings = {"UTF-8","UTF-16"};
	
	public AirNowData parseData(InputStream is){
		byte[] data;
		try{
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			byte[] buffer = new byte[1024];
			int read;
			while ((read = is.read(buffer)) >= 0){
				if (read > 0)
					bos.write(buffer,0,read);
			}
			data = bos.toByteArray();
		}
		catch (Exception e){
			return null;
		}
		AirNowData airNowData = null;;
		for (String encoding : possibleEncodings){
			InputStream bis = new ByteArrayInputStream(data);
			try{
				airNowData = parseData(bis,encoding);
				bis.close();
				break;
			}
			catch (Exception e){
				try {
					bis.close();
				} catch (IOException e1) {
				}
			}
		}
		return airNowData;
		
	}
	
	public AirNowData parseSiteInfo(InputStream is){
		try{
			AirNowData data = new AirNowData();
			BufferedReader reader = new BufferedReader(new InputStreamReader(is));
			String curLine;
			while ((curLine = reader.readLine()) != null){
				String[] lineParts = curLine.split("\\|");
				if (lineParts.length == 21){
					String siteId = lineParts[SITE_SITE_ID_INDEX];
					String parameterId = lineParts[SITE_PARAMETER_ID_INDEX];
					String siteName = lineParts[SITE_SITE_NAME_INDEX];
					double latitude = Double.parseDouble(lineParts[SITE_LATITUDE_INDEX]);
					double longitude = Double.parseDouble(lineParts[SITE_LONGITUDE_INDEX]);
					double elevation = Double.parseDouble(lineParts[SITE_ELEVATION]);
					AirNowDataSite temp = new AirNowDataSite(siteId,siteName);
					AirNowDataPoint.initDataTypesIfNot(workingDir);
					temp.addDataType(parameterId);
					temp.setLatitude(latitude);
					temp.setLongitude(longitude);
					temp.setElevation(elevation);
					data.addDataSite(temp);
				}
				else if (lineParts.length != 1)
					System.out.println(lineParts.length);
			}
			reader.close();
			return data;
		}
		catch (Exception e){
			e.printStackTrace();
			return null;
		}
	}
}
