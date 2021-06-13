package com.laioffer.jupiter.db;

import java.sql.Connection;
import java.sql.DriverManager;

import com.laioffer.jupiter.entity.Item;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.laioffer.jupiter.entity.ItemType;
import com.laioffer.jupiter.entity.User;

//similar to TwitchClient

public class MySQLConnection {
    private final Connection conn;

    public MySQLConnection() throws MySQLException {
        try {
            //com.mysql.cj.jdbc.Driver driver = new com.mysql.cj.jdbc.Driver() //new一个driver对象 （与下面那行一样)
            Class.forName("com.mysql.cj.jdbc.Driver").newInstance();//jbdc library has problem, use this line to handle some corner case
            //上面一行是反射机制，真正到运行到这个环节的时候，才能知道他的behavior （run time的时候才能知道用那个class）
            conn = DriverManager.getConnection(MySQLDBUtil.getMySQLAddress());//connect with mysql on RDS
        //above, you can only intialize only one time when you call the constructor
        } catch (Exception e) {//if you didn't connect successfully, throw exception
            e.printStackTrace();
            throw new MySQLException("Failed to connect to Database");
        }
    }

    public void close() {
        if (conn != null) {
            try {
                conn.close();//断开与数据库的连接
                //1。连接数据库的时候，资源时候是有限的（比如数据库最多只能同时有五十个连接，你不用回占用资源）
                //2。每次连接一次外部资源，twitch or db，会耗费一部分内存 eg1。connect 2。read 3 return（第三部时候其实你已经不用数据库了，
                // 你就可以先close）这样你的一部分内存资源会被回收，不占用数据库资源，而且你的内存会释放一部分内存里咩用的东西，这样程序会运行的更快
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void setFavoriteItem(String userId, Item item) throws MySQLException {//添加一个fav
        if (conn == null) {
            System.err.println("DB connection failed");
            throw new MySQLException("Failed to connect to Database");
        }
        //insert userid and itemid fav table, maybe insert item into item table?
        //需要添加的item别人没有添加过，或者item table是空的，
        saveItem(item);
        String sql = "INSERT IGNORE INTO favorite_records (user_id, item_id) VALUES (?, ?)";
        //如果一个user重复收藏同一个fav呢？程序会出现exception，频繁出现MySQLException，加上IGNORE：如果有duplicate的话，就IGNORE他，不抛出异常，也不出error
        try {
            PreparedStatement statement = conn.prepareStatement(sql);//sql的保护机制，防止恶意的攻击
            statement.setString(1, userId);
            statement.setString(2, item.getId());
            statement.executeUpdate();//执行一下sql语句
        } catch (SQLException e) {
            e.printStackTrace();
            throw new MySQLException("Failed to save favorite item to Database");
        }
    }

    public void unsetFavoriteItem(String userId, String itemId) throws MySQLException {
        if (conn == null) {
            System.err.println("DB connection failed");
            throw new MySQLException("Failed to connect to Database");
        }
        String sql = "DELETE FROM favorite_records WHERE user_id = ? AND item_id = ?";
        //shoudl we do deleteItem(item)???
        //1.if no one else fav this item, you can immediately delete it,
        //2. or you can wait for several items and delete them together(this save time)
        try {
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setString(1, userId);
            statement.setString(2, itemId);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new MySQLException("Failed to delete favorite item to Database");
        }
    }
//fav table 是一个foreign key，他必须指向item table里的一行，假如item里面没有这个项目，我们需要在item里面也插入
    public void saveItem(Item item) throws MySQLException {
        if (conn == null) {
            System.err.println("DB connection failed");
            throw new MySQLException("Failed to connect to Database");
        }
        String sql = "INSERT IGNORE INTO items VALUES (?, ?, ?, ?, ?, ?, ?)";
        try {
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setString(1, item.getId());
            statement.setString(2, item.getTitle());
            statement.setString(3, item.getUrl());
            statement.setString(4, item.getThumbnailUrl());
            statement.setString(5, item.getBroadcasterName());
            statement.setString(6, item.getGameId());
            statement.setString(7, item.getType().toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new MySQLException("Failed to add item to Database");
        }
    }

    public Set<String> getFavoriteItemIds(String userId) throws MySQLException {
        if (conn == null) {
            System.err.println("DB connection failed");
            throw new MySQLException("Failed to connect to Database");
        }

        Set<String> favoriteItems = new HashSet<>();
        String sql = "SELECT item_id FROM favorite_records WHERE user_id = ?";
        try {
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setString(1, userId);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                String itemId = rs.getString("item_id");
                favoriteItems.add(itemId);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new MySQLException("Failed to get favorite item ids from Database");
        }

        return favoriteItems;
    }

    public Map<String, List<Item>> getFavoriteItems(String userId) throws MySQLException {
        if (conn == null) {
            System.err.println("DB connection failed");
            throw new MySQLException("Failed to connect to Database");
        }
        Map<String, List<Item>> itemMap = new HashMap<>();
        for (ItemType type : ItemType.values()) {
            itemMap.put(type.toString(), new ArrayList<>());
        }
        Set<String> favoriteItemIds = getFavoriteItemIds(userId);
        String sql = "SELECT * FROM items WHERE id = ?";
        try {
            PreparedStatement statement = conn.prepareStatement(sql);
            for (String itemId : favoriteItemIds) {
                statement.setString(1, itemId);
                ResultSet rs = statement.executeQuery();
                if (rs.next()) {
                    ItemType itemType = ItemType.valueOf(rs.getString("type"));
                    Item item = new Item.Builder().id(rs.getString("id")).title(rs.getString("title"))
                            .url(rs.getString("url")).thumbnailUrl(rs.getString("thumbnail_url"))
                            .broadcasterName(rs.getString("broadcaster_name")).gameId(rs.getString("game_id")).type(itemType).build();
                    itemMap.get(rs.getString("type")).add(item);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new MySQLException("Failed to get favorite items from Database");
        }
        return itemMap;
    }

    public Map<String, List<String>> getFavoriteGameIds(Set<String> favoriteItemIds) throws MySQLException {
        if (conn == null) {
            System.err.println("DB connection failed");
            throw new MySQLException("Failed to connect to Database");
        }
        Map<String, List<String>> itemMap = new HashMap<>();
        for (ItemType type : ItemType.values()) {
            itemMap.put(type.toString(), new ArrayList<>());
        }
        String sql = "SELECT game_id, type FROM items WHERE id = ?";
        try {
            PreparedStatement statement = conn.prepareStatement(sql);
            for (String itemId : favoriteItemIds) {
                statement.setString(1, itemId);
                ResultSet rs = statement.executeQuery();
                if (rs.next()) {
                    itemMap.get(rs.getString("type")).add(rs.getString("game_id"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new MySQLException("Failed to get favorite game ids from Database");
        }
        return itemMap;
    }
//class 10
public String verifyLogin(String userId, String password) throws MySQLException {
    if (conn == null) {
        System.err.println("DB connection failed");
        throw new MySQLException("Failed to connect to Database");
    }
    String name = "";
    String sql = "SELECT first_name, last_name FROM users WHERE id = ? AND password = ?";
    try {
        PreparedStatement statement = conn.prepareStatement(sql);
        statement.setString(1, userId);
        statement.setString(2, password);
        ResultSet rs = statement.executeQuery();
        if (rs.next()) {
            name = rs.getString("first_name") + " " + rs.getString("last_name");
        }
    } catch (SQLException e) {
        e.printStackTrace();
        throw new MySQLException("Failed to verify user id and password from Database");
    }
    return name;
}

    public boolean addUser(User user) throws MySQLException {
        if (conn == null) {
            System.err.println("DB connection failed");
            throw new MySQLException("Failed to connect to Database");
        }

        String sql = "INSERT IGNORE INTO users VALUES (?, ?, ?, ?)";
        try {
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setString(1, user.getUserId());
            statement.setString(2, user.getPassword());
            statement.setString(3, user.getFirstname());
            statement.setString(4, user.getLastname());

            return statement.executeUpdate() == 1;
            //executeUpdate返回1，是判断是否insert了一行，返回值就是1；如果插入三行，返回3。
//没有成功插入，就返回0
        } catch (SQLException e) {
            e.printStackTrace();
            throw new MySQLException("Failed to get user information from Database");
        }
    }


}
