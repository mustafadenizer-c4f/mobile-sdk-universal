# 🇬🇧 SurveySDK - Technical Documentation (v2.1)

## 1. Architecture & File Structure

The project follows a **3-Tier Architecture** to separate business logic from platform-specific code.

### 📂 Directory Hierarchy

**1. Native Core Module (The Brain)**
Handles survey rules, API requests, queues, and displaying views (Dialogs/BottomSheets).

```text
/surveysdk/src/main/java/com/example/surveysdk/
├── SurveySDK.kt                  # 🧠 CORE SINGLETON. Manages all logic.
├── Config.kt                     # Data Models (SurveyConfig, Triggers).
├── core/SurveyPlatform.kt        # Interface contract.
└── android/AndroidSurveySDK.kt   # 🔌 WRAPPER. The interface React Native/Flutter talks to.

```

**2. React Native Bridge (The Scanner)**
Uses "Continuous Scanning" to detect UI changes in React Native.

```text
/surveysdk-react-native/android/src/.../reactnative/
├── SurveySDKPackage.kt
└── SurveySDKModule.kt            # 🕵️ SCANNER. Runs 'GlobalLayoutListener' to find views.

```

**3. Flutter Bridge (The Signal Receiver)**
Since Flutter draws its own pixels, this module receives signals from Dart widgets.

```text
/surveysdk_flutter/android/src/.../surveysdk_flutter/
├── SurveySdkFlutterPlugin.kt     # 📡 RECEIVER. Receives signals from Dart MethodChannel.

```

---

## 2. Execution Flow (How it works under the hood)

### Scenario A: Auto-Setup

What happens when `autoSetup()` is called?

1. **Platform Side (JS/Dart):** Calls `autoSetup`.
2. **Native Side:**
* **Android:** Attaches `ActivityLifecycleCallbacks` to track App Start/Exit.
* **React Native:** Starts a `GlobalLayoutListener` to scan the View Tree for `nativeID`.
* **Flutter:** Sets up the communication channel, waiting for signals.



### Scenario B: Button Click (Trigger Flow)

When a user clicks a button marked for a survey:

1. **User Action:** User touches the button.
2. **Detection:**
* **Android/RN:** The injected `OnTouchListener` intercepts the touch.
* **Flutter:** The `SurveyTrigger` widget captures the `onPointerUp` event.


3. **Signal:** The ID (e.g., `"btn_checkout"`) is sent to `SurveySDK.kt`.
4. **Core Logic:**
* Checks Config: Is there a survey for `"btn_checkout"`?
* Checks Rules: Is user excluded? Is cooling period active?


5. **Result:** If valid, the `SurveyDialogFragment` or `BottomSheet` is launched on top of the Activity.

---

## 3. Integration Guide

### 🤖 Android Native (Kotlin)

Direct access. No bridge needed.

```kotlin
// MainActivity.kt
SurveySDK.initialize(this, "API_KEY")
SurveySDK.getInstance().autoSetup(this)

// XML Layout
<Button android:tag="checkout_button" ... />

```

### ⚛️ React Native

Uses the **Magic Scanner** to find Native IDs.

**App.js:**

```javascript
import { NativeModules } from 'react-native';
const { SurveySDK } = NativeModules;

// 1. Init
useEffect(() => {
  SurveySDK.initialize("API_KEY");
  SurveySDK.autoSetup();
}, []);

// 2. Navigation
<NavigationContainer onStateChange={(state) => {
   const route = state.routes[state.index].name;
   SurveySDK.triggerNavigationSurvey(route);
}}>

// 3. UI
<TouchableOpacity nativeID="checkout_button">...</TouchableOpacity>

```

### 💙 Flutter (New!)

Uses **Smart Widgets** to signal the Native SDK.

**main.dart:**

```dart
import 'package:surveysdk_flutter/surveysdk_flutter.dart';

// 1. Init
await SurveySdkFlutter.initialize('API_KEY');
await SurveySdkFlutter.autoSetup();

// 2. Button Trigger
SurveyTrigger(
  triggerId: "checkout_button",
  child: ElevatedButton(child: Text("Buy"), onPressed: (){}),
)

// 3. Scroll Trigger
SurveyScrollView(
  threshold: 500,
  child: Column(...),
)

// 4. Navigation
MaterialApp(
  navigatorObservers: [SurveyNavigationObserver()],
  ...
)

```

---

## 4. Debugging & Testing

Filter Logcat by the tag: **`SurveySDK`**

* `👀 Continuous Scanning Started`: React Native scanner is active.
* `👆 Auto-Detected Click`: A click was caught and sent to Core.
* `✅ Found specific survey match`: Logic successful, survey opening.
* `❌ Cannot show survey`: Rules prevented display (Cooldown, Frequency Cap).

---

---

# 🇹🇷 SurveySDK - Teknik Dokümantasyon (v2.1)

## 1. Mimari ve Dosya Yapısı

Proje, iş mantığını platform kodlarından ayırmak için **3 Katmanlı Mimari** kullanır.

### 📂 Dizin Hiyerarşisi

**1. Native Core Modülü (Beyin)**
Anket kuralları, API istekleri, kuyruk yönetimi ve görünüm (Dialog/BottomSheet) buradadır.

```text
/surveysdk/src/main/java/com/example/surveysdk/
├── SurveySDK.kt                  # 🧠 CORE SINGLETON. Tüm mantık merkezi.
├── Config.kt                     # Veri Modelleri (SurveyConfig).
├── core/SurveyPlatform.kt        # Arayüz sözleşmesi.
└── android/AndroidSurveySDK.kt   # 🔌 WRAPPER. RN ve Flutter'ın konuştuğu kapı.

```

**2. React Native Bridge (Tarayıcı)**
React Native arayüzündeki değişimleri algılamak için "Sürekli Tarama" kullanır.

```text
/surveysdk-react-native/android/src/.../reactnative/
├── SurveySDKPackage.kt
└── SurveySDKModule.kt            # 🕵️ TARAYICI. 'GlobalLayoutListener' ile View ağacını gezer.

```

**3. Flutter Bridge (Sinyal Alıcı)**
Flutter kendi piksellerini çizdiği için, bu modül Dart widget'larından gelen sinyalleri dinler.

```text
/surveysdk_flutter/android/src/.../surveysdk_flutter/
├── SurveySdkFlutterPlugin.kt     # 📡 ALICI. Dart MethodChannel'dan gelen emirleri uygular.

```

---

## 2. Çalışma Mantığı ve Akış (Execution Flow)

### Senaryo A: Otomatik Kurulum (Auto-Setup)

`autoSetup()` çağrıldığında arka planda ne olur?

1. **Platform Tarafı (JS/Dart):** `autoSetup` komutunu gönderir.
2. **Native Tarafı:**
* **Android:** Uygulama Açılış/Kapanışlarını takip etmek için `ActivityLifecycleCallbacks` başlatır.
* **React Native:** View Ağacını tarayıp `nativeID` bulmak için `GlobalLayoutListener` başlatır.
* **Flutter:** İletişim kanalını açar ve sinyal beklemeye başlar.



### Senaryo B: Buton Tıklaması (Trigger Flow)

Kullanıcı tanımlı bir butona tıkladığında:

1. **Kullanıcı Eylemi:** Ekrana dokunur.
2. **Algılama:**
* **Android/RN:** Enjekte edilen `OnTouchListener` dokunuşu yakalar.
* **Flutter:** `SurveyTrigger` widget'ı `onPointerUp` olayını yakalar.


3. **Sinyal:** Buton ID'si (örn: `"btn_checkout"`) `SurveySDK.kt`'ye iletilir.
4. **Core Mantık:**
* Config Kontrolü: Bu ID için bir anket var mı?
* Kural Kontrolü: Kullanıcı engelli mi? Soğuma süresi bitti mi?


5. **Sonuç:** Her şey uygunsa, Activity üzerinde `SurveyDialogFragment` veya `BottomSheet` açılır.

---

## 3. Entegrasyon Kılavuzu

### 🤖 Android Native (Kotlin)

Köprüye gerek yoktur. Doğrudan erişim sağlanır.

```kotlin
// MainActivity.kt
SurveySDK.initialize(this, "API_KEY")
SurveySDK.getInstance().autoSetup(this)

// XML Layout
<Button android:tag="checkout_button" ... />

```

### ⚛️ React Native

Native ID'leri bulmak için **Sihirli Tarayıcı** kullanır.

**App.js:**

```javascript
import { NativeModules } from 'react-native';
const { SurveySDK } = NativeModules;

// 1. Başlatma
useEffect(() => {
  SurveySDK.initialize("API_KEY");
  SurveySDK.autoSetup();
}, []);

// 2. Navigasyon
<NavigationContainer onStateChange={(state) => {
   const route = state.routes[state.index].name;
   SurveySDK.triggerNavigationSurvey(route);
}}>

// 3. Arayüz
<TouchableOpacity nativeID="checkout_button">...</TouchableOpacity>

```

### 💙 Flutter (Yeni!)

Native SDK'ya sinyal göndermek için **Akıllı Widget'lar** kullanır.

**main.dart:**

```dart
import 'package:surveysdk_flutter/surveysdk_flutter.dart';

// 1. Başlatma
await SurveySdkFlutter.initialize('API_KEY');
await SurveySdkFlutter.autoSetup();

// 2. Buton Tetikleyici
SurveyTrigger(
  triggerId: "checkout_button",
  child: ElevatedButton(child: Text("Satın Al"), onPressed: (){}),
)

// 3. Scroll Tetikleyici
SurveyScrollView(
  threshold: 500,
  child: Column(...),
)

// 4. Navigasyon
MaterialApp(
  navigatorObservers: [SurveyNavigationObserver()],
  ...
)

```

---

## 4. Test ve Debugging

Logcat üzerinden **`SurveySDK`** etiketiyle filtreleyin.

* `👀 Continuous Scanning Started`: React Native tarayıcısı aktif.
* `👆 Auto-Detected Click`: Tıklama yakalandı ve Core'a iletildi.
* `✅ Found specific survey match`: Mantık başarılı, anket açılıyor.
* `❌ Cannot show survey`: Kurallar gösterimi engelledi (Soğuma süresi vb.).