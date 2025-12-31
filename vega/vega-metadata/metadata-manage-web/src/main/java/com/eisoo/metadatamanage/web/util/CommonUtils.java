package com.eisoo.metadatamanage.web.util;

import com.eisoo.metadatamanage.lib.enums.ResUploadType;
import com.eisoo.metadatamanage.util.constant.Constants;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.security.UserGroupInformation;

import java.io.IOException;

/**
 * @Author: WangZiYu
 * @description:com.eisoo.metadatamanage.web.util
 * @Date: 2023/4/1 16:39
 */
public class CommonUtils {
    private CommonUtils() {
        throw new UnsupportedOperationException("Construct CommonUtils");
    }

    /**
     * if upload resource is HDFS and kerberos startup is true , else false
     *
     * @return true if upload resource is HDFS and kerberos startup
     */
    public static boolean getKerberosStartupState() {
        String resUploadStartupType = PropertyUtils.getUpperCaseString(Constants.RESOURCE_STORAGE_TYPE);
        ResUploadType resUploadType = ResUploadType.valueOf(resUploadStartupType);
        Boolean kerberosStartupState = PropertyUtils.getBoolean(Constants.HADOOP_SECURITY_AUTHENTICATION_STARTUP_STATE, false);
        return resUploadType == ResUploadType.HDFS && kerberosStartupState;
    }

    /**
     * load kerberos configuration
     *
     * @param configuration
     * @return load kerberos config return true
     * @throws IOException errors
     */
    public static boolean loadKerberosConf(Configuration configuration) throws IOException {
        return loadKerberosConf(PropertyUtils.getString(Constants.JAVA_SECURITY_KRB5_CONF_PATH),
                PropertyUtils.getString(Constants.LOGIN_USER_KEY_TAB_USERNAME),
                PropertyUtils.getString(Constants.LOGIN_USER_KEY_TAB_PATH), configuration);
    }

    /**
     * load kerberos configuration
     *
     * @param javaSecurityKrb5Conf javaSecurityKrb5Conf
     * @param loginUserKeytabUsername loginUserKeytabUsername
     * @param loginUserKeytabPath loginUserKeytabPath
     * @throws IOException errors
     */
    public static void loadKerberosConf(String javaSecurityKrb5Conf, String loginUserKeytabUsername,
                                        String loginUserKeytabPath) throws IOException {
        Configuration configuration = new Configuration();
        configuration.setClassLoader(configuration.getClass().getClassLoader());
        loadKerberosConf(javaSecurityKrb5Conf, loginUserKeytabUsername, loginUserKeytabPath, configuration);
    }

    /**
     * load kerberos configuration
     *
     * @param javaSecurityKrb5Conf javaSecurityKrb5Conf
     * @param loginUserKeytabUsername loginUserKeytabUsername
     * @param loginUserKeytabPath loginUserKeytabPath
     * @param configuration configuration
     * @return load kerberos config return true
     * @throws IOException errors
     */
    public static boolean loadKerberosConf(String javaSecurityKrb5Conf, String loginUserKeytabUsername,
                                           String loginUserKeytabPath, Configuration configuration) throws IOException {
        if (CommonUtils.getKerberosStartupState()) {
            System.setProperty(Constants.JAVA_SECURITY_KRB5_CONF, StringUtils.defaultIfBlank(javaSecurityKrb5Conf,
                    PropertyUtils.getString(Constants.JAVA_SECURITY_KRB5_CONF_PATH)));
            configuration.set(Constants.HADOOP_SECURITY_AUTHENTICATION, Constants.KERBEROS);
            UserGroupInformation.setConfiguration(configuration);
            UserGroupInformation.loginUserFromKeytab(
                    StringUtils.defaultIfBlank(loginUserKeytabUsername,
                            PropertyUtils.getString(Constants.LOGIN_USER_KEY_TAB_USERNAME)),
                    StringUtils.defaultIfBlank(loginUserKeytabPath, PropertyUtils.getString(Constants.LOGIN_USER_KEY_TAB_PATH)));
            return true;
        }
        return false;
    }

    public static String getDataQualityJarName() {
        String dqsJarName = PropertyUtils.getString(Constants.DATA_QUALITY_JAR_NAME);

        if (StringUtils.isEmpty(dqsJarName)) {
            return "dolphinscheduler-data-quality.jar";
        }

        return dqsJarName;
    }

    /**
     * hdfs udf dir
     *
     * @param tenantCode tenant code
     * @return get udf dir on hdfs
     */
    public static String getHdfsUdfDir(String tenantCode) {
        return String.format("%s/udfs", getHdfsTenantDir(tenantCode));
    }

    /**
     * @param tenantCode tenant code
     * @return file directory of tenants on hdfs
     */
    public static String getHdfsTenantDir(String tenantCode) {
        return String.format("%s/%s", getHdfsDataBasePath(), tenantCode);
    }

    /**
     * get data hdfs path
     *
     * @return data hdfs path
     */
    public static String getHdfsDataBasePath() {
        String resourceUploadPath = PropertyUtils.getString(Constants.RESOURCE_UPLOAD_PATH, "/dolphinscheduler");
        if ("/".equals(resourceUploadPath)) {
            // if basepath is configured to /, the generated url may be //default/resources (with extra leading /)
            return "";
        } else {
            return resourceUploadPath;
        }
    }
}
