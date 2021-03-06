package net.jplugin.ext.webasic.impl.web;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.jplugin.core.kernel.api.PluginEnvirement;
import net.jplugin.ext.webasic.api.IController;
import net.jplugin.ext.webasic.api.IControllerSet;
import net.jplugin.ext.webasic.api.ObjectDefine;
import net.jplugin.ext.webasic.impl.rests.ServiceController;

/**
 *
 * @author: LiuHang
 * @version 创建时间：2015-2-10 下午02:02:05
 **/

public class WebControllerSet implements IControllerSet{
	private Map<String, IController> controllerMap;

	public void init() {
		Map<String, ObjectDefine> defs = PluginEnvirement.getInstance().getExtensionMap(net.jplugin.ext.webasic.Plugin.EP_WEBCONTROLLER,ObjectDefine.class);
		
		controllerMap = new ConcurrentHashMap<String, IController>();
		
		for (Entry<String, ObjectDefine> en:defs.entrySet()){
			controllerMap.put(en.getKey(), new WebController(en.getValue()));
		}
	}
	
	public Set<String> getAcceptPaths() {
		return controllerMap.keySet();
	}

	public void dohttp(String path,HttpServletRequest req, HttpServletResponse res,String innerPath)
			throws Throwable {
		controllerMap.get(path).dohttp(path,req, res,innerPath);
	}
	

}
