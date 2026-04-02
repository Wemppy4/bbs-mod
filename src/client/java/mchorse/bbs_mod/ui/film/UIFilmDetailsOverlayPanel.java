package mchorse.bbs_mod.ui.film;

import mchorse.bbs_mod.camera.utils.TimeUtils;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextarea;
import mchorse.bbs_mod.ui.framework.elements.input.text.utils.TextLine;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.utils.UILabel;
import mchorse.bbs_mod.ui.utils.UIConstants;
import mchorse.bbs_mod.ui.utils.UI;

public class UIFilmDetailsOverlayPanel extends UIOverlayPanel
{
    private Film film;

    public UITextarea<TextLine> description;
    public UILabel timeLabel;

    private long lastTimeUpdate;

    public UIFilmDetailsOverlayPanel(Film film)
    {
        super(L10n.lang("bbs.ui.film.details.title"));

        this.film = film;

        /* Stats */
        int replaysCount = film.replays.getList().size();
        int clipsCount = film.camera.get().size();
        int duration = film.camera.calculateDuration();
        long timeSpentActiveTicks = film.timeSpentActive.get();

        String timeFormatted = this.formatTime(timeSpentActiveTicks);
        String durationFormatted = TimeUtils.formatTime(duration);
        String createdFormatted = Film.formatCreatedAtForDisplay(film.createdAt.get());

        UILabel nameLabel = UI.label(L10n.lang("bbs.ui.film.details.name").format(film.getId())).background();
        UILabel createdLabel = UI.label(L10n.lang("bbs.ui.film.details.created").format(createdFormatted != null ? createdFormatted : "—")).background();
        UILabel statsLabel = UI.label(L10n.lang("bbs.ui.film.details.stats").format(replaysCount, clipsCount)).background();
        UILabel durationLabel = UI.label(L10n.lang("bbs.ui.film.details.duration").format(durationFormatted)).background();
        this.timeLabel = UI.label(L10n.lang("bbs.ui.film.details.time_spent").format(timeFormatted)).background();

        /* Description */
        this.description = new UITextarea<>((t) -> this.film.description.set(t));
        this.description.setText(film.description.get());
        this.description.background().wrap(true).padding(6);
        this.description.h(80); // Set a reasonable height for the description area

        /* Layout */
        // Name -> Created -> Description -> Stats -> Duration -> Time
        UIElement column = UI.column(
            nameLabel,
            createdLabel,
            UI.label(L10n.lang("bbs.ui.film.details.description")).marginTop(UIConstants.SECTION_GAP),
            this.description,
            statsLabel.marginTop(UIConstants.SECTION_GAP),
            durationLabel,
            this.timeLabel
        );
        
        column.relative(this.content).xy(10, 10).w(1F, -20).h(1F, -20);

        this.content.add(column);
    }

    @Override
    public void render(UIContext context)
    {
        super.render(context);

        if (System.currentTimeMillis() - this.lastTimeUpdate >= 1000)
        {
            this.updateTimeDisplay();
            this.lastTimeUpdate = System.currentTimeMillis();
        }
    }

    private void updateTimeDisplay()
    {
        long timeSpentActiveTicks = this.film.timeSpentActive.get();
        String timeFormatted = this.formatTime(timeSpentActiveTicks);

        this.timeLabel.label = L10n.lang("bbs.ui.film.details.time_spent").format(timeFormatted);
    }

    private String formatTime(long ticks)
    {
        long seconds = ticks / 20;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, secs);
    }
}
