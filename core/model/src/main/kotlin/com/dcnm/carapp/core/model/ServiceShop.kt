package com.dcnm.carapp.core.model

import kotlinx.serialization.Serializable

/** 번들 정비소 (POI 텍스트 검색 API가 3rd party에 닫혀 있어 좌표를 앱에 동봉). */
@Serializable
data class ServiceShop(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val phone: String? = null,
    val preferred: Boolean = false,
)
