package dao;

import java.sql.*;
import util.Log;

public class NumberCountDAO {

    public NumberCountDAO() {
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
    number_count_table:

    count_id INT UNSIGNED AUTO_INCREMENT
    count_name VARCHAR NOT NULL
    count INT
    */

    public static void init() {
        String createTable = "CREATE TABLE IF NOT EXISTS number_count_table (count_id INT UNSIGNED AUTO_INCREMENT, count_name VARCHAR(100) NOT NULL, count INT, PRIMARY KEY (count_id))ENGINE=InnoDB DEFAULT CHARSET=utf8;";

        String queryClick = "SELECT * FROM number_count_table WHERE count_name='click_count';";
        String initClick = "INSERT INTO number_count_table VALUES(null, 'click_count', 0);";
        String queryArticle = "SELECT * FROM number_count_table WHERE count_name='article_count';";
        String initArticle = "INSERT INTO number_count_table VALUES(null, 'article_count', 0);";

        try (Connection conn = getConnection();
             Statement stat = conn.createStatement();) {

            stat.executeUpdate(createTable);

            /* Notice:
             *  You must use your ResultSet right after getting it before getting anther one!
             */
            ResultSet rsClick = stat.executeQuery(queryClick);
            if (!rsClick.next()) {
                stat.executeUpdate(initClick);
            }

            ResultSet rsArticle = stat.executeQuery(queryArticle);
            if (!rsArticle.next()) {
                stat.executeUpdate(initArticle);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return;
    }

    public static void clickCountIncrement() {

        try (Connection conn = getConnection(); Statement stat = conn.createStatement();) {
            String sql = "UPDATE number_count_table SET count=count+'1' WHERE count_name='click_count';";
            stat.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return;
    }

    public static int getClickCount() {

        int count = 0;

        try (Connection conn = getConnection(); Statement stat = conn.createStatement();) {
            String sql = "SELECT * FROM number_count_table WHERE count_name='click_count';";
            ResultSet rs = stat.executeQuery(sql);
            if (rs.next()) {
                count = rs.getInt("count");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return count;
    }

    public static void articleCountIncrement(int num) {

        if (num <= 0) {
            return;
        }

        String sql = "UPDATE number_count_table SET count=count+? WHERE count_name='article_count';";

        try (Connection conn = getConnection(); PreparedStatement stat = conn.prepareStatement(sql);) {
            stat.setInt(1, num);
            stat.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return;
    }

    public static void articleCountDecrement(int num) {
        if (num <= 0) {
            return;
        }

        String sql = "UPDATE number_count_table SET count=count-? WHERE count_name='article_count';";
        try (Connection conn = getConnection();
                    PreparedStatement stat = conn.prepareStatement(sql);) {
            stat.setInt(1, num);
            stat.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static int getArticleCount() {

        int count = 0;

        try (Connection conn = getConnection(); Statement stat = conn.createStatement();) {
            String sql = "SELECT * FROM number_count_table WHERE count_name='article_count';";
            ResultSet rs = stat.executeQuery(sql);
            if (rs.next()) {
                count = rs.getInt("count");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return count;
    }
};
