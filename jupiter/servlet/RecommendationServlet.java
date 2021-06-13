package com.laioffer.jupiter.servlet;

import com.laioffer.jupiter.entity.Item;
import com.laioffer.jupiter.recommendation.ItemRecommender;
import com.laioffer.jupiter.recommendation.RecommendationException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@WebServlet(name = "RecommendationServlet", urlPatterns = {"/recommendation"})
public class RecommendationServlet extends HttpServlet {
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);//判断用户有没有登陆，false的意思是如果用户没有登陆不create session，
        // 他去之前的记录里找，如果找到了就返回之前的，如果没有找到就返回一个null，不创建新的
        ItemRecommender itemRecommender = new ItemRecommender();
        Map<String, List<Item>> itemMap;
        try {
            if (session == null) {//说明用户没有登陆，就按照topgame来推荐
                itemMap = itemRecommender.recommendItemsByDefault();
            } else {//如果登陆了，就拿到session里的user id 然后按照recommendItemsByUser(）来推荐
                String userId = (String) request.getSession().getAttribute("user_id");
                itemMap = itemRecommender.recommendItemsByUser(userId);
            }
        } catch (RecommendationException e) {
            throw new ServletException(e);
        }

        ServletUtil.writeItemMap(response, itemMap);
    }
}

