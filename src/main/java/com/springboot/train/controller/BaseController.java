package com.springboot.train.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.springboot.train.util.HttpRequestNg;

public class BaseController {
	private String name="login";
	public void setSession(HttpServletRequest request,String value){
		HttpSession session = request.getSession();
		session.setAttribute(name, value);
	}
	public void getSession(HttpServletRequest request){
		HttpSession session = request.getSession();
		session.getAttribute(name);
	}
	public void clearSession(HttpServletRequest request){
		HttpSession session = request.getSession();
		session.invalidate();
	}
	public static HttpRequestNg getHttpClient(HttpServletRequest request){
		HttpRequestNg httpclient=(HttpRequestNg) request.getAttribute("httpClient");
		return httpclient;
	}
}
