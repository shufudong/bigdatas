package org.frameworkset.bigdata.imp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.frameworkset.bigdata.imp.monitor.JobStatic;
import org.frameworkset.event.Event;
import org.frameworkset.event.EventHandle;
import org.frameworkset.event.EventImpl;

public class ReassignTaskJob {

	public ReassignTaskJob() {
		// TODO Auto-generated constructor stub
	}
	
	public void execute(ReassignTask reassignTask)
	{
		String localnode = Imp.getImpStaticManager().getLocalNode();
		 JobStatic jobStatic = Imp.getImpStaticManager().addReassignTaskJobStatic(reassignTask);
		 JobStatic localjobStatic  = Imp.getImpStaticManager().getLocalJobStatic(reassignTask.getReassigntaskJobname());
		 if(localjobStatic == null)
		 {
			 jobStatic.setErrormsg("作业"+reassignTask.getReassigntaskJobname()+"不存在.");
		 }
		 else if(localjobStatic.stopped() )
		 {
			 jobStatic.setErrormsg("作业"+reassignTask.getReassigntaskJobname()+"已经结束.");
		 }
		 else//分配重新分配reassigntaskJobname作业开始
		 {
		 
			 Map<String, Integer> hostTaskInfos =reassignTask.getHostTaskInfos();
			 int servers = hostTaskInfos.size() + 1;
//			 int[] perservertasks = new int[servers];//存储每个节点应该分配的任务数
			 String[] servseradd = new String[servers];
			 servseradd[0] = localnode;
			 synchronized(localjobStatic)
			 {
				 localjobStatic.eval();
				 int unhandletask = localjobStatic.getUnruntasks();
				 if(unhandletask == 0)
				 {
					 jobStatic.setErrormsg("作业"+reassignTask.getReassigntaskJobname()+"未处理任务已经分派完毕，不需要进行重新分配.");
				 }
				 else
				 {
					 //计算每个节点需要重新分配的任务数，根据节点监控数据来分配：排队任务，未处理任务，正在处理任务
					int unhandletasks = localjobStatic.getUnruntasks() ;
					int selfinhandle = localjobStatic.getWaittasks() + localjobStatic.getRuntasks();
					int totaltasks = unhandletasks + selfinhandle;
					Iterator<Entry<String, Integer>> it = hostTaskInfos.entrySet().iterator();
					int i = 1;
					while(it.hasNext())
					{
						Entry<String, Integer> tasks  = it.next();
						totaltasks = totaltasks + tasks.getValue().intValue(); 
						servseradd[i] = tasks.getKey();
						i ++;
						
					}
					int newtasks = totaltasks / servers;
					int div = totaltasks % servers;
					int startpos = localjobStatic.getCurrentposition() + 1;
					Map<String,List<TaskInfo>> perserverTasks = new HashMap<String,List<TaskInfo>>();
					 
					List<TaskInfo> temp = null;
					ExecutorJob executorjob = Imp.getImpStaticManager().getExecutorJob(reassignTask.getReassigntaskJobname());
					List<TaskInfo> taskinfos = executorjob.getTasks();
					int localtotalsize = localjobStatic.getTotaltasks();
					for(int j = 0 ; j < servers; j ++)
					{
						int  perservertasks = 0;
						if(j < div)
							perservertasks = newtasks + 1;
						else
							perservertasks = newtasks;
						if(j == 0 )
						{
							perservertasks = perservertasks - selfinhandle;
							if(perservertasks > 0)
								startpos = startpos + perservertasks;
						}
						
						else
						{
							perservertasks = perservertasks - hostTaskInfos.get(servseradd[j]).intValue();
							if(perservertasks > 0 )
							{
								temp = new ArrayList<TaskInfo>(perservertasks);
								int l = 0;
								for(int k = startpos; k < taskinfos.size(); k ++ )
								{
									TaskInfo task = taskinfos.get(k);
									if(task.isReassigned())
									{
										startpos ++;
										continue;
									}
									else if(l < perservertasks)
									{
										task.setReassigned(true);
										temp.add(task);
										executorjob.reassignTask(task.getTaskNo());
										startpos ++;
										l ++;
									}
									else
									{
										break;
									}
								}
								if(temp.size() > 0)
								{
									perserverTasks.put(servseradd[j], temp);
									localtotalsize = localtotalsize - temp.size();//调整本地作业的分配的总任务数
								}
//								startpos = startpos + perservertasks;
							}
						}
						
					}
					localjobStatic.setTotaltasks(localtotalsize);
					ReassignTaskConfig reassignTaskConfig = new ReassignTaskConfig();
					reassignTaskConfig.setJobname(reassignTask.getJobname());
					reassignTaskConfig.setTasks(perserverTasks);
					Event<ReassignTaskConfig> event = new EventImpl<ReassignTaskConfig>(
							reassignTaskConfig, HDFSUploadData.hdfs_upload_monitor_reassigntasks_response_commond);
					/**
					 * 消息以异步方式传递
					 */

					EventHandle.getInstance().change(event, false);
					
//					int startpos = localjobStatic.getCurrentposition() + 1 + perservertasks[0];
//					ExecutorJob executorjob = Imp.getImpStaticManager().getExecutorJob(localnode);
//					TaskInfo[] taskinfos = executorjob.config.getTasks();
//					int k = 0;
//					for(int j = startpos ; j < taskinfos.length; j ++)
//					{
//						
//					}
					 
				 }
			 }
			 StringBuilder builder = new StringBuilder();
	//		 for(String dbname:dbnames)
	//		 {
	//			 try {
	//				DBUtil.stopPool(dbname);
	//				
	//			} catch (Exception e) {
	//				builder.append("停止dbname[").append(dbname).append("]失败:\r\n").append(SimpleStringUtil.exceptionToString(e)).append("\r\n");
	//			}
	//		 }
			 if(builder.length() > 0)
			 {
				 jobStatic.setErrormsg(builder.toString());
				
			 }
		 }
		 jobStatic.setEndTime(System.currentTimeMillis());
		 jobStatic.setStatus(1);
		
	}

}
