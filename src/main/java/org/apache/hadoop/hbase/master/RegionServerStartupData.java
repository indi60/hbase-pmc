package org.apache.hadoop.hbase.master;

@org.ucare.cpn.annotations.Message
public class RegionServerStartupData {
	@org.ucare.cpn.annotations.State
	final int port;
    final long serverStartCode;
    final long serverCurrentTime;

    public RegionServerStartupData(final int port,
      final long serverStartCode, final long serverCurrentTime){
    	this.port = port;
    	this.serverStartCode = serverStartCode;
    	this.serverCurrentTime = serverCurrentTime;
    }
    
}
