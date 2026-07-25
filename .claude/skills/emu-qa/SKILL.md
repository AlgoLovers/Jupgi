---
name: emu-qa
description: 에뮬레이터에 앱을 띄워 스크린샷으로 3D 뷰어·UI를 눈으로 검증하는 QA 루프. 폴딩 렌더링·스크럽·회전·가이드선 확인, UI 변경 후, 스토어 스크린샷 제작에 사용.
---

# /emu-qa — 에뮬레이터 스크린샷 QA 루프

3D 폴딩은 단위 테스트로 잡히지 않는다(정점 좌표는 맞아도 컬링·법선·깊이 정렬·z-fighting·카메라가
틀릴 수 있다). 실제 화면을 눈으로 봐야 안다. 실증: 이 골격의 뷰어(반접기 3D·양면색·계곡 파선)를
이 루프의 라이트/다크 스샷으로 검증했다.

## 부팅 (함정 주의)

```
export PATH="$PATH:$HOME/Library/Android/sdk/emulator:$HOME/Library/Android/sdk/platform-tools"
nohup emulator -avd Pixel_API_TiramisuPrivacySandbox -no-window -no-audio -no-boot-anim -no-snapshot -gpu host &
adb wait-for-device
# ⚠️ macOS엔 `timeout`이 없다(coreutils gtimeout). 아래 순수 루프를 쓴다:
adb shell 'while [ "$(getprop sys.boot_completed)" != "1" ]; do sleep 2; done'
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0
```

- **`-gpu host` 필수** — swiftshader면 screencap이 흰 화면(8KB)만 뱉는다. 정상 스샷은 100KB+.
- 폰=`Pixel_API_TiramisuPrivacySandbox`(1080×1920), 태블릿=`ddakpul_tablet`(1280×800).
  `tablet_pc` AVD는 QEMU 행으로 멈춤 — 쓰지 말 것.

## 루프

1. `./gradlew -q :app:assembleDebug` → `adb install -r app/build/outputs/apk/debug/app-debug.apk`
2. 실행: `adb shell am start -n com.jupgi.origami/.MainActivity`
3. 캡처: `adb exec-out screencap -p > shot.png` → Read로 눈 확인.
4. **3D 폴딩 검증(핵심)**: 재생 위치를 바꿔가며(스텝 0/중간/완료) 접힘이 맞는지, 앞/뒷면 색이
   구분되는지, 깊이 정렬(겹침)이 맞는지, 계곡=파선/산=일점쇄선 가이드선이 맞는지 본다.
   조작은 `adb shell uiautomator dump` → bounds 파싱 → `input tap`(다음/이전 버튼) 또는
   `input swipe`(슬라이더 스크럽·종이 회전 드래그).
5. 문제 발견 → 코드 수정 → 1로.

## ⚠️ 테마·기기 매트릭스 (UI 변경이면 필수 — docs/DESIGN.md)

- **라이트와 다크 둘 다**: `adb shell cmd uimode night yes`(다크) / `no`(라이트).
  주의: 다크에서 종이 뒷면색이 배경과 가까우면 접힌 면 대비가 약해진다(알려진 폴리시 항목).
- 레이아웃 분기가 다르면 **폰과 태블릿 둘 다**(WindowSizeClass 분기·인셋 계열 사고는 한쪽에서만 난다).

## 함정 모음

- assets(작품 FOLD/JSON) 교체 후엔 `adb shell pm clear com.jupgi.origami` — `-r` 재설치만으론
  재시딩 안 될 수 있음(Room/캐시 도입 후).
- 스토어 스크린샷은 알파 제거(PIL convert RGB) 후 `docs/store/screenshots/`에 저장.
- 끝나면 `adb emu kill`로 정리.
