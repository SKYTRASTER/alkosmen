# Alkosmen

2D-игра на Java (Swing/AWT) про сбор бутылок, прыжки и стелс от патрулирующих копов.

## Что есть в игре

- стартовое меню с кнопками `Start / Settings / Exit`;
- музыка в меню и в уровне (MIDI), звуки шагов, прыжка и подбора предметов;
- компактный top-down demo-уровень: лабиринт ночного рынка, пять бутылок и выход;
- проходные NPC, которые оживляют локацию, но не блокируют маршрут;
- самостоятельная Windows-сборка с `Alkosmen.exe` и встроенной Java Runtime.

## Требования

- JDK 21;
- Gradle (или использование `gradlew`/`gradlew.bat` из репозитория);
- Windows/Linux/macOS.

Проект настроен на Java toolchain 21 в `build.gradle`.

## Запуск

Из корня проекта:

```bash
./gradlew run
```

Для Windows PowerShell:

```powershell
.\gradlew.bat run
```

## Windows `.exe`

```powershell
.\gradlew.bat packageWindowsExe
```

Готовое приложение появится в `build/portable/Alkosmen/Alkosmen.exe`. Рядом с ним лежит встроенная Java Runtime, поэтому на целевом Windows-компьютере не нужны Gradle и JDK. Копируй целиком папку `build/portable/Alkosmen`, а не один `.exe`.

Точка входа приложения: `alkosmen.app.DesktopStartGame`.

## Управление

- движение: `W/A/S/D` или стрелки;
- рестарт уровня: `R`.

## Карта и объекты

- `#` - стена (коллизия);
- `.` - пустая клетка;
- `P` - точка спавна игрока;
- `B` - бутылка (цель для сбора);
- `C` - проходной NPC-патруль;
- `E` - выход из уровня, открывается после сбора всех бутылок;
- `N`/`M` - декоративные NPC.

## Конфигурация

Базовые параметры окна читаются из:

- `src/main/resources/alkosmen/config.properties`

Там можно менять размер окна и часть UI-настроек.

## Структура (основное)

- `src/main/java/alkosmen/app/StartGame.java` - запуск меню и старта игры;
- `src/main/java/alkosmen/Game.java` - игровой цикл, рендер и обработка ввода;
- `src/main/java/alkosmen/game/CopSystem.java` - логика патруля/обнаружения копов;
- `src/main/java/alkosmen/game/GameHudRenderer.java` - отрисовка HUD и overlay;
- `src/main/resources/alkosmen/...` - карты, спрайты, музыка и звуки.
