package com.pascloud.module.pasdev.web;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.commons.CommonsMultipartFile;
import org.springframework.web.servlet.ModelAndView;

import com.pascloud.constant.Constants;
import com.pascloud.module.common.web.BaseController;
import com.pascloud.module.config.PasCloudConfig;
import com.pascloud.module.pasdev.service.PasdevService;
import com.pascloud.module.passervice.service.ConfigService;
import com.pascloud.utils.FileUtils;
import com.pascloud.utils.HttpUtils;
import com.pascloud.utils.PasCloudCode;
import com.pascloud.vo.common.TreeVo;
import com.pascloud.vo.pasdev.PasfileVo;
import com.pascloud.vo.result.ResultCommon;
import com.pascloud.vo.result.ResultPageVo;

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
	private ConfigService m_configService;
	
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
	public ResultPageVo<PasfileVo> getPasFiles(HttpServletRequest request,
			@RequestParam(value="dirId",defaultValue="",required=true) String dirId,
			@RequestParam(value="key",defaultValue="",required=false) String key,
			@RequestParam(value="page",defaultValue="",required=true) Integer page,
			@RequestParam(value="rows",defaultValue="",required=true) Integer rows){
		ResultPageVo<PasfileVo> result = new ResultPageVo<>(PasCloudCode.SUCCESS);
		
		if(dirId.length() <=0 || page < 0 || rows < 0){
			result = new ResultPageVo<>(PasCloudCode.PARAMEXCEPTION);
		}
		if(key.length()>0){
			result = m_pasdevService.getPageData(dirId,key, page, rows);
		}else{
			result = m_pasdevService.getPageData(dirId, page, rows);
		}
		
		//result = m_pasdevService.getPasdevFiles(dirId);
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
			vo.setId(dirs.get(i));
			
			if(dirs.get(i).equals(Constants.PASCLOUD_PUBLIC_DB)){
				vo.setText("标准版");
			}else{
				String cn =m_configService.getCnByDnname(dirs.get(i));
				if(null!=cn){
					vo.setText(cn);
				}else{
					vo.setText(dirs.get(i));
				}
			}
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
		
		if(name.equals(Constants.PASCLOUD_PUBLIC_DB)){
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
			result = m_pasdevService.copyPasfileWidthID(name);
		}else{
			result = new ResultCommon(PasCloudCode.ERROR.getCode(),"已经存在");
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
		
		if(name.length()==0){
			return new ResultCommon(PasCloudCode.NULLDATA);
		} 
		
		if(name.equals(Constants.PASCLOUD_PUBLIC_DB)){
			return new ResultCommon(PasCloudCode.PARAMEXCEPTION);
		} 
		
		t = m_pasdevService.delPasfileByID(name);
		if(t>0){
			
		}else{
			result = new ResultCommon(PasCloudCode.ERROR);
		}
		return result;
	}
	
	/**
	 * 将租户的PAS+文件目录上传到应用服务器的DATA目录下
	 * @param request
	 * @param name
	 * @return
	 */
	@RequestMapping("/uploadPasfile.json")
	@ResponseBody
	public ResultCommon  uploadPasfile(HttpServletRequest request,
			@RequestParam(value="name",defaultValue="",required=true) String name){
		ResultCommon result = null;
		log.info("上传前进行压缩");
		
		if("".equals(name)){
			result = new ResultCommon(PasCloudCode.ERROR);
			return result;
		}
		if(!"".equals(name) && m_pasdevService.uploadPasfileWithIDToServer(name)){
			result = new ResultCommon(PasCloudCode.SUCCESS);
		}else{
			result = new ResultCommon(PasCloudCode.ERROR);
		}
		log.info("上传完成");
		return result;
	}
	
	
	@RequestMapping("/putPasfileToRedis.json")
	@ResponseBody
	public ResultCommon putPasfileToRedis(HttpServletRequest request,
			@RequestParam(value="name",defaultValue="",required=true) String name){
		ResultCommon result = null;
		/*
		String url = "http://192.168.0.7:8311/module/system/admin/v100/reloadConfig.json";
		
		
        try {
        	
        	List<NameValuePair> header = new ArrayList<NameValuePair>();
        	Map<String,NameValuePair> params = new HashMap<>();
        	params.put("db", new BasicNameValuePair("db",name));
            String r= HttpUtils.httpGetTool(url,params,header);
            
            if(null!=r){
            	result = new ObjectMapper().readValue(r, new TypeReference<ResultCommon>() {});
            }else{
            	result = new ResultCommon(PasCloudCode.ERROR);
            }
            
			
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			result = new ResultCommon(PasCloudCode.ERROR);
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			result = new ResultCommon(PasCloudCode.ERROR);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			result = new ResultCommon(PasCloudCode.ERROR);
		}
        */
        result = new ResultCommon(PasCloudCode.SUCCESS);
        
		return result;
	}
	
	/**
	 * 上传
	 * @param request
	 * @param file
	 * @param dirId
	 * @return
	 */
	@RequestMapping("/uploadfile.json")
	@ResponseBody
	public ResultCommon uploadfile(HttpServletRequest request,
			 @RequestParam(name = "file", required = false) CommonsMultipartFile file,
			 @RequestParam(name = "dirId",defaultValue="", required = true) String dirId){
		ResultCommon result = new ResultCommon(PasCloudCode.SUCCESS);
		
		if(null == dirId || dirId.length()<=0){
			result = new ResultCommon(PasCloudCode.PARAMEXCEPTION);
			return result;
		}
		
		log.info(dirId);
		
		result = m_pasdevService.uploadPasfile(file, dirId);
		return result;
		
	}
	/**
	 * 同步到数据库，同时并租户的文件同步到缓存上
	 * @param request
	 * @param dirId
	 * @return
	 */
	@RequestMapping("/sysPasfileToDB.json")
	@ResponseBody
	public ResultCommon sysPasfileToDB(HttpServletRequest request,
			@RequestParam(name = "dirId",defaultValue="", required = true) String dirId){
		ResultCommon result = null;
		log.info("同步pas+文件到公共数据库 开始");
		if(dirId.length() <=0){
			result = new ResultCommon(PasCloudCode.PARAMEXCEPTION);
			return result;
		}
		
		result = m_pasdevService.sysPasfileToDB(dirId);
		log.info("同步pas+文件到公共数据库 结束");
		return result;
	}
	
	@RequestMapping("/deletePasfileByFunId.json")
	@ResponseBody
	public ResultCommon deletePasfileByFunId(HttpServletRequest request,
			@RequestParam(name = "funId",defaultValue="", required = true) String funId,
			@RequestParam(name = "dirId",defaultValue="", required = true) String dirId){
		ResultCommon result = new ResultCommon(PasCloudCode.SUCCESS);
		
		if(funId.length()<=0 || dirId.length()<=0){
			result = new ResultCommon(PasCloudCode.PARAMEXCEPTION);
			return result;
		}
		
		log.info(dirId);
		
		result = m_pasdevService.delPasfileByFunId(funId, dirId);
		return result;
		
	}
	
	@RequestMapping("/publish.json")
	@ResponseBody
	public ResultCommon publish(HttpServletRequest request,
			@RequestParam(name = "funId",defaultValue="", required = true) String funId,
			@RequestParam(name = "dirId",defaultValue="", required = true) String dirId,
			@RequestParam(name = "newfunId",defaultValue="", required = true) String newfunId,
			@RequestParam(name = "title",defaultValue="", required = true) String title,
			@RequestParam(name = "desc",defaultValue="", required = true) String  desc){
        ResultCommon result = new ResultCommon(PasCloudCode.SUCCESS);
		
		if(funId.length()<=0 || dirId.length()<=0){
			result = new ResultCommon(PasCloudCode.PARAMEXCEPTION);
			return result;
		}


		if(m_pasdevService.checkFunIdIsExist(newfunId)){
			result = new ResultCommon(PasCloudCode.ERROR.getCode(),"该funId已经存在公共库");
			return result;
		}else{
			result = m_pasdevService.copyPasfileFromDN(funId, dirId, newfunId, title, desc);
		}
		
		
		
		return result;
	}
}
