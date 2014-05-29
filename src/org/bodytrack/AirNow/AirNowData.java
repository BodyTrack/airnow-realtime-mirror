package org.bodytrack.AirNow;

import java.util.ArrayList;
import java.util.LinkedList;

/**
 * @author Justin Loutsenhizer <nano351@gmail.com>
 */
public class AirNowData {
	private LinkedList<AirNowDataSite> dataSites = new LinkedList<AirNowDataSite>();
	
	public void merge(AirNowData data){
		if (data == null)
			return;
		for (AirNowDataSite site : data.dataSites){
			addDataSite(site);
		}
	}
	
	public void addDataPoint(String siteId, String siteName, AirNowDataPoint dataPoint){
		int index = dataSites.indexOf(new AirNowDataSite(siteId, siteName));
		if (index != -1){
			dataSites.get(index).addDataPoint(dataPoint);
		}
		else{
			AirNowDataSite temp = new AirNowDataSite(siteId, siteName);
			temp.addDataPoint(dataPoint);
			dataSites.add(temp);
		}
	}
	
	public void addDataSite(AirNowDataSite site){
		int index = dataSites.indexOf(site);
		if (index == -1){
			dataSites.add(site);
		}
		else{
			AirNowDataSite mySite = dataSites.get(index);
			mySite.merge(site);
		}
	}
	
	public AirNowDataSite[] getDataSites(){
		return dataSites.toArray(new AirNowDataSite[]{});
	}
	
	public String getSiteLocationCSV(){
		StringBuilder temp = new StringBuilder("longitude,latitude,name");
		for (AirNowDataSite site : dataSites){
			temp.append("\n");
			temp.append(site.getLongitude());
			temp.append(",");
			temp.append(site.getLatitude());
			temp.append(",");
			temp.append("\"").append(site.getName()).append("\"");
		}
		return temp.toString();
	}
	
	private void addInOrder(ArrayList<Long> timestamps, ArrayList<Double> values, long timestamp, double value){
		for (int i = 0; i < timestamps.size(); i++){
			if (timestamps.get(i) > timestamp){
				timestamps.add(i,timestamp);
				values.add(i,value);
				return;
			}
		}
		timestamps.add(timestamp);
		values.add(value);
	}
	
	/**convoluted format, oh well:<br />
	*[0][x] = device_id<br />
	*[1][x] = device_nickname<br />
	*[2][x] = channels for segment<br />
	*[3][x] = channel specs for segment<br />
	*[4][x] = data segment<br />
	*<br />
	*average ammount of strings/characters generated per file decreases<br />
	*as number of files increase! Could be optimized to combine channels<br />
	*into one upload for contiguous data sets, but becomes too complicated.<br />
	*max strings generated is roughly 96000 (3000 sites, 32 data types).
	**/
	public String[][] getJSON(){
		@SuppressWarnings("unchecked")
		ArrayList<String>[] temp = new ArrayList[5];
		for (int i = 0; i < temp.length; i++){
			temp[i] = new ArrayList<String>();
		}
		for (AirNowDataSite site : dataSites){
			for (AirNowDataPoint[] dataArray : site.getDataPoints()){
				temp[0].add(site.getId());
				temp[1].add(site.getName());
				temp[2].add("[\"" + dataArray[0].getDataTypeName() + "\"]");
				temp[3].add("{\"" + dataArray[0].getDataTypeName() + "\":{\"type\":\"Float\",\"units\":\"" + dataArray[0].getDataTypeUnits() + "\"}}");
				StringBuilder dataJSON = new StringBuilder("[");
				ArrayList<Long> times = new ArrayList<Long>();
				ArrayList<Double> values = new ArrayList<Double>();
				for (AirNowDataPoint dataPoint : dataArray){
					addInOrder(times,values,dataPoint.getTimeStamp(),dataPoint.getValue());
				}
				for (int i = 0; i < times.size(); i++){
					if (i != 0)
						dataJSON.append(",");
					dataJSON.append("[");
					dataJSON.append(times.get(i) / 1000.0);
					dataJSON.append(",").append(values.get(i));
					dataJSON.append("]");
				}
				dataJSON.append("]");
				temp[4].add(dataJSON.toString());
			}
		}
		
		String[][] retArray = new String[temp.length][];
		for (int i = 0; i < retArray.length; i++){
			retArray[i] = temp[i].toArray(new String[]{});
		}
		return retArray;
	}
	
	/**convoluted format, oh well:<br />
	*[0][x] = device_id<br />
	*[1][x] = device_nickname<br />
	*[2][x] = channels for segment<br />
	*[3][x] = data segment<br />
	*<br />
	**/
	public String[][] getLocationJSON(){
		@SuppressWarnings("unchecked")
		ArrayList<String>[] temp = new ArrayList[4];
		for (int i = 0; i < temp.length; i++){
			temp[i] = new ArrayList<String>();
		}
		String time = "" + (System.currentTimeMillis() / 1000.0);
		for (AirNowDataSite site : dataSites){
			if (site.getLatitude() != 0 && site.getDataTypes().length != 0){
				temp[0].add(site.getId());
				temp[1].add(site.getName());
				temp[2].add("[\"Longitude\",\"Latitude\",\"Elevation\"]");
				temp[3].add("[[" + time + "," + site.getLongitude() + "," + site.getLatitude() + "," + site.getElevation() + "]]");
			}
		}		
		String[][] retArray = new String[temp.length][];
		for (int i = 0; i < retArray.length; i++){
			retArray[i] = temp[i].toArray(new String[]{});
		}
		return retArray;
	}
}
