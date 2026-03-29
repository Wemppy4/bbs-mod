package mchorse.bbs_mod.ui.utils.pose;

import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIStringList;

import java.util.List;
import java.util.function.Consumer;

/**
 * Bone list for {@link UIPoseEditor}: plain click selects one bone; Shift or Ctrl + click toggles
 * membership for multi-selection (unlike the default {@code UIList} Shift = range selection).
 */
public class UIPoseBoneStringList extends UIStringList
{
    public UIPoseBoneStringList(Consumer<List<String>> callback)
    {
        super(callback);

        this.multi();
    }

    @Override
    protected void applySelectionOnClick(int index)
    {
        if (Window.isShiftPressed() || Window.isCtrlPressed())
        {
            this.toggleIndex(index);
        }
        else
        {
            this.setIndex(index);
        }
    }
}
