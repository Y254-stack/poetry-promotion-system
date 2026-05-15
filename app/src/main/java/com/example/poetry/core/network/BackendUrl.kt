package com.example.poetry.core.network

object BackendUrl {

    /** 将后端返回的相对路径（如 `/uploads/avatars/x.png`）或完整 URL 转为可请求的绝对地址。 */
    fun toAbsolute(urlOrPath: String?): String? {
        val raw = urlOrPath?.trim().orEmpty()
        if (raw.isEmpty()) return null
        if (raw.startsWith("http://", ignoreCase = true) || raw.startsWith("https://", ignoreCase = true)) {
            return raw
        }
        val base = NetworkModule.apiOrigin()
        return if (raw.startsWith("/")) base + raw else "$base/$raw"
    }
}
