package org.bodytrack.AirNow.Mirror;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Properties;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

/**
 * @author Justin Loutsenhizer <nano351@gmail.com>
 * @author Chris Bartley <bartley@cmu.edu>
 */
public class AirNowMirror {
   /**
    * @param args
    * @throws IOException
    * @throws SocketException
    */

   private static boolean done = false;

   public static void main(String[] args) throws SocketException, IOException {
      if (args.length != 1) {
         throw new RuntimeException("invalid number of arguments. Must receive one argument specifying the path to the config.properties file!!!");
      }
      final Properties props = new Properties();
      props.load(new FileInputStream(args[0]));
      final String dataDirectory = props.getProperty("data.dir");
      final String ftpServer = props.getProperty("ftp.server");
      final String ftpUsername = props.getProperty("ftp.username");
      final String ftpPassword = props.getProperty("ftp.password");

      String workingDirStr = dataDirectory.replace("\\", "/");
      if (workingDirStr.charAt(workingDirStr.length() - 1) != '/') {
         workingDirStr += '/';
      }
      final File workingDir = new File(workingDirStr);
      workingDir.mkdirs();
      if (workingDir.isDirectory()) {
         System.out.println("Working directory: " + workingDir.getAbsolutePath());
      }
      else {
         System.err.println("Aborting due to invalid working directory: " + workingDir.getAbsolutePath());
         System.exit(1);
      }

      new File(workingDir, "data/tmp/").delete();
      final FTPClient ftp = new FTPClient();
      System.out.println("Connecting...");
      ftp.connect(ftpServer);
      System.out.print(ftp.getReplyString());
      if (!FTPReply.isPositiveCompletion(ftp.getReplyCode())) {
         System.out.println("Connection Refused by server!");
         ftp.disconnect();
         return;
      }
      System.out.println("Logging in...");
      ftp.login(ftpUsername, ftpPassword);
      System.out.print(ftp.getReplyString());
      if (!FTPReply.isPositiveCompletion(ftp.getReplyCode())) {
         System.out.println("Failed to login!");
         ftp.disconnect();
         return;
      }

      System.out.println("Downloading /Locations/monitoring_site_locations.dat...");
      final File sitesTempFile = new File(workingDir, "sites.tmp");
      FileOutputStream fos = new FileOutputStream(sitesTempFile);
      ftp.retrieveFile("/Locations/monitoring_site_locations.dat", fos);
      fos.close();
      System.out.println(ftp.getReplyString());
      if (!FTPReply.isPositiveCompletion(ftp.getReplyCode())) {
         System.out.println("Failed to get site info!!!");
         sitesTempFile.delete();
      }
      else {
         final File sitesDataFile = new File(workingDir, "sites.dat");
         sitesDataFile.delete();
         sitesTempFile.renameTo(sitesDataFile);
      }

      ArrayList<String> directoriesToBrowse = new ArrayList<String>();
      ArrayList<String> filesPresent = new ArrayList<String>();

      directoriesToBrowse.add("/HourlyData/");
      while (directoriesToBrowse.size() > 0) {
         String currentDirectory = directoriesToBrowse.remove(0);
         System.out.println("Getting file list for " + currentDirectory + "...");
         FTPFile[] files = ftp.listFiles(currentDirectory);
         System.out.print(ftp.getReplyString());
         if (!FTPReply.isPositiveCompletion(ftp.getReplyCode())) {
            System.out.println("Failed to get file list!");
         }
         for (FTPFile file : files) {
            if (file.isFile()) {
               filesPresent.add(currentDirectory + file.getName());
            }
            else {
               directoriesToBrowse.add(currentDirectory + file.getName() + "/");
            }
         }
      }
      new File(workingDir, "data/tmp/").mkdirs();
      System.out.println("Got full file list!");
      int current = 0;
      int count = 0;
      long start = System.currentTimeMillis();
      for (final String file : filesPresent) {
         current++;
         final String localFileName = file.substring(file.lastIndexOf("/"));
         if (!(new File(workingDir, "data/" + localFileName).exists() || new File(workingDir, "data/imported/" + localFileName).exists())) {
            System.out.println("Downloading " + file + "...(" + current + " of " + filesPresent.size() + ")");
            done = false;
            Thread downloader = new Thread() {
               public void run() {
                  try {
                     FileOutputStream fos = new FileOutputStream(new File(workingDir, "data/tmp/" + localFileName));
                     ftp.retrieveFile(file, fos);
                     fos.close();
                     new File(workingDir, "data/tmp/" + localFileName).renameTo(new File(workingDir, "data/" + localFileName));
                     System.out.print(ftp.getReplyString());
                     if (!FTPReply.isPositiveCompletion(ftp.getReplyCode())) {
                        System.out.print("Failed to download file!");
                        new File(workingDir, "data/tmp/" + localFileName).delete();
                     }
                     else {
                        System.out.print("Successfully downloaded file!");
                     }
                     done = true;
                  }
                  catch (Exception e) {
                     System.out.print("Failed to download file!");
                     new File(workingDir, "data/tmp/" + localFileName).delete();
                     done = true;
                  }
               }
            };
            downloader.start();
            long startTime = System.currentTimeMillis();
            while (!done && (System.currentTimeMillis() - startTime) < 60000) {
               try {
                  Thread.sleep(1000);
               }
               catch (InterruptedException e) {
               }
            }
            if (!done) {
               downloader.stop();
               ftp.abort();
               System.out.print("Failed to download file!");
               new File(workingDir, "data/tmp/" + localFileName).delete();
            }
            count++;
            System.out.println(" ETA:" + (((System.currentTimeMillis() - start) / count) * (filesPresent.size() - current)));
         }
         else {
            System.out.println(file + " is already downloaded!(" + current + " of " + filesPresent.size() + ")");
         }
      }
      new File(workingDir, "data/tmp/").delete();
      System.out.println("Finished! Exiting!");
      System.exit(0); //sometimes doesn't like to end the process on return?
   }
}
