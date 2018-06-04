package com.pascloud.module.database.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.pascloud.constant.Constants;
import com.pascloud.module.common.service.AbstractBaseService;
import com.pascloud.module.server.service.ServerService;
import com.pascloud.utils.FileUtils;
import com.pascloud.vo.database.DBInfo;
import com.pascloud.vo.server.ServerVo;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.SCPClient;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;

@Service
public class DBServerService extends AbstractBaseService {

	@Autowired
	private ServerService m_serverService;

	private static Logger log = LoggerFactory.getLogger(DBServerService.class);

	public List<DBInfo> getOracleSidWithServer(ServerVo server) {
		List<DBInfo> result = new ArrayList<>();
		List<String> lists = getOracleSid(server.getIp());
		if (null != lists && lists.size() > 0) {
			for (String s : lists) {
				DBInfo db = new DBInfo();
				if (s.contains("ora_pmon")) {
					s = s.replaceAll("ora_pmon_", "");
					db.setId(s);
					db.setName(s);
					result.add(db);
				}
			}
		}
		return result;
	}

	public List<String> getOracleSid(String ip) {
		Connection conn = null;
		List<String> lists = new ArrayList<>();
		StringBuffer sb = new StringBuffer();
		InputStream stdout = null;
		StringTokenizer tokenStat = null;
		Session session = null;
		BufferedReader br = null;
		int i = 0;
		try {
			ServerVo vo = m_serverService.getByIP(ip);
			// SCPClient scpClient = conn.createSCPClient();
			if (null != vo) {
				conn = getScpClientConn(vo.getIp(), vo.getUsername(), vo.getPassword());
				session = conn.openSession();
				session.execCommand("ps -ef | grep ora_pmon");
				stdout = new StreamGobbler(session.getStdout());
				br = new BufferedReader(new InputStreamReader(stdout));
				while (true) {
					String line = br.readLine();
					if (line == null)
						break;
					if (line.contains("oracle")) {
						tokenStat = new StringTokenizer(line);
						for (i = 0; i < 7; i++) {
							tokenStat.nextToken();
						}
						lists.add(tokenStat.nextToken());
					} else {
						continue;
					}
					System.out.println(line);
					sb.append(line);
				}
			}

		} catch (IOException e) {
			log.error(e.getMessage());
		} finally {
			// session.close();
			try {
				stdout.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				log.error(e1.getMessage());
			}
			try {
				br.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				log.error(e.getMessage());
			}
			session.close();
			conn.close();
		}

		return lists;
	}

	public Boolean createOracleWithSid(String ip, String sid) {
		Boolean flag = false;
		ServerVo vo = m_serverService.getByIP(ip);
		log.info("ip:" + ip + ",sid:" + sid);
		flag = createOracleBySID(sid,ip);
		return flag;
	}

	private Boolean createOracleBySID(String sid, String ip) {
		Boolean flag = false;
		StringBuffer sb = new StringBuffer();
		InputStream stdout = null;
		Session session = null;
		BufferedReader br = null;
		int i = 0;
		Connection conn = null;

		try {
			if (null!= ip) {
				conn = getScpClientConn(ip, "oracle", "oracle");
				session = conn.openSession();
				session.execCommand("~/script/create_database.sh" + " " + sid);
				stdout = new StreamGobbler(session.getStdout());
				br = new BufferedReader(new InputStreamReader(stdout));
				while (true) {
					String line = br.readLine();
					if (line == null) {
						flag = true;
						break;
					} else {
						log.info(line);
					}
					sb.append(line);
				}
				System.out.println(flag);
			}else{
				return flag;
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			log.error(e.getMessage());
		} finally {
			// session.close();
			try {
				stdout.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				log.error(e1.getMessage());
			}
			try {
				br.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				log.error(e.getMessage());
			}
			session.close();
			conn.close();
		}

		return flag;
	}

	public Boolean deleteOracleWithSid(String ip, String sid, String oratabfilePath) {

		Boolean flag = false;
		Connection conn = null;
		try {
			log.info("删除数据库开始" + sid);
			ServerVo vo = m_serverService.getByIP(ip);
			if (null != vo) {
				conn = getScpClientConn(vo.getIp(), vo.getUsername(), vo.getPassword());
				if (shutDownOracleWithSid(conn, sid)) {
					if (deleteOracleWithSid(conn, sid)) {
						if (delSIDWithOratab(conn, sid, oratabfilePath)) {
							flag = true;
						}
					}
				}
			}
		} catch (Exception e) {
			log.info("删除数据库开始" + sid + "异常");
		} finally {
			conn.close();
		}
		return flag;
	}

	public Boolean startOracleWithSid(String sid) {
		Boolean flag = false;
		try {

		} catch (Exception e) {

		}

		return flag;
	}

	private Boolean shutDownOracleWithSid(Connection conn, String sid) {
		Boolean flag = false;
		Session session = null;
		InputStream stdout = null;
		BufferedReader br = null;
		try {
			log.info("关闭数据库");
			session = conn.openSession();
			session.execCommand("/home/oracle/script/shutdown.sh" + " " + sid);
			stdout = new StreamGobbler(session.getStdout());
			br = new BufferedReader(new InputStreamReader(stdout));
			while (true) {
				String line = br.readLine();
				if (line == null) {
					log.info("关闭数据库成功");
					flag = true;
					break;
				} else {
					log.info(line);
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
			log.error("关闭数据库异常");
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				log.error(e.getMessage());
			}
			try {
				stdout.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				log.error(e.getMessage());
			}
			session.close();
		}
		return flag;
	}

	private Boolean deleteOracleWithSid(Connection conn, String sid) {
		Boolean flag = false;
		Session session = null;
		InputStream stdout = null;
		BufferedReader br = null;
		try {
			log.info("删除数据库文件");
			session = conn.openSession();
			session.execCommand("/home/oracle/script/delete_database.sh" + " " + sid + " " + getSidEx(sid));
			stdout = new StreamGobbler(session.getStdout());
			br = new BufferedReader(new InputStreamReader(stdout));
			while (true) {
				String line = br.readLine();
				if (line == null) {
					log.info("删除数据库文件成功");
					flag = true;
					break;
				} else {
					log.info(line);
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
			log.info("删除数据库文件异常");
			log.error(e.getMessage());
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				log.error(e.getMessage());
			}
			try {
				stdout.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				log.error(e.getMessage());
			}
			session.close();
		}
		return flag;
	}

	private Boolean delSIDWithOratab(Connection conn, String sid, String oratabfilePath) {
		StringBuffer sb = new StringBuffer();
		Boolean flag = false;
		Session session = null;
		InputStream stdout = null;
		BufferedReader br = null;
		try {
			log.info("删除oratab文件里面的SID信息");
			session = conn.openSession();
			session.execCommand("cat " + "/etc/oratab");
			stdout = new StreamGobbler(session.getStdout());
			br = new BufferedReader(new InputStreamReader(stdout));
			while (true) {
				String line = br.readLine();
				if (line == null) {
					flag = true;
					break;
				}
				if (!line.startsWith("#")) {
					if (line.contains(sid)) {
					} else {
						sb.append(line).append("\n");
					}
				}
			}
			if (flag) {
				File file = new File(oratabfilePath);
				if (!file.exists()) {
					FileUtils.createOrExistsFile(file);
				}
				FileUtils.writeFileFromString(file, sb.toString(), false);
				uploadOratabToServer(conn,oratabfilePath);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
			log.info("删除oratab文件里面的SID信息异常");
			log.error(e.getMessage());
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				log.error(e.getMessage());
			}
			try {
				stdout.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				log.error(e.getMessage());
			}
			session.close();
		}
		return flag;
	}

	private String getSidEx(String sid) {
		String ex = Constants.ORACLE_SID_EX_PREEFIX;
		String sidNum = sid.replace(Constants.ORACLE_SID_PREEFIX, "");
		ex = ex + sidNum + "*";
		log.info("正则表达式：" + ex);
		return ex;
	}

	private Boolean uploadOratabToServer(Connection conn, String oratabfilePath) {
		Boolean flag = false;
		SCPClient scpClient = null;
		try {
			log.info("上传" + oratabfilePath + "文件");
			scpClient = conn.createSCPClient();
			scpClient.put(oratabfilePath, "/etc/");
			flag = true;
			log.info("上传" + oratabfilePath + "文件成功");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
			log.info("上传" + oratabfilePath + "文件异常");
		} finally {

		}

		return flag;
	}

	public static void main(String[] args) throws Exception {
		/*
		 * StringBuffer sb=new StringBuffer(); String oratabfilepath =
		 * "d:/oratab";
		 * 
		 * String sid = "cpas04"; String newline =
		 * sid+":/u01/app/oracle/product/11.2.0/dbhome_1:N"; Boolean existSID =
		 * false; DBServerService s = new DBServerService(); InputStream stdout
		 * = null; BufferedReader br = null; Session session = null; Connection
		 * conn = s.getScpClientConn("192.168.1.234", "root", "tccp@2012");
		 * //SCPClient scpClient = conn.createSCPClient(); session =
		 * conn.openSession(); session.execCommand("cat " + "/etc/oratab");
		 * stdout = new StreamGobbler(session.getStdout()); br = new
		 * BufferedReader(new InputStreamReader(stdout)); while (true) { String
		 * line = br.readLine(); if (line == null){ break; }
		 * 
		 * if(!line.startsWith("#")){ if(!line.contains(sid)){
		 * 
		 * }else{ existSID = true; } }
		 * 
		 * if(line.startsWith("#")){ if(!line.contains(sid)){
		 * 
		 * }else{ existSID = true; line = line.replaceAll("#", ""); } }
		 * 
		 * sb.append(line).append("\n");
		 * 
		 * }
		 * 
		 * if(!existSID){ sb.append(newline).append("\n"); }
		 * 
		 * File file = new File(oratabfilepath); if(!file.exists()){
		 * FileUtils.createOrExistsFile(file); }
		 * 
		 * FileUtils.writeFileFromString(file, sb.toString(), false);
		 * 
		 * br.close(); stdout.close(); session.close(); conn.close();
		 */

		String sid = "cpas01";

	}

}