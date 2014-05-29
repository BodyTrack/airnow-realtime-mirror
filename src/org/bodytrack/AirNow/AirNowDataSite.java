package org.bodytrack.AirNow;

import java.util.ArrayList;
import java.util.LinkedList;

/**
 * @author Justin Loutsenhizer <nano351@gmail.com>
 */
public class AirNowDataSite {
	private LinkedList<AirNowDataPoint> dataPoints = new LinkedList<AirNowDataPoint>();
	private LinkedList<String> dataTypes = new LinkedList<String>();
	
	private String siteId;
	private String siteName;
	
	private double latitude = 0;
	private double longitude = 0;
	private double elevation = 0;
	
	public AirNowDataSite(String siteId, String siteName){
		this.siteId = siteId;
		this.siteName = siteName;
	}
	
	public String getId(){
		return siteId;
	}
	
	public String getName(){
		return siteName;
	}
	
	public boolean equals(AirNowDataSite site){
		return siteId.equals(site.getId());
	}
	
	public boolean equals(String siteId){
		return this.siteId.equals(siteId);
	}
	
	public boolean equals(Object obj){
		if (obj.getClass() == AirNowDataSite.class)
			return equals((AirNowDataSite) obj);
		else if (obj.getClass() == String.class)
			return equals((String) obj);
		return false;
	}
	
	public void addDataPoint(AirNowDataPoint dataPoint){
		dataPoints.add(dataPoint);
		String dataType = dataPoint.getDataTypeName();
		if (dataTypes.indexOf(dataType) == -1){
			dataTypes.add(dataType);
		}
	}
	
	public AirNowDataPoint[][] getDataPoints(){
		ArrayList<ArrayList<AirNowDataPoint>> temp = new ArrayList<ArrayList<AirNowDataPoint>>();
		for (AirNowDataPoint dataPoint : dataPoints){
			String variableType = dataPoint.getParameterCd();
			boolean added = false;
			for (ArrayList<AirNowDataPoint> a : temp){
				if (a.get(0).getParameterCd().equals(variableType)){
					a.add(dataPoint);
					added = true;
					break;
				}
			}
			if (!added){
				ArrayList<AirNowDataPoint> a = new ArrayList<AirNowDataPoint>();
				a.add(dataPoint);
				temp.add(a);
			}
		}
		AirNowDataPoint[][] retLists = new AirNowDataPoint[temp.size()][];
		for (int i = 0; i < retLists.length; i++){
			retLists[i] = temp.get(i).toArray(new AirNowDataPoint[]{});
		}
		return retLists;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	public double getLatitude() {
		return latitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public void setElevation(double elevation) {
		this.elevation = elevation;
	}

	public double getElevation() {
		return elevation;
	}
	
	public boolean hasData(){
		return dataPoints.size() != 0;
	}
	
	public void addDataType(String parameterCode){
		String dataType = AirNowDataPoint.getDataTypeName(parameterCode);
		if (dataType != null)
			if (dataTypes.indexOf(dataType) == -1)
				dataTypes.add(dataType);
	}
	
	public String[] getDataTypes(){
		return dataTypes.toArray(new String[]{});
	}
	
	public void merge(AirNowDataSite site){
		if (site.getLatitude() != 0 || site.getLongitude() != 0 || site.getElevation() != 0){
			setLatitude(site.getLatitude());
			setLongitude(site.getLongitude());
			setElevation(site.getElevation());
		}
		AirNowDataPoint[][] dataPoints = site.getDataPoints();
		for (AirNowDataPoint[] a : dataPoints){
			for (AirNowDataPoint dataPoint : a){
				addDataPoint(dataPoint);
			}
		}
		for (String dataType : site.dataTypes){
			if (dataTypes.indexOf(dataType) == -1){
				dataTypes.add(dataType);
			}
		}
	}
}
