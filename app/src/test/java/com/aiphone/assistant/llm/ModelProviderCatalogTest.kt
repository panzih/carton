package com.aiphone.assistant.llm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// sdk 必须写死：CI 里 Robolectric 以 offline 模式从固定目录取 android-all
// （见 app/build.gradle.kts 的 robolectricDeps）。不写就按 targetSdk 35 找，
// 那个制品不在离线目录里，会报成"找不到 android-all"。
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ModelProviderCatalogTest {

    private val json = """
        [
          {
            "id": "deepseek",
            "name": "DeepSeek",
            "baseUrl": "https://api.deepseek.com/v1",
            "category": "apikey",
            "models": ["deepseek-chat", "deepseek-reasoner"]
          },
          {
            "id": "broken",
            "name": "",
            "baseUrl": "",
            "models": ["ignored"]
          }
        ]
    """.trimIndent()

    @Test
    fun `解析目录并丢掉没有地址的条目`() {
        val providers = ModelProviderCatalog.parse(json)

        assertEquals(1, providers.size)
        val deepseek = providers.single()
        assertEquals("deepseek", deepseek.id)
        assertEquals("DeepSeek", deepseek.name)
        assertEquals("https://api.deepseek.com/v1", deepseek.baseUrl)
        assertEquals(listOf("deepseek-chat", "deepseek-reasoner"), deepseek.models)
    }

    @Test
    fun `按接口地址匹配供应商并忽略结尾斜杠与大小写`() {
        val providers = ModelProviderCatalog.parse(json)

        assertTrue(ModelProviderCatalog.find(providers, "https://api.deepseek.com/v1/") != null)
        assertTrue(ModelProviderCatalog.find(providers, "HTTPS://API.DEEPSEEK.COM/V1") != null)
        assertNull(ModelProviderCatalog.find(providers, "https://example.com/v1"))
        assertNull(ModelProviderCatalog.find(providers, ""))
    }
}
