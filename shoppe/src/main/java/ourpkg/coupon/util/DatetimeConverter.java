package ourpkg.coupon.util;

import java.text.SimpleDateFormat;
import java.util.Date;


public class DatetimeConverter {
	public static String toString(Date datetime, String format) {
		String result = "";
		try {
			if (datetime != null) {
				result = new SimpleDateFormat(format).format(datetime);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	public static Date parse(String datetime, String format) {
		Date result = new Date();
		try {
			 if (datetime != null && !datetime.isEmpty()) {  // 檢查 datetime 是否為 null 或空字串
		            result = new SimpleDateFormat(format).parse(datetime);  // 進行解析
		        } else {
		            // 若 datetime 為 null 或空字串，這裡可以選擇其他處理方式
		            System.out.println("datetime 字串為 null 或空，返回預設日期。");
		        }
		} catch (Exception e) {
			result = new Date();
			e.printStackTrace();
		}
		return result;
	}

}
