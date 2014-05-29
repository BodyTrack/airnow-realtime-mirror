package org.bodytrack.AirNow;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * @author Justin Loutsenhizer <nano351@gmail.com>
 */
public class AirNowDataTypes {
	public static final String DATA_TYPE_FILE_NAME = "AirNowDataTypes";
	private ArrayList<String> variableCodes = new ArrayList<String>();
	private ArrayList<String> variableNames = new ArrayList<String>();
	private ArrayList<String> variableUnits = new ArrayList<String>();
	
	private AirNowDataTypes(){
		
	}

	public static AirNowDataTypes getAirNowDataTypes(String workingDir) {
		AirNowDataTypes temp = null;
		try{
			FileInputStream fis = new FileInputStream(workingDir + DATA_TYPE_FILE_NAME);
			temp = parseAirNowDataTypesFile(fis);
			fis.close();
		}
		catch (Exception e){
			
		}
		return temp;
	}
	
	private static AirNowDataTypes parseAirNowDataTypesFile(InputStream is){
		try{
			BufferedReader reader = new BufferedReader(new InputStreamReader(is));
			String curLine;
			AirNowDataTypes temp = new AirNowDataTypes();
			while ((curLine = reader.readLine()) != null){
				if (curLine.charAt(0) != '#'){
					String[] parts = curLine.split(",");
					if (parts.length == 3){
						try{
							temp.variableCodes.add(parts[0]);
							temp.variableNames.add(parts[1]);
							temp.variableUnits.add(parts[2]);
						}
						catch (Exception e){
						}
					}
				}
			}
			reader.close();
			return temp;
		}
		catch (Exception e){
			return null;
		}
	}

	
	public String getDataTypeName(String variableCode){
		int index = variableCodes.indexOf(variableCode);
		if (index == -1)
			return null;
		return variableNames.get(index);
	}
	
	public String getDataTypeUnits(String variableCode){
		int index = variableCodes.indexOf(variableCode);
		if (index == -1)
			return null;
		return variableUnits.get(index);
	}
}
