package org.bodytrack.AirNow;

/**
 * @author Justin Loutsenhizer <nano351@gmail.com>
 */
public class AirNowDataPoint {
	
	private long timestamp;
	private String parameterCd;
	private double value;
	
	private static AirNowDataTypes dataTypes;
	
	public AirNowDataPoint(long timestamp, String parameterCd, double value){
		this.timestamp = timestamp;
		this.parameterCd = parameterCd;
		this.value = value;
	}

	public String getParameterCd() {
		return parameterCd;
	}

	public double getValue() {
		return value;
	}
	
	public long getTimeStamp(){
		return timestamp;
	}
	
	public static String getDataTypeName(String variableCode){
		return dataTypes.getDataTypeName(variableCode);
	}
	
	public String getDataTypeName(){
		return getDataTypeName(parameterCd);
	}
	
	public static String getDataTypeUnits(String variableCode){
		return dataTypes.getDataTypeUnits(variableCode);
	}
	
	public String getDataTypeUnits(){
		return getDataTypeUnits(parameterCd);
	}
	
	public static void initDataTypesIfNot(String workingDir){
		if (dataTypes == null)
		dataTypes = AirNowDataTypes.getAirNowDataTypes(workingDir);
	}
	
	public static void verifyDataTypeSupport(String workingDir, String currentVariableCode) {
		initDataTypesIfNot(workingDir);
		if (getDataTypeName(currentVariableCode) == null)
			throw new RuntimeException("Unsopported data type: " + currentVariableCode);
	}
}
