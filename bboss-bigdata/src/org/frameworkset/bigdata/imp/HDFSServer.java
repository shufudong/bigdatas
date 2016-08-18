package org.frameworkset.bigdata.imp;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.log4j.Logger;

public class HDFSServer {
	private static Map<String,FileSystem>  FileSystems = new HashMap<String,FileSystem>();
	 private static Logger log = Logger.getLogger(HDFSServer.class);
	public static FileSystem getFileSystem(String hadoop)
	{
		if(hadoop == null)
			return null;
		FileSystem fileSystem = FileSystems.get(hadoop);
		if(fileSystem != null)
		{
			return fileSystem;
		}
		else
		{
			synchronized(HDFSServer.class)
			{
				fileSystem = FileSystems.get(hadoop);
				if(fileSystem != null)
				{
					return fileSystem;
				}
				
				UserGroupInformation user_ = null;
				try {
					log.info("连接hdfs服务器："+hadoop + " use user[root].");
				 	user_ = UserGroupInformation.getBestUGI(null, "root");
					UserGroupInformation.setLoginUser(user_);
					 
					 
					URI uri = new URI(hadoop);
					 
					Configuration conf = new Configuration();
					conf.addResource("core-site.xml");
					conf.addResource("hdfs-site.xml");
					fileSystem = FileSystem.get(uri,conf);
					log.info("连接hdfs服务器："+hadoop + " FileSystem.get(uri,new Configuration()) .");	
					FileSystems.put(hadoop, fileSystem);
					
					log.info("Init fileSystem success:HADOOP_PATH="+hadoop+" ,user="+user_);
					return fileSystem;
				} catch (IOException e) {
					log.error("init fileSystem fail:HADOOP_PATH="+hadoop+" ,user="+user_,e);
					throw new java.lang.IllegalArgumentException("init fileSystem fail:HADOOP_PATH="+hadoop+" ,user="+user_,e);
				} catch (Exception e) {
					log.error("init fileSystem fail:HADOOP_PATH="+hadoop+" ,user="+user_,e);
					throw new java.lang.IllegalArgumentException("init fileSystem fail:HADOOP_PATH="+hadoop+" ,user="+user_,e);
				} 
				
			}
		}
	}
	
	public static void main(String[] args)
	{
		String hadoop = "hdfs://10.0.15.40:9000";
		FileSystem fs = getFileSystem(hadoop);
	}
			

}
