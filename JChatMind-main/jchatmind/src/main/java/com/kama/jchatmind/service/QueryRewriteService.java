package com.kama.jchatmind.service;

/**
 * 查询重写服务。
 *
 * <p>用于将用户的原始问题改写成更适合检索/召回的“检索查询（search query）”，
 * 一般应更短、更聚焦、实体更明确，便于向量检索与关键词检索。</p>
 */
public interface QueryRewriteService {

    /**
     * 对用户查询进行重写。
     *
     * @param userQuery 用户原始输入（通常是本轮用户问题）
     * @return 重写后的检索查询；若重写不可用则返回原始输入的清洗版本
     */
    String rewrite(String userQuery);
}

