# Localization Maintenance Workflow

This document explains how to keep the translation files in sync when the mod is updated.  
The golden rule: **Always update the English source first, then propagate the changes to each language.**

The `data/` folder at the root is always the **English reference**.  
Each language has its own `data/` folder with the exact same file structure – only the text inside the files is translated.

<br>

## Localization

### 1. Update the English version

- Edit the files inside the root `data/` folder.
- Commit changes.


### 2. Generate the diff between English and the given language

- If a **new file** was added, it **must** be copied manually.

- Run `diff` to see exactly what changed compared to the cutoff date.

```bash
# For Chinese
git diff HEAD@{2026-06-01} -- data/ > source_changes.diff
```

- `---` / `+++` --> lines that changed.  
  - `-` lines are the old English text.  
  - `+` lines are the new English text.


### 3. Translate only the changed lines

- Open the `.diff` file for the language you want to update.
- Look for the `+` lines (the new English text).
- The translator only needs to translate the *new/changed* strings and not the whole file.
- Replace the old translation with the new translation for those specific lines.

<br>

## Distribution

- Create a copy of the English mod.
- For each language, copy the translated `data/` from `localization_folder/<lang>/data/` **over** the English `data/` in that ZIP.
- Rename the ZIP file accordingly.

<br>

## Further instructions
- All CSV and JSON files must be saved as UTF‑8 without BOM.