# Alkosmen

2D-игра на Java (Swing/AWT) про сбор бутылок, прыжки и стелс от патрулирующих копов.

## Что есть в игре

- стартовое меню с кнопками `Start / Settings / Exit`;
- музыка в меню и в уровне (MIDI), звуки шагов, прыжка и подбора предметов;
- один demo-уровень с камерой, HUD и состоянием `GAME OVER`;
- механика укрытия: при удержании `S`/`Down` персонаж может скрываться от обнаружения;
- система копов с патрулированием, обзором и реакцией на игрока.

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

Точка входа приложения: `alkosmen.app.DesktopStartGame`.

## Управление

- движение: `A/D` или `←/→`;
- прыжок: `Space`, `W` или `↑`;
- скрыться: удерживать `S` или `↓`.

## Карта и объекты

- `#` - стена (коллизия);
- `.` - пустая клетка;
- `P` - точка спавна игрока;
- `B` - бутылка (цель для сбора);
- `C` - коп (после загрузки уровня становится динамическим NPC);
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
