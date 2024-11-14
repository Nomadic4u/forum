package com.example.utils;

import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 敏感词处理工具 - DFA算法实现
 * 用于检测和替换文本中的敏感词
 * @author Java新视界
 * @modifier Java新视界
 * @date 2023/10/25 16:58
 */
@Component
public class SensitiveWordUtil {
    // 敏感词匹配规则
    public static final int MIN_MATCH_TYPE = 1; // 最小匹配规则
    public static final int MAX_MATCH_TYPE = 2; // 最大匹配规则

    /**
     * 初始化敏感词映射，将敏感词构建成树形结构
     * @param sensitiveWordSet 敏感词集合
     * @return 树形结构的敏感词映射
     */
    private static Map<String, Object> initSensitiveWordMap(Set<String> sensitiveWordSet) {
        // 初始化敏感词容器，使用负载因子减少扩容
        Map<String, Object> map = new HashMap<>(Math.max((int) (sensitiveWordSet.size() / .75f) + 1, 16));

        // 迭代每个敏感词
        for (String aKeyWordSet : sensitiveWordSet) {
            Map nowMap = map; // 当前Map指针
            for (int i = 0; i < aKeyWordSet.length(); i++) {
                char keyChar = aKeyWordSet.charAt(i); // 当前字符
                Object wordMap = nowMap.get(keyChar); // 查找当前字符对应的Map
                if (wordMap != null) {
                    nowMap = (Map<String, Object>) wordMap; // 如果存在该字符，直接赋值
                } else {
                    // 如果不存在，则构建一个新的Map，并将isEnd设置为0
                    Map<String, String> newWorMap = new HashMap<>(3);
                    newWorMap.put("isEnd", "0");
                    nowMap.put(keyChar, newWorMap);
                    nowMap = newWorMap; // 更新当前Map指针
                }
                if (i == aKeyWordSet.length() - 1) { // 判断是否是最后一个字符
                    nowMap.put("isEnd", "1"); // 设置为结束标志
                }
            }
        }
        return map; // 返回构建的敏感词映射
    }

    /**
     * 获取文本中的所有敏感词
     * @param sensitiveWordSet 敏感词集合
     * @param txt 待检测的文本
     * @param matchType 匹配类型（最小/最大）
     * @return 包含所有敏感词的集合
     */
    public static Set<String> getSensitiveWord(Set<String> sensitiveWordSet, String txt, int matchType) {
        Set<String> sensitiveWordList = new HashSet<>(); // 存放检测到的敏感词
        Map<String, Object> map = initSensitiveWordMap(sensitiveWordSet); // 初始化敏感词映射

        // 遍历文本中的每个字符
        for (int i = 0; i < txt.length(); i++) {
            int length = checkSensitiveWord(map, txt, i, matchType); // 检查是否有敏感词匹配
            if (length > 0) { // 存在敏感词
                sensitiveWordList.add(txt.substring(i, i + length)); // 加入敏感词列表
                i = i + length - 1; // 移动到敏感词末尾，避免重复检查
            }
        }

        return sensitiveWordList; // 返回敏感词集合
    }

    /**
     * 替换文本中的敏感词
     * @param sensitiveWordSet 敏感词集合
     * @param txt 待处理的文本
     * @param replaceChar 替换字符
     * @param matchType 匹配类型
     * @return 替换后的文本
     */
    public static String replaceSensitiveWord(Set<String> sensitiveWordSet, String txt, char replaceChar, int matchType) {
        String resultTxt = txt; // 初始文本
        // 获取所有的敏感词
        Set<String> set = getSensitiveWord(sensitiveWordSet, txt, matchType);
        Iterator<String> iterator = set.iterator(); // 创建迭代器
        String word; // 存放敏感词
        String replaceString; // 存放替换字符串
        while (iterator.hasNext()) {
            word = iterator.next(); // 获取下一个敏感词
            replaceString = getReplaceChars(replaceChar, word.length()); // 获取替换字符
            resultTxt = resultTxt.replaceAll(word, replaceString); // 执行替换
        }

        return resultTxt; // 返回处理后的文本
    }

    /**
     * 替换文本中的敏感词
     * @param sensitiveWordSet 敏感词集合
     * @param txt 待处理的文本
     * @param replaceStr 替换字符串
     * @param matchType 匹配类型
     * @return 替换后的文本
     */
    public static String replaceSensitiveWord(Set<String> sensitiveWordSet, String txt, String replaceStr, int matchType) {
        String resultTxt = txt; // 初始文本
        // 获取所有的敏感词
        Set<String> set = getSensitiveWord(sensitiveWordSet, txt, matchType);
        Iterator<String> iterator = set.iterator(); // 创建迭代器
        String word; // 存放敏感词
        while (iterator.hasNext()) {
            word = iterator.next(); // 获取下一个敏感词
            resultTxt = resultTxt.replaceAll(word, replaceStr); // 执行替换
        }

        return resultTxt; // 返回处理后的文本
    }

    /**
     * 生成替换字符
     * @param replaceChar 替换字符
     * @param length 替换字符的长度
     * @return 由替换字符组成的字符串
     */
    private static String getReplaceChars(char replaceChar, int length) {
        String resultReplace = String.valueOf(replaceChar); // 初始化结果字符串
        for (int i = 1; i < length; i++) { // 循环生成替换字符
            resultReplace += replaceChar; // 添加替换字符
        }

        return resultReplace; // 返回替换字符字符串
    }

    /**
     * 检查文本中是否存在敏感词
     * @param nowMap 当前的敏感词Map
     * @param txt 待检测的文本
     * @param beginIndex 检查的起始索引
     * @param matchType 匹配类型
     * @return 匹配到的敏感词长度
     */
    private static int checkSensitiveWord(Map<String, Object> nowMap, String txt, int beginIndex, int matchType) {
        boolean flag = false; // 匹配结束标志
        int matchFlag = 0; // 当前匹配的长度标识
        int maxMatchFlag = 0; // 记录最大匹配长度
        char word; // 当前字符

        for (int i = beginIndex; i < txt.length(); i++) {
            word = txt.charAt(i); // 获取当前字符
            nowMap = (Map<String, Object>) nowMap.get(word); // 获取当前字符对应的子Map

            if (nowMap != null) { // 如果存在该字符的子Map
                matchFlag++; // 匹配长度加1

                // 检查是否为最后一个字符
                if ("1".equals(nowMap.get("isEnd"))) {
                    flag = true; // 设置结束标志为true
                    maxMatchFlag = matchFlag; // 更新最大匹配长度
                    // 如果是最小匹配规则，直接返回
                    if (MIN_MATCH_TYPE == matchType) {
                        break;
                    }
                }
            } else { // 如果不存在该字符的子Map
                break; // 直接退出
            }
        }

        // 如果没有匹配到有效的敏感词，返回0
        if (maxMatchFlag < 2 || !flag) {
            maxMatchFlag = 0;
        }
        return maxMatchFlag; // 返回最大匹配长度
    }

}
