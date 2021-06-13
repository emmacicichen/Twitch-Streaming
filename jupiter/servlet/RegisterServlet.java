package com.laioffer.jupiter.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laioffer.jupiter.db.MySQLConnection;
import com.laioffer.jupiter.db.MySQLException;
import com.laioffer.jupiter.entity.User;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
@WebServlet(name = "RegisterServlet", urlPatterns = {"/register"})
public class RegisterServlet extends HttpServlet {

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        //ObjectMapper mapper = new ObjectMapper();
        //User user = mapper.readValue(request.getReader(), User.class);
        User user = ServletUtil.readRequestBody(User.class, request);
        if (user == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        boolean isUserAdded = false;
        MySQLConnection connection = null;
        try {
            connection = new MySQLConnection();
            user.setPassword(ServletUtil.encryptPassword(user.getUserId(), user.getPassword()));
            //isUserAdded = connection.addUser(body.getUserId(), body.getPassword(), body.getFirstName(), body.getLastName());
            //            connection.close();
            isUserAdded = connection.addUser(user);
        } catch (MySQLException e) {
            throw new ServletException(e);
            //e.printStackTrace();
        } finally {
            connection.close();
        }

        if (!isUserAdded) {
            response.setStatus(HttpServletResponse.SC_CONFLICT);
        }
    }
}


//加密的pw，不要存明文的密码

//request.getReader() 返回stream格式的request body