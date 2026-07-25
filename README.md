# 접기 (Jupgi)

**3D로 따라 접는 종이접기 학습 앱.**

유튜브 영상·책으로 종이접기를 배우면 복잡해질수록 따라 접기가 어렵다. 접기는 3D 종이 모델을
**언제든 멈추고 · 한 단계씩 전/후진하고 · 자유롭게 돌려보며 · 단계마다 가이드선과 팁**을 제공해
이 페인 포인트를 해결한다.

## 어떻게 동작하나 (한 문장 아키텍처)

폴딩 기하(어떤 종이가 재생 위치 `progress`에서 어떤 모양인가)는 **순수 Kotlin domain**에서 결정적으로
계산(로드리게스 힌지 회전)하고, **3D 엔진은 그 메시를 그리기만** 한다. 이 분리 덕에 렌더러를 무손실
교체할 수 있다(현재 Compose Canvas 소프트 3D → 이후 SceneView/Filament). 상세: [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).

## 기술 스택

Kotlin · Jetpack Compose(M3) · Clean Architecture 3계층 + MVVM · Hilt · minSdk 26 · JDK 17 ·
Gradle KTS + Version Catalog.

## 자주 쓰는 명령

```bash
./gradlew testDebugUnitTest      # 폴딩 수학 유닛 테스트 (게이트)
./gradlew :app:assembleDebug     # 디버그 APK
./gradlew spotlessApply detekt   # 포맷 + 정적분석
./gradlew installDebug           # 연결된 기기/에뮬레이터에 설치
```

SDK 경로는 `local.properties`의 `sdk.dir`(커밋 안 됨). JDK 17.

## 문서

- 프로젝트 헌법: [`CLAUDE.md`](CLAUDE.md) · 상태: [`STATE.md`](STATE.md) · 로드맵: [`docs/ROADMAP.md`](docs/ROADMAP.md)
- 폴딩 모델: [`docs/FOLD_MODEL.md`](docs/FOLD_MODEL.md) · 디자인: [`docs/DESIGN.md`](docs/DESIGN.md)

## 현재 상태

M0 골격 — 순수 Kotlin 폴딩 도메인(+유닛테스트) + Compose Canvas 3D 뷰어(스크럽·회전·가이드선) +
데모 작품("반으로 두 번 접기"). 다음: 작품 라이브러리·FOLD 임포터·Filament 전환([`docs/ROADMAP.md`](docs/ROADMAP.md)).
