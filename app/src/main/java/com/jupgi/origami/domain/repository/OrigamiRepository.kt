package com.jupgi.origami.domain.repository

import com.jupgi.origami.domain.model.OrigamiModel

/**
 * 종이접기 작품을 공급한다. 구현은 data 계층(M1: 하드코딩 데모 → 이후 assets 의 FOLD/JSON 번들).
 * 의존 방향: presentation/data → domain (역방향 금지).
 */
interface OrigamiRepository {
    fun models(): List<OrigamiModel>

    fun modelById(id: String): OrigamiModel?
}
