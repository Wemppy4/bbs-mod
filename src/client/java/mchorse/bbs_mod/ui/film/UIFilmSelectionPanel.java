package mchorse.bbs_mod.ui.film;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.utils.UIRenderable;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.BBSSettings;

public class UIFilmSelectionPanel extends UIElement
{
    private UIFilmPanel panel;

    public UIFilmSelectionPanel(UIFilmPanel panel)
    {
        this.panel = panel;

        UIFilmOverlayPanel overlay = (UIFilmOverlayPanel) panel.overlay;

        overlay.content.removeFromParent();
        overlay.icons.removeFromParent();

        UIElement iconRow = UI.row(5, overlay.icons.getChildren().toArray(new UIElement[0]));
        iconRow.row(0).resize();

        UIElement layout = UI.column(
            UI.label(UIKeys.FILM_TITLE).background(BBSSettings.primaryColor(Colors.A50)).marginTop(10).marginBottom(10),
            overlay.content,
            iconRow.marginTop(10).marginBottom(10)
        );

        layout.relative(this).x(0.5F).y(0.5F).w(500).h(400).anchor(0.5F, 0.5F);

        overlay.content.h(300);

        this.add(new UIRenderable(this::renderBackground), layout);
    }

    private void renderBackground(UIContext context)
    {
        context.batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.ey(), Colors.A75);

        int cx = this.area.mx();
        int cy = this.area.my();
        int w = 270;
        int h = 220;

        context.batcher.box(cx - w, cy - h, cx + w, cy + h, Colors.A75);
    }
}