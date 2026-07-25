---
name: release-aab
description: Play Store 업로드용 서명 AAB를 빌드하고 서명·버전을 검증해 바탕화면에 놓는다. 사용자가 "AAB 만들어줘", "스토어 올릴 빌드", "릴리스 빌드" 등을 요청할 때 사용.
disable-model-invocation: true
---

# /release-aab — 서명 릴리스 번들 생성

> ⚠️ **선행 조건(최초 1회)**: 아직 업로드 키스토어가 없다. 릴리스 서명 전에 사람이 업로드 키를
> 생성하고(`keytool -genkeypair ...`), `keystore.properties`(gitignore)에 경로·비밀번호를 적고,
> **키스토어 2파일을 안전한 곳(구글 드라이브 등)에 백업**해야 한다. 업로드 키 분실 = 앱 업데이트 불가.
> 키가 없으면 릴리스 빌드는 디버그 키로 폴백되어 **스토어 업로드 불가**(빌드 자체는 통과).

## 절차

1. **main 최신 확인**: `git fetch origin && git checkout main && git pull`. 릴리스는 항상 main에서
   빌드한다(develop 미머지 변경이 섞이면 안 됨 — 필요하면 release PR 먼저).
2. **버전 확인**: `app/build.gradle.kts`의 versionCode/versionName. Play Console은 같은 versionCode
   재업로드를 거부하므로, 이미 업로드한 적 있으면 +1 올리고 커밋(PR) 후 진행.
3. **빌드**: `./gradlew -q :app:bundleRelease` (2~5분).
   `keystore.properties`가 없으면 디버그 키로 폴백되니 **반드시 서명 검증을 통과해야 산출물로 인정**.
4. **서명 검증** (필수):
   ```
   unzip -p app/build/outputs/bundle/release/app-release.aab META-INF/*.RSA | keytool -printcert | grep SHA1
   ```
   기대 지문(업로드 키 생성 후 이 문서에 기록)과 일치해야 한다. 다르면 업로드 금지하고 키스토어 위치부터 확인.
5. **전달**: `cp app/build/outputs/bundle/release/app-release.aab ~/Desktop/jupgi-v<버전>-release.aab`
   후 사용자에게 파일명·크기·버전 보고.

## 주의

- 베타/실험 기능이 main에 머지돼 있는지, 스토어에 나가도 되는 상태인지 릴리스 전에 사용자와 확인.
- **큰 AAB는 텔레그램 첨부 금지**(봇 한계 50MB). GitHub 릴리스에 올려 링크만 전달:
  `gh release create <tag> app/build/outputs/bundle/release/app-release.aab#jupgi-<version>.aab --title "<제목>" --notes "<메모>" --prerelease`
