package org.bodytrack.AirNow.Uploader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.List;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.bodytrack.AirNow.AirNowData;
import org.bodytrack.AirNow.AirNowParser;

/**
 * @author Justin Loutsenhizer <nano351@gmail.com>
 */
public class AirNowUploader {
	public static final int MAX_FILES_PER_UPLOAD = 200;
	public static final int MAX_UPLOAD_RETRIES = 5;
	
	private static String workingDir;
	
	public static final String SITE_UID_FILE_NAME = "BodyTrackUID";
	
	private static String siteUID;
	
	public static final String DEFAULT_SITE_USER_ID = "17";
	private static String btHost;
	
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
		if (args.length != 2){
			throw new RuntimeException("invalid number of arguments. Must receive two argument specifying working directory and server hostname!!!");
		}
		workingDir = args[0].replace("\\", "/");
		if (workingDir.charAt(workingDir.length() - 1) != '/')
			workingDir += '/';
		new File(workingDir).mkdirs();
		btHost = args[1];
		System.out.println(btHost);
		
		siteUID = loadSiteUID();
		
		AirNowParser parser = new AirNowParser(workingDir);
		
		System.out.println("getting file list!");
		String[] fileList = new File(workingDir + "data/").list(new datFilter());
		int count = 0;
		AirNowData data = new AirNowData();
		System.out.println(fileList.length + " files to parse!");
		
		LinkedList<String> filesBeingUploaded = new LinkedList<String>();
		
		new File(workingDir + "data/imported/").mkdirs();
		
		for (String fileName : fileList){
			System.out.println("parsing " + fileName);
			try {
				FileInputStream fis = new FileInputStream(workingDir + "data/" + fileName);
				AirNowData newData = parser.parseData(fis);
				fis.close();
				if (newData != null){
					filesBeingUploaded.add(fileName);
					System.out.println("Parse success");
					count++;
					
					if (count == MAX_FILES_PER_UPLOAD){
						count = 0;
						if (uploadData(data)){
							moveFiles(filesBeingUploaded);
							filesBeingUploaded.clear();
							data = newData;
						}
						else{
							data.merge(newData);
						}
					}
					else{
						data.merge(newData);
					}
				}
				else{
					System.out.println("Parse failed!");
				}		
				
			} catch (IOException e) {
				System.out.println("Parse failed");
			}	
		}
		if (uploadData(data)){
			moveFiles(filesBeingUploaded);
		}
		System.out.println("Finished! Exiting!");
		System.exit(0); //sometimes doesn't like to end the process on return?
	}
	
	private static void moveFiles(List<String> files){
		for (String file : files){
			new File(workingDir + "data/" + file).renameTo(new File(workingDir + "data/imported/" + file));
		}
	}
	
	private static boolean uploadData(AirNowData airDat){
		String[][] data = airDat.getJSON();
		for (int i = 0; i < data[0].length; i++){
			System.out.println("Uploading " + (i + 1) + " of " + data[0].length);
			int count = 0;
			while (!uploadDataSource(data[0][i],data[1][i],data[2][i],data[3][i],data[4][i])){
				System.out.println("Retrying to upload " + (i + 1) + " of " + data[0].length);
				count++;
				if (count == MAX_UPLOAD_RETRIES)
					return false;
			}
		}
		return true;
	}
	
	public static boolean uploadDataSource(String devId, String devName, String channels, String channelSpecs, String data){
		HttpEntity reqEntity = null;
		MultipartEntity mPartEntity;
		if (data.length() > 1024){//require at least a killobyte... just too costly for small data sizes
			try{
		    	mPartEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
		    	mPartEntity.addPart("device_id", new StringBody(devId));
		    	mPartEntity.addPart("timezone", new StringBody("UTC"));
		    	mPartEntity.addPart("device_class", new StringBody("USGS"));
		    	mPartEntity.addPart("dev_nickname", new StringBody(devName));
		    	mPartEntity.addPart("channel_names", new StringBody(channels));
		    	if (channelSpecs != null)
		    		mPartEntity.addPart("channel_specs", new StringBody(channelSpecs));
			    mPartEntity.addPart("data", new StringBody(data));
			}
			catch (Exception e){
				System.out.println("failed to create data upload stuff");
				return false;
			}
			reqEntity = mPartEntity;
		}
		else{
	    
		
			List<NameValuePair> postParams = new LinkedList<NameValuePair>();
			postParams.add(new BasicNameValuePair("device_id",devId));
			postParams.add(new BasicNameValuePair("timezone","UTC"));
			postParams.add(new BasicNameValuePair("device_class","AirNowDataSource"));
			postParams.add(new BasicNameValuePair("dev_nickname", devName));
			postParams.add(new BasicNameValuePair("channel_names", channels));
			if (channelSpecs != null)
				postParams.add(new BasicNameValuePair("chnanel_specs", channelSpecs));
			postParams.add(new BasicNameValuePair("data", data));
			try {
				reqEntity = new UrlEncodedFormEntity(postParams);
			} catch (UnsupportedEncodingException e1) {
				System.out.println("failed to create data upload stuff");
				return false;
			}
		}
		
    	HttpClient mHttpClient = new DefaultHttpClient();
    	HttpPost postToServer = new HttpPost("http://" + btHost + "/users/" + siteUID + "/upload");
		postToServer.setEntity(reqEntity);
		HttpResponse response;
		try {
			response = mHttpClient.execute(postToServer);
		} catch (Exception e){
			System.out.println("Failed to upload to server!");
			return false;
		}
		int statusCode = response.getStatusLine().getStatusCode();
		long bytes = data.length();
		long overhead = reqEntity.getContentLength() - bytes;
		if (statusCode >= 200 && statusCode < 300){
    		System.out.println("Upload success!");
		}
		else{
			System.out.println("Upload failed! " + statusCode);
			return false;
		}
		System.out.println("data: " + bytes);
		System.out.println("overhead: " + overhead);
		return true;
	}
	
	private static class datFilter implements FilenameFilter{

		@Override
		public boolean accept(File dir, String name) {
			try{
				if (name.substring(name.lastIndexOf(".")).equalsIgnoreCase(".dat"))
					return true;
			}
			catch (Exception e){
				
			}
			return false;
		}
		
	}
}
