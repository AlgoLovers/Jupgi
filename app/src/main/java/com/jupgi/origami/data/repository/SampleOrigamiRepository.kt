package com.jupgi.origami.data.repository

import com.jupgi.origami.data.sample.DemoOrigami
import com.jupgi.origami.data.sample.FanOrigami
import com.jupgi.origami.data.sample.GateFoldOrigami
import com.jupgi.origami.data.sample.PaperPlaneOrigami
import com.jupgi.origami.domain.model.OrigamiModel
import com.jupgi.origami.domain.repository.OrigamiRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * M1 구현 — 코드로 저작한 전통(퍼블릭 도메인) 작품들.
 * 모든 작품은 `FoldInvariants.sweep()` 통과가 수용 기준(LOOP-2)이며 테스트로 고정된다.
 * 이후 assets 의 FOLD/JSON 번들을 임포트하는 구현으로 확장한다.
 */
@Singleton
class SampleOrigamiRepository
    @Inject
    constructor() : OrigamiRepository {
        private val all: List<OrigamiModel> by lazy {
            listOf(
                DemoOrigami.model(),
                GateFoldOrigami.model(),
                FanOrigami.model(),
                PaperPlaneOrigami.model(),
            )
        }

        override fun models(): List<OrigamiModel> = all

        override fun modelById(id: String): OrigamiModel? = all.firstOrNull { it.id == id }
    }
