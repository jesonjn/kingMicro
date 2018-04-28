package com.king.aspect;

import java.lang.reflect.Method;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.stereotype.Component;
import com.king.common.annotation.Log;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import com.king.api.smp.SysLogService;
import com.king.common.utils.network.HttpContextUtils;
import com.king.common.utils.network.IPUtils;
import com.king.common.utils.security.ShiroUtils;
import com.king.dal.gen.model.smp.SysLog;
import com.king.dal.gen.model.smp.SysUser;


/**
 * 系统日志，切面处理类
 * @author King chen
 * @email 396885563@qq.com
 * @date 2017年12月29日
 */
@Aspect
//@EnableAspectJAutoProxy
@Component
public class SysLogAspect {
	@Autowired
	private SysLogService sysLogService;
	
	@Pointcut("@annotation(com.king.common.annotation.Log)")
	public void logPointCut() { 
		
	}

	@Around("logPointCut()")
	public Object around(ProceedingJoinPoint point) throws Throwable {
		long beginTime = System.currentTimeMillis();
		//执行方法
		String username=null;
		if (ShiroUtils.getSubject().getPrincipal() != null) {
			username= ((SysUser) ShiroUtils.getSubject().getPrincipal()).getUsername();
		}		
		Object result = point.proceed();
		//执行时长(毫秒)
		long time = System.currentTimeMillis() - beginTime;
		//输出
		JSONObject jsonObject = (JSONObject) JSONObject.toJSON(result);
		String data =jsonObject.getString("data");
		//保存日志
		saveSysLog(point, time,data,username);

		return result;
	}

	private void saveSysLog(ProceedingJoinPoint joinPoint, long time,String data,String username) {
		MethodSignature signature = (MethodSignature) joinPoint.getSignature();
		Method method = signature.getMethod();
	//	if (ShiroUtils.getSubject().getPrincipal() != null) {
			SysLog sysLog = new SysLog();
			com.king.common.annotation.Log log = method.getAnnotation(com.king.common.annotation.Log.class);
			if (log != null) {
				// 注解上的描述
				sysLog.setOperation(log.value());
			}

			// 请求的方法名
			String className = joinPoint.getTarget().getClass().getName();
			String methodName = signature.getName();
			sysLog.setMethod(className + "." + methodName + "()");
			sysLog.setResult(data);
			// 请求的参数
			Object[] args = joinPoint.getArgs();
			String params=null;
			try {
				params= new Gson().toJson(args[0]);
				sysLog.setParams(params);
			} catch (Exception e) {

			}
			// 获取request
			HttpServletRequest request = HttpContextUtils.getHttpServletRequest();
			// 设置IP地址
			sysLog.setIp(IPUtils.getIpAddr(request));
			// 用户名
			if ( username==null && params!=null) {		
				sysLog.setUsername(params.replaceAll("\"", ""));
			}else{
				sysLog.setUsername(username);
			}
			sysLog.setTime(time);
			sysLog.setCreateDate(new Date());
			// 保存系统日志
			sysLogService.save(sysLog);
//		}
	}
}
