package com.pascloud.module.pasdev.web;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.pascloud.bean.pasdev.PasfileVo;
import com.pascloud.constant.Constants;
import com.pascloud.module.common.web.BaseController;
import com.pascloud.module.config.PasCloudConfig;
import com.pascloud.module.pasdev.service.PasdevService;
import com.pascloud.utils.FileUtils;
import com.pascloud.utils.PasCloudCode;
import com.pascloud.vo.common.TreeVo;
import com.pascloud.vo.result.ResultCommon;

/**
 * pas+文件版本管理
 * @author chenly
 *
 */
@Controller
@RequestMapping("module/pasdev")
public class PasdevController extends BaseController {
	
	@Autowired
	private PasdevService m_pasdevService;
	
	@Autowired
	private PasCloudConfig m_config;
	
	@RequestMapping("/index.html")
	public ModelAndView index(HttpServletRequest request){
		ModelAndView view = new ModelAndView("pasdev/index");
		return view;
	}

	/**
	 * 根据目录查询PAS+文件
	 * @param request
	 * @param dirId
	 * @return
	 */
	@RequestMapping("/pasfiles.json")
	@ResponseBody
	public List<PasfileVo> getPasFilesWithDir(HttpServletRequest request,
			@RequestParam(value="dirId",defaultValue="",required=true) String dirId){
		List<PasfileVo> result = new ArrayList<>();
		result = m_pasdevService.getPasdevFiles(dirId);
		return result;
	}
	
	/*
	@RequestMapping("/modifyPasfiles.json")
	@ResponseBody
	public ResultCommon modifyPasFiles(HttpServletRequest request){
		ResultCommon result = new ResultCommon(PasCloudCode.SUCCESS);
		Integer total = m_pasdevService.modifyPasdevFilesWidthID("dn0");
		result.setDesc("总共修改了"+total+"Id");
		return result;
	}*/
	
	/**
	 * 获取本地所有PAS+的目录列表
	 * @param request
	 * @return
	 */
	@RequestMapping("/pasfiledirs.json")
	@ResponseBody
	public List<String> getPasFileDirForList(HttpServletRequest request){
		List<String> dirs = new ArrayList<>();
		dirs = m_pasdevService.getPasfileDir();
		return dirs;
	}
	
	/**
	 * 获取本地所有PAS+的目录树
	 * @param request
	 * @return
	 */
	@RequestMapping("/pasfiledirsfortree.json")
	@ResponseBody
	public List<TreeVo> getPasFileDirForTree(HttpServletRequest request){
		List<TreeVo> trees = new ArrayList<>();
		List<String> dirs = new ArrayList<>();
		dirs = m_pasdevService.getPasfileDir();
		
		for(int i=0;i<dirs.size();i++){
			TreeVo vo = new TreeVo();
			vo.setId(String.valueOf(i+1));
			vo.setText(dirs.get(i));
			vo.setLeaf(true);
			vo.setUrl("#");
			vo.setIconCls("icon-folder");
			trees.add(vo);
		}
		return trees;
	}
	
	/**
	 * 复制pasdev目录到其他目录 ，并修改XML文件的ID 
	 * @param request
	 * @param name
	 * @return
	 */
	@RequestMapping("/copyPasfileWithName.json")
	@ResponseBody
	public ResultCommon  copyPasfileWithName(HttpServletRequest request,
			@RequestParam(value="name",defaultValue="",required=true) String name){
		ResultCommon result = null;
		
		List<String> dirs = new ArrayList<>();
		dirs = m_pasdevService.getPasfileDir();
		Boolean flag = false;
		
		if(name.equals(Constants.PASCLOUD_DEV_DEFAULT)){
			result = new ResultCommon(PasCloudCode.ERROR);
			return result;
		}
		
		if(dirs.size()>0){
			for(String s:dirs){
				if(name.equals(s)){
					flag = true;
				}
			}
		}
		
		
		
		if(!flag){
			m_pasdevService.copyPasfileWidthID(name);
			result = new ResultCommon(PasCloudCode.SUCCESS);
		}else{
			result = new ResultCommon(PasCloudCode.ERROR);
		}
		return result;
	}
	
	/**
	 * 权限目录名称删除PAS+的目录 
	 * @param request
	 * @param name
	 * @return
	 */
	@RequestMapping("/delPasfileWithName.json")
	@ResponseBody
	public ResultCommon  delPasfileWithName(HttpServletRequest request,
			@RequestParam(value="name",defaultValue="",required=true) String name){
		ResultCommon result = new ResultCommon(PasCloudCode.SUCCESS);
		Integer t = 0;
		t = m_pasdevService.delPasfileWidthID(name);
		if(t>0){
			
		}else{
			result = new ResultCommon(PasCloudCode.ERROR);
		}
		return result;
	}
}