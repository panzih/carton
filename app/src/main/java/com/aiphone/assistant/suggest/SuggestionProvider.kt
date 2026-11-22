package com.aiphone.assistant.suggest

/**
 * 「接下来你可能想做什么」的输出解析。
 *
 * ## 为什么没有独立的 LlmClient
 *
 * 请求本身走 [com.aiphone.assistant.agent.Agent.suggestNext]：它复用本趟任务的
 * system prompt + 全部历史，只在末尾追加一条提问 —— 前面几十轮全是缓存命中。
 * 早先这里自己起了一套独立上下文，看着干净，实际上等于把整段历史按
 * **未命中**价重算一遍，比复用它贵一个数量级。
 *
 * 这个文件只负责把模型吐出来的脏文本收拾干净。
 */
object SuggestionProvider {

    /** 最多显示几条 */
    const val MAX_SUGGESTIONS = 3

    /** 一句建议的长度上限 —— 太长的句子在列表里读起来很累 */
    private const val MAX_LINE_CHARS = 60

    /**
     * 把模型的原始回复解析成建议列表。
     *
     * 模型的输出很脏：可能带编号、带项目符号、带引号、先来一句
     * "以下是三条建议："，也可能直接回「无」。抽成纯函数才测得了。
     */
    fun parse(text: String): List<String> {
        val t = text.trim()
        if (t.isBlank()) return emptyList()
        // 「无」= 模型判断没有明确方向，不是故障
        if (t == "无" || t.startsWith("无\n") || t.startsWith("无（") || t.startsWith("无(")) {
            return emptyList()
        }

        return t.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { line ->
                // 去掉常见前缀：1. / 1、/ - / * / 1) / •
                line.replace(Regex("^\\s*(\\d+\\s*[.、)．]|[-*•·])\\s*"), "")
            }
            .map { it.trim().trim('"', '「', '」', '\'', '“', '”') }
            .filter { it.isNotBlank() }
            // 模型偶尔会先加一句"以下是三条建议："—— 含冒号结尾的短句通常是它
            .filterNot { it.endsWith("：") || it.endsWith(":") }
            .map { if (it.length > MAX_LINE_CHARS) it.take(MAX_LINE_CHARS) else it }
            .distinct()
            .take(MAX_SUGGESTIONS)
    }
}
