package servlet;

import dao.NumberCountDAO;
import dao.ArticleManagerDAO;
import dao.UserManagerDAO;
import dao.ClassManagerDAO;

import util.ArticleInfo;
import util.ClassInfo;
import util.Log;

import java.io.File;
import java.io.IOException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;

public class ArticleViewerServlet extends HttpServlet {

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        Log.Verbose("article viewer servlet");

        String action = request.getParameter("action");
        if (action == null) {
            Log.Error("action is null");
            return;
        }

        HttpSession session = request.getSession(true);
        String logStatus = (String)session.getAttribute("status");
        int userId = -1;
        int userGroup = -1;
        if (logStatus != null && logStatus.equals("login")) {
            userId = (int)session.getAttribute("userId");
            userGroup = (int)session.getAttribute("userGroup");
        }

        String index = request.getParameter("index");
        String num = request.getParameter("num");
        String order = request.getParameter("order");
        String articleId = request.getParameter("articleId");
        String classId = request.getParameter("classId");
        Log.Verbose("action is " + action);

        StringBuilder json = new StringBuilder("{");
        response.setContentType("text/plain");

        switch (action) {
            /* ?action=preview&index=1&num=5&order=last_modify_time
               {
                    "article_id": 0,
                    "auther_id": 0,
                    "auther_name": "",
                    "title": "",
                    "summary": "",
                    "tags": "",
                    "bodyMD": "",
                    "create_time": "",
                    "last_modify_time": "",
                    "permission": 0
                };
            */
            case "preview":
                ArticleInfo[] list = ArticleManagerDAO.getLatestArticles(
                        Integer.parseInt(index), Integer.parseInt(num),
                            Integer.parseInt(classId), order);
                if (list == null || list.length == 0) {
                    Log.Error("get article failure");
                    break;
                }
                json.append("\"list\":[");
                for (int i = 0; i < list.length; i++) {
                    String className =
                                new StringBuilder(",\"class_name\":\"")
                                .append(ClassManagerDAO.searchClass(list[i].mClassId).mClassName)
                                .append("\"").toString();
                    json.append(ClassInfo.jsonAppend(list[i].toJson(), className));
                    if (i != list.length - 1) {
                        json.append(",");
                    }
                }
                json.append("]}");
                response.getWriter().write(
                        json.toString()
                        .replace("\r", "\\r")
                        .replace("\n", "\\n"));
            break;
            // ?action=view&articleId=1
            case "view":
                ArticleInfo info = ArticleManagerDAO.searchArticle(Integer.parseInt(articleId));
                String className =
                        new StringBuilder(",\"class_name\":\"")
                                .append(ClassManagerDAO.searchClass(info.mClassId).mClassName)
                                .append("\"").toString();
                response.getWriter().write(ClassInfo.jsonAppend(info.toJson(), className));
            break;
            /* ?action=modify&autherId=1&articleId
             * {
             *     "autherName": "wwr",
             *     "isOwner": true
             * }
             */
            case "modify":
                String autherName = "";
                String autherId = request.getParameter("autherId");
                if (autherId != null && autherId.length() > 0) {
                    autherName = UserManagerDAO.getUserInfo(Integer.parseInt(autherId)).mUserName;
                }
                json.append("\"autherName\":\"")
                    .append(autherName)
                    .append("\",\"isOwner\":");
                /*
                * here we do not strictly judge whether the user is the owner of the article,
                * but only trust the autherId pass from the front end because this API only control the visibility,
                * It front-end cheated, he can only get a button, our article manager will finally decide his permission.
                */
                if (userId == 1 || userGroup > 0 || userId == Integer.parseInt(autherId)) {
                    json.append("true");
                } else {
                    json.append("false");
                }
                response.getWriter().write(json.append("}").toString());
            break;
            default:
                Log.Warn("unrecognized action " + action);
                break;
        }
    }
}
