package com.laioffer.jupiter.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laioffer.jupiter.db.MySQLConnection;
import com.laioffer.jupiter.db.MySQLException;
import com.laioffer.jupiter.entity.LoginRequestBody;
import com.laioffer.jupiter.entity.LoginResponseBody;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
/*
@WebServlet(name = "LoginServlet", urlPatterns = {"/login"})
public class LoginServlet extends HttpServlet {

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        //request body里
        //step1:验证身份，生成id
        ObjectMapper mapper = new ObjectMapper();
        LoginRequestBody body = mapper.readValue(request.getReader(), LoginRequestBody.class);
        if (body == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);//error 400
            return;
        }

        String username;
        MySQLConnection connection = null;
        try {
            connection = new MySQLConnection();
            String userId = body.getUserId();
            String password = ServletUtil.encryptPassword(body.getUserId(), body.getPassword());//根据前端传过来的明文的pw，转换成密文的pw
            //也需要加密，因为去数据库比对的是密文的密码，
            username = connection.verifyLogin(userId, password);//验证
        } catch (MySQLException e) {
            throw new ServletException(e);
        } finally {
            connection.close();
        }
    //step2：如果验证ok，返回session！
        if (!username.isEmpty()) {//username ！= null 不可以，因为上面默认值不是null
            HttpSession session = request.getSession();//获取一个新的session,这是一个login请求，之前没登录成功过，id也给你设置好了
            // 当前request没有一个session，所以要create一个新的session
            session.setAttribute("user_id", body.getUserId());
            //session.setMaxInactiveInterval(600);//也可以设置一个过期时间，例如600s
            //这些attribute不用加密，因为他只会返回一个session id
        //下面要返回一些response
            LoginResponseBody loginResponseBody = new LoginResponseBody(body.getUserId(), username);
            response.setContentType("application/json;charset=UTF-8");
            mapper.writeValue(response.getWriter(), loginResponseBody);
            //response里session id呢？？？你的后端运行在tomcat环境里，所有tomcat帮你自动实现。这些servlet都是你自己customize
            //一些常见的功能都是tomcat帮你（session id的绑定，response返回都是tomcat帮你）

        } else {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);//登录失败 返回401：unauthorized
        }
    }
}

 */
@WebServlet(name = "LoginServlet", urlPatterns = {"/login"})
public class LoginServlet extends HttpServlet {

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        //ObjectMapper mapper = new ObjectMapper();
        //LoginRequestBody body = mapper.readValue(request.getReader(), LoginRequestBody.class);
        LoginRequestBody body = ServletUtil.readRequestBody(LoginRequestBody.class, request);
        if (body == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        String username;
        MySQLConnection connection = null;
        try {
            connection = new MySQLConnection();
            String userId = body.getUserId();
            String password = ServletUtil.encryptPassword(body.getUserId(), body.getPassword());
            username = connection.verifyLogin(userId, password);
        } catch (MySQLException e) {
            throw new ServletException(e);
        } finally {
            connection.close();
        }

        if (!username.isEmpty()) {
            HttpSession session = request.getSession();
            session.setAttribute("user_id", body.getUserId());
            session.setMaxInactiveInterval(600);

            LoginResponseBody loginResponseBody = new LoginResponseBody(body.getUserId(), username);
            response.setContentType("application/json;charset=UTF-8");
            ObjectMapper mapper = new ObjectMapper();//新加的
            response.getWriter().print(new ObjectMapper().writeValueAsString(loginResponseBody));
        } else {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }
}


