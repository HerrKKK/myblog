package servlet;

import dao.NumberCountDAO;
import dao.ArticleManagerDAO;
import util.ArticleInfo;
import util.Log;

import java.io.File;
import java.io.IOException;
import java.sql.Date;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;

public class ArticleManagerServlet extends HttpServlet {

    private int mArticleNumber;

    public void init() throws ServletException {
        super.init();
        Log.Verbose("start Article Servlet");

        mArticleNumber = NumberCountDAO.getArticleCount();
        Log.Info("ArticleManagerServlet.init() article number = " + mArticleNumber);
    }

    /*
    Functions:
    1. Post an article
    2. Delete an article
    3. Update an article
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null) {
            Log.Error("no session");
            return;
        }
        Log.Info(session.getId());
        String logStatus = (String)session.getAttribute("status");
        if (logStatus == null || !logStatus.equals("login")) {
            Log.Error("did not logged in");
            return;
        }
        int userGroup = (int)session.getAttribute("userGroup");
        if (userGroup < 1) {
            Log.Error("unauthorized group");
            return;
        }
        int userId = (int) session.getAttribute("userId");
        Log.Info("article manager invoked, user is " + userId + " belongs to group " + userGroup);

        String action = request.getParameter("action");

        String articleIdS = request.getParameter("articleId");
        String classIdS = request.getParameter("classId");
        String title = request.getParameter("title");
        String summary = request.getParameter("summary");
        String tags = request.getParameter("tags");
        String bodyMD = request.getParameter("bodyMD");
        String permission = request.getParameter("permission");

        int articleId = 0;
        int classId = 0;
        if (articleIdS != null && articleIdS.length() != 0) {
            articleId = Integer.parseInt(articleIdS);
        }
        if (classIdS != null && classIdS.length() != 0) {
            classId = Integer.parseInt(classIdS);
        }

        ArticleInfo info = new ArticleInfo();
        java.sql.Date currentTime = new java.sql.Date(System.currentTimeMillis());
        response.setContentType("text/plain");

        switch (action) {
            case "post":
                Log.Info("post an article");
                info.setValue(0, userId, classId, title, summary, tags, bodyMD, currentTime.toString(), currentTime.toString(), 0);
                // articleId, userId, classId, title, summary, tags, body, createtime, lstmodftime, permission
                int createId = ArticleManagerDAO.createArticle(info);
                if (createId > 0) {
                    Log.Info("article id is " + createId);
                    mArticleNumber++;
                    response.getWriter().write(String.valueOf(createId));
                } else {
                    Log.Info("article id is " + createId);
                    response.getWriter().write("failure");
                }
            break;
            case "delete":
                Log.Info("delete article No. " + articleId);
                if (ArticleManagerDAO.legalAuthor(articleId, userId) != true) {
                    Log.Warn("auther id failure");
                    break;
                }
                ArticleManagerDAO.deleteArticle(articleId);
                mArticleNumber--;
                response.getWriter().write("success");
            break;
            case "update":
                /* ?action=update&articleId=1&title=ww&summary=ww&tags=www&bodyMD=www?permission=1 */
                Log.Info("update article No. " + articleId);
                if (ArticleManagerDAO.legalAuthor(articleId, userId) != true) {
                    Log.Warn("auther id failure");
                    response.getWriter().write("failure");
                    break;
                }
                info.setValue(0, userId, classId, title, summary, tags, bodyMD, null, currentTime.toString(), 0);
                ArticleManagerDAO.updateArticle(articleId, info);
            break;
            default:
                Log.Info("unrecognized action " + action);
        }
    }
}
