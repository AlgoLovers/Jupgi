package com.jupgi.origami.domain.model

/**
 * 작품 카테고리. 라이브러리 화면이 이 순서대로 섹션을 그린다.
 * 표시 이름은 presentation 의 strings.xml 이 담당한다(도메인은 식별자만).
 *
 * 콘텐츠 전략(STATE.md): 전통(퍼블릭 도메인) 작품으로 카테고리를 하나씩 채워간다.
 * 아직 작품이 없는 카테고리도 라이브러리에 "준비 중"으로 보여 로드맵을 드러낸다.
 */
enum class OrigamiCategory {
    /** 기본 접기 연습 — 계곡/산/아코디언 등 기본기. */
    BASICS,

    /** 부채·상자·컵 같은 실용 작품. */
    PRACTICAL,

    /** 비행기·배 같은 탈것. */
    VEHICLES,

    /** 학·개구리 같은 동물 — 꽃잎접기·역접기가 필요해 M2 에서 채운다. */
    ANIMALS,
}
