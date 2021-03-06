package com.pascloud.module.docker.service;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import com.pascloud.module.server.service.ServerService;
import com.pascloud.vo.docker.ContainerVo;
import com.pascloud.vo.docker.NodeVo;
import com.pascloud.vo.server.ServerVo;
import com.spotify.docker.client.DefaultDockerClient;

/**
 * 容器服务
 * @author chenly
 *
 */
@Service
public class ContainerService {
	
	private static Logger log = LoggerFactory.getLogger(ContainerService.class);
	
	@Autowired
	private DockerService m_dockerService;
	
	@Autowired
	private ServerService m_serverService;
	
	/**
	 * 查询所有运行节点运行的容器
	 * @param str
	 * @return
	 */
	public List<ContainerVo> getContainers(String str){
		List<ContainerVo> containers = new ArrayList<>();
		//containers = m_dockerService.getContainer(dockerClient);
		//List<NodeVo> nodes = new ArrayList<>();
		//nodes = m_dockerService.getNodes(getDockerClient());
		/*
		for(NodeVo vo: nodes){
			DefaultDockerClient client = DefaultDockerClient.builder()
					.uri("http://"+vo.getAddr()+":2375").build();
			List<ContainerVo> subContainers = m_dockerService.getContainer(client,str);
			if(null != subContainers && subContainers.size()>0){
				containers.addAll(subContainers);
			}
		}*/
		List<ServerVo> servers = new ArrayList<ServerVo>();
		servers = m_serverService.getAppServers();
		if(servers.size()>0){
			for(ServerVo vo:servers){
				DefaultDockerClient client = DefaultDockerClient.builder()
						.uri("http://"+vo.getIp()+":2375").build();
				List<ContainerVo> subContainers = m_dockerService.getContainer(client,str);
				if(null != subContainers && subContainers.size()>0){
					containers.addAll(subContainers);
				}
			}
		}
		
		return containers;
	}
	
	public DefaultDockerClient getDockerClient(){
		return DefaultDockerClient.builder().uri(m_serverService.getMasterDockerUrl()).build();
	}

}
