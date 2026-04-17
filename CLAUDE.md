# java-grok

WhaTap Grok 패턴 파싱 라이브러리 (io.krakens 포크).

## 빌드

```bash
./gradlew test                    # 테스트 실행
./gradlew publishJavaGrokMavenPublicationToMavenLocal  # 로컬 m2 배포
```

- Java 8 호환
- GPG 서명 필요 (로컬 배포 시)

## 패턴 리소스 수정 규칙

**패턴 파일(`resources/patterns/*`)을 수정할 때 반드시 샘플 로그도 함께 업데이트:**

1. `src/main/resources/patterns/{name}` — 패턴 정의
2. `src/main/resources/samples/{name}.json` — 해당 패턴의 샘플 로그 (**반드시 동기화**)
3. `src/test/java/io/whatap/grok/api/patterns/{Name}PatternTest.java` — 패턴 테스트

### 샘플 로그 JSON 형식
```json
{
  "PATTERN_NAME": "실제 파싱 가능한 샘플 로그 문자열",
  "ANOTHER_PATTERN": "또 다른 샘플"
}
```

### 검증
- `PatternSampleTest`가 모든 샘플 로그의 파싱 성공을 검증
- 패턴 추가/수정 후 반드시 `./gradlew test` 실행

## 주요 API

- `PatternType` enum — 패턴 타입 정의 (Category 포함)
- `PatternRepository.getInstance()` — 싱글톤 패턴 매니저
  - `loadPatterns(PatternType)` — 패턴 정의 로드
  - `getSampleLogs(PatternType)` — 샘플 로그 로드
  - `getPatternTypesByCategory()` — 카테고리별 분류
- `GrokCompiler` — 패턴 컴파일러
  - `registerPatterns(PatternType...)` — 선택적 등록
  - `registerAllPatterns()` — 전체 등록

## 배포 좌표

```
groupId: io.github.whatap
artifactId: java-grok
```
