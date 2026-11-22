package com.aiphone.assistant.memory

import com.aiphone.assistant.llm.ChatTurn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 记忆并进模型历史（0.17.7）。
 *
 * ## 为什么是"普通消息"而不是只写文件
 *
 * 记忆文件只在**新开上下文**时注入系统提示词。那之后这段对话里的模型
 * 就再也看不到新的记忆了 —— 要等用户手动清空上下文才生效。
 * 而刚归纳出来的东西，往往正是下一句话要用的。
 *
 * ## 为什么必须追加在末尾
 *
 * 服务端的上下文缓存按**前缀逐字节匹配**。在末尾追加一条不会改动任何
 * 已有字节，前面那段的缓存照常命中；在中间插入或改动任何一条，
 * 从那里往后全部按未命中计价。
 *
 * ## 为什么是 assistant
 *
 * 这不是用户说的话。标成 user 会让模型以为用户在给它下新指令。
 */
class MemoryIntoHistoryTest {

    private fun memoryTurn(title: String, body: String) = ChatTurn(
        ChatTurn.ASSISTANT,
        "（记忆）$title\n\n$body".trim(),
    )

    @Test
    fun `记忆追加在末尾且不改动已有消息`() {
        val history = listOf(
            ChatTurn(ChatTurn.USER, "打开设置"),
            ChatTurn(ChatTurn.ASSISTANT, "好的"),
        )
        val merged = history + memoryTurn("常用设置", "- 常打开系统设置")

        assertEquals("原有消息必须原样保留在前", history, merged.take(2))
        assertEquals("新记忆在最后一条", "（记忆）常用设置\n\n- 常打开系统设置", merged.last().text)
    }

    @Test
    fun `记忆用 assistant 角色而不是 user`() {
        val turn = memoryTurn("常用设置", "- 常打开系统设置")
        assertTrue(
            "记忆不是用户下发的指令，必须是 assistant",
            turn.role == ChatTurn.ASSISTANT,
        )
    }

    @Test
    fun `记忆消息带标题和正文`() {
        val turn = memoryTurn("常用设置与微信", "- 常打开系统设置\n- 用微信发消息")
        assertTrue("要带标题", turn.text.contains("常用设置与微信"))
        assertTrue("要带正文", turn.text.contains("常打开系统设置"))
        assertTrue("要标出来源是记忆", turn.text.startsWith("（记忆）"))
    }

    @Test
    fun `没有正文时不留下多余空行`() {
        val turn = memoryTurn("只有标题", "")
        assertEquals("（记忆）只有标题", turn.text)
    }
}
