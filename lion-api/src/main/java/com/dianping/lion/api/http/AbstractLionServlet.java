/**
 * Project: com.dianping.lion.lion-api-0.0.1
 * 
 * File Created at 2012-8-1
 * $Id$
 * 
 * Copyright 2010 dianping.com.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Dianping Company. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with dianping.com.
 */
package com.dianping.lion.api.http;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.dianping.lion.entity.User;
import com.dianping.lion.exception.RuntimeBusinessException;
import com.dianping.lion.service.ConfigService;
import com.dianping.lion.service.EnvironmentService;
import com.dianping.lion.service.ProjectService;
import com.dianping.lion.service.UserService;
import com.dianping.lion.util.SecurityUtils;

/**
 * TODO Comment of AbstractLionServlet
 * @author danson.liu
 *
 */
public abstract class AbstractLionServlet extends HttpServlet {
	protected Logger logger = LoggerFactory.getLogger(getClass());
	
	private static final long serialVersionUID = -9154351276219364129L;
	
	public static final String 		PARAM_IDENTITY 	= "id";		//调用者身份id
	public static final String 		PARAM_PASSWD 	= "ps";		//调用者密码
	public static final String 		PARAM_PROJECT 	= "p";		//项目名
	public static final String 		PARAM_ENV 		= "e";		//环境名
	public static final String 		PARAM_KEY 		= "k";		//配置名，不包含项目名前缀
	public static final String 		PARAM_FEATURE 	= "f";		//项目feature名称
	public static final String 		PARAM_VALUE 	= "v";		//配置项值
	public static final String 		PARAM_TASKID 	= "t";		//项目任务id
	public static final String 		PARAM_EFFECT 	= "ef";		//配置是否立即生效
	public static final String 		PARAM_PUSH 		= "p";		//配置变更是否实时推送到app
	public static final String 		PARAM_CORRECT 	= "c";		//设置registerpoint是否正确
	
	public static final String 		SUCCESS_CODE 	= "0";		//正确返回码
	public static final String 		ERROR_CODE 		= "1|";		//错误返回码
	
	protected ApplicationContext 	applicationContext;
	protected ProjectService 		projectService;
	protected EnvironmentService 	environmentService;
	protected ConfigService 		configService;
	
	@Override
	public void init() throws ServletException {
		applicationContext = WebApplicationContextUtils.getRequiredWebApplicationContext(getServletContext());
		projectService = getBean(ProjectService.class);
		environmentService = getBean(EnvironmentService.class);
		configService = getBean(ConfigService.class);
	}

	@SuppressWarnings("unchecked")
	protected <T> T getBean(Class<T> clazz) {
		return (T) BeanFactoryUtils.beanOfType(applicationContext, clazz);
	}
	
	@SuppressWarnings("unchecked")
	protected <T> T getBean(String name) {
		return (T) applicationContext.getBean(name);
	}
	
	protected String getRequiredParameter(HttpServletRequest servletRequest, String param) {
		String paramVal = servletRequest.getParameter(param);
		if (paramVal == null) {
			throw new RuntimeBusinessException("Parameter[" + param + "] is required.");
		}
		return paramVal;
	}
	
	protected String[] getRequiredParameters(HttpServletRequest servletRequest, String param) {
		String[] paramVals = servletRequest.getParameterValues(param);
		if (paramVals == null || paramVals.length == 0) {
			throw new RuntimeBusinessException("Parameter[" + param + "] is required.");
		}
		return paramVals;
	}
	
	protected User getRequiredIdentity(HttpServletRequest servletRequest) {
		String identity = getRequiredParameter(servletRequest, PARAM_IDENTITY);
		int userId = 0;
		try {
			userId = Integer.parseInt(identity);
		} catch (NumberFormatException e) {
			throw new RuntimeBusinessException("Illegal format with identity parameter, must be integer.");
		}
		UserService userService = getBean(UserService.class);
		User user = userService.findById(userId);
		if (user == null) {
			throw new RuntimeBusinessException("User with identity[" + userId + "] not found.");
		}
		return user;
	}
	
	@SuppressWarnings("unused")
	protected void checkUserPassword(HttpServletRequest servletRequest) {
		String passwd = getRequiredParameter(servletRequest, PARAM_PASSWD);
		//TODO do check
	}
	
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setContentType("text/html;charset=utf-8");
		PrintWriter writer = resp.getWriter();
		try {
			User user = getRequiredIdentity(req);
			SecurityUtils.setCurrentUser(user);
			doService(req, resp);
		} catch (Exception e) {
			logger.warn("Error happend in [" + getClass().getSimpleName() + "], detail: ", e);
			writer.print(ERROR_CODE + e.getMessage());
		} finally {
			SecurityUtils.clearCurrentUser();
			writer.flush();
		}
	}

	/**
	 * @param req
	 * @param resp
	 * @param user
	 * @throws IOException 
	 * @throws ServletException 
	 */
	protected abstract void doService(HttpServletRequest req, HttpServletResponse resp) throws Exception;
	
}