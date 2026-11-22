package com.aiphone.assistant.memory

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 归纳器的结果解析（0.17.5）。
 *
 * ## 这一条为什么要单独钉
 *
 * 原先的兜底逻辑是：模型回「无」→ 拿用户的任务原文塞进记忆文件。
 * 当时的前提是归纳器只拿得到「任务 + 结论」两句话 —— 那种输入下它
 * 当然只能回「无」，而兜底会把「打开设置看看存储」这种任务原文
 * 当成一条记忆存下来。
 *
 * 现在主循环的思考/动作/错误也进 turns 了，模型回「无」是看过过程之后
 * 的正常判断，必须尊重；继续兜底等于保证每次任务都往记忆文件里塞垃圾，
 * 而记忆只增不减，垃圾会挤掉以后真正有用的条目。
 *
 * 但**空响应仍然是故障**，不能当成「没什么可记」静默放过 ——
 * 那样接口挂了用户也不会知道。
 */
class DistillParseTest {

    @Test
    fun `回无时不写入记忆`() {
        val r = LlmDistiller.parseDistill("无")
        assertTrue("回「无」应当是放弃写入，实际=$r", r is DistillResult.GaveUp)
    }

    @Test
    fun `无后面跟原因也不写入`() {
        val r = LlmDistiller.parseDistill("无\n这次只是打开了一个设置页，没有新信息")
        assertTrue("「无」带解释也应当放弃写入，实际=$r", r is DistillResult.GaveUp)
    }

    @Test
    fun `无括号形式也不写入`() {
        val r = LlmDistiller.parseDistill("无（和已有记忆重复）")
        assertTrue("「无（…）」应当放弃写入，实际=$r", r is DistillResult.GaveUp)
    }

    @Test
    fun `空响应按故障处理而不是当成没有内容`() {
        val r = LlmDistiller.parseDistill("   \n  ")
        assertTrue("空响应应当 GaveUp，实际=$r", r is DistillResult.GaveUp)
        val reason = (r as DistillResult.GaveUp).reason
        assertTrue(
            "空响应的原因要说成「空内容」而不是「没有值得记的」，实际=$reason",
            reason.contains("空"),
        )
    }

    @Test
    fun `正常回复解析成标题加正文`() {
        val r = LlmDistiller.parseDistill(
            "常用设置与微信\n\n- 常打开系统设置\n- 会用微信发消息",
        )
        assertTrue("应当是 Entry，实际=$r", r is DistillResult.Entry)
        val e = (r as DistillResult.Entry).entry
        assertEquals("常用设置与微信", e.title)
        assertTrue("正文要保留列表", e.body.contains("常打开系统设置"))
    }

    @Test
    fun `标题带 markdown 前缀要清掉`() {
        val r = LlmDistiller.parseDistill("## 用户偏好\n\n- 喜欢深色模式")
        val e = (r as DistillResult.Entry).entry
        assertEquals("用户偏好", e.title)
    }

    @Test
    fun `只有一行时正文回退成那一行`() {
        val r = LlmDistiller.parseDistill("常用微信")
        val e = (r as DistillResult.Entry).entry
        assertEquals("常用微信", e.title)
        assertEquals("只有一行时正文不该为空", "常用微信", e.body)
    }
}
