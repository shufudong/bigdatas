package org.frameworkset.bigdata.imp;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.BlockingQueue;

import org.apache.log4j.Logger;

import com.frameworkset.common.poolman.DBUtil;
import com.frameworkset.common.poolman.NestedSQLException;
import com.frameworkset.common.poolman.SQLExecutor;
import com.frameworkset.common.poolman.handle.ResultSetNullRowHandler;
import com.frameworkset.common.poolman.sql.PoolManResultSetMetaData;
import com.frameworkset.orm.transaction.TransactionManager;
import com.frameworkset.util.SimpleStringUtil;

public class WriteDataTask {
	 private static Logger log = Logger.getLogger(WriteDataTask.class);
	 BlockingQueue<FileSegment> upfileQueues;
	 GenFileHelper genFileHelper;
	 FileSegment fileSegment;
	 public WriteDataTask(GenFileHelper genFileHelper, BlockingQueue<FileSegment> upfileQueues,FileSegment fileSegment)
	{
			this.fileSegment = fileSegment;
		 this.upfileQueues = upfileQueues;
		 this.genFileHelper = genFileHelper;
	 }
	 
	 public WriteDataTask(GenFileHelper genFileHelper)
		{
			
			 this.genFileHelper = genFileHelper;
		 }

	 PoolManResultSetMetaData metaData;
	 PoolManResultSetMetaData submetaData;
	 StringBuilder buidler = null;
	 
	 private Object handleDate(ResultSet row,int i)
	 {
		 Object value = null;
		 try {
				try {
					value = row.getTimestamp(i+1);
					if(value != null)
						value = ((java.sql.Timestamp)value).getTime();
					else
						value  = 0;
				} catch (Exception e) {
					value = row.getDate(i+1);
					if(value != null)
						value = ((java.sql.Date)value).getTime();
					else
						value  = 0;
					
				}
				
			} catch (Exception e) {
				value  = 0;
			}
		 return value;
	 }
	private Object getValue(int colType,FileSegment fileSegment ,ResultSet row,int i,String colName) throws Exception
	{
		Object value = null;
		
		try {
			if(colType == java.sql.Types.TIMESTAMP )
			{
				try {
					value = row.getTimestamp(i+1);
					if(value != null)
						value = ((java.sql.Timestamp)value).getTime();
					else
						value  = 0;
				} catch (Exception e) {
					value  = 0;
				}
			}
			else if(colType == java.sql.Types.DATE)
			{
//				try {
//					value = row.getDate(i+1);
//					if(value != null)
//						value = ((java.sql.Date)value).getTime();
//					else
//						value  = 0;
//				} catch (Exception e) {
//					value  = 0;
//				}
				value = handleDate(row,i);
			}
			else
			{
				/**
				 * try resolved oracle 数组越界问题，如果字段是number类型，并且值类似于1.30020034599143E-115，通过 row.getString(i+1)方法会抛出以下异常：
				 * java.lang.ArrayIndexOutOfBoundsException: -128
	at oracle.sql.LnxLibThin.lnxnuc(LnxLibThin.java:5746)
	at oracle.sql.NUMBER.toText(NUMBER.java:2682)
	at oracle.jdbc.driver.NumberCommonAccessor.getString(NumberCommonAccessor.java:6220)
	at oracle.jdbc.driver.T4CNumberAccessor.getString(T4CNumberAccessor.java:70)
	at oracle.jdbc.driver.OracleResultSetImpl.getString(OracleResultSetImpl.java:397)
	at oracle.jdbc.driver.OracleResultSet.getString(OracleResultSet.java:1515)
	at org.frameworkset.bigdata.imp.Solver$2.handleRow(Solver.java:122)
	at com.frameworkset.common.poolman.handle.ResultSetNullRowHandler.handleRow(ResultSetNullRowHandler.java:34)
	at com.frameworkset.common.poolman.ResultMap.buildRecord(ResultMap.java:403)
	at com.frameworkset.common.poolman.StatementInfo.buildResult(StatementInfo.java:730)
	at com.frameworkset.common.poolman.StatementInfo.buildResultMap(StatementInfo.java:953)
	at com.frameworkset.common.poolman.PreparedDBUtil.doPrepareSelectCommon(PreparedDBUtil.java:2082)
	at com.frameworkset.common.poolman.PreparedDBUtil.innerExecute(PreparedDBUtil.java:1524)
	at com.frameworkset.common.poolman.PreparedDBUtil.executePreparedForObject(PreparedDBUtil.java:1233)
	at com.frameworkset.common.poolman.PreparedDBUtil.executePreparedWithRowHandler(PreparedDBUtil.java:1193)
	at com.frameworkset.common.poolman.PreparedDBUtil.executePreparedWithRowHandler(PreparedDBUtil.java:1188)
	at com.frameworkset.common.poolman.SQLInfoExecutor.queryWithDBNameByNullRowHandler(SQLInfoExecutor.java:1149)
	at com.frameworkset.common.poolman.SQLExecutor.queryWithDBNameByNullRowHandler(SQLExecutor.java:1344)
	at org.frameworkset.bigdata.imp.Solver.main(Solver.java:116)
				 */
				try {
					value = row.getString(i+1);
				} catch (Exception e) {
					
					if(this.fileSegment.job.config.pkname != null)
					{
						try {
							String pkvalue = row.getString(this.fileSegment.job.config.pkname);
							log.error("Get column["+colName+"] value by  ResultSet.getString method for row that pkvalue["+this.fileSegment.job.config.pkname+"="+pkvalue+"] failed,Use ResultSet.getObject method again.",e);
						} catch (Exception e1) {
							log.error("Get column["+colName+"] value failed,Use ResultSet.getObject method again.",e);
						}
					}
					else
					{
						log.error("Get column["+colName+"] value failed,Use ResultSet.getObject method again.",e);							
					}
					
					value = row.getObject(i+1);
					
				}
			}
				
		} catch (Exception e) {
			if(this.fileSegment.job.config.pkname != null)
			{
				String pkvalue = row.getString(this.fileSegment.job.config.pkname);
				log.error("Get column["+colName+"] value for row that pkvalue["+this.fileSegment.job.config.pkname+"="+pkvalue+"] failed:",e);
				throw new RowHandlerException("Get column["+colName+"] value for row that pkvalue["+this.fileSegment.job.config.pkname+"="+pkvalue+"] failed:",e);
			}
			else
			{
				log.error("Get column["+colName+"] value failed:",e);
				
				throw new RowHandlerException("Get column["+colName+"] value failed:",e);
			}
			 
		}
		return value;
	}
	
	private Object getRightJoinBy(int colType,ResultSet row,String colName) throws Exception
	{
		return row.getObject(colName);
	}
	private void write(FileSegment fileSegment ,ResultSet row) throws Exception
    {
		try
		{
//			if(fileSegment.getRows() == 2)
//			{
//				throw new RowHandlerException("测试");
//			}
			if(metaData == null)
				metaData = PoolManResultSetMetaData.getCopy(row.getMetaData());
			
	
			String rightJoinByColumn = fileSegment.getRightJoinBy();
			boolean usesubquery = fileSegment.getSubQuerystatement() != null && !fileSegment.getSubQuerystatement().equals("") && !SimpleStringUtil.isEmpty(rightJoinByColumn);
			Object rightJoinBy = null;
			
	    	int counts = metaData.getColumnCount();
	    	if(fileSegment.job.config.datatype == null || fileSegment.job.config.datatype.equals("json"))
	    	{
		    	buidler.append("{");
				for(int i =0; i < counts; i++)
				{
					String colName = metaData.getColumnLabelUpperByIndex(i);
					int colType = metaData.getColumnTypeByIndex(i);
					if("ROWNUM__".equals(colName))//去掉oracle的行伪列
						continue;
					
					Object value = getValue(  colType,  fileSegment ,  row,  i,  colName);
					if(usesubquery && colName.equalsIgnoreCase(rightJoinByColumn))
					{
						rightJoinBy = getRightJoinBy(  colType,  row,  colName);
					}
					if(i == 0)
					{
						buidler.append("\""+colName+"\":\""+value  +"\"");
					}
					else
					{
						buidler.append(",\""+colName+"\":\""+value  +"\"");
					}
				}
				if(usesubquery && rightJoinBy != null)
					appendSubTableColumns(buidler,rightJoinBy);
				buidler.append("}");
				
	    	}
	    	else
	    	{
	    		for(int i =0; i < counts; i++)
	    		{
		    		String colName = metaData.getColumnLabelUpperByIndex(i);
					int colType = metaData.getColumnTypeByIndex(i);
					
					if("ROWNUM__".equals(colName))//去掉oracle的行伪列
						continue;
					Object value = getValue(  colType,  fileSegment ,  row,  i,  colName);
					if(usesubquery && colName.equalsIgnoreCase(rightJoinByColumn))
					{
						rightJoinBy = getRightJoinBy(  colType,  row,  colName);
					}	
					if(i == 0)
					{
						buidler.append(colName).append("#").append(value );
					}
					else
					{
						buidler.append("#").append(colName).append("#").append(value );
					}
	    		}
	    		if(usesubquery && rightJoinBy != null)
					appendSubTableColumns(buidler,rightJoinBy);
	    	}
	    	
	    	fileSegment.writeLine(buidler.toString());
	    	
		}
		catch(NestedSQLException e)
		{
			fileSegment.errorrow();
			if(e.getCause() != null && e.getCause() instanceof RowHandlerException)
			{
				if(fileSegment.reacherrorlimit())				
				{
					fileSegment.appendErrorMsg(SimpleStringUtil.formatException(e.getCause()));//只记录最后一个异常
					throw (RowHandlerException)e.getCause();
				}
				else if(fileSegment.job.getErrorrowslimit() >= 0)
				{
					fileSegment.appendErrorMsg(SimpleStringUtil.formatException(e.getCause()));//只记录最后一个异常
				}
			}
			else if(e.getCause() != null && e.getCause() instanceof ForceStopException) 
			{
				throw (ForceStopException)e.getCause();
			}
			else
			{
				if(fileSegment.reacherrorlimit())
				{
					fileSegment.appendErrorMsg(SimpleStringUtil.formatException(e.getCause() == null?e:e.getCause()));
					throw e;
				}
				else if(fileSegment.job.getErrorrowslimit() >= 0)
				{
					fileSegment.appendErrorMsg(SimpleStringUtil.formatException(e.getCause() == null?e:e.getCause()));//只记录最后一个异常
				}
				else
				{
					log.error("",e);
				}
			}
		}
		catch(ForceStopException e)
		{
			fileSegment.errorrow();
			throw e;
		}
		catch(RowHandlerException e)
		{
			fileSegment.errorrow();
			if(fileSegment.reacherrorlimit())	
			{
				fileSegment.appendErrorMsg(SimpleStringUtil.formatException(e));
				throw e;
			}
			else if(fileSegment.job.getErrorrowslimit() >= 0)
			{
				fileSegment.appendErrorMsg(SimpleStringUtil.formatException(e));//只记录最后一个异常
			}
			
		}
		catch(Exception e)
		{
			fileSegment.errorrow();
		
			if(fileSegment.reacherrorlimit())
			{
				fileSegment.appendErrorMsg(SimpleStringUtil.formatException(e));
				throw e;
			}
			else if(fileSegment.job.getErrorrowslimit() >= 0)
			{
				fileSegment.appendErrorMsg(SimpleStringUtil.formatException(e));//只记录最后一个异常
			}
			else
			{
				log.error("",e);
			}
				
		}
		finally
		{
			buidler.setLength(0);
		}
		
 
    	
    	
    }
	
	
	
	private void writeSub(Object rightJoinBy,StringBuilder builder ,ResultSet row) throws Exception
    {
		try
		{
			if(submetaData == null)
				submetaData = PoolManResultSetMetaData.getCopy(row.getMetaData());
			
	
		
			
	    	int counts = submetaData.getColumnCount();
	    	if(fileSegment.job.config.datatype == null || fileSegment.job.config.datatype.equals("json"))
	    	{
		    
				for(int i =0; i < counts; i++)
				{
					String colName = submetaData.getColumnLabelUpperByIndex(i);
					int colType = submetaData.getColumnTypeByIndex(i);
					if("ROWNUM__".equals(colName))//去掉oracle的行伪列
						continue;
					
					Object value = getValue(  colType,  fileSegment ,  row,  i,  colName);
					buidler.append(",\""+colName+"\":\""+value  +"\"");
					
				}
				 
				 
				
	    	}
	    	else
	    	{
	    		for(int i =0; i < counts; i++)
	    		{
		    		String colName = submetaData.getColumnLabelUpperByIndex(i);
					int colType = submetaData.getColumnTypeByIndex(i);
					
					if("ROWNUM__".equals(colName))//去掉oracle的行伪列
						continue;
					Object value = getValue(  colType,  fileSegment ,  row,  i,  colName);
					buidler.append("#").append(colName).append("#").append(value );
					
	    		}
	    		 
	    	}
	    	
	    	 
		}
		catch(Exception e)
		{
			
			throw new RowHandlerException("Get sub table record by join["+rightJoinBy+"]failed:",e);
		}
		
 
    	
    	
    }
	
	private void appendSubTableColumns(final StringBuilder builder,final Object rightJoinBy) throws Exception
	{
		 
		
			SQLExecutor.queryWithDBNameByNullRowHandler(new ResultSetNullRowHandler(){
	 			
				@Override
				public void handleRow(ResultSet row) throws Exception {
					if(genFileHelper.isforceStop())
						throw new ForceStopException();
					writeSub( rightJoinBy, builder ,row);
					if(genFileHelper.isforceStop())
						throw new ForceStopException();
				}
	    		
	    	}, fileSegment.getDBName(), fileSegment.getSubQuerystatement(),rightJoinBy);
		
	}
 
	 private void genrangequery(final FileSegment fileSegment) throws SQLException
	 {
		 if(fileSegment.dateRange())
			{
	 			SQLExecutor.queryWithDBNameByNullRowHandler(new ResultSetNullRowHandler(){
	 				
					@Override
					public void handleRow(ResultSet row) throws Exception {
						if(genFileHelper.isforceStop())
							throw new ForceStopException();
						write(  fileSegment,row);
						if(genFileHelper.isforceStop())
							throw new ForceStopException();
					}
		    		
		    	}, fileSegment.getDBName(), fileSegment.getQuerystatement(),new java.sql.Timestamp(fileSegment.getEndoffset()),new java.sql.Timestamp(fileSegment.getStartoffset()));
			}
			else if(fileSegment.timestampRange())
			{
				SQLExecutor.queryWithDBNameByNullRowHandler(new ResultSetNullRowHandler(){
	 				
					@Override
					public void handleRow(ResultSet row) throws Exception {
						if(genFileHelper.isforceStop())
							throw new ForceStopException();
						write(  fileSegment,row);
						if(genFileHelper.isforceStop())
							throw new ForceStopException();
					}
		    		
		    	}, fileSegment.getDBName(), fileSegment.getQuerystatement(),new java.sql.Timestamp(fileSegment.getEndoffset()),new java.sql.Timestamp(fileSegment.getStartoffset()));
			}
			else
			{
				SQLExecutor.queryWithDBNameByNullRowHandler(new ResultSetNullRowHandler(){
	 				
					@Override
					public void handleRow(ResultSet row) throws Exception {
						if(genFileHelper.isforceStop())
							throw new ForceStopException();
						write(  fileSegment,row);
						if(genFileHelper.isforceStop())
							throw new ForceStopException();
					}
		    		
		    	}, fileSegment.getDBName(), fileSegment.getQuerystatement(),fileSegment.getEndoffset(),fileSegment.getStartoffset());
			}
	 }
 
	 private void genpage(final FileSegment fileSegment  ) throws Exception
	    {
		 
		 	TransactionManager tm = new TransactionManager();
		 	try
			 	{
		 		tm.begin(TransactionManager.RW_TRANSACTION);
			 	if(fileSegment.usepartition())
			 	{
			 		if(!fileSegment.partitiondataraged())
			 		{
				 		SQLExecutor.queryWithDBNameByNullRowHandler(new ResultSetNullRowHandler(){
				 			
							@Override
							public void handleRow(ResultSet row) throws Exception {
								if(genFileHelper.isforceStop())
									throw new ForceStopException();
								write(  fileSegment,row);
								if(genFileHelper.isforceStop())
									throw new ForceStopException();
							}
				    		
				    	}, fileSegment.getDBName(), fileSegment.getQuerystatement());
			 		}
			 		else
			 		{
//			 			if(fileSegment.dateRange())
//			 			{
//				 			SQLExecutor.queryWithDBNameByNullRowHandler(new ResultSetNullRowHandler(){
//				 				
//								@Override
//								public void handleRow(ResultSet row) throws Exception {
//									if(genFileHelper.isforceStop())
//										throw new ForceStopException();
//									write(  fileSegment,row);
//									if(genFileHelper.isforceStop())
//										throw new ForceStopException();
//								}
//					    		
//					    	}, fileSegment.getDBName(), fileSegment.getQuerystatement(),new java.sql.Date(fileSegment.getEndoffset()),new java.sql.Date(fileSegment.getStartoffset()));
//			 			}
//			 			else if(fileSegment.timestampRange())
//			 			{
//			 				SQLExecutor.queryWithDBNameByNullRowHandler(new ResultSetNullRowHandler(){
//				 				
//								@Override
//								public void handleRow(ResultSet row) throws Exception {
//									if(genFileHelper.isforceStop())
//										throw new ForceStopException();
//									write(  fileSegment,row);
//									if(genFileHelper.isforceStop())
//										throw new ForceStopException();
//								}
//					    		
//					    	}, fileSegment.getDBName(), fileSegment.getQuerystatement(),new java.sql.Timestamp(fileSegment.getEndoffset()),new java.sql.Timestamp(fileSegment.getStartoffset()));
//			 			}
//			 			else
//			 			{
//			 				SQLExecutor.queryWithDBNameByNullRowHandler(new ResultSetNullRowHandler(){
//				 				
//								@Override
//								public void handleRow(ResultSet row) throws Exception {
//									if(genFileHelper.isforceStop())
//										throw new ForceStopException();
//									write(  fileSegment,row);
//									if(genFileHelper.isforceStop())
//										throw new ForceStopException();
//								}
//					    		
//					    	}, fileSegment.getDBName(), fileSegment.getQuerystatement(),fileSegment.getEndoffset(),fileSegment.getStartoffset());
//			 			}
			 			genrangequery(fileSegment);
			 				
			 		}
			 	}
			 	else if(!fileSegment.usepagine())//采用主键分区模式
			 	{
//			    	SQLExecutor.queryWithDBNameByNullRowHandler(new ResultSetNullRowHandler(){
//		
//						@Override
//						public void handleRow(ResultSet row) throws Exception {
//							if(genFileHelper.isforceStop())
//								throw new ForceStopException();
//							write(  fileSegment,row);
//							if(genFileHelper.isforceStop())
//								throw new ForceStopException();
//						}
//			    		
//			    	}, fileSegment.getDBName(), fileSegment.getQuerystatement(),fileSegment.getEndoffset(),fileSegment.getStartoffset());
			 		genrangequery(fileSegment);
			 	}
			 	else//采用分页分区模式，mysql，oracle
			 	{
			 		DBUtil.getDBAdapter(fileSegment.getDBName()).queryByNullRowHandler(new ResultSetNullRowHandler(){
			 			
						@Override
						public void handleRow(ResultSet row) throws Exception {
							if(genFileHelper.isforceStop())
								throw new ForceStopException();
							write(  fileSegment,row);
							if(genFileHelper.isforceStop())
								throw new ForceStopException();
						}
			    		
			    	}, fileSegment.getDBName(), fileSegment.getPageinestatement(),fileSegment.getStartoffset(),(int)fileSegment.getPagesize());
			 		
			 	}
			 	tm.commit();
		    }
		 	finally
		 	{
		 		tm.release();
		 	}
	    }
	 
	 int fileNo = 0;
	 long pos = 0;
	 long startpos = 0;
	 
	 private void initSegement() throws Exception
	 {
		 this.fileSegment = genFileHelper.createSingleFileSegment(fileNo, pos);
	 		fileSegment.genstarttimestamp =  System.currentTimeMillis();
	 		fileSegment.init();
	 		fileSegment.taskStatus.setTaskInfo(fileSegment.toString());
	 		log.info("开始生成文件："+fileSegment.toString());
	 }
	 
	 private void finishSegement() throws Exception
	 {
		 	fileSegment.flush();
			
			fileSegment.genendtimestamp =  System.currentTimeMillis();
			
			genFileHelper.job.completeTask(fileSegment.taskInfo.taskNo);
			if(fileSegment.taskStatus.getStatus() != 2)
				fileSegment.taskStatus.setStatus(1);
			fileSegment.taskStatus.setTaskInfo(fileSegment.toString());
			log.info("生成文件结束："+fileSegment.toString());
			fileSegment.handleerrormsgs();
			fileSegment.close();
			fileNo ++ ;
	 }
	 int offsetcount = 0;
	 boolean startWithfileno ;
	 private void gensinglepage()  
	    {
		 
		 	TransactionManager tm = new TransactionManager();
		 	StringBuilder errorinfo = new StringBuilder();
		 	
		 	try
			 {
		 		 buidler = new StringBuilder();
		 		startWithfileno = genFileHelper.config.getStartfileNo() > 0 && genFileHelper.config.getRowsperfile() > 0;
		 		 if(startWithfileno)
		 		 {
		 			 this.startpos = genFileHelper.config.getStartfileNo() * genFileHelper.config.getRowsperfile();
		 		 }
		 		 else
		 		 {

		 			 initSegement();//初始化第一个文档
		 		 }
			 
		 		tm.begin(TransactionManager.RW_TRANSACTION);
			  
		 		SQLExecutor.queryWithDBNameByNullRowHandler(new ResultSetNullRowHandler(){
		 			
					@Override
					public void handleRow(ResultSet row) throws Exception {
						
						if(genFileHelper.isforceStop())
							throw new ForceStopException();
						
						if(startWithfileno)
						{
							if(offsetcount == startpos)
							{
								fileNo = genFileHelper.config.getStartfileNo();
								pos = offsetcount;
								 initSegement();//初始化第一个文档
								 startWithfileno = false;
							}
							else if(offsetcount < startpos)
							{
								offsetcount ++;
								return;
							}
							
							
						}
						
						write(  fileSegment,row);
						pos ++;
						
						if(genFileHelper.isforceStop())
							throw new ForceStopException();
					 
						 
						if(fileSegment.reachlimitsize())
						{
//								fileSegment.flush();
//								
//								fileSegment.genendtimestamp =  System.currentTimeMillis();
//								fileSegment.close();
//								genFileHelper.job.completeTask(fileSegment.taskInfo.taskNo);
//								
//								log.info("生成文件结束："+fileSegment.toString());
//								fileNo ++ ;
							finishSegement();
//								fileSegment =  genFileHelper.createSingleFileSegment(fileNo, pos);
//								fileSegment.genstarttimestamp =  System.currentTimeMillis();
//							 	fileSegment.init();
//							 	log.info("开始生成文件："+fileSegment.toString());
							initSegement();
						}
						 
					}
		    		
		    	}, genFileHelper.getDBName(), genFileHelper.getQuerystatement());
			 	 
			 	tm.commit();
			 	if(!fileSegment.isFlushed())//所有的数据放到一个文件中
			 	{
//				 	fileSegment.flush();
//					fileSegment.genendtimestamp =  System.currentTimeMillis();
//					this.genFileHelper.job.completeTask(fileSegment.taskInfo.taskNo);
//					log.info("生成文件结束："+fileSegment.toString());
			 		finishSegement();
			 	}
		    }
		 	catch(NestedSQLException e)
		 	{
		 		Throwable innere = e.getCause();
		 		if(innere == null)
		 		{
		 			if(fileSegment != null)//所有的数据放到一个文件中
					{
						this.genFileHelper.job.completeTask(fileSegment.taskInfo.taskNo);
						fileSegment.taskStatus.setStatus(2);
						 
						fileSegment.taskStatus.setErrorInfo(SimpleStringUtil.exceptionToString(e));
						fileSegment.taskStatus.setTaskInfo(fileSegment.toString());
						log.error("生成文件异常结束："+fileSegment.toString(),e);
					}
					errorinfo.append(SimpleStringUtil.exceptionToString(e)).append("\r\n");
		 		}
		 		else if(innere instanceof ForceStopException)
		 		{
		 			if(fileSegment != null)//所有的数据放到一个文件中
			 		{
						fileSegment.taskStatus.setStatus(2);
						 
						fileSegment.taskStatus.setErrorInfo("强制停止任务！");
						fileSegment.taskStatus.setTaskInfo(fileSegment.toString());
			 		}
		 		}
		 		else
		 		{
		 			if(fileSegment != null)//所有的数据放到一个文件中
					{
						this.genFileHelper.job.completeTask(fileSegment.taskInfo.taskNo);
						fileSegment.taskStatus.setStatus(2);
						 
						fileSegment.taskStatus.setErrorInfo(SimpleStringUtil.exceptionToString(innere));
						fileSegment.taskStatus.setTaskInfo(fileSegment.toString());
						log.error("生成文件异常结束："+fileSegment.toString(),innere);
					}
					errorinfo.append(SimpleStringUtil.exceptionToString(innere)).append("\r\n");
		 		}
		 		return;
		 		
		 	}
		 	catch(ForceStopException e)
		    {
		 		if(fileSegment != null)//所有的数据放到一个文件中
		 		{
					fileSegment.taskStatus.setStatus(2);
					 
					fileSegment.taskStatus.setErrorInfo("强制停止任务！");
					fileSegment.taskStatus.setTaskInfo(fileSegment.toString());
		 		}
				return ;
		    }
			catch (Exception e) {
				if(fileSegment != null)//所有的数据放到一个文件中
				{
					this.genFileHelper.job.completeTask(fileSegment.taskInfo.taskNo);
					fileSegment.taskStatus.setStatus(2);
					 
					fileSegment.taskStatus.setErrorInfo(SimpleStringUtil.exceptionToString(e));
					fileSegment.taskStatus.setTaskInfo(fileSegment.toString());
					log.error("生成文件异常结束："+fileSegment.toString(),e);
				}
				errorinfo.append(SimpleStringUtil.exceptionToString(e)).append("\r\n");
				return;
			}
			finally
			{
				tm.release();
				this.buidler = null;
				if(fileSegment != null && !fileSegment.isClosed())//所有的数据放到一个文件中
				{
					fileSegment.handleerrormsgs();
					fileSegment.close();
				}
				
				cleanSingleJob( errorinfo);
				
			}
		 	
	    }
	 
	 private void cleanSingleJob(StringBuilder errorinfo)
		{
			 
			 if(errorinfo.length() == 0)
			 {
				 if(this.genFileHelper.job.jobStatic.getStatus() == 0 || this.genFileHelper.job.jobStatic.getStatus() == -1)
					 this.genFileHelper.job.jobStatic.setStatus(1);
			 }
			 else
			 {
				 this.genFileHelper.job.jobStatic.setStatus(2);
				 this.genFileHelper.job.jobStatic.setErrormsg(errorinfo.toString());
			 }
			
			 this.genFileHelper.job.jobStatic.setEndTime(System.currentTimeMillis());
			 
			
		}
	 
	private void runmulti()
	{
		try {
			
			 buidler = new StringBuilder();
			fileSegment.init();
			log.info("开始生成文件："+fileSegment.toString());
			genpage( fileSegment  ) ;
			fileSegment.flush();
			
			fileSegment.genendtimestamp =  System.currentTimeMillis();
			this.genFileHelper.job.completeTask(fileSegment.taskInfo.taskNo);
			log.info("生成文件结束："+fileSegment.toString());
		} 
		catch(NestedSQLException e)
	 	{
	 		Throwable innere = e.getCause();
	 		if(innere == null)
	 		{
	 			this.genFileHelper.job.completeTask(fileSegment.taskInfo.taskNo);
				fileSegment.taskStatus.setStatus(2);
				 
				fileSegment.taskStatus.setErrorInfo(SimpleStringUtil.exceptionToString(e));
				log.error("生成文件异常结束："+fileSegment.toString(),e);
				if(genFileHelper.genlocalfile())
					genFileHelper.countdownupfilecount();
	 		}
	 		else if(innere instanceof ForceStopException)
	 		{
	 			fileSegment.taskStatus.setStatus(2);
				 
				fileSegment.taskStatus.setErrorInfo("强制停止任务！");
	 		}
	 		else
	 		{
	 			this.genFileHelper.job.completeTask(fileSegment.taskInfo.taskNo);
				fileSegment.taskStatus.setStatus(2);
				 
				fileSegment.taskStatus.setErrorInfo(SimpleStringUtil.exceptionToString(innere));
				log.error("生成文件异常结束："+fileSegment.toString(),innere);
				if(genFileHelper.genlocalfile())
					genFileHelper.countdownupfilecount();
	 		}
	 		return;
	 		
	 	}
		catch(ForceStopException e)
	    {
			fileSegment.taskStatus.setStatus(2);
			 
			fileSegment.taskStatus.setErrorInfo("强制停止任务！");
			return ;
	    }
		catch (Exception e) {
			this.genFileHelper.job.completeTask(fileSegment.taskInfo.taskNo);
			fileSegment.taskStatus.setStatus(2);
			 
			fileSegment.taskStatus.setErrorInfo(SimpleStringUtil.exceptionToString(e));
			log.error("生成文件异常结束："+fileSegment.toString(),e);
			if(genFileHelper.genlocalfile())
				genFileHelper.countdownupfilecount();
			return;
		}
		finally
		{
			this.buidler = null;
			fileSegment.handleerrormsgs();
			fileSegment.close();
		}
		if(genFileHelper.genlocalfile())
		{
			if(fileSegment.getRows() == 0)//忽略空文件上传
			{
				genFileHelper.countdownupfilecount();
			}
			else
			{
				try {
					upfileQueues.put(fileSegment);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					genFileHelper.countdownupfilecount();
				} 
			}
		}
	}
	 
	 
	public void run() {
		if(!genFileHelper.isOnejob())
			runmulti();
		else
		{
			gensinglepage();
			
		}
	}
}
