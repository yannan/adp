package com.eisoo.metadatamanage.util.constant;

import org.apache.commons.lang3.StringUtils;

import java.util.Date;
import java.text.SimpleDateFormat;

/**
 * @Author: WangZiYu
 * @description:com.eisoo.metadatamanage.util.constant
 * @Date: 2023/5/19 13:15
 */
public class ConvertUtil  extends com.eisoo.standardization.common.util.ConvertUtil {

    public static Date toDate(String value) {
        String dateFormat = "yyyy-MM-dd HH:mm:ss";
        String shortDateFormat = "yyyy-MM-dd";
        String timeStampFormat = "^\\d+$";

        if(StringUtils.isEmpty(value)) {
            return null;
        }
        value = value.trim();
        try {
            if (value.contains("-")) {
                SimpleDateFormat formatter;

                if (value.contains(":")) {
                    formatter = new SimpleDateFormat(dateFormat);

                } else {
                    formatter = new SimpleDateFormat(shortDateFormat);

                }

                return formatter.parse(value);

            } else if (value.matches(timeStampFormat)) {
                Long lDate = new Long(value);

                return new Date(lDate);

            }

        } catch (Exception e) {
            throw new RuntimeException(String.format("parser %s to Date fail", value));
        }
        throw new RuntimeException(String.format("parser %s to Date fail", value));
    }

}


