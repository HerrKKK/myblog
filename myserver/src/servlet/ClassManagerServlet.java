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

public class ClassManagerServlet extends HttpServlet {

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {


        HttpSession session = request.getSession(true);

        String action = request.getParameter("action");

        String classIdS = request.getParameter("classId");
        String className = request.getParameter("className");
        String fatherIdS = request.getParameter("fatherId");
        String groupS = request.getParameter("group");

        int classId = 0;
        int fatherId = 0;
        int group = -1;

        if (fatherIdS != null && fatherIdS.length() != 0) {
            fatherId = Integer.parseInt(fatherIdS);
        }
        if (classIdS != null && classIdS.length() != 0) {
            classId = Integer.parseInt(classIdS);
        }
        if (groupS != null && groupS.length() != 0) {
            group = Integer.parseInt(groupS);
        }

        ClassInfo info = new ClassInfo();
        StringBuilder json = new StringBuilder("{");
        response.setContentType("text/plain");

        switch (action) {
            /* ?action=create&className=ww&fatherId=0&group=0 */
            case "create":
                Log.Verbose("create a class");
                if (!isUserAuthorized(session)) {
                    Log.Error("Unauthorized access!");
                }
                info.setValue(0, className, fatherId, group);
                // classId, className, fatherId, group
                int createId = ClassManagerDAO.createClass(info);
                if (createId > 0) {
                    Log.Info("class id is " + createId);
                    response.getWriter().write(String.valueOf(createId));
                } else {
                    Log.Info("class id is " + createId);
                    response.getWriter().write("failure");
                }
                break;
            case "delete":
                /* ?action=delete&classId=1 */
                // return deleted id
                if (!isUserAuthorized(session)) {
                    Log.Error("Unauthorized access!");
                    break;
                }
                Log.Info("delete class No. " + classId);
                response.getWriter().write(String.valueOf(ClassManagerDAO.deleteClass(classId)));
                break;
            case "update":
                /* ?action=update&articleId=1&title=ww&summary=ww&tags=www&bodyMD=www?permission=1 */
                if (!isUserAuthorized(session)) {
                    Log.Error("Unauthorized access!");
                    break;
                }
                Log.Info("update article No. " + classId);
                info.setValue(classId, className, fatherId, group);
                ClassManagerDAO.updateClass(info);
            break;
            /* ?action=allclasses */
            case "allclasses":
                Log.Verbose("num is " + ClassManagerDAO.subArticleCount(0));
                ClassInfo[] allClasses = ClassManagerDAO.allTopClasses();
                if (allClasses == null || allClasses.length == 0) {
                    response.getWriter().write("failure");
                }
                json.append("\"list\":[");
                for (int i = 0; i < allClasses.length; i++) {
                    int subArtNum = ClassManagerDAO.subArticleCount(allClasses[i].mClassId);
                    StringBuilder pair = new StringBuilder(",\"articleCount\":").append(String.valueOf(subArtNum));
                    json.append(ClassInfo.jsonAppend(allClasses[i].toJson(), pair.toString()));
                    if (i != allClasses.length - 1) {
                        json.append(",");
                    }
                }
                json.append("]}");
                response.getWriter().write(json.toString());
            break;
            case "subclasses":
                ClassInfo[] subClasses = ClassManagerDAO.subClasses(classId);
                if (subClasses == null || subClasses.length == 0) {
                    response.getWriter().write("failure");
                }
                json.append("\"list\":[");
                for (int i = 0; i < subClasses.length; i++) {
                    String fatherName = ClassManagerDAO.searchClass(subClasses[i].mFatherId).mClassName;
                    json.append(subClasses[i].toJson(fatherName));
                    if (i != subClasses.length - 1) {
                        json.append(",");
                    }
                }
                json.append("]}");
                response.getWriter().write(json.toString());
            break;
            default:
                Log.Info("unrecognized action " + action);
        }
    }

    private boolean isUserAuthorized(HttpSession session) {
        if (session == null) {
            return false;
        }

        String logStatus = (String)session.getAttribute("status");
        if (logStatus == null || !logStatus.equals("login")) {
            Log.Error("not logged in!");
            return false;
        }

        int userGroup = (int)session.getAttribute("userGroup");
        int userId = (int) session.getAttribute("userId");
        if (userGroup < 1) {
            Log.Error("unauthorized group, user is "
                        + userId + " group is " + userGroup);
            return false;
        }

        return true;
    }
}
