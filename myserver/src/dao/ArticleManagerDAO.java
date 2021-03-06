package dao;

import util.ArticleInfo;
import util.ClassInfo;
import util.Log;

import java.io.File;
import java.io.IOException;

import java.sql.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.DriverManager;

public class ArticleManagerDAO {

    public ArticleManagerDAO() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    protected static Connection getConnection() {
        try {
            return DriverManager.getConnection(InitDAO.DBURL, InitDAO.DBUSER, InitDAO.DBCODE);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /*
    article_table:

    article_id INT UNSIGNED AUTO_INCREMENT
    auther_id VARCHAR NOT NULL
    class_id INT UNSIGNED
    title VARCHAR
    summary VARCHAR
    tags VARCHAR
    body_md VARCHAR
    create_time DATE
    last_modify_time DATE
    permission INT
    */

    public static void init() {
        String sql = "CREATE TABLE IF NOT EXISTS article_table (article_id INT UNSIGNED AUTO_INCREMENT, auther_id INT UNSIGNED, class_id INT UNSIGNED, title VARCHAR(1024), summary VARCHAR(1024), tags VARCHAR(1024), body_md MEDIUMTEXT, create_time DATE, last_modify_time DATE, permission INT, PRIMARY KEY (article_id ))ENGINE=InnoDB DEFAULT CHARSET=utf8;";
        try (Connection conn = getConnection();
             PreparedStatement stat = conn.prepareStatement(sql);) {
             stat.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static ArticleInfo searchArticle(int articleId) {
        if (articleId <= 0) {
            return null;
        }

        ArticleInfo info = new ArticleInfo();
        String sql = "SELECT * FROM article_table WHERE article_id=?;";
        try (Connection conn = getConnection();
             PreparedStatement stat = conn.prepareStatement(sql);) {
            stat.setInt(1, articleId);

            ResultSet rs = stat.executeQuery();
            if (rs.next()) {
                info.mArticleId = rs.getInt("article_id");
                info.mAutherId = rs.getInt("auther_id");
                info.mClassId = rs.getInt("class_id");
                info.mTitle = rs.getString("title");
                info.mSummary = rs.getString("summary");
                info.mTags = rs.getString("tags");
                info.mBodyMD = rs.getString("body_md");
                info.mCreateTime = rs.getString("create_time");
                info.mLastModifyTime = rs.getString("last_modify_time");
                info.mPermission = rs.getInt("permission");
            } else {
                Log.Error("result set is null");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return info;
    }

    public static int updateArticle(int articleId, ArticleInfo info) {
        if (info == null || articleId <= 0) {
            return -1;
        }

        int ret = 0;
        String sql = "UPDATE article_table SET class_id=?, title=?, summary=?, tags=?, body_md=?, last_modify_time=?, permission=? WHERE article_id=?;";
        try (Connection conn = getConnection();
             PreparedStatement stat = conn.prepareStatement(sql);) {
            stat.setInt(1, info.mClassId);
            stat.setString(2, info.mTitle);
            stat.setString(3, info.mSummary);
            stat.setString(4, info.mTags);
            stat.setString(5, info.mBodyMD);
            stat.setString(6, info.mLastModifyTime);
            stat.setInt(7, info.mPermission);
            stat.setInt(8, articleId);

            ret = stat.executeUpdate();
            if (ret == 0) {
                Log.Error("nothing updated!");
                return -2;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return -3;
        }
        return 0;
    }

    public static ArticleInfo[] getLatestArticles(int start, int num, int classId, String order) {

        if (start < 0) {
            start = 0;
        }
        if (num <= 0) {
            num = 1;
        }
        if (order == null ||
            !order.equals("article_id") &&
            !order.equals("create_time") &&
            !order.equals("last_modify_time")) {
            order = "last_modify_time";
        }

        StringBuilder sql_builder = new StringBuilder(
            "SELECT article_id, auther_id, class_id, title, summary, tags, create_time, last_modify_time FROM article_table ");
        if (classId >= 0) {
            // specify 0 as classId to search default classes and minus to search all.
            sql_builder.append("WHERE class_id=? ");
        }
        sql_builder.append("ORDER BY ").append(order).append(" DESC LIMIT ?, ?;");

        try (Connection conn = getConnection();
                PreparedStatement stat = conn.prepareStatement(sql_builder.toString());) {

            int s = 1;
            if (classId >= 0) {
                stat.setInt(s++, classId);
            }
            stat.setInt(s++, start);
            stat.setInt(s++, num);

            // Log.Info(stat.toString());

            int count = 0;
            ResultSet rs = stat.executeQuery();
            rs.last();
            int line = rs.getRow();
            if (line == 0) {
                Log.Error("get no result");
                return new ArticleInfo[0];
            }
            rs.beforeFirst();
            ArticleInfo[] result = new ArticleInfo[line];

            while (rs.next() && count < num) {
                ArticleInfo info = new ArticleInfo();
                info.mArticleId = rs.getInt("article_id");
                info.mAutherId = rs.getInt("auther_id");
                info.mClassId = rs.getInt("class_id");
                info.mTitle = rs.getString("title");
                info.mSummary = rs.getString("summary");
                info.mTags = rs.getString("tags");
                info.mBodyMD = "";
                info.mCreateTime = rs.getString("create_time");
                info.mLastModifyTime = rs.getString("last_modify_time");
                info.mPermission = 0;
                result[count++] = info;
            }
            return result;
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return new ArticleInfo[0];
    }

    public static int createArticle(ArticleInfo info) {

        if (!validArticle(info)) {
            return -1;
        }

        int insNums = 0;
        int articleId = 0;
        String sql = "INSERT INTO article_table VALUES(null, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
        String sqlId = "SELECT LAST_INSERT_ID();";

        try (Connection conn = getConnection();
             PreparedStatement stat = conn.prepareStatement(sql);
             Statement statement = conn.createStatement();) {
            stat.setInt(1, info.mAutherId);
            stat.setInt(2, info.mClassId);
            stat.setString(3, info.mTitle);
            stat.setString(4, info.mSummary);
            stat.setString(5, info.mTags);
            stat.setString(6, info.mBodyMD);
            stat.setString(7, info.mCreateTime);
            stat.setString(8, info.mLastModifyTime);
            stat.setInt(9, info.mPermission);

            insNums = stat.executeUpdate();

            ResultSet rs = statement.executeQuery(sqlId);
            if (rs.next()) {
                articleId = rs.getInt(1);
                if (articleId <= 0) {
                    return -2;
                }
            }

            if (insNums != 0) {
                NumberCountDAO.articleCountIncrement(insNums);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return articleId;
    }

    public static void deleteArticle(int articleId) {
        if (articleId < 0) {
            return;
        }

        int delNums = 0;
        String sql = "DELETE FROM article_table WHERE article_id=?;";

        try (Connection conn = getConnection();
             PreparedStatement stat = conn.prepareStatement(sql);) {
            stat.setInt(1, articleId);
            delNums = stat.executeUpdate();
            if (delNums != 0) {
                NumberCountDAO.articleCountDecrement(delNums);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static boolean legalAuthor(int articleId, int autherId) {
        if (autherId == 1) {
            return true;
        }
        if (autherId < 0 || autherId < 0) {
            return false;
        }

        String sql = "SELECT auther_id FROM article_table WHERE article_id=?;";
        try (Connection conn = getConnection();
             PreparedStatement stat = conn.prepareStatement(sql);) {
            stat.setInt(1, articleId);
            ResultSet rs = stat.executeQuery();
            if (rs.next() && rs.getInt("auther_id") == autherId) {
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    protected static boolean validArticle(ArticleInfo info) {
        if (0 > info.mArticleId || 0 > info.mAutherId) {
            Log.Error("article id is " + info.mArticleId
                    + "auther id is " + info.mAutherId);
            return false;
        }

        if (null == info.mTitle ||
                info.mTitle.length() == 0 ||
                info.mTitle.length() > 1024) {
            Log.Error("title is " + info.mTitle);
            info.mTitle = info.mTitle.substring(1024);
        }

        if (null != info.mSummary) {
            if (info.mSummary.length() > 2048) {
                Log.Error("summary length is " + info.mSummary.length());
                info.mSummary = info.mSummary.substring(2048);
            }
        }

        return true;
    }
};
