<div align="center">

# 💡 Lumio

### Light, exactly how you like it.

**Free & open-source RGB lighting control for Android**  
**KLVCorp by Sckewe**

[![Version](https://img.shields.io/badge/version-1.0.0-B995FF?style=for-the-badge)](https://github.com/KLVCorp/Lumio/releases)
[![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://github.com/KLVCorp/Lumio)
[![License](https://img.shields.io/badge/license-Apache%202.0-6384FF?style=for-the-badge)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-Compose-9664FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://github.com/KLVCorp/Lumio)

**[Русский](#-русский)** · **[English](#-english)** · **[Releases](https://github.com/KLVCorp/Lumio/releases)** · **[Issues](https://github.com/KLVCorp/Lumio/issues)**

</div>

---

## 🇷🇺 Русский

### ✨ О проекте

**Lumio** — бесплатное Android-приложение с открытым исходным кодом для управления совместимыми RGB-лентами по Bluetooth. Цвета, эффекты, сценарии и таймеры — в одном удобном интерфейсе. Для локального управления поддерживаемой Bluetooth-лентой интернет не нужен.

Разработчик: **KLVCorp by Sckewe**.

### 🎨 Возможности

| | Функция | Описание |
|---|---|---|
| 🎨 | Цвета | Цветовой круг, HEX, яркость и готовые оттенки |
| ⭐ | Мои цвета | Сохранение собственных оттенков для быстрого выбора |
| 🌈 | Эффекты | RGB-переливания, дыхание, пульсация и другие режимы |
| ⚡ | Аппаратные эффекты | Экспериментальная поддержка эффектов контроллера |
| ⏲️ | Автоматизация | Таймер сна, плавное затухание и расписания |
| 🏠 | Устройства | Поиск, сохранение и переименование лент; индивидуальные настройки |
| 🌙 | Интерфейс | Тёмная тема, русский и английский языки |
| 📦 | Резервное копирование | Экспорт и импорт настроек в JSON |

### 📱 Совместимость

- **Android 8.0+** (API 26+).
- Для Bluetooth требуется устройство с поддержкой BLE и необходимые разрешения Android.
- **Протестировано:** `ELK-BLEDOM-INTRO` (BLE, сервис `FFF0`, характеристика записи `FFF3`).
- Другие контроллеры `ELK-BLEDOM` могут работать, но совместимость зависит от прошивки и пока не гарантируется.
- **Алиса / Яндекс и Wi-Fi:** в версии 1.0 не поддерживаются.

> **Важно:** на данный момент Lumio активно управляет одним Bluetooth-контроллером за раз. Программные эффекты и автоматизация могут зависеть от фоновых ограничений Android; после принудительной остановки приложения или перезагрузки устройства их выполнение не гарантируется.

### ⬇️ Установка

1. Откройте **[Releases](https://github.com/KLVCorp/Lumio/releases)**.
2. Скачайте APK из последнего опубликованного релиза, когда он станет доступен.
3. Установите APK на Android, при необходимости разрешив установку из выбранного источника.
4. Включите Bluetooth, выдайте приложению запрашиваемые разрешения и подключите совместимую ленту.

Если на устройстве уже установлена тестовая версия, подписанная другим ключом, для установки релиза может потребоваться удаление предыдущей версии. **Сначала сохраните настройки.**

### 🛠️ Сборка из исходников

Понадобятся **Android Studio**, Android SDK и JDK, настроенный через Android Studio.

```bash
git clone https://github.com/KLVCorp/Lumio.git
cd Lumio
```

Откройте папку проекта в Android Studio и дождитесь **Gradle Sync**. Для тестовой сборки используйте **Build APK(s)**. Для распространения используйте собственный ключ подписи и сборку **release**.

**Никогда не публикуйте** `.jks`, `.keystore`, `local.properties`, пароли или токены. APK из локальной сборки не обязательно подписан официальным ключом разработчика.

### 🤝 Участие в проекте

Нашли ошибку или хотите предложить поддержку нового контроллера? Создайте [Issue](https://github.com/KLVCorp/Lumio/issues). По возможности укажите модель устройства, Android-версию, шаги воспроизведения и журналы **без личных данных**. Pull requests приветствуются.

### 🔐 Конфиденциальность и лицензия

Информация о конфиденциальности: [PRIVACY.md](PRIVACY.md).  
Исходный код распространяется по лицензии [Apache License 2.0](LICENSE). Уведомления об авторстве: [NOTICE](NOTICE).

---

## 🇬🇧 English

### ✨ About

**Lumio** is a free, open-source Android app for controlling compatible RGB LED strips over Bluetooth. Pick colors, run lighting effects, configure timers, and manage saved devices from one interface. Internet access is not required for local control of a supported Bluetooth strip.

Created by **KLVCorp by Sckewe**.

### 🎨 Features

| | Feature | Description |
|---|---|---|
| 🎨 | Color control | Color wheel, HEX input, brightness, and presets |
| ⭐ | Saved colors | Keep favorite custom shades ready to use |
| 🌈 | Lighting effects | RGB transitions, breathing, pulsing, and more |
| ⚡ | Hardware effects | Experimental controller-side effects |
| ⏲️ | Automation | Sleep timer, gradual fading, and schedules |
| 🏠 | Devices | Scan, save, rename, and retain per-device settings |
| 🌙 | Interface | Dark theme and Russian/English localization |
| 📦 | Backup | JSON settings export and import |

### 📱 Compatibility

- **Android 8.0+** (API 26+).
- A BLE-capable Android device and the required Android permissions are needed.
- **Tested:** `ELK-BLEDOM-INTRO` (BLE service `FFF0`, writable characteristic `FFF3`).
- Other `ELK-BLEDOM` controllers may work, depending on firmware; compatibility is not guaranteed.
- **Yandex Alice and Wi-Fi control are not included in v1.0.**

> **Note:** Lumio currently controls one active Bluetooth device at a time. Phone-driven effects and automations may be affected by Android background restrictions; execution is not guaranteed after force-stop or reboot.

### ⬇️ Installation

1. Visit **[Releases](https://github.com/KLVCorp/Lumio/releases)**.
2. Download the APK from the latest published release once available.
3. Install it on Android, allowing installation from that source if prompted.
4. Enable Bluetooth, grant the requested permissions, and connect a compatible light strip.

If an older test build was signed with another key, you may need to uninstall it first. **Back up your settings before doing so.**

### 🛠️ Build from source

Install **Android Studio** and the required Android SDK. Use the JDK configured through Android Studio.

```bash
git clone https://github.com/KLVCorp/Lumio.git
cd Lumio
```

Open the project in Android Studio, complete **Gradle Sync**, and select **Build APK(s)** for a debug build. Use your own signing key for a distributable release build.

Never commit signing keys, passwords, `local.properties`, or access tokens. Local builds are not necessarily signed with the project's official release key.

### 🤝 Contributing

Bug reports, device compatibility information, feature suggestions, and pull requests are welcome. Please use [GitHub Issues](https://github.com/KLVCorp/Lumio/issues) and avoid sharing personal information in logs.

### 🔐 Privacy & License

Privacy information: [PRIVACY.md](PRIVACY.md).  
License: [Apache License 2.0](LICENSE). Attribution notices: [NOTICE](NOTICE).

---

<div align="center">

**Lumio · KLVCorp by Sckewe**  
Made for people who want simple, flexible lighting control. 💜

</div>
