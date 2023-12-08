package cc.vastsea.toyou.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;

/**
 * 网络工具类
 */
@Slf4j
public class NetUtil {

	/**
	 * 判断是否为本地请求
	 */
	public static boolean isLocalAddress(HttpServletRequest request){
		String ipAddress = getIpAddress(request);
		return ipAddress.equalsIgnoreCase("127.0.0.1") ||
				ipAddress.equalsIgnoreCase("::1") ||
				ipAddress.equalsIgnoreCase("0:0:0:0:0:0:0:1");
	}

	/**
	 * 获取客户端 IP 地址
	 */
	public static String getIpAddress(HttpServletRequest request) {
		String ip = request.getHeader("x-forwarded-for");
		if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("Proxy-Client-IP");
		}
		if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("WL-Proxy-Client-IP");
		}
		if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getRemoteAddr();
			if (ip.equals("127.0.0.1")) {
				// 根据网卡取本机配置的 IP
				InetAddress inet = null;
				try {
					inet = InetAddress.getLocalHost();
				} catch (Exception e) {
					e.printStackTrace();
				}
				if (inet != null) {
					ip = inet.getHostAddress();
				}
			}
		}
		// 多个代理的情况，第一个IP为客户端真实IP,多个IP按照','分割
		if (ip != null && ip.length() > 15) {
			if (ip.indexOf(",") > 0) {
				ip = ip.substring(0, ip.indexOf(","));
			}
		}
		if (ip == null) {
			return "127.0.0.1";
		}
		return ip;
	}

	/**
	 * 判断是否为移动端设备
	 *
	 * @param request 请求
	 * @return 是否为移动端设备, true为移动端设备, false为PC端设备
	 */
	public static boolean isMobileDevice(HttpServletRequest request) {
		String requestHeader = request.getHeader("user-agent");
		String[] deviceArray = {"android", "windows phone"};
		if (requestHeader == null) {
			return false;
		}
		requestHeader = requestHeader.toLowerCase();
		for (String device : deviceArray) {
			if (requestHeader.contains(device)) {
				return true;
			}
		}
		return false;
	}
}
