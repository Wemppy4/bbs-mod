package mchorse.bbs_mod.ui.film.replays;

import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.cubic.data.animation.Animation;
import mchorse.bbs_mod.cubic.data.animation.AnimationPart;
import mchorse.bbs_mod.film.replays.PerLimbService;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.renderers.ModelFormRenderer;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.math.molang.expressions.MolangExpression;
import mchorse.bbs_mod.ui.film.ICursor;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeEditor;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UIPoseKeyframeFactory;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UIPoseTransformKeyframeFactory;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UITransformKeyframeFactory;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.graphs.IUIKeyframeGraph;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.StringUtils;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.KeyframeSegment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class UIReplaysEditorUtils
{
    public static UIPropTransform getEditableTransform(UIKeyframeEditor editor)
    {
        if (editor == null || editor.editor == null)
        {
            return null;
        }

        if (editor.editor instanceof UITransformKeyframeFactory transformKeyframeFactory)
        {
            return transformKeyframeFactory.transform;
        }
        else if (editor.editor instanceof UIPoseKeyframeFactory keyframeFactory)
        {
            return keyframeFactory.poseEditor.transform;
        }
        else if (editor.editor instanceof UIPoseTransformKeyframeFactory keyframeFactory)
        {
            return keyframeFactory.transform;
        }

        return null;
    }

    /* Picking form and form properties */

    public static void pickForm(UIKeyframeEditor keyframeEditor, ICursor cursor, Form form, String bone)
    {
        pickForm(keyframeEditor, cursor, form, bone, false);
    }

    public static void pickForm(UIKeyframeEditor keyframeEditor, ICursor cursor, Form form, String bone, boolean insert)
    {
        if (form == null || keyframeEditor == null || bone.isEmpty())
        {
            return;
        }

        String path = FormUtils.getPath(form);
        String boneKey = PerLimbService.toPoseBoneKey(path, bone);

        if (insert)
        {
            pickProperty(keyframeEditor, cursor, bone, boneKey, true);
            return;
        }

        UIKeyframeSheet sheet = resolveBoneSheet(keyframeEditor, boneKey, path);

        if (sheet != null)
        {
            pickProperty(keyframeEditor, cursor, bone, sheet, false);
        }
    }

    private static UIKeyframeSheet resolveBoneSheet(UIKeyframeEditor keyframeEditor, String boneKey, String formPath)
    {
        IUIKeyframeGraph graph = keyframeEditor.view.getGraph();
        UIKeyframeSheet sheet = graph.getSheet(boneKey);

        if (sheet != null)
        {
            return sheet;
        }

        return getActivePoseSheet(keyframeEditor, formPath);
    }

    private static UIKeyframeSheet getActivePoseSheet(UIKeyframeEditor keyframeEditor, String formPath)
    {
        IUIKeyframeGraph graph = keyframeEditor.view.getGraph();
        Keyframe selected = graph.getSelected();
        UIKeyframeSheet sheet = selected != null ? graph.getSheet(selected) : graph.getLastSheet();

        if (sheet == null || sheet.id == null)
        {
            return null;
        }

        String name = StringUtils.fileName(sheet.id);

        if (!name.startsWith("pose"))
        {
            return null;
        }

        if (sheet.property != null)
        {
            Form sheetForm = FormUtils.getForm(sheet.property);

            if (sheetForm != null)
            {
                return FormUtils.getPath(sheetForm).equals(formPath) ? sheet : null;
            }
        }

        if (formPath.isEmpty())
        {
            return sheet.id.contains(FormUtils.PATH_SEPARATOR) ? null : sheet;
        }

        String prefix = formPath + FormUtils.PATH_SEPARATOR;

        return sheet.id.startsWith(prefix) ? sheet : null;
    }

    private static void pickProperty(UIKeyframeEditor keyframeEditor, ICursor cursor, String bone, String key, boolean insert)
    {
        UIKeyframeSheet sheet = keyframeEditor.view.getGraph().getSheet(key);

        if (sheet != null)
        {
            pickProperty(keyframeEditor, cursor, bone, sheet, insert);
        }
    }

    private static void pickProperty(UIKeyframeEditor keyframeEditor, ICursor filmPanel, String bone, UIKeyframeSheet sheet, boolean insert)
    {
        IUIKeyframeGraph graph = keyframeEditor.view.getGraph();
        int tick = filmPanel.getCursor();

        if (insert)
        {
            Keyframe keyframe = graph.addKeyframe(sheet, tick, null);
            graph.selectKeyframe(keyframe);
            return;
        }

        Keyframe closest = getClosestKeyframe(sheet, tick);

        if (closest != null)
        {
            selectKeyframe(graph, closest);
            updatePoseEditorBoneSelection(keyframeEditor, bone);
            filmPanel.setCursor((int) closest.getTick());
        }
        else
        {
            updatePoseEditorBoneSelection(keyframeEditor, bone);
        }
    }

    private static Keyframe getClosestKeyframe(UIKeyframeSheet sheet, int tick)
    {
        KeyframeSegment segment = sheet.channel.find(tick);

        return segment != null ? segment.getClosest() : null;
    }

    private static void selectKeyframe(IUIKeyframeGraph graph, Keyframe closest)
    {
        if (graph.getSelected() == closest)
        {
            return;
        }

        boolean select = true;

        for (UIKeyframeSheet graphSheet : graph.getSheets())
        {
            if (graphSheet.selection.getSelected().contains(closest))
            {
                select = false;
                break;
            }
        }

        if (select) graph.selectKeyframe(closest);
        else graph.pickKeyframe(closest);
    }

    private static void updatePoseEditorBoneSelection(UIKeyframeEditor keyframeEditor, String bone)
    {
        /* Обновляем выбор кости в редакторе позы, если он доступен */
        if (keyframeEditor.editor instanceof UIPoseKeyframeFactory poseFactory)
        {
            poseFactory.poseEditor.selectBone(bone);
        }
        
        /* Также обновляем выбор в основном редакторе ключевых кадров */
        keyframeEditor.view.getGraph().pickKeyframe(keyframeEditor.view.getGraph().getSelected());
    }

    /* Converting Blockbench model keyframes to pose keyframes */

    public static void animationToPoseKeyframes(
        UIKeyframeEditor keyframeEditor, UIKeyframeSheet sheet,
        ModelForm modelForm, IEntity entity,
        int tick, String animationKey, boolean onlyKeyframes, int length, int step
    ) {
        ModelInstance model = ModelFormRenderer.getModel(modelForm);
        Animation animation = model.animations.get(animationKey);

        if (animation != null)
        {
            keyframeEditor.view.getDopeSheet().clearSelection();

            if (onlyKeyframes)
            {
                List<Float> list = getTicks(animation);

                for (float i : list)
                {
                    fillAnimationPose(sheet, i, model, entity, animation, tick);
                }
            }
            else
            {
                for (int i = 0; i < length; i += step)
                {
                    fillAnimationPose(sheet, i, model, entity, animation, tick);
                }
            }

            keyframeEditor.view.getDopeSheet().pickSelected();
        }
    }

    private static List<Float> getTicks(Animation animation)
    {
        Set<Float> integers = new HashSet<>();

        for (AnimationPart value : animation.parts.values())
        {
            for (KeyframeChannel<MolangExpression> channel : value.channels)
            {
                for (Keyframe<MolangExpression> keyframe : channel.getKeyframes())
                {
                    integers.add(keyframe.getTick());
                }
            }
        }

        ArrayList<Float> ticks = new ArrayList<>(integers);

        Collections.sort(ticks);

        return ticks;
    }

    private static void fillAnimationPose(UIKeyframeSheet sheet, float i, ModelInstance model, IEntity entity, Animation animation, int current)
    {
        model.model.resetPose();
        model.model.apply(entity, animation, i, 1F, 0F, false);

        int insert = sheet.channel.insert(current + i, model.model.createPose());

        sheet.selection.add(insert);
    }

    /* Offer bone hierarchy options */

    public static void offerAdjacent(UIContext context, Form form, String bone, Consumer<String> consumer)
    {
        if (form == null)
        {
            return;
        }

        if (!bone.isEmpty() && form instanceof ModelForm modelForm)
        {
            ModelInstance model = ModelFormRenderer.getModel(modelForm);

            if (model == null)
            {
                return;
            }

            context.replaceContextMenu((menu) ->
            {
                for (String modelGroup : model.model.getAdjacentGroups(bone))
                {
                    if (model.disabledBones.contains(modelGroup))
                    {
                        continue;
                    }

                    menu.action(Icons.LIMB, IKey.constant(modelGroup), () -> consumer.accept(modelGroup));
                }

                menu.autoKeys();
            });
        }
    }

    public static void offerHierarchy(UIContext context, Form form, String bone, Consumer<String> consumer)
    {
        if (form == null)
        {
            return;
        }

        if (!bone.isEmpty() && form instanceof ModelForm modelForm)
        {
            ModelInstance model = ModelFormRenderer.getModel(modelForm);

            if (model == null)
            {
                return;
            }

            context.replaceContextMenu((menu) ->
            {
                for (String modelGroup : model.model.getHierarchyGroups(bone))
                {
                    if (model.disabledBones.contains(modelGroup))
                    {
                        continue;
                    }

                    menu.action(Icons.LIMB, IKey.constant(modelGroup), () -> consumer.accept(modelGroup));
                }

                menu.autoKeys();
            });
        }
    }
}
