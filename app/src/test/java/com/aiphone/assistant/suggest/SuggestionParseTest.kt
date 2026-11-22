package com.aiphone.assistant.suggest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 「接下来你可能想做什么」的解析（0.17.9）。
 *
 * 模型的输出很脏：可能带编号、带项目符号、带引号、先来一句
 * "以下是三条建议："，也可能直接回「无」。这层容错全靠这里，
 * 真实调用里根本调不动。
 */
class SuggestionParseTest {

    @Test
    fun `三行纯文本解析成三条`() {
        val r = SuggestionProvider.parse("打开设置看看存储\n把亮度调最高\n给妈妈发我到家了")
        assertEquals(3, r.size)
        assertEquals("打开设置看看存储", r[0])
    }

    @Test
    fun `带编号前缀要剥掉`() {
        val r = SuggestionProvider.parse("1. 打开微信\n2、发条消息\n3) 退出")
        assertEquals(listOf("打开微信", "发条消息", "退出"), r)
    }

    @Test
    fun `项目符号要剥掉`() {
        val r = SuggestionProvider.parse("- 打开设置\n* 调亮度\n• 退出")
        assertEquals(listOf("打开设置", "调亮度", "退出"), r)
    }

    @Test
    fun `引号要剥掉`() {
        val r = SuggestionProvider.parse("「打开设置」\n\"调亮度\"")
        assertEquals(listOf("打开设置", "调亮度"), r)
    }

    @Test
    fun `回无时一条都不给`() {
        assertTrue(SuggestionProvider.parse("无").isEmpty())
        assertTrue(SuggestionProvider.parse("无\n这次任务看不出方向").isEmpty())
        assertTrue(SuggestionProvider.parse("无（任务只是验证）").isEmpty())
    }

    @Test
    fun `空回复不给建议`() {
        assertTrue(SuggestionProvider.parse("   \n  ").isEmpty())
    }

    @Test
    fun `最多三条`() {
        val r = SuggestionProvider.parse("一\n二\n三\n四\n五")
        assertEquals(SuggestionProvider.MAX_SUGGESTIONS, r.size)
    }

    @Test
    fun `重复的只留一条`() {
        val r = SuggestionProvider.parse("打开设置\n打开设置\n调亮度")
        assertEquals(listOf("打开设置", "调亮度"), r)
    }

    @Test
    fun `以冒号结尾的引导句要丢掉`() {
        val r = SuggestionProvider.parse("以下是三条建议：\n打开设置\n调亮度")
        assertEquals(listOf("打开设置", "调亮度"), r)
    }

    @Test
    fun `超长的一行要截断`() {
        val long = "打".repeat(200)
        val r = SuggestionProvider.parse(long)
        assertEquals(1, r.size)
        assertTrue("单条不该无限长", r[0].length <= 60)
    }

    @Test
    fun `空行不影响结果`() {
        val r = SuggestionProvider.parse("打开设置\n\n\n调亮度\n\n")
        assertEquals(listOf("打开设置", "调亮度"), r)
    }
}
