package org.bodytrack.AirNow.LocationUploader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import org.bodytrack.AirNow.AirNowData;
import org.bodytrack.AirNow.AirNowParser;
import org.bodytrack.AirNow.Uploader.AirNowUploader;

/**
 * @author Justin Loutsenhizer <nano351@gmail.com>
 */
public class AirNowLocationUploader {
	private static String workingDir;
	
	public static final String SITE_UID_FILE_NAME = "BodyTrackUID";
		
		private static String siteUID;
		
		public static final String DEFAULT_SITE_USER_ID = "24";

		private static final int MAX_UPLOAD_RETRIES = 5;
		
		private static String loadSiteUID(){
			String uid = DEFAULT_SITE_USER_ID;
			try{
				FileInputStream fis = new FileInputStream(workingDir + SITE_UID_FILE_NAME);
				try{
					BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
					try {
						uid = reader.readLine();
					} catch (IOException e) {}
					reader.close();
				}
				catch (Exception e){}
				fis.close();
			} catch (Exception e){}
			return uid;
		}

		public static void main(String[] args){
			if (args.length != 1){
				throw new RuntimeException("invalid number of arguments. Must receive one argument specifying working directory!!!");
			}
			workingDir = args[0].replace("\\", "/");
			if (workingDir.charAt(workingDir.length() - 1) != '/')
				workingDir += '/';
			new File(workingDir).mkdirs();
			
			siteUID = loadSiteUID();
			
			AirNowParser parser = new AirNowParser(workingDir);
			System.out.println("Attempting to upload location data");
			try{
				FileInputStream fis = new FileInputStream (workingDir + "sites.dat");
				AirNowData siteData = parser.parseSiteInfo(fis);
				fis.close();
				if (uploadLocationData(siteData))
					System.out.println("Successfuly uploaded site locations!");
				else
					System.out.println("Failed to upload site locations!");
			}
			catch (Exception e){
				System.out.println("Failed to upload site locations!");
			}
			System.exit(0);
		}
		
		private static boolean uploadLocationData(AirNowData airDat){
			String[][] data = airDat.getLocationJSON();
			for (int i = 0; i < data[0].length; i++){
				System.out.println("Uploading " + (i + 1) + " of " + data[0].length);
				int count = 0;
				while (!AirNowUploader.uploadDataSource(data[0][i],data[1][i],data[2][i],null,data[3][i])){
					System.out.println("Retrying to upload " + (i + 1) + " of " + data[0].length);
					count++;
					if (count == MAX_UPLOAD_RETRIES)
						return false;
				}
			}
			return true;
		}
}
