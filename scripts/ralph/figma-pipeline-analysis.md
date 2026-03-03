# Figma → Ralph UI Pipeline Analysis
_Opus analysis, 2026-03-01_

## Передача дизайна из Figma в Ralph

### Рекомендуемый подход: Figma MCP + скриншоты

`--mcp-config` работает с `claude --print`, поэтому Figma MCP подключается напрямую в ralph.sh:

```bash
claude --dangerously-skip-permissions --print \
  --mcp-config '{"figma":{"transport":"http","url":"https://mcp.figma.com/mcp"}}' \
  < "$SCRIPT_DIR/CLAUDE.md"
```

**Figma MCP даёт:** точные значения (spacing, colors, typography, tokens)
**Скриншот даёт:** визуальный контекст композиции

**Расширение prd.json для UI stories:**
```json
{
  "id": "US-001",
  "figmaNodeUrl": "https://figma.com/design/FILE_KEY?node-id=123-456",
  "designScreenshot": "scripts/ralph/designs/statistics-screen.png"
}
```

### Ограничения Figma MCP
- Требует Personal Access Token через env
- Большие фреймы = много токенов на вход
- Возвращает JSON-дерево нод, не картинку — нужен скриншот для визуального контекста

---

## Visual Verification

**Полностью автоматического "Figma vs Android" пайплайна из коробки не существует.**

### Варианты

| Вариант | Как работает | Сложность |
|---------|-------------|-----------|
| **Claude Vision self-check** | Ralph читает два PNG через Read tool и сам сравнивает | Нулевая — работает уже сейчас |
| **Pixelmatch/SSIM скрипт** | Python/Node скрипт, числовой скор diff | 2-3 часа |
| **Applitools Eyes** | Коммерческий AI visual diff, Figma plugin | Дорого, overkill |

### Итерационный цикл с верификацией

```
1. Ralph читает Figma MCP данные (layout, tokens)
2. Ralph видит designScreenshot (multimodal)
3. Пишет Compose код
4. assembleDebug passes
5. ./gradlew :app:recordRoborazziDebug → скриншот
6. Сравнивает скриншот с designScreenshot
7. Если есть отличия → фиксит → повторяет до N раз
8. Коммитит
```

### Claude Vision self-check (Фаза 1)
Добавить в CLAUDE.md:
```markdown
## Visual Verification
After recordRoborazziDebug, compare screenshots:
1. Read generated screenshot from app/src/test/screenshots/
2. Read design reference from prd.json designScreenshot field
3. Check: layout, spacing, colors, typography, icons
4. Fix and re-record if needed (max 3 attempts)
5. Mark passes:true only when visual match is acceptable
```

### Pixelmatch скрипт (Фаза 2)
```python
# scripts/ralph/visual-diff.py
# SSIM сравнение двух PNG
# < 5% diff = pass, 5-20% = Claude Vision review, > 20% = fail
# Caveat: Figma и Robolectric рендерят по-разному → baseline ~10-20% diff даже для идеального UI
```

---

## Практический потолок точности

| Аспект | Точность | Причина |
|--------|----------|---------|
| Layout structure | 95%+ | MCP даёт иерархию |
| Colors | 98%+ | Exact hex из MCP |
| Typography | 90%+ | line-height/letter-spacing часто теряются |
| Spacing/padding | 80-85% | Самое слабое место |
| Corner radius, icons | 90-95%+ | Конкретные числа |
| Pixel-perfect | Невозможно | Figma ≠ Android рендеринг |

**С первой итерации: 80-90%. С 2-3 итерациями visual feedback loop: 90-95%.**

---

## План действий

**Фаза 1 (быстрый старт):**
1. Подключить Figma MCP к ralph.sh через `--mcp-config`
2. В prd.json добавить `figmaNodeUrl` и `designScreenshot` поля
3. В CLAUDE.md добавить инструкцию visual self-check через Read tool
4. Roborazzi тест как обязательный шаг для UI stories

**Фаза 2 (автоматизация):**
1. Написать `scripts/ralph/visual-diff.py` (SSIM)
2. Добавить шаг верификации в ralph.sh после recordRoborazzi
3. Калибровать порог на нескольких экранах

**Фаза 3 (опционально):**
1. Автоэкспорт скриншотов из Figma API при изменении макетов
2. Git LFS для PNG design references
