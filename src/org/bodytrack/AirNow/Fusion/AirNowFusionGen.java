package org.bodytrack.AirNow.Fusion;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import org.bodytrack.AirNow.AirNowData;
import org.bodytrack.AirNow.AirNowDataPoint;
import org.bodytrack.AirNow.AirNowDataSite;
import org.bodytrack.AirNow.AirNowParser;

/**
 * @author Justin Loutsenhizer <nano351@gmail.com>
 */
public class AirNowFusionGen {
	private static String workingDir;

	public static void main(String[] args) throws IOException{
		if (args.length != 1){
			throw new RuntimeException("invalid number of arguments. Must receive one argument specifying working directory!!!");
		}
		workingDir = args[0].replace("\\", "/");
		if (workingDir.charAt(workingDir.length() - 1) != '/')
			workingDir += '/';
		new File(workingDir).mkdirs();
		
		String[] fileList = new File(workingDir + "data/imported/").list(new datFilter());
		
		if (fileList == null || fileList.length == 0){
			return;
		}
		AirNowParser parser = new AirNowParser(workingDir);
		ArrayList<String> dataSiteNames = new ArrayList<String>();
		ArrayList<Double> latitudes = new ArrayList<Double>();
		ArrayList<Double> longitudes = new ArrayList<Double>();
		ArrayList<Double> elevations = new ArrayList<Double>();
		ArrayList<Long> minTimes = new ArrayList<Long>();
		ArrayList<ArrayList<String>> dataTypes = new ArrayList<ArrayList<String>>();
		ArrayList<ArrayList<Double>> minValues = new ArrayList<ArrayList<Double>>();
		ArrayList<ArrayList<Double>> maxValues = new ArrayList<ArrayList<Double>>();
		
		System.out.println("Parsing " + workingDir + "sites.dat");
		FileInputStream fis = new FileInputStream (workingDir + "sites.dat");
		AirNowData data = parser.parseSiteInfo(fis);
		fis.close();
		
		for (AirNowDataSite site : data.getDataSites()){
			if (dataSiteNames.indexOf(site.getName()) == -1){
				dataSiteNames.add(site.getName());
				latitudes.add(site.getLatitude());
				longitudes.add(site.getLongitude());
				elevations.add(site.getElevation());
				minTimes.add(System.currentTimeMillis());
				dataTypes.add(new ArrayList<String>());
				minValues.add(new ArrayList<Double>());
				maxValues.add(new ArrayList<Double>());
			}
			int index = dataSiteNames.indexOf(site.getName());
			ArrayList<String> dataTypesArray = dataTypes.get(index);
			ArrayList<Double> minValuesArray = minValues.get(index);
			ArrayList<Double> maxValuesArray = maxValues.get(index);
			for (String dataType : site.getDataTypes()){
				if (dataTypesArray.indexOf(dataType) == -1){
					dataTypesArray.add(dataType);
					minValuesArray.add(Double.MAX_VALUE);
					maxValuesArray.add(-Double.MAX_VALUE);
				}
			}
		}
		
		for (String fileName : fileList){
			System.out.println("Parsing " + workingDir + "data/imported/" + fileName);
			fis = new FileInputStream(workingDir + "data/imported/" + fileName);
			data = parser.parseData(fis);
			fis.close();
			
			for (AirNowDataSite site : data.getDataSites()){
				if (dataSiteNames.indexOf(site.getName()) == -1){
					System.out.println("WARNING: no meta data present for " + site.getName() + "! Skipping site");
					continue;
				}
				int siteIndex = dataSiteNames.indexOf(site.getName());
				ArrayList<String> dataTypesArray = dataTypes.get(siteIndex);
				ArrayList<Double> minValuesArray = minValues.get(siteIndex);
				ArrayList<Double> maxValuesArray = maxValues.get(siteIndex);
				String[] datatypes = site.getDataTypes();
				AirNowDataPoint[][] points = site.getDataPoints();
				for (int i = 0; i < datatypes.length; i++){
					String dataType = datatypes[i];
					AirNowDataPoint[] dataPoints = points[i];
					if (dataTypesArray.indexOf(dataType) == -1){
						dataTypesArray.add(dataType);
						minValuesArray.add(Double.MAX_VALUE);
						maxValuesArray.add(Double.MIN_VALUE);
					}
					int index = dataTypesArray.indexOf(dataType);
					for (AirNowDataPoint point : dataPoints){
						if (minTimes.get(siteIndex) > point.getTimeStamp())
							minTimes.set(siteIndex,point.getTimeStamp());
						if (minValuesArray.get(index) > point.getValue())
							minValuesArray.set(index, point.getValue());
						if (maxValuesArray.get(index) < point.getValue())
							maxValuesArray.set(index, point.getValue());
					}
				}
			}
		}
		
		
		
		System.out.println("Done parsing writing file");
		PrintWriter out = new PrintWriter(new FileOutputStream(workingDir + "airnowsites.csv"));
		out.println("Latitude,Longitude,Elevation,startTime,formattedName,ChannelData");
		
		for (int i = 0; i < dataSiteNames.size(); i++){
			StringBuilder temp = new StringBuilder("\"[");
			boolean first = true;
			for (int j = 0; j < dataTypes.get(i).size(); j++){
				String dataType = formatString(dataTypes.get(i).get(j));
				double minValue = minValues.get(i).get(j);
				double maxValue = maxValues.get(i).get(j);
				if (minValue != Double.MAX_VALUE){
					if (!first)
						temp.append(",");
					else
						first = false;
					temp.append("[\"\"");
					temp.append(dataType);
					temp.append("\"\",");
					temp.append(minValue);
					temp.append(",");
					temp.append(maxValue);
					temp.append("]");
				}
			}
			temp.append("]\"");
			String channelData = temp.toString();
			if (!"\"[]\"".equals(channelData)){
				out.print(latitudes.get(i));
				out.print(",");
				out.print(longitudes.get(i));
				out.print(",");
				out.print(elevations.get(i));
				out.print(",");
				out.print(minTimes.get(i) / 1000.0);
				out.print(",\"");
				String name = formatString(dataSiteNames.get(i));
				out.print(name);
				out.print("\",");
				out.println(channelData);
				
			}
		}
		
		out.close();
		System.out.println("Done writing!");
	}
	
	private static String formatString(String string){
		StringBuilder temp = new StringBuilder(string);
		boolean hasBeenGood = true;
		for (int i = 0; i < temp.length(); i++){
			char curChar = temp.charAt(i);
			if ((curChar >= 'a' && curChar <= 'z') ||
					(curChar >= 'A' && curChar <= 'Z') ||
					(curChar >= '0' && curChar <= '9') ||
					curChar == '_')
				hasBeenGood = true;
			else{
				if (hasBeenGood){
					temp.setCharAt(i, '_');
					hasBeenGood = false;
				}
				else{
					temp.deleteCharAt(i--);
				}
			}
		}
		return temp.toString();
	}
	
	private static class datFilter implements FilenameFilter{

		@Override
		public boolean accept(File dir, String name) {
			try{
				if (name.substring(name.lastIndexOf(".")).equalsIgnoreCase(".dat"))
					return true;
			}
			catch (Exception e){
            System.err.println("Exception caught: " + e);
         }
			return false;
		}
		
	}
}
