package mchorse.bbs_mod.ui.forms.editors.panels.widgets;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.utils.UIConstants;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import java.util.function.Consumer;

public class UIBlockStateEditor extends UIElement
{
    public UIButton pick;

    private Consumer<BlockState> callback;
    private BlockState blockState;

    public UIBlockStateEditor(Consumer<BlockState> callback)
    {
        this.callback = callback;
        this.h(UIConstants.CONTROL_HEIGHT);
        this.pick = new UIButton(UIKeys.FORMS_EDITORS_BLOCK_PICKER_OPEN, (b) ->
        {
            UIUnifiedPickOverlayPanel panel = UIUnifiedPickOverlayPanel.forBlock((state) ->
            {
                this.acceptBlockState(state);
            }, this.blockState);

            UIOverlay.addOverlay(this.getContext(), panel, 0.68F, 0.62F);
        });

        this.add(this.pick);
        this.pick.relative(this).xy(0, 0).w(1F).h(1F);
    }

    public void setBlockState(BlockState blockState)
    {
        this.blockState = blockState;
        this.pick.label = IKey.constant(Registries.BLOCK.getId(blockState.getBlock()).toString());
    }

    private void acceptBlockState(BlockState blockState)
    {
        this.blockState = blockState;
        this.pick.label = IKey.constant(Registries.BLOCK.getId(blockState.getBlock()).toString());

        if (this.callback != null)
        {
            this.callback.accept(blockState);
        }
    }
}