# Docs package validation notes

This package was built as a full replacement copy of the provided docs archive plus the current material/geology/autorecipe integration updates.

Validation performed during packaging:

- preserve existing non-edited docs and spreadsheet;
- add canonical current-task and integration-plan files;
- remove obsolete real-world element-derived mineral-family shortcuts from markdown;
- verify links/paths are relative to the existing docs tree where practical;
- regenerate `FILE_INDEX.md`;
- preserve `Industron Checklist.xlsx` byte-for-byte;
- ZIP integrity test before delivery.

The docs do not claim that the described Java implementation already exists. They describe what must be implemented and the gates required before claiming completion.
