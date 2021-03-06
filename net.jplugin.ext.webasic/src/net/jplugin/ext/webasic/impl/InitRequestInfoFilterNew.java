package net.jplugin.ext.webasic.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.jplugin.core.kernel.api.ctx.Cookies;
import net.jplugin.core.kernel.api.ctx.RequesterInfo;
import net.jplugin.core.kernel.api.ctx.RequesterInfo.Content;
import net.jplugin.core.kernel.api.ctx.ThreadLocalContextManager;
import net.jplugin.ext.webasic.api.WebFilter;

/**
 * This filter must not conflict with InitRequestInfoFilter。This is two seperate
 * method。
 * 
 * @author Administrator
 *
 */
public class InitRequestInfoFilterNew implements WebFilter {

	private static final String _AID = "_aid";
	private static final String _OID = "_oid";
	private static final String _OTK = "_otk";
	private static final String _ATK = "_atk";
	private static final String APPLICATION_JSON = "application/json";

	public boolean doFilter(HttpServletRequest req, HttpServletResponse res) {
//		try {
//			req.setCharacterEncoding("utf-8");
//		} catch (UnsupportedEncodingException e) {
//			throw new RuntimeException(e);
//		}
		
		RequesterInfo requestInfo = ThreadLocalContextManager.getRequestInfo();
		Content content = requestInfo.getContent();
		
		//get content
		parseContent(content, req);
		
		//fill request url
		requestInfo.setRequestUrl(req.getRequestURL().toString());
		
		//fill client ip
		requestInfo.setCallerIpAddress(getClientIp(req));
		
		//fill other attribute
		fillFromContent(requestInfo);
		
		//fill cookie
		Cookie[] arr = req.getCookies();
		if (arr!=null){
			Cookies cookies = requestInfo.getCookies();
			for (Cookie c:arr){
				cookies.setCookie(c.getName(), c.getValue());
			}
		}
		return true;
	}

	public static void fillFromContent(RequesterInfo requestInfo) {
		Content content = requestInfo.getContent();
		
		Map map;
		//2016-12-08 因为把jsonContent解析放入paramContent，不需要区分了
//		if (APPLICATION_JSON.equals(content.getContentType())) {
//			map = content.getMapForJsonContent();
//		} else {
//			map = content.getParamContent();
//		}
		map = content.getParamContent();

		String clientAppToken = (String) map.get(_ATK);
		String operatorToken = (String) map.get(_OTK);
		//如此判断，避免和InitRequestInfoFilter冲突了
		if (clientAppToken!=null  || operatorToken!=null){
			requestInfo.setClientAppToken(clientAppToken);
			requestInfo.setOperatorToken(operatorToken);
			requestInfo.setOperatorId((String) map.get(_OID));
			requestInfo.setClientAppCode((String) map.get(_AID));
		}
	}

	private String getClientIp(HttpServletRequest request) {
		String ip = request.getHeader("x-forwarded-for");
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("Proxy-Client-IP");
		}
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("WL-Proxy-Client-IP");
		}
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getRemoteAddr();
		}
		if(ip!=null && ip.length()>15){ //"***.***.***.***".length() = 15
			if(ip.indexOf(",")>0){
				ip = ip.substring(0,ip.indexOf(","));
			}
		}
		return "0:0:0:0:0:0:0:1".equals(ip) ? "127.0.0.1" : ip;
	}

	private void parseContent(Content content, HttpServletRequest req) {
		content.setContentType(req.getContentType());
		if (APPLICATION_JSON.equals(content.getContentType())) {
			InputStream is = null;
			String json;
			try {
				req.getInputStream();
				ByteArrayOutputStream os = new ByteArrayOutputStream();
				byte[] buffer = new byte[512];
				int len;
				is = req.getInputStream();
				if (is == null) {
					json = "";
				} else {
					while ((len = is.read(buffer)) > 0) {
						os.write(buffer, 0, len);
					}
					json = os.toString("utf-8");
				}
			} catch (IOException e) {
				throw new RuntimeException("get json error:" + e.getMessage(), e);
			} finally {
				if (is != null)
					try {
						is.close();
					} catch (Exception e) {
					}
			}
			content.setJsonContent(json);
		} else {
			Enumeration nms = req.getParameterNames();
			HashMap<String, String> param = new HashMap<String, String>();
			while (nms.hasMoreElements()) {
				String name = (String) nms.nextElement();
				param.put(name, req.getParameter(name));
			}
			content.setParamContent(param);
		}
	}

	public void doAfter(HttpServletRequest req, HttpServletResponse res, Throwable th) {
	}

}
