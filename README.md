# AirNow Realtime Mirror

Code to download data files from AirNow and mirror to a BodyTrack datastore server.

## Prerequisites

This project assumes you have the following installed:

  * [Java 6+ JRE](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
  * [Apache Ant](http://ant.apache.org)
    
## Build

To build, open a terminal window, and `cd` to this project's root directory.  Run `ant` to build.

## Configure

The mirror app depends on a config file to define your login to the AirNow FTP directory and the destination directory for downloaded files.  Copy the `config.template.properties` file to `config.properties`, and edit accordingly.

## Run

To run, open a terminal window, and `cd` to this project's root directory.  Run `./run-mirror.sh` to run the mirroring app.

## Known Issues

The uploading app is currently broken.  It uses a now deprecated interface to the BodyTrack system.