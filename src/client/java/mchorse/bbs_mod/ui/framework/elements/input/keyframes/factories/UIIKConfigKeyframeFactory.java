package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.cubic.ik.ModelIKConfig;
import mchorse.bbs_mod.cubic.ik.ModelIKIO;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.renderers.ModelFormRenderer;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIStringList;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.UIConstants;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.keyframes.Keyframe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UIIKConfigKeyframeFactory extends UIKeyframeFactory<BaseType>
{
    private static final float DEFAULT_WEIGHT = ModelIKConfig.DEFAULT_WEIGHT;

    private final UIKeyframeSheet sheet;

    public UIStringList bones;
    public UIToggle enabled;
    public UIButton locator;
    public UIButton root;
    public UITrackpad weight;
    public UITrackpad poleX;
    public UITrackpad poleY;
    public UITrackpad poleZ;

    private final List<String> availableBones = new ArrayList<>();
    private final Map<String, IKData> ikData = new HashMap<>();
    private String selectedBone = "";
    private boolean syncingUI;

    private static class IKData
    {
        public String locator = "";
        public String root = "";
        public boolean enabled = true;
        public float weight = DEFAULT_WEIGHT;
        public float poleX;
        public float poleY;
        public float poleZ;
    }

    public UIIKConfigKeyframeFactory(Keyframe<BaseType> keyframe, UIKeyframes editor)
    {
        super(keyframe, editor);

        this.sheet = editor.getGraph().getSheet(keyframe);

        IKey axis = IKey.constant("%s (%s)");

        this.bones = new UIStringList((l) ->
        {
            this.selectedBone = l.isEmpty() ? "" : l.get(0);
            this.updateFields();
        });
        this.bones.background().h(UIConstants.LIST_ITEM_HEIGHT * 8);

        this.enabled = new UIToggle(UIKeys.FORMS_EDITORS_MODEL_IK_ENABLED, (b) ->
        {
            if (this.syncingUI || this.selectedBone.isEmpty())
            {
                return;
            }

            IKData data = this.getOrCreateData(this.selectedBone);
            data.enabled = b.getValue();

            this.updateFields();
            this.applyChanges();
        });

        this.locator = new UIButton(IKey.EMPTY, (b) ->
        {
            if (this.selectedBone.isEmpty())
            {
                return;
            }

            IKData data = this.getOrCreateData(this.selectedBone);

            this.openBoneMenu(data.locator, (bone) ->
            {
                data.locator = bone;
                this.updateFields();
                this.applyChanges();
            });
        });

        this.root = new UIButton(IKey.EMPTY, (b) ->
        {
            if (this.selectedBone.isEmpty())
            {
                return;
            }

            IKData data = this.getOrCreateData(this.selectedBone);

            this.openBoneMenu(data.root, (bone) ->
            {
                data.root = bone;
                this.updateFields();
                this.applyChanges();
            });
        });

        this.weight = new UITrackpad((v) ->
        {
            if (this.syncingUI || this.selectedBone.isEmpty())
            {
                return;
            }

            IKData data = this.getOrCreateData(this.selectedBone);
            data.weight = v.floatValue();
            this.applyChanges();
        });
        this.weight.onlyNumbers().values(0.1D, 0.01D, 0.25D).increment(0.01D).limit(0D, 1D);
        this.weight.tooltip(UIKeys.FORMS_EDITORS_MODEL_IK_WEIGHT);

        this.poleX = new UITrackpad((v) ->
        {
            if (this.syncingUI || this.selectedBone.isEmpty())
            {
                return;
            }

            IKData data = this.getOrCreateData(this.selectedBone);
            data.poleX = v.floatValue();
            this.applyChanges();
        });
        this.poleX.block().onlyNumbers();
        this.poleX.tooltip(axis.format(UIKeys.FORMS_EDITORS_MODEL_IK_POLE, UIKeys.GENERAL_X));
        this.poleX.textbox.setColor(Colors.RED);

        this.poleY = new UITrackpad((v) ->
        {
            if (this.syncingUI || this.selectedBone.isEmpty())
            {
                return;
            }

            IKData data = this.getOrCreateData(this.selectedBone);
            data.poleY = v.floatValue();
            this.applyChanges();
        });
        this.poleY.block().onlyNumbers();
        this.poleY.tooltip(axis.format(UIKeys.FORMS_EDITORS_MODEL_IK_POLE, UIKeys.GENERAL_Y));
        this.poleY.textbox.setColor(Colors.GREEN);

        this.poleZ = new UITrackpad((v) ->
        {
            if (this.syncingUI || this.selectedBone.isEmpty())
            {
                return;
            }

            IKData data = this.getOrCreateData(this.selectedBone);
            data.poleZ = v.floatValue();
            this.applyChanges();
        });
        this.poleZ.block().onlyNumbers();
        this.poleZ.tooltip(axis.format(UIKeys.FORMS_EDITORS_MODEL_IK_POLE, UIKeys.GENERAL_Z));
        this.poleZ.textbox.setColor(Colors.BLUE);

        this.scroll.add(
            UI.label(UIKeys.FORMS_EDITORS_MODEL_IK_BONES),
            this.bones,
            UI.label(UIKeys.FORMS_EDITORS_MODEL_IK_SETTINGS).background().marginTop(UIConstants.SECTION_GAP),
            this.enabled,
            this.root,
            this.locator,
            UI.label(UIKeys.FORMS_EDITORS_MODEL_IK_WEIGHT),
            this.weight,
            UI.label(UIKeys.FORMS_EDITORS_MODEL_IK_POLE).marginTop(UIConstants.SECTION_GAP),
            UI.row(2, 0, UIConstants.CONTROL_HEIGHT, this.poleX, this.poleY, this.poleZ)
        );

        this.load();
    }

    private void load()
    {
        this.collectAvailableBones();
        this.ikData.clear();

        ModelIKConfig config = ModelIKIO.fromData(this.getCurrentMap());

        if (config != null && config.chains() != null)
        {
            for (ModelIKConfig.Chain chain : config.chains())
            {
                if (chain == null || chain.controller() == null || chain.controller().isEmpty())
                {
                    continue;
                }

                IKData data = new IKData();
                data.locator = chain.locator();
                data.root = chain.root();
                data.enabled = chain.enabled();
                data.weight = chain.weight();
                data.poleX = chain.poleX();
                data.poleY = chain.poleY();
                data.poleZ = chain.poleZ();
                this.ikData.put(chain.controller(), data);
            }
        }

        List<String> list = new ArrayList<>(this.availableBones);
        List<String> extras = new ArrayList<>();

        for (String bone : this.ikData.keySet())
        {
            if (!list.contains(bone))
            {
                extras.add(bone);
            }
        }

        Collections.sort(extras);
        list.addAll(extras);

        this.bones.setList(list);

        if (!list.isEmpty())
        {
            if (!list.contains(this.selectedBone))
            {
                this.selectedBone = list.get(0);
            }

            this.bones.setCurrentScroll(this.selectedBone);
        }
        else
        {
            this.selectedBone = "";
        }

        this.updateFields();
    }

    private void collectAvailableBones()
    {
        this.availableBones.clear();

        if (this.sheet.property == null || !(FormUtils.getForm(this.sheet.property) instanceof ModelForm form))
        {
            return;
        }

        ModelInstance model = ModelFormRenderer.getModel(form);

        if (model == null || model.model == null)
        {
            return;
        }

        List<String> bones = new ArrayList<>(model.model.getGroupKeysInHierarchyOrder());
        bones.removeIf(model.disabledBones::contains);
        this.availableBones.addAll(bones);
    }

    private void updateFields()
    {
        IKData data = this.ikData.get(this.selectedBone);
        boolean active = data != null && data.enabled;
        boolean canEdit = !this.selectedBone.isEmpty() && this.bones.isEnabled() && active;

        this.syncingUI = true;

        try
        {
            this.locator.label = UIKeys.FORMS_EDITORS_MODEL_IK_LOCATOR.format(this.formatBone(data == null ? "" : data.locator));
            this.root.label = UIKeys.FORMS_EDITORS_MODEL_IK_ROOT.format(this.formatBone(data == null ? "" : data.root));
            this.weight.setValue(data == null ? DEFAULT_WEIGHT : data.weight);
            this.poleX.setValue(data == null ? 0F : data.poleX);
            this.poleY.setValue(data == null ? 0F : data.poleY);
            this.poleZ.setValue(data == null ? 0F : data.poleZ);
            this.enabled.setEnabled(!this.selectedBone.isEmpty());
            this.enabled.setValue(active);
        }
        finally
        {
            this.syncingUI = false;
        }

        this.locator.setEnabled(canEdit);
        this.root.setEnabled(canEdit);
        this.weight.setEnabled(canEdit);
        this.poleX.setEnabled(canEdit);
        this.poleY.setEnabled(canEdit);
        this.poleZ.setEnabled(canEdit);
    }

    private IKData getOrCreateData(String bone)
    {
        return this.ikData.computeIfAbsent(bone, (k) -> new IKData());
    }

    private String formatBone(String bone)
    {
        return bone == null || bone.isEmpty() ? "-" : bone;
    }

    private void openBoneMenu(String current, java.util.function.Consumer<String> callback)
    {
        if (this.bones.getList().isEmpty())
        {
            return;
        }

        this.getContext().replaceContextMenu((menu) ->
        {
            boolean none = current == null || current.isEmpty();

            menu.action(Icons.REMOVE, UIKeys.GENERAL_NONE, none, () -> callback.accept(""));

            for (String bone : this.bones.getList())
            {
                boolean selected = bone.equals(current);

                menu.action(Icons.LIMB, IKey.constant(bone), selected, () -> callback.accept(bone));
            }
        });
    }

    private void applyChanges()
    {
        List<ModelIKConfig.Chain> out = new ArrayList<>();

        for (Map.Entry<String, IKData> entry : this.ikData.entrySet())
        {
            String controller = entry.getKey();
            IKData data = entry.getValue();

            if (controller == null || controller.isEmpty() || data == null)
            {
                continue;
            }

            if (data.locator == null || data.locator.isEmpty() || data.root == null || data.root.isEmpty())
            {
                continue;
            }

            out.add(new ModelIKConfig.Chain(controller, data.locator, data.root, data.enabled, data.poleX, data.poleY, data.poleZ, ModelIKConfig.PoleSpace.ROOT, data.weight));
        }

        ModelIKConfig config = out.isEmpty() ? null : new ModelIKConfig(out);
        MapType data = config == null ? new MapType() : ModelIKIO.toData(config);

        this.applyToSelectedKeyframes(data);
    }

    @SuppressWarnings("unchecked")
    private void applyToSelectedKeyframes(MapType data)
    {
        List<Keyframe> selected = new ArrayList<>(this.sheet.selection.getSelected());

        if (selected.isEmpty())
        {
            selected.add(this.keyframe);
        }

        for (Keyframe keyframe : selected)
        {
            keyframe.preNotify();

            keyframe.setValue(data.copy());

            keyframe.postNotify();
        }
    }

    private MapType getCurrentMap()
    {
        BaseType value = this.keyframe.getValue();

        if (value instanceof MapType map)
        {
            return map;
        }

        MapType map = new MapType();
        this.keyframe.setValue(map);

        return map;
    }
}