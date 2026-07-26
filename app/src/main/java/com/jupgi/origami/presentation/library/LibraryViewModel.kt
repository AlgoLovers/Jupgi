package com.jupgi.origami.presentation.library

import androidx.lifecycle.ViewModel
import com.jupgi.origami.domain.model.OrigamiCategory
import com.jupgi.origami.domain.model.OrigamiModel
import com.jupgi.origami.domain.repository.OrigamiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** 카테고리 섹션 하나 — 비어 있으면 화면이 "준비 중"으로 그린다. */
data class LibrarySection(
    val category: OrigamiCategory,
    val models: List<OrigamiModel>,
)

@HiltViewModel
class LibraryViewModel
    @Inject
    constructor(
        repository: OrigamiRepository,
    ) : ViewModel() {
        /** 카테고리 enum 선언 순서대로, 작품은 난이도순. 정적 컨텐츠라 상태 흐름이 필요 없다. */
        val sections: List<LibrarySection> =
            run {
                val byCategory = repository.models().groupBy { it.category }
                OrigamiCategory.entries.map { category ->
                    LibrarySection(
                        category = category,
                        models = byCategory[category].orEmpty().sortedBy { it.difficulty },
                    )
                }
            }
    }
