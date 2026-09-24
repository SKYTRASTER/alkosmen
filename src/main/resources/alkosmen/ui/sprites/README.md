Future character animations go in alkosmen/ and tolya/ as transparent horizontal PNG sprite sheets.

Alkosmen: idle.png, walk_left.png, walk_right.png, walk_up.png, walk_down.png,
laugh.png, fall.png.

Tolya: idle.png, walk_left.png, walk_right.png, accordion.png, dance.png.

All frames in one sheet must have the same width and height. Load them through
CharacterSpriteAssets.loadHorizontalSheet(character, action, frameWidth, frameHeight).
Missing sheets return an empty array, so the current temporary art stays usable.

The provided Alkosmen walk_atlas_v1.png is a 4-column, 5-row grid:
idle front, walk right, walk left, walk down, walk up. The game slices this
atlas at runtime. portrait_reference_v1.png is a visual reference, not a
gameplay frame.
The atlas has stray pixels below some poses, so the loader trims 30 source
pixels from the bottom of each cell without changing the supplied PNG.
