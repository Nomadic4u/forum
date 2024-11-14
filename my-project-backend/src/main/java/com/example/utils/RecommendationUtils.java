package com.example.utils;

import org.checkerframework.checker.units.qual.C;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 基于用户的协同过滤算法
 */
@Component
public class RecommendationUtils {
    /**
     * 根据用户推荐帖子
     *
     * @param sim         用户相似度矩阵
     * @param valUserItem 用户与其点赞帖子的映射
     * @param K           选择的相似用户数量
     * @param N           推荐的帖子数量
     * @param targetUser  用户ID
     * @return 推荐帖子ID集合
     */
    public Set<Integer> recommendForUser(Map<String, Map<String, Double>> sim,
                                         Map<String, Set<Integer>> valUserItem,
                                         int K, int N, String targetUser) {
        System.out.println("给用户进行推荐");
        // 计算帖子的推荐得分
        Map<Integer, Double> itemRank = new HashMap<>();

        if (valUserItem.containsKey(targetUser)) {
            // 该用户点赞的帖子集合
            Set<Integer> items = valUserItem.get(targetUser);

            // 根据相似度排序, 然后取前K个与该用户相似度最高的用户
            List<Map.Entry<String, Double>> sortedSim = new ArrayList<>(sim.get(targetUser).entrySet());
            Collections.sort(sortedSim, new Comparator<Map.Entry<String, Double>>() {
                public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
                    return Double.compare(o2.getValue(), o1.getValue());
                }
            });

            System.out.println("检查对相似度矩阵排序后的矩阵");
            for (Map.Entry<String, Double> entry : sortedSim) {
                String item = entry.getKey();
                Double similarity = entry.getValue();
                System.out.println("用户: " + item + ", 相似度: " + similarity);
            }

            for (int i = 0; i < K; i++) {
                // 前K个相似度高的用户
                if (i >= sortedSim.size())
                    break;

                String similarUser = sortedSim.get(i).getKey();
                double score = sortedSim.get(i).getValue();

                // 找出相似用户中有交互的物品, 但当前用户并未交互的物品进行推荐
                for (int item : valUserItem.get(similarUser)) {
                    // 用户已点赞, 跳过
                    if (items.contains(item)) {
                        continue;
                    }

                    itemRank.put(item, itemRank.getOrDefault(item, 0.0) + score);

                }
            }

        }

        // 根据评分进行排序, 取排名靠前的N个物品作为推荐列表
        List<Map.Entry<Integer, Double>> topNItems = new ArrayList<>(itemRank.entrySet());
        Collections.sort(topNItems, new Comparator<Map.Entry<Integer, Double>>() {
            public int compare(Map.Entry<Integer, Double> o1, Map.Entry<Integer, Double> o2) {
                return Double.compare(o2.getValue(), o1.getValue());
            }
        });

        Set<Integer> recommendedItems = new HashSet<>();
        for (int i = 0; i < Math.min(N, topNItems.size()); i++) {
            recommendedItems.add(topNItems.get(i).getKey());
        }

        return recommendedItems;

    }

    /**
     * 得到倒排表 帖子id : Set<用户id>
     *
     * @param accountToBlogs 用户id : Set<帖子id>
     * @return 倒排表
     */
    public Map<Integer, Set<String>> getItemToUser(Map<String, Set<Integer>> accountToBlogs) {
        Map<Integer, Set<String>> ItemToUsers = new HashMap<>();

        for (Map.Entry<String, Set<Integer>> entry : accountToBlogs.entrySet()) {
            String userId = entry.getKey();
            Set<Integer> blogs = entry.getValue();

            for (Integer blogId : blogs) {
                Set<String> users = ItemToUsers.getOrDefault(blogId, new HashSet<>());
                users.add(userId);
                ItemToUsers.put(blogId, users);
            }
        }

        return ItemToUsers;
    }

    /**
     * 计算协同过滤矩阵
     *
     * @param itemToUsers 倒排表
     * @return 协同过滤矩阵
     */
    public Map<String, Map<String, Integer>> getCFMatrix(Map<Integer, Set<String>> itemToUsers) {
        // 用户与用户之间的相似度
        Map<String, Map<String, Integer>> CFMatrix = new HashMap<>();
        System.out.println("开始构建协同过滤矩阵...");

        // 遍历所有帖子, 统计用户两两之间点赞的帖子
        for (Map.Entry<Integer, Set<String>> entry : itemToUsers.entrySet()) {
            Integer item = entry.getKey();
            Set<String> accounts = entry.getValue();

            // 统计每个用户点赞的帖子数
            for (String account : accounts) {

                // 统计用户与其他用户共同点赞的帖子数
                if (!CFMatrix.containsKey(account)) {
                    CFMatrix.put(account, new HashMap<>()); // 如果不存在, 就新创建一个HashMap
                }

                for (String temp_account : accounts) {
                    if (!temp_account.equals(account)) {
                        if (!CFMatrix.get(account).containsKey(temp_account)) {
                            CFMatrix.get(account).put(temp_account, 0);
                        }
                        CFMatrix.get(account).put(temp_account, CFMatrix.get(account).get(temp_account) + 1);
                    }
                }
            }
        }
        return CFMatrix;
    }

    /**
     * 计算相似度
     * @param CFMatrix 协同过滤矩阵
     * @param num 每个用户点赞帖子总数
     * @return 用户与其他用户的相似度
     */
    public Map<String, Map<String, Double>> getSimMatrix(Map<String, Map<String, Integer>> CFMatrix, Map<String, Integer> num) {
        Map<String, Map<String, Double>> sim = new HashMap<>();
        System.out.println("构建用户相似度矩阵...");

        // 遍历协同矩阵每一个键值对
        for (Map.Entry<String, Map<String, Integer>> entry : CFMatrix.entrySet()) {
            String account = entry.getKey();
            Map<String, Integer> otherAccount = entry.getValue();

            for (Map.Entry<String, Integer> accountSorce : otherAccount.entrySet()) {
                String temp_Account = accountSorce.getKey();
                int score = accountSorce.getValue();

                if (!sim.containsKey(account)) {
                    sim.put(account, new HashMap<>());
                }
                sim.get(account).put(temp_Account, CFMatrix.get(account).get(temp_Account)
                                                   / Math.sqrt(num.get(account) * num.get(temp_Account)));
            }
        }
        return sim;
    }


}














