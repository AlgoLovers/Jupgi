## 무엇을 / 왜

<!-- 관측 가능한 결과 하나. "개선"이 아니라 무엇이 바뀌는지. -->

## 체크리스트

- [ ] 한 PR = 한 논리적 변경 (수직 슬라이스)
- [ ] `./gradlew testDebugUnitTest` 초록불 (폴딩 도메인 변경 시 테스트 동반)
- [ ] `./gradlew :app:assembleDebug` 성공
- [ ] `./gradlew detekt` 신규 심각 위반 없음
- [ ] UI/3D 변경이면 `/emu-qa` 라이트·다크 스크린샷 확인 (첨부)
- [ ] 작품 추가/수정이면 `fold-auditor` 감사 통과
- [ ] 비밀(keystore/local.properties)·대용량 바이너리(APK/AAB) 미포함
- [ ] `domain/`에 `android.*` import 없음 (순수 Kotlin)

## 스크린샷 (UI 변경 시)

| 라이트 | 다크 |
|---|---|
|  |  |
