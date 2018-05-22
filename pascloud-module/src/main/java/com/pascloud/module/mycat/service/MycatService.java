package com.pascloud.module.mycat.service;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.dom4j.Document;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.druid.pool.DruidDataSource;
import com.google.gson.Gson;
import com.pascloud.bean.docker.ContainerVo;
import com.pascloud.bean.docker.NodeVo;
import com.pascloud.bean.mycat.DataHostVo;
import com.pascloud.bean.mycat.DataNodeVo;
import com.pascloud.bean.mycat.DataSourceVo;
import com.pascloud.constant.Constants;
import com.pascloud.module.config.PasCloudConfig;
import com.pascloud.module.docker.service.DockerService;
import com.pascloud.utils.PasCloudCode;
import com.pascloud.utils.xml.MycatXmlUtils;
import com.pascloud.utils.xml.XmlParser;
import com.pascloud.vo.database.DBColumnVo;
import com.pascloud.vo.result.ResultListData;
import com.spotify.docker.client.DefaultDockerClient;


@Service
public class MycatService {
	
	private static Logger log = LoggerFactory.getLogger(MycatService.class);
	
	@Autowired
	private PasCloudConfig m_config;
	
	@Autowired
	private DockerService m_dockerService;
	
	
	public List<DataHostVo> getDataHosts(){
		
		List<DataHostVo> result = new ArrayList<>();
		
		String mycat_schema_path = System.getProperty(Constants.WEB_APP_ROOT_DEFAULT)+m_config.getPASCLOUD_MYCAT_DIR()+File.separator+Constants.MYCAT_SCHEMA;
		Document doc = XmlParser.getDocument(mycat_schema_path);
		Element root = doc.getRootElement();
		List<Element> nodes = root.elements("dataHost");
		
		if(nodes.size()>0){
			Iterator<Element> it = nodes.iterator();
			while(it.hasNext()){
				Element e = it.next();
				DataHostVo vo = new DataHostVo();
				vo.setName(e.attributeValue("name"));
				vo.setDbType(e.attributeValue("dbType"));
				vo.setDbDriver(e.attributeValue("dbDriver"));
				parserWritehost(e,vo);
				
				result.add(vo);
			}
		}
		
		return result;
		
	}
	
	public List<DataNodeVo> getDataNodes(){
		List<DataNodeVo> result = new ArrayList<>();
		String mycat_schema_path = System.getProperty(Constants.WEB_APP_ROOT_DEFAULT)+m_config.getPASCLOUD_MYCAT_DIR()+File.separator+Constants.MYCAT_SCHEMA;
		Document doc = XmlParser.getDocument(mycat_schema_path);
		Element root = doc.getRootElement();
		List<Element> nodes = root.elements("dataNode");
		
		if(nodes.size()>0){
			Iterator<Element> it = nodes.iterator();
			while(it.hasNext()){
				Element e = it.next();
				DataNodeVo vo = new DataNodeVo();
				vo.setName(e.attributeValue("name"));
				vo.setDataHost(e.attributeValue("dataHost"));
				vo.setDatabase(e.attributeValue("database"));
				DataHostVo dvo = getDataHostByName(vo.getDataHost(),root);
				if(null != dvo){
					vo.setUrl(dvo.getUrl());
					vo.setPassword(dvo.getPassword());
					vo.setUser(dvo.getUser());
					vo.setDbType(dvo.getDbType());
					vo.setDbDriver(dvo.getDbDriver());
					vo.setPort(parserPort(dvo.getUrl(),dvo.getDbType()));
					vo.setIp(parserIP(dvo.getUrl(),dvo.getDbType()));
				}
				
				result.add(vo);
			}	
		}
		return result;
	}
	
	public void addDatanode(String name,String dbType,String ip,String user,String password,String database,Integer port){
		String mycat_schema_path = System.getProperty(Constants.WEB_APP_ROOT_DEFAULT)+m_config.getPASCLOUD_MYCAT_DIR()+File.separator+Constants.MYCAT_SCHEMA;
		String mycat_server_path = System.getProperty(Constants.WEB_APP_ROOT_DEFAULT)+m_config.getPASCLOUD_MYCAT_DIR()+File.separator+Constants.MYCAT_SERVER;
		
		MycatXmlUtils.addSchemaAndNodeAndHost(mycat_schema_path, name, dbType, ip, user, password, database, port);
		
		MycatXmlUtils.addServer(mycat_server_path, name);
		
		//MycatXmlUtils.addSchemaAndNodeAndHost(mycat_schema_path,"dn22", "oracle", "192.168.0.16", "pas", "pas", "cpas", 1521);
	}
	
	public void delDatanode(String name){
		String mycat_schema_path = System.getProperty(Constants.WEB_APP_ROOT_DEFAULT)+m_config.getPASCLOUD_MYCAT_DIR()+File.separator+Constants.MYCAT_SCHEMA;
		String mycat_server_path = System.getProperty(Constants.WEB_APP_ROOT_DEFAULT)+m_config.getPASCLOUD_MYCAT_DIR()+File.separator+Constants.MYCAT_SERVER;
		MycatXmlUtils.delSchemaAndNodeAndHost(mycat_schema_path, name);
		
		MycatXmlUtils.delServer(mycat_server_path, name);
	}
	
    private List<DataHostVo> getDataHosts(Element root){
		List<DataHostVo> result = new ArrayList<>();
		
		//String mycat_schema_path = m_config.getPASCLOUD_MYCAT_DIR()+File.separator+"schema.xml";
		//Document doc = XmlParser.getDocument(mycat_schema_path);
		//Element root = doc.getRootElement();
		List<Element> nodes = root.elements("dataHost");
		
		if(nodes.size()>0){
			Iterator<Element> it = nodes.iterator();
			while(it.hasNext()){
				Element e = it.next();
				DataHostVo vo = new DataHostVo();
				vo.setName(e.attributeValue("name"));
				vo.setDbType(e.attributeValue("dbType"));
				vo.setDbDriver(e.attributeValue("dbDriver"));
				parserWritehost(e,vo);
				//vo.setUrl(paserPort(vo.getUrl(),vo.getDbType()));
				result.add(vo);
			}
		}
		
		return result;
		
	}
    
    
    private DataHostVo getDataHostByName(String name,Element root){
		DataHostVo vo = null;
		List<DataHostVo> result = getDataHosts(root);
		if(null != result && result.size()>0){
			for(DataHostVo v : result){
				if(v.getName().equals(name)){
					vo = v;
				}
			}
		}
		return vo;
	}
	
	
	private void parserWritehost(Element e,DataHostVo vo){
		
		Element children = (Element) e.selectSingleNode("writeHost");
		vo.setUrl(children.attributeValue("url"));
		vo.setPassword(children.attributeValue("password"));
		vo.setUser(children.attributeValue("user"));
		
		//System.out.println(children.asXML());
	}
	
	private String parserIP(String url,String dbType){
		String ip = "";
		if(url.length()>0){
			if(dbType.equals("mysql")){
				int index = url.lastIndexOf(":");
				//System.out.println(index);
				url = url.substring(0, index);
			}else if(dbType.equals("oracle")){
				int index = url.lastIndexOf("@");
				url = url.substring(index+1,url.length());
				index = url.indexOf(":");
				url = url.substring(0, index);
				//ip = url;
			}else if(dbType.equals("db2")){
				//int index = url.indexOf(":");
				url = url.replace("jdbc:db2://", "");
				url = url.substring(0, url.length());
				int index = url.indexOf(":");
				url = url.substring(0, index);
			}
			ip = url;
			//System.out.println(url);
		}
		return ip;
	}
	
	private String parserPort(String url,String dbType){
		//System.out.println(url+dbType);
		String port = "";
		
		if(url.length()>0){
			if(dbType.equals("mysql")){
				int index = url.lastIndexOf(":");
				//System.out.println(index);
				port = url.substring(index+1,url.length());
			}else if(dbType.equals("oracle")){
				int index = url.lastIndexOf(":");
				url = url.substring(0, index);
				index = url.lastIndexOf(":");
				port = url.substring(index+1,url.length());
			}else if(dbType.equals("db2")){
				int index = url.lastIndexOf("/");
				url = url.substring(0, index);
				index = url.lastIndexOf(":");
				port = url.substring(index+1,url.length());
			}
		}
		return port;
		
	}
	
	@SuppressWarnings("unchecked")
	public List<DataSourceVo> getDataSourceList(DataSource dataSource){
		List<DataSourceVo> result = new ArrayList<>();
		
		Connection conn = null;
		String sql = "show @@datasource";
		try {
			conn = dataSource.getConnection();
			QueryRunner qRunner = new QueryRunner();  
			result =  (List<DataSourceVo>)qRunner.query(conn,sql, new BeanListHandler(DataSourceVo.class));
			Gson g = new Gson();
			System.out.println(g.toJson(result));
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			log.error(e.getMessage());
		}finally{
			try {
				conn.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				log.error(e.getMessage());
			}
		}
		return result;
	}
	
	public List<DataSourceVo> getDataSource(DefaultDockerClient dockerClient,
			Integer defaultPort){
		
		//ComboPooledDataSource dataSource = new ComboPooledDataSource();
		DruidDataSource dataSource = new DruidDataSource();
		List<ContainerVo> containers = new ArrayList<>();
		List<DataSourceVo> ds = new ArrayList<>();
		
		try {
			List<NodeVo> nodes = new ArrayList<>();
			nodes = m_dockerService.getNodes(dockerClient);
			/****查询运行的服务***/
			for(NodeVo vo: nodes){
				DefaultDockerClient client = DefaultDockerClient.builder()
						.uri("http://"+vo.getAddr()+":"+defaultPort).build();
				List<ContainerVo> subContainers = m_dockerService.getContainer(client,"pascloud_mycat");
				if(null != subContainers && subContainers.size()>0){
					containers.addAll(subContainers);
				}
			}
			if(null != containers.get(0)){
				ContainerVo vo = containers.get(0);
				dataSource.setUsername("root");
				//dataSource.setDataSourceName("alldb");
				//dataSource.setName("alldb");
				dataSource.setUrl("jdbc:mysql://"+vo.getIp()+":9066/alldb");
				dataSource.setPassword("root");
				dataSource.setDriverClassName(Constants.DB_MYSQL_DIRVERCLASS);
				ds = getDataSourceList(dataSource);
				
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			log.error(e.getMessage());
		}
		return ds;
	}

}