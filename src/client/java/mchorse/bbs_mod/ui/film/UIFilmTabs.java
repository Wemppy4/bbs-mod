package mchorse.bbs_mod.ui.film;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.UIScrollView;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.ui.framework.elements.utils.UIRenderable;
import mchorse.bbs_mod.ui.utils.ScrollDirection;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Colors;

import java.util.ArrayList;

public class UIFilmTabs extends UIElement
{
    private static final int TAB_HEIGHT = UIFilmPanel.FILM_TABS_HEIGHT_PX;
    private static final int TAB_MIN_WIDTH = 110;
    private static final int TAB_MAX_WIDTH = 230;
    private static final int TABS_GAP = 0;

    public final UIFilmPanel panel;
    public final UIScrollView scroll;
    public final UIIcon add;
    private final ArrayList<UIFilmTabElement> tabs = new ArrayList<>();

    public UIFilmTabs(UIFilmPanel panel)
    {
        this.panel = panel;
        this.scroll = new UIScrollView(ScrollDirection.HORIZONTAL);
        this.scroll.scroll.scrollSpeed = 20;
        this.scroll.relative(this).x(TABS_GAP).w(1F, -TABS_GAP * 2).h(TAB_HEIGHT);
        this.scroll.column(TABS_GAP).scroll();
        this.scroll.scroll.noScrollbar();

        this.add = new UIIcon(Icons.ADD, (b) -> panel.addTab());
        this.add.wh(TAB_HEIGHT, TAB_HEIGHT);

        this.add(new UIRenderable(this::renderBackground), this.scroll);
    }

    public void sync()
    {
        double scrollPos = this.scroll.scroll.getScroll();
        int count = this.panel.tabs.size();

        while (this.tabs.size() < count)
        {
            this.tabs.add(new UIFilmTabElement(this.panel, TAB_HEIGHT));
        }

        while (this.tabs.size() > count)
        {
            UIFilmTabElement removed = this.tabs.remove(this.tabs.size() - 1);
            removed.removeFromParent();
        }

        this.scroll.removeAll();
        FontRenderer font = Batcher2D.getDefaultTextRenderer();
        int baseMin = UIFilmTabElement.measureWidth(font, UIKeys.FILM_TABS_NEW_TAB);
        baseMin = Math.max(TAB_MIN_WIDTH, Math.min(TAB_MAX_WIDTH, baseMin));
        boolean hasNewTab = false;

        for (int i = 0; i < count; i++)
        {
            FilmTab tab = this.panel.tabs.get(i);
            IKey label = tab.filmId == null ? UIKeys.FILM_TABS_NEW_TAB : IKey.raw(tab.filmId);
            int w = UIFilmTabElement.measureWidth(font, label);
            w = Math.max(baseMin, Math.min(TAB_MAX_WIDTH, w));
            hasNewTab |= this.panel.isNewTab(tab);

            UIFilmTabElement tabElement = this.tabs.get(i);
            tabElement.setTab(tab, label, tab.filmId == null ? Icons.SEARCH : Icons.FILM);
            tabElement.wh(w, TAB_HEIGHT);
            this.scroll.add(tabElement);
        }

        if (!hasNewTab)
        {
            this.scroll.add(this.add);
        }
        this.scroll.resize();
        this.scroll.scroll.setScroll(scrollPos);
    }

    private void renderBackground(UIContext context)
    {
        context.batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.ey(), Colors.A100);
    }
}
