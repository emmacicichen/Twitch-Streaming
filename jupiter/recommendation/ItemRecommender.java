package com.laioffer.jupiter.recommendation;

import com.laioffer.jupiter.entity.Game;
import com.laioffer.jupiter.entity.Item;
import com.laioffer.jupiter.entity.ItemType;
import com.laioffer.jupiter.external.TwitchClient;
import com.laioffer.jupiter.external.TwitchException;
import com.laioffer.jupiter.db.MySQLConnection;
import com.laioffer.jupiter.db.MySQLException;

import java.util.*;

import java.util.function.Function;
import java.util.stream.Collectors;





public class ItemRecommender {
    private static final int DEFAULT_GAME_LIMIT = 3;//如果你没收藏过item，我用top3给你推荐
    private static final int DEFAULT_PER_GAME_RECOMMENDATION_LIMIT = 10;//每个游戏最多推荐几个
    private static final int DEFAULT_TOTAL_RECOMMENDATION_LIMIT = 20;//最多一共推荐几个，上限
//game id出现次数越多越优先考虑

    //1。 没登录或者没收藏过任何
    //返回一个list， input：type（video，stream，clip）
    private List<Item> recommendByTopGames(ItemType type, List<Game> topGames) throws RecommendationException {
        List<Item> recommendedItems = new ArrayList<>();//返回的list
        TwitchClient client = new TwitchClient();//需要调用TwitchClient里的topgame

        outerloop://标记这是outerloop，方便后面break在这里
        for (Game game : topGames) {//遍历topGame里所有的游戏
            List<Item> items;//searchByType返回的是list of item
            try {
                items = client.searchByType(game.getId(), type, DEFAULT_PER_GAME_RECOMMENDATION_LIMIT);//client.searchByType就是得到topgame
            } catch (TwitchException e) {//search twitch的时候可能出exception
                throw new RecommendationException("Failed to get recommendation result");
            }
            for (Item item : items) {//loop每一个item，看根据他推荐的数目是否到上限
                if (recommendedItems.size() == DEFAULT_TOTAL_RECOMMENDATION_LIMIT) {
                    break outerloop;//当到了上限，我们就break
                }
                recommendedItems.add(item);
            }
        }
        return recommendedItems;
    }
//2。 收藏过，根据收藏来推荐
    //input有一个item id，why？
    private List<Item> recommendByFavoriteHistory(//通过mySQLconnection来得到你想要的参数，然后来调用这个function来实现推荐
            Set<String> favoritedItemIds, List<String> favoriteGameIds, ItemType type/*video*/) throws RecommendationException {
        //favoriteGameIds -> [1234,1234,2345] 1234出现次数多，优先推荐他 -> {1234: 2, 2345: 1} 把list转成map，value按照降序排序
        Map<String, Long> favoriteGameIdByCount = favoriteGameIds.parallelStream()//count是long因为保证太多 不越界
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));//groupingBy(str -> str, Collectors.counting())
        //str -> str: 是lambda表达式，就是input是str，返回str，也就是返回自己，（str是变量名字，只是直观的表示string，你写x->x 也是一样的，与Function.identity()一样的意思
        //java stream的方法  groupingBy：把相同的结果combine到一起，Function.identity()：key  Collectors.counting()：value是什么
        //parallelStream() 是在stream上parelle进行
//groupingBy：我把array里面相同的元素变成一组，结果变成一个map，[1234:2,2345:1]
        //假如favoriteGameid -> [game, game, game],都是obj，game里面有price。。。这些特性 根据特性来group，可以算sub，average
        List<Map.Entry<String, Long>> sortedFavoriteGameIdListByCount = new ArrayList<>(
                favoriteGameIdByCount.entrySet());//把Entry根据value来sort
        sortedFavoriteGameIdListByCount.sort((Map.Entry<String, Long> e1, Map.Entry<String, Long> e2) -> Long
                .compare(e2.getValue(), e1.getValue()));//通过Long.compare来sort

        if (sortedFavoriteGameIdListByCount.size() > DEFAULT_GAME_LIMIT) {//如果fav game太多了，我就只取下来前三个
            sortedFavoriteGameIdListByCount = sortedFavoriteGameIdListByCount.subList(0, DEFAULT_GAME_LIMIT);
        }

        List<Item> recommendedItems = new ArrayList<>();
        TwitchClient client = new TwitchClient();

        outerloop:
        for (Map.Entry<String, Long> favoriteGame : sortedFavoriteGameIdListByCount) {
            List<Item> items;//一个gameid一种游戏，有很多up主发的视频，所以是list
            try {//favoriteGame.getKey()是gameid
                items = client.searchByType(favoriteGame.getKey(), type, DEFAULT_PER_GAME_RECOMMENDATION_LIMIT);
            } catch (TwitchException e) {
                throw new RecommendationException("Failed to get recommendation result");
            }

            for (Item item : items) {
                if (recommendedItems.size() == DEFAULT_TOTAL_RECOMMENDATION_LIMIT) {
                    break outerloop;
                }
                if (!favoritedItemIds.contains(item.getId())) {//如果已经推荐过了，就不加
                    recommendedItems.add(item);
                }
            }
        }
        return recommendedItems;
    }
//3。如果知道user是谁，首先判断有没有收藏记录（1。无收藏记录：推荐topgame， 2。 有收藏记录：通过gameid来推荐）
    //这个func是public，因为你要在recommendservlet上去访问他，（因为servelt和这个func不在一个package里）上面两个private func是helper func
    public Map<String, List<Item>> recommendItemsByUser(String userId) throws RecommendationException {
        Map<String, List<Item>> recommendedItemMap = new HashMap<>();
        Set<String> favoriteItemIds;
        Map<String, List<String>> favoriteGameIds;//why itemid是list，but favoriteGameIds是个map呢？{"video" :"1234, 2345",
        //"stream": "3456"， "clip": ""} 因为gameId里面有不同类型的data，所以要拿到gameId不同类型的fav
        //假如里面clip是空的，我就按照topgame来推荐，其他的有东西的type，我就按history推荐
        MySQLConnection connection = null;
        try {
            connection = new MySQLConnection();//通过MySQLConnection()得到user收藏了什么itemid，和gameid
            favoriteItemIds = connection.getFavoriteItemIds(userId);
            favoriteGameIds = connection.getFavoriteGameIds(favoriteItemIds);
        } catch (MySQLException e) {
            throw new RecommendationException("Failed to get user favorite history for recommendation");
        } finally {
            connection.close();
        }

        for (Map.Entry<String, List<String>> entry : favoriteGameIds.entrySet()) {//favoriteGameIds有三种type，所以要iterate hashmap
            if (entry.getValue().size() == 0) {
                TwitchClient client = new TwitchClient();
                List<Game> topGames;
                try {
                    topGames = client.topGames(DEFAULT_GAME_LIMIT);
                } catch (TwitchException e) {
                    throw new RecommendationException("Failed to get game data for recommendation");
                }
                recommendedItemMap.put(entry.getKey(), recommendByTopGames(ItemType.valueOf(entry.getKey()), topGames));
            } else {
                recommendedItemMap.put(entry.getKey(), recommendByFavoriteHistory(favoriteItemIds, entry.getValue(), ItemType.valueOf(entry.getKey())));
            }
        }
        return recommendedItemMap;
    }
//4。 没传进来user是谁的时候
    public Map<String, List<Item>> recommendItemsByDefault() throws RecommendationException {
        Map<String, List<Item>> recommendedItemMap = new HashMap<>();//返回给前端的结果是个hashmap，他跟search的结果很接近
        //key：type  value：arraylist of你推荐的一些game
        TwitchClient client = new TwitchClient();
        List<Game> topGames;//先拿到topgame
        try {
            topGames = client.topGames(DEFAULT_GAME_LIMIT);
        } catch (TwitchException e) {
            throw new RecommendationException("Failed to get game data for recommendation");
        }

        for (ItemType type : ItemType.values()) {
            recommendedItemMap.put(type.toString(), recommendByTopGames(type, topGames));//调用recommendByTopGames，直接给你推荐topgame
        }
        return recommendedItemMap;
    }




}
