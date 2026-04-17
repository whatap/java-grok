---
name: maven-central-deploy
description: java-grok Maven Central 수동 배포 번들 생성
trigger: "maven central", "메이븐 배포", "central 배포", "수동 배포", "번들 생성"
---

# Maven Central 수동 배포

java-grok 라이브러리를 Maven Central Portal에 수동 업로드하기 위한 번들 ZIP을 생성합니다.

## 실행 조건
- `./gradlew test` 전체 통과 확인
- `./gradlew publishJavaGrokMavenPublicationToMavenLocal` 로컬 배포 완료
- GPG 서명 아티팩트(.asc) 생성됨

## 순서

### 1. 버전 확인
```bash
grep "VERSION" variable.gradle
```

### 2. 테스트 실행
```bash
./gradlew test
```

### 3. 로컬 Maven 배포 (GPG 서명 포함)
```bash
./gradlew publishJavaGrokMavenPublicationToMavenLocal
```

### 4. 번들 ZIP 생성
VERSION을 실제 버전으로 치환:
```bash
VERSION=$(grep "VERSION" variable.gradle | grep -o "'[^']*'" | tr -d "'")
M2=~/.m2/repository/io/github/whatap/java-grok/$VERSION
OUT=/tmp/java-grok-${VERSION}-bundle
rm -rf $OUT
mkdir -p $OUT/io/github/whatap/java-grok/$VERSION
DEST=$OUT/io/github/whatap/java-grok/$VERSION

for f in java-grok-${VERSION}.jar java-grok-${VERSION}-sources.jar java-grok-${VERSION}-javadoc.jar java-grok-${VERSION}.pom; do
  cp "$M2/$f" "$M2/${f}.asc" "$DEST/"
  md5 -q "$DEST/$f" > "$DEST/${f}.md5"
  shasum -a 1 "$DEST/$f" | awk '{print $1}' > "$DEST/${f}.sha1"
done

cd $OUT && zip -r /tmp/java-grok-${VERSION}-bundle.zip .
echo "번들 생성 완료: /tmp/java-grok-${VERSION}-bundle.zip"
```

### 5. 사용자에게 안내
```
Central Portal 업로드 방법:
1. https://central.sonatype.com → Deployments → Upload
2. Deployment Name: java-grok-{VERSION}-bundle.zip
3. ZIP 파일 업로드 → 검증 통과 → Publish
```

### 6. 배포 확인 후 git tag
```bash
git tag v${VERSION}
git push --tags
```
