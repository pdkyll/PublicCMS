package com.publiccms.common.base;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.ibatis.jdbc.ScriptRunner;

import com.fasterxml.jackson.core.type.TypeReference;
import com.publiccms.common.constants.CommonConstants;
import com.publiccms.logic.component.site.SiteComponent;
import com.publiccms.views.pojo.entities.CmsModel;

/**
 *
 * AbstractCmsUpgrader
 *
 */
public abstract class AbstractCmsUpgrader {
    /**
     * 表名_ID_SEQ SEQUENCE主键策略
     */
    public static final String IDENTIFIER_GENERATOR_SEQUENCE = "com.publiccms.common.database.IDSequenceStyleGenerator";
    /**
     * ID自增主键策略
     */
    public static final String IDENTIFIER_GENERATOR_IDENTITY = "org.hibernate.id.IdentityGenerator";
    /**
     * 主键策略
     */
    public static final String IDENTIFIER_GENERATOR = IDENTIFIER_GENERATOR_IDENTITY;
    protected String version;
    protected Properties config;

    public AbstractCmsUpgrader(Properties config) {
        this.config = config;
    }

    /**
     * @param connection
     * @param fromVersion
     * @throws SQLException
     * @throws IOException
     */
    public abstract void update(Connection connection, String fromVersion) throws SQLException, IOException;

    /**
     * @return version list
     */
    public abstract List<String> getVersionList();

    /**
     * @return default port
     */
    public abstract int getDefaultPort();

    /**
     * @param dbconfig
     * @param host
     * @param port
     * @param database
     * @throws IOException
     * @throws URISyntaxException
     */
    public abstract void setDataBaseUrl(Properties dbconfig, String host, String port, String database)
            throws IOException, URISyntaxException;

    protected void updateModelAddFieldList(Connection connection) {
        try (Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery("select * from sys_site");) {
            while (rs.next()) {
                String filePath = CommonConstants.CMS_FILEPATH + CommonConstants.SEPARATOR + SiteComponent.TEMPLATE_PATH
                        + CommonConstants.SEPARATOR + SiteComponent.SITE_PATH_PREFIX + rs.getString("id")
                        + CommonConstants.SEPARATOR + SiteComponent.MODEL_FILE;
                File file = new File(filePath);
                try {
                    Map<String, CmsModel> modelMap = CommonConstants.objectMapper.readValue(file,
                            new TypeReference<Map<String, CmsModel>>() {
                            });
                    if (null != modelMap && !modelMap.isEmpty()) {
                        for (CmsModel model : modelMap.values()) {
                            List<String> fieldList = new ArrayList<>();
                            Map<String, String> fieldTextMap = new HashMap<>();
                            List<String> requiredFieldList = new ArrayList<>();
                            fieldTextMap.put("title", "标题");
                            if (model.isOnlyUrl()) {
                                fieldTextMap.put("url", "网址");
                            } else {
                                fieldList.add("copied");
                                fieldTextMap.put("copied", "转载");
                                fieldTextMap.put("source", "来源");
                                fieldTextMap.put("sourceUrl", "来源网址");
                                requiredFieldList.add("source");
                                requiredFieldList.add("sourceUrl");
                                fieldList.add("content");
                                fieldTextMap.put("content", "正文");
                                requiredFieldList.add("content");
                                fieldList.add("tag");
                                fieldTextMap.put("tag", "标签");
                                fieldList.add("author");
                                fieldTextMap.put("author", "作者");
                                fieldList.add("editor");
                                fieldTextMap.put("editor", "编辑");
                            }
                            fieldList.add("description");
                            fieldTextMap.put("description", "描述");
                            requiredFieldList.add("description");
                            fieldList.add("cover");
                            fieldTextMap.put("cover", "封面图");

                            model.setFieldList(fieldList);
                            model.setFieldTextMap(fieldTextMap);
                            model.setRequiredFieldList(requiredFieldList);
                        }
                    }
                    try {
                        CommonConstants.objectMapper.writeValue(file, modelMap);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } catch (IOException | ClassCastException e) {
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    protected void runScript(Connection connection, String fromVersion, String toVersion) throws SQLException, IOException {
        ScriptRunner runner = new ScriptRunner(connection);
        runner.setLogWriter(null);
        runner.setErrorLogWriter(null);
        runner.setAutoCommit(true);
        try (InputStream inputStream = getClass()
                .getResourceAsStream("/initialization/upgrade/" + fromVersion + "-" + toVersion + ".sql");) {
            if (null != inputStream) {
                runner.runScript(new InputStreamReader(inputStream, CommonConstants.DEFAULT_CHARSET));
            }
        }
        version = toVersion;
    }

    /**
     * @return the version
     */
    public String getVersion() {
        return version;
    }
}
