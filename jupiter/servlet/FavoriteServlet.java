package com.laioffer.jupiter.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laioffer.jupiter.db.MySQLConnection;
import com.laioffer.jupiter.db.MySQLException;
import com.laioffer.jupiter.entity.FavoriteRequestBody;
import com.laioffer.jupiter.entity.Item;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@WebServlet(name = "FavoriteServlet", urlPatterns = {"/favorite"})//postman里面的地址是发到/favorite里
public class FavoriteServlet extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        //class 10
        HttpSession session = request.getSession(false);
        if (session == null) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        String userId = (String) session.getAttribute("user_id");//从session里获取userid，为什么，因为生成session里就有userid
        //String userId = request.getParameter("user_id");

        //ObjectMapper mapper = new ObjectMapper();//json to java obj
        //FavoriteRequestBody body = mapper.readValue(request.getReader(), FavoriteRequestBody.class);
        //request body 的json string 转换成object
        FavoriteRequestBody body = ServletUtil.readRequestBody(FavoriteRequestBody.class, request);

        if (body == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        MySQLConnection connection = null;
        try {
            connection = new MySQLConnection();
            connection.setFavoriteItem(userId, body.getFavoriteItem());
        } catch (MySQLException e) {
            throw new ServletException(e);
        } finally {
            if (connection != null) {
                connection.close();
            }
        }
    }
//这个servlet专门针对fav的请求

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        //class 10
        HttpSession session = request.getSession(false);
        if (session == null) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        String userId = (String) session.getAttribute("user_id");
        //String userId = request.getParameter("user_id");
        Map<String, List<Item>> itemMap;
        MySQLConnection connection = null;
        try {
            connection = new MySQLConnection();
            itemMap = connection.getFavoriteItems(userId);
            //response.setContentType("application/json;charset=UTF-8");
            //response.getWriter().print(new ObjectMapper().writeValueAsString(itemMap));
            ServletUtil.writeItemMap(response, itemMap);
        } catch (MySQLException e) {
            throw new ServletException(e);
        } finally {
            if (connection != null) {
                connection.close();
            }
        }
    }


    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        //class 10
        HttpSession session = request.getSession(false);
        if (session == null) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        String userId = (String) session.getAttribute("user_id");
        //String userId = request.getParameter("user_id");
        //ObjectMapper mapper = new ObjectMapper();
        //FavoriteRequestBody body = mapper.readValue(request.getReader(), FavoriteRequestBody.class);
        FavoriteRequestBody body = ServletUtil.readRequestBody(FavoriteRequestBody.class, request);

        if (body == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        MySQLConnection connection = null;
        try {
            connection = new MySQLConnection();
            connection.unsetFavoriteItem(userId, body.getFavoriteItem().getId());
        } catch (MySQLException e) {
            throw new ServletException(e);
        } finally {
            if (connection != null) {
                connection.close();
            }
        }
    }
}

