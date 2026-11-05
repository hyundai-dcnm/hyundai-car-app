package com.dcnm.carapp.core.navi

import android.content.Context
import com.dcnm.carapp.core.model.ServiceShop
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * assets/service_shops.json 에 동봉한 정비소 목록.
 * 출처: 공공데이터포털 자동차정비업체 표준데이터 (시연 지역 20~50곳만 추려 넣는다).
 */
class BundledShopProvider(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }
    private val cache: List<ServiceShop> by lazy {
        context.assets.open("service_shops.json").bufferedReader().use { json.decodeFromString(ListSerializer(ServiceShop.serializer()), it.readText()) }
    }
    fun all(): List<ServiceShop> = cache
}
