# Game Launcher

Android cihazdaki oyunları sade bir Material 3 arayüzünde listeleyen oyun başlatıcı.

- Oyun simgeleri ve isimleri; tek dokunuşla başlatma.
- Sistem simge maskesi; bulunamazsa köşeleri yuvarlatılmış kare.
- Açık/koyu tema ve Android 12+ dinamik renk desteği.
- Android 6.0 (API 23) ve üzeri.

Oyun tespiti uygulamanın Android oyun kategorisine veya eski oyun bayrağına dayanır. Kendisini oyun olarak tanımlamayan uygulamalar listelenmeyebilir.

## Derleme

Projeyi Android Studio ile açın, SDK 37 kurulu olduğundan emin olun ve Gradle eşitlemesini tamamlayın.

```shell
./gradlew assembleRelease lintRelease
```

Windows üzerinde `gradlew.bat` kullanın. Dağıtım için Android Studio içinden **Generate Signed Bundle / APK** ile kendi anahtarınızla imzalayın. İmza anahtarları ve yerel ayarlar depoya dahil edilmez.

## İndirme

İmzalı APK, GitHub Releases bölümünde paylaşılır.

## Logo

SVG kaynak: `design/game-launcher-logo.svg`.
