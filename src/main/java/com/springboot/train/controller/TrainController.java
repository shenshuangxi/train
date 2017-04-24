package com.springboot.train.controller;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.springboot.train.model.Passengers;
import com.springboot.train.service.TrainService;
import com.springboot.train.util.Cookie;
import com.springboot.train.util.CookieUtil;
import com.springboot.train.util.DateUtils;
import com.springboot.train.util.HttpsRequestNg;

/**
 * 
 * @author lijintao
 *
 */
@Controller
@RequestMapping("/index")
public class TrainController extends BaseController {
	private final static Logger logger = Logger.getLogger(TrainController.class);

	// 首页
	@RequestMapping
	public String index(Model model) {
		model.addAttribute("startDate", DateUtils.format(new Date()));
		model.addAttribute("minDate", DateUtils.format(new Date()));
		model.addAttribute("maxDate", DateUtils.format(DateUtils.addDateTime(new Date(), 59)));
		return "train/index";
	}

	// 查询列车经停站
	@ResponseBody
	@RequestMapping("/queryByTrainNo")
	public JSONObject queryByTrainNo(HttpServletRequest request, String trainNo, String fromStation,
			String toStation, String startDate) throws IOException {
		String urlStr = "https://kyfw.12306.cn/otn/czxx/queryByTrainNo?train_no=" + trainNo + "&from_station_telecode="
				+ fromStation + "&to_station_telecode=" + toStation + "&depart_date=" + startDate;
		logger.info("urlStr----" + urlStr);
		String result = new String(HttpsRequestNg.getHttpClient().doGet(urlStr));
		logger.info("result----" + result);
		return JSON.parseObject(result);
	}

	// 登录
	@ResponseBody
	@RequestMapping("/loginAysnSuggest")
	public JSONObject loginAysnSuggest(HttpServletRequest request,HttpServletResponse response, String user_name, String password, String randCode)
			throws IOException {
		String urlStr = "https://kyfw.12306.cn/otn/login/loginAysnSuggest?loginUserDTO.user_name=" + user_name
				+ "&userDTO.password=" + password + "&randCode=" + randCode + "&rand=sjrand";
		logger.info("urlStr----" + urlStr);
		String result = new String(HttpsRequestNg.getHttpClient().doPost(urlStr));
		setSession(request, result);
		logger.info("result----" + result);
		Map<String, Cookie> cookies = HttpsRequestNg.getHttpClient().cookies;
		Set<Map.Entry<String, Cookie>> entrys = cookies.entrySet();
		for (Map.Entry<String, Cookie> entry : entrys) {
			Cookie cookie = entry.getValue();
			CookieUtil.setCookie(response, cookie.getName(), cookie.getValue());
		}
		String string = new String(HttpsRequestNg.getHttpClient().doPost("https://kyfw.12306.cn/otn/login/userLogin"));
		String reg = "<span style=\"width:50px;\">[\u4e00-\u9fa5]{0,100}</span>";
		Matcher m = Pattern.compile(reg).matcher(string);
		String username="";
		while (m.find()) {
			String r = m.group().trim();
			username=r.trim()!=""?r.split("\">")[1]:"";
		}
		HttpSession session = request.getSession();
		username = username.substring(0,username.length()-7);
		session.setAttribute("username", username);
		JSONObject fromObject = JSON.parseObject(result);
		fromObject.put("username", username);
		return fromObject;
	}
	//退出登录
	@ResponseBody
	@RequestMapping("/loginOut")
	public String loginOut(HttpServletRequest request) throws IOException {
		clearSession(request);
		String result = new String(HttpsRequestNg.getHttpClient().doPost("https://kyfw.12306.cn/otn/login/loginOut"));
		logger.info("loginOut----" + result);
		return "1";
	}
	@RequestMapping("/login")
	public String login(HttpServletRequest request) throws IOException {
		clearSession(request);
		return "train/login";
	}
	@RequestMapping("/img")
	public String img(Model model,@RequestParam(defaultValue="login")String moduled) throws IOException {
		model.addAttribute("moduled", moduled);
		return "train/img";
	}
	//预提交订单
	@ResponseBody
	@RequestMapping("/submitOrderRequest")
	public JSONObject submitOrderRequest(HttpServletRequest request, String secretStr, String train_date,
			String query_from_station_name, String query_to_station_name) throws IOException {
		JSONObject submitOrderRequest = TrainService.submitOrderRequest(secretStr, train_date, query_from_station_name, query_to_station_name);
		return submitOrderRequest;
	}
	//检查订单有效
	@ResponseBody
	@RequestMapping("/checkOrderInfo")
	public String checkOrderInfo(HttpServletRequest request, String randCode, String oldPassengerStr,
			String passengerTicketStr, String repeat_submit_token, String train_date, String train_no,
			String stationTrainCode, String seatType, String fromStationTelecode, String toStationTelecode,
			String leftTicket, String key_check_isChange, String train_location) throws IOException {
		String urlStr = "https://kyfw.12306.cn/otn/confirmPassenger/checkOrderInfo?cancel_flag=2&bed_level_order_num=000000000000000000000000000000&tour_flag=dc&_json_att=&REPEAT_SUBMIT_TOKEN="
				+ repeat_submit_token + "&randCode=" + randCode + "&passengerTicketStr=" + passengerTicketStr
				+ "&oldPassengerStr=" + oldPassengerStr;
		logger.info("urlStr----" + urlStr);
		String result = new String(HttpsRequestNg.getHttpClient().doPost(urlStr));
		logger.info("result----" + result);
		JSONObject json = JSON.parseObject(result);
		if (!json.getBoolean("status")) {
			JSONObject jsons=new JSONObject();
			jsons.put("submitStatus", json.getBoolean("status"));
			jsons.put("errMsg", json.getString("messages"));
			json.put("data", jsons);
			return json.toString();
		}
		result=TrainService.getQueueCount(train_date, train_no, stationTrainCode, seatType, fromStationTelecode, toStationTelecode,
				leftTicket, repeat_submit_token, passengerTicketStr, oldPassengerStr, randCode, key_check_isChange,
				train_location);
		return result==null?"下单出错":result;
	}
	@ResponseBody
	@RequestMapping("/queryOrderWaitTime")
	public String queryOrderWaitTime(HttpServletRequest request, String repeat_submit_token) throws IOException {
		String urlStr = "https://kyfw.12306.cn/otn/confirmPassenger/queryOrderWaitTime?random="+System.currentTimeMillis()
		+"&tourFlag=dc&_json_att=&REPEAT_SUBMIT_TOKEN="+repeat_submit_token;
		logger.info("urlStr----" + urlStr);
		String result = new String(HttpsRequestNg.getHttpClient().doPost(urlStr));
		logger.info("result----" + result);
		return JSON.parseObject(result).toString();
	}
	//取消订单
	@ResponseBody
	@RequestMapping("/cancelNoCompleteMyOrder")
	public String cancelNoCompleteMyOrder(HttpServletRequest request, String repeat_submit_token) throws IOException {
		String urlStr = "https://kyfw.12306.cn/otn/queryOrder/cancelNoCompleteMyOrder?sequence_no=E076926220&cancel_flag=cancel_order&_json_att=";
		logger.info("urlStr----" + urlStr);
		String result = new String(HttpsRequestNg.getHttpClient().doPost(urlStr));
		logger.info("result----" + result);
		return JSON.parseObject(result).toString();
	}
	//查询未完成订单
	@ResponseBody
	@RequestMapping("/queryMyOrderNoComplete")
	public JSONArray queryMyOrderNoComplete(HttpServletRequest request) throws IOException {
		JSONArray queryMyOrderNoComplete = TrainService.queryMyOrderNoComplete();
		return queryMyOrderNoComplete;
	}

	// 获取乘客信息
	@RequestMapping("/passengers")
	public String passengers(HttpServletRequest request, Model model) throws IOException {
		List<Passengers> passengers = TrainService.passengers();
		model.addAttribute("list", passengers);
		return "train/passengers";
	}
	//检查用户登录是否有效
	@ResponseBody
	@RequestMapping("/checkUser")
	public String checkUser(HttpServletRequest request, Model model) throws IOException {
		String urlStr = "https://kyfw.12306.cn/otn/login/checkUser";
		String result = new String(HttpsRequestNg.getHttpClient().doPost(urlStr));
		return result;
	}

	// 校验验证码
	@ResponseBody
	@RequestMapping("/checkRandCodeAnsyn")
	public JSONObject checkRandCodeAnsyn(HttpServletRequest request, String randCode,
			@RequestParam(value = "repeat_submit_token", required = false) String repeat_submit_token)
					throws IOException {
		JSONObject checkRandCodeAnsyn = TrainService.checkRandCodeAnsyn(randCode, repeat_submit_token);
		return checkRandCodeAnsyn;
	}

	// 获取验证码
	@ResponseBody
	@RequestMapping("/getPassCodeNew")
	public void getPassCodeNew(HttpServletRequest request, HttpServletResponse response,
			@RequestParam(defaultValue = "login") String module) throws IOException {
		byte[] doGet = TrainService.getPassCodeNew(module);
		response.setHeader("Pragma", "no-cache");
		response.setHeader("Cache-Control", "no-cache");
		response.setDateHeader("Expires", 0);
		response.setContentType("image/jpeg");
		InputStream buffin = new ByteArrayInputStream(doGet);
		BufferedImage img = ImageIO.read(buffin);
		// 将图像输出到Servlet输出流中。
		ServletOutputStream sos = response.getOutputStream();
		ImageIO.write(img, "jpeg", sos);
		sos.close();
	}

	// 查询余票
	@RequestMapping("/query")
	public String train(HttpServletRequest request, Model model,
			String fromStation, String toStation, String startDate) {
		try {
			JSONArray queryTrain = TrainService.queryTrain(fromStation, toStation, startDate);
			model.addAttribute("list", queryTrain);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "train/train";
	}

	
}
