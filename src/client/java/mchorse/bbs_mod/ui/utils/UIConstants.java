package mchorse.bbs_mod.ui.utils;

/**
 * Central UI dimension constants for a consistent, readable layout.
 * Spacing scale: MARGIN (4) for related items, SECTION_GAP (8) for visual blocks.
 */
public final class UIConstants
{
    /** Height of standard form controls: trackpad, button, textbox, keybind, color strip. */
    public static final int CONTROL_HEIGHT = 16;

    /** Base spacing between related elements (within a row, or label + control). */
    public static final int MARGIN = 4;

    /** Height of toggles and small switches. */
    public static final int TOGGLE_HEIGHT = 14;

    /** Padding inside scroll areas. */
    public static final int SCROLL_PADDING = 4;

    /** Gap between logical sections in clip/form panels (kept small so layout stays compact). */
    public static final int SECTION_GAP = 4;

    /** Height of list items in dropdowns (e.g. bone list) for a compact list. */
    public static final int LIST_ITEM_HEIGHT = 14;

    private UIConstants()
    {}
}
