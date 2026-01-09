Here is the comprehensive **SurveySDK Installation & Usage Guide** for developers who will use your SDK. This document includes the specific package manager instructions (JitPack, NPM, Pub) you requested.

---

# 📚 SurveySDK - Developer Guide / Geliştirici Kılavuzu

---

## 🇬🇧 ENGLISH DOCUMENTATION

### 📦 Part 1: Installation

#### 1. Android Native (Gradle / JitPack)

Add the JitPack repository to your build file.

**`settings.gradle`** (or project level `build.gradle`):

```gradle
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' } // <--- Add this
    }
}

```

**`build.gradle` (Module: app):**

```gradle
dependencies {
    // Replace 'User' and 'Repo' with your GitHub username and repository name
    implementation 'com.github.User:Repo:1.0.0'
}

```

#### 2. React Native (NPM)

Install the package via npm or yarn.

```bash
npm install surveysdk-react-native
# or
yarn add surveysdk-react-native

```

*Note: Since the module uses native code, you might need to rebuild your android project:*

```bash
cd android && ./gradlew clean

```

#### 3. Flutter (Pub)

Add the dependency to your `pubspec.yaml`.

```yaml
dependencies:
  flutter:
    sdk: flutter
  # Add the SDK here
  surveysdk_flutter:
    git:
      url: https://github.com/User/Repo.git
      path: surveysdk_flutter
    # OR if published to pub.dev:
    # surveysdk_flutter: ^1.0.0

```

Run `flutter pub get` to install.

---

### 🚀 Part 2: Quick Start

#### 🤖 Android Native (Kotlin)

**`MainActivity.kt`**:

```kotlin
import com.example.surveysdk.SurveySDK

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // 1. Initialize
    SurveySDK.initialize(this, "YOUR_API_KEY")
    
    // 2. Enable Auto-Detection
    SurveySDK.getInstance().autoSetup(this)
}

```

#### ⚛️ React Native (JavaScript)

**`App.js`**:

```javascript
import { NativeModules } from 'react-native';
const { SurveySDK } = NativeModules;

// 1. Initialize
React.useEffect(() => {
  SurveySDK.initialize("YOUR_API_KEY");
  SurveySDK.autoSetup();
}, []);

```

#### 💙 Flutter (Dart)

**`main.Here is the continuation and completion of the document, including the Turkish translation.

```dart
  // 1. Initialize
  await SurveySdkFlutter.initialize('YOUR_API_KEY');
  await SurveySdkFlutter.autoSetup();
  
  runApp(const MyApp());
}

```

---

### 🎮 Part 3: Usage & Triggers

How to mark your UI elements so the SDK can detect them.

#### 🤖 Android Native

**Button Trigger:** Add a `tag` to your XML view.

```xml
<Button
    android:id="@+id/btn_buy"
    android:text="Buy Now"
    android:tag="checkout_button" /> ```

#### ⚛️ React Native
**Button Trigger:** Use the `nativeID` prop.
```javascript
<TouchableOpacity nativeID="checkout_button">
  <Text>Buy Now</Text>
</TouchableOpacity>

```

**Navigation:** Add listener to `NavigationContainer`.

```javascript
<NavigationContainer onStateChange={(s) => SurveySDK.triggerNavigationSurvey(s.routes[s.index].name)}>

```

#### 💙 Flutter

**Button Trigger:** Wrap buttons with `SurveyTrigger`.

```dart
SurveyTrigger(
  triggerId: "checkout_button",
  child: ElevatedButton(onPressed: () {}, child: Text("Buy")),
)

```

**Scroll Trigger:** Use `SurveyScrollView`.

```dart
SurveyScrollView(
  threshold: 500,
  child: Column(...),
)

```

**Navigation:** Add observer to `MaterialApp`.

```dart
MaterialApp(navigatorObservers: [SurveyNavigationObserver()], home: Home())

```

---

---

## 🇹🇷 TÜRKÇE DOKÜMANTASYON

### 📦 Bölüm 1: Kurulum (Installation)

#### 1. Android Native (Gradle / JitPack)

JitPack deposunu projenize ekleyin.

**`settings.gradle`**:

```gradle
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' } // <--- Bunu ekleyin
    }
}

```

**`build.gradle` (Module: app):**

```gradle
dependencies {
    // 'User' ve 'Repo' kısımlarını GitHub kullanıcı adınızla değiştirin
    implementation 'com.github.User:Repo:1.0.0'
}

```

#### 2. React Native (NPM)

Paketi npm veya yarn ile kurun.

```bash
npm install surveysdk-react-native
# veya
yarn add surveysdk-react-native

```

#### 3. Flutter (Pub)

`pubspec.yaml` dosyanıza ekleyin.

```yaml
dependencies:
  flutter:
    sdk: flutter
  # SDK'yı buraya ekleyin
  surveysdk_flutter:
    git:
      url: https://github.com/User/Repo.git
      path: surveysdk_flutter

```

Kurmak için `flutter pub get` çalıştırın.

---

### 🚀 Bölüm 2: Hızlı Başlangıç

#### 🤖 Android Native (Kotlin)

**`MainActivity.kt`**:

```kotlin
import com.example.surveysdk.SurveySDK

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // 1. Başlatma
    SurveySDK.initialize(this, "API_ANAHTARINIZ")
    
    // 2. Otomatik Taramayı Aç
    SurveySDK.getInstance().autoSetup(this)
}

```

#### ⚛️ React Native (JavaScript)

**`App.js`**:

```javascript
import { NativeModules } from 'react-native';
const { SurveySDK } = NativeModules;

// 1. Başlatma
React.useEffect(() => {
  SurveySDK.initialize("API_ANAHTARINIZ");
  SurveySDK.autoSetup();
}, []);

```

#### 💙 Flutter (Dart)

**`main.dart`**:

```dart
import 'package:surveysdk_flutter/surveysdk_flutter.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  
  // 1. Başlatma
  await SurveySdkFlutter.initialize('API_ANAHTARINIZ');
  await SurveySdkFlutter.autoSetup();
  
  runApp(const MyApp());
}

```

---

### 🎮 Bölüm 3: Kullanım ve Tetikleyiciler

Arayüz elemanlarını (butonlar, sayfalar) SDK'nın algılayabilmesi için nasıl işaretlemelisiniz?

#### 🤖 Android Native

**Buton Tetikleyici:** XML görünümüne `tag` ekleyin.

```xml
<Button
    android:id="@+id/btn_buy"
    android:text="Satın Al"
    android:tag="checkout_button" /> ```

#### ⚛️ React Native
**Buton Tetikleyici:** `nativeID` özelliğini kullanın.
```javascript
<TouchableOpacity nativeID="checkout_button">
  <Text>Satın Al</Text>
</TouchableOpacity>

```

**Navigasyon:** `NavigationContainer` içine dinleyici ekleyin.

```javascript
<NavigationContainer onStateChange={(s) => SurveySDK.triggerNavigationSurvey(s.routes[s.index].name)}>

```

#### 💙 Flutter

**Buton Tetikleyici:** Butonları `SurveyTrigger` ile sarmalayın.

```dart
SurveyTrigger(
  triggerId: "checkout_button",
  child: ElevatedButton(onPressed: () {}, child: Text("Satın Al")),
)

```

**Scroll Tetikleyici:** `SurveyScrollView` kullanın.

```dart
SurveyScrollView(
  threshold: 500, // 500 piksel kaydırınca tetikler
  child: Column(...),
)

```

**Navigasyon:** `MaterialApp`'e gözlemci (observer) ekleyin.

```dart
MaterialApp(navigatorObservers: [SurveyNavigationObserver()], home: Home())

```