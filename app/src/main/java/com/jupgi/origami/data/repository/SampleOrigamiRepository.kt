package com.jupgi.origami.data.repository

import com.jupgi.origami.data.sample.DemoOrigami
import com.jupgi.origami.domain.model.OrigamiModel
import com.jupgi.origami.domain.repository.OrigamiRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * M1 구현 — 하드코딩된 데모 작품 1개. 이후 assets 의 FOLD/JSON 번들을 임포트하는 구현으로 교체.
 */
@Singleton
class SampleOrigamiRepository
    @Inject
    constructor() : OrigamiRepository {
        private val all: List<OrigamiModel> by lazy { listOf(DemoOrigami.model()) }

        override fun models(): List<OrigamiModel> = all

        override fun modelById(id: String): OrigamiModel? = all.firstOrNull { it.id == id }
    }
