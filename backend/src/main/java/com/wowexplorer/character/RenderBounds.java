package com.wowexplorer.character;

/**
 * Vertical bounding box of the visible character within its (mostly transparent)
 * render PNG, expressed as fractions of the image height (0..1). The UI uses these
 * to scale the render so the character fills the frame regardless of race/model size.
 *
 * @param top    fraction from the image top to the first non-transparent row
 * @param height fraction of the image height the character occupies
 */
public record RenderBounds(double top, double height) {}
