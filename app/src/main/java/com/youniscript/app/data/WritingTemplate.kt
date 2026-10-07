package com.youniscript.app.data

import org.json.JSONArray
import org.json.JSONObject

data class WritingTemplate(
    val id: String,
    val title: String,
    val body: String,
)

object WritingTemplateCodec {
    fun encode(templates: List<WritingTemplate>): String = JSONArray().apply {
        templates.take(100).forEach { template ->
            put(JSONObject().put("id", template.id).put("title", template.title.take(80)).put("body", template.body.take(8000)))
        }
    }.toString()

    fun decode(json: String?): List<WritingTemplate> = runCatching {
        val source = JSONArray(json ?: "[]")
        buildList {
            for (index in 0 until minOf(source.length(), 100)) {
                val item = source.optJSONObject(index) ?: continue
                val id = item.optString("id").take(100)
                val title = item.optString("title").trim().take(80)
                val body = item.optString("body").take(8000)
                if (id.isNotBlank() && title.isNotBlank()) add(WritingTemplate(id, title, body))
            }
        }.distinctBy { it.id }
    }.getOrDefault(emptyList())
}
