package mchorse.bbs_mod.film.replays;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.settings.values.base.BaseKeyframeFactoryValue;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.settings.values.base.BaseValueBasic;
import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.interps.Interpolations;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.KeyframeSegment;
import mchorse.bbs_mod.utils.keyframes.factories.IKeyframeFactory;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;

import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.utils.pose.Pose;
import mchorse.bbs_mod.utils.pose.PoseTransform;
import mchorse.bbs_mod.utils.pose.Transform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class FormProperties extends ValueGroup
{
    private static final String POSE_PROPERTY = "pose";

    public final Map<String, KeyframeChannel> properties = new HashMap<>();

    public FormProperties(String id)
    {
        super(id);
    }

    public void shift(float tick)
    {
        for (KeyframeChannel<?> value : this.properties.values())
        {
            for (Keyframe<?> keyframe : value.getKeyframes())
            {
                keyframe.setTick(keyframe.getTick() + tick);
            }
        }
    }

    public KeyframeChannel getOrCreate(Form form, String key)
    {
        BaseValue value = this.get(key);
        BaseValue property = FormUtils.getProperty(form, key);

        if (value instanceof KeyframeChannel channel)
        {
            return channel;
        }

        return property != null ? this.create(property) : null;
    }

    public KeyframeChannel create(BaseValue property)
    {
        if (property.isVisible() && property instanceof BaseKeyframeFactoryValue<?> keyframeFactoryValue)
        {
            String key = FormUtils.getPropertyPath(property);
            KeyframeChannel channel = new KeyframeChannel(key, keyframeFactoryValue.getFactory());

            this.properties.put(key, channel);
            this.add(channel);

            return channel;
        }

        return null;
    }

    public KeyframeChannel registerChannel(String key, IKeyframeFactory factory)
    {
        KeyframeChannel channel = this.properties.get(key);

        if (channel == null)
        {
            channel = new KeyframeChannel(key, factory);

            this.properties.put(key, channel);
            this.add(channel);
        }

        return channel;
    }

    public void applyProperties(Form form, float tick)
    {
        this.applyProperties(form, tick, 1F);
    }

    public void applyProperties(Form form, float tick, float blend)
    {
        if (form == null)
        {
            return;
        }

        float clampedBlend = MathUtils.clamp(blend, 0F, 1F);
        List<KeyframeChannel> poseBoneChannels = new ArrayList<>();

        for (KeyframeChannel value : this.properties.values())
        {
            if (!PerLimbService.isPoseBoneChannel(value.getId()))
            {
                this.applyProperty(tick, form, value, clampedBlend);
            }
            else
            {
                poseBoneChannels.add(value);
            }
        }

        for (KeyframeChannel value : poseBoneChannels)
        {
            this.applyProperty(tick, form, value, clampedBlend);
        }
    }

    private void applyProperty(float tick, Form form, KeyframeChannel value, float blend)
    {
        PerLimbService.PoseBonePath poseBonePath = PerLimbService.parsePoseBonePath(value.getId());

        if (poseBonePath != null)
        {
            String formPath = poseBonePath.formPath();
            String bone = poseBonePath.bone();
            Form targetForm = FormUtils.getForm(form, formPath);

            if (targetForm instanceof ModelForm modelForm)
            {
                KeyframeSegment segment = value.find(tick);

                if (segment != null)
                {
                    PoseTransform transform = modelForm.pose.get().get(bone);

                    if (transform == null)
                    {
                        transform = new PoseTransform();
                        modelForm.pose.get().transforms.put(bone, transform);
                    }

                    Transform interpolated = (Transform) this.interpolateValue(value, transform, segment, blend);
                    transform.copy(interpolated);
                }
                else if (blend >= 1F && !this.hasBasePoseBone(formPath, bone, tick))
                {
                    modelForm.pose.get().transforms.remove(bone);
                }
            }

            return;
        }

        BaseValueBasic property = FormUtils.getProperty(form, value.getId());

        if (property == null)
        {
            return;
        }

        KeyframeSegment segment = value.find(tick);

        if (segment != null)
        {
            Object interpolated = this.interpolateValue(value, property.get(), segment, blend);
            property.setRuntimeValue(interpolated);
        }
        else
        {
            property.setRuntimeValue(null);
        }
    }

    private Object interpolateValue(KeyframeChannel value, Object current, KeyframeSegment segment, float blend)
    {
        if (blend < 1F)
        {
            IKeyframeFactory factory = value.getFactory();
            Object v = factory.copy(current);
            Object a = factory.copy(segment.createInterpolated());
            Object interpolated = factory.interpolate(v, v, a, a, Interpolations.LINEAR, blend);

            return factory.copy(interpolated);
        }

        return segment.createInterpolated();
    }

    private boolean hasBasePoseBone(String formPath, String bone, float tick)
    {
        String posePath = this.toPosePropertyKey(formPath);
        KeyframeChannel poseChannel = this.properties.get(posePath);

        if (poseChannel == null)
        {
            return false;
        }

        KeyframeSegment poseSegment = poseChannel.find(tick);

        if (poseSegment == null)
        {
            return false;
        }

        Pose pose = (Pose) poseSegment.createInterpolated();

        return pose.transforms.containsKey(bone);
    }

    private String toPosePropertyKey(String formPath)
    {
        if (formPath == null || formPath.isEmpty())
        {
            return POSE_PROPERTY;
        }

        return formPath + FormUtils.PATH_SEPARATOR + POSE_PROPERTY;
    }

    public void resetProperties(Form form)
    {
        if (form == null)
        {
            return;
        }

        for (KeyframeChannel value : this.properties.values())
        {
            BaseValueBasic property = FormUtils.getProperty(form, value.getId());

            if (property == null)
            {
                return;
            }

            property.setRuntimeValue(null);
        }
    }

    public void cleanUp()
    {
        Iterator<KeyframeChannel> it = this.properties.values().iterator();

        while (it.hasNext())
        {
            KeyframeChannel next = it.next();

            if (next.isEmpty())
            {
                it.remove();
                this.remove(next);
            }
        }
    }

    @Override
    public void fromData(BaseType data)
    {
        super.fromData(data);

        this.properties.clear();

        if (!data.isMap())
        {
            return;
        }

        MapType map = data.asMap();

        for (String key : map.keys())
        {
            MapType mapType = map.getMap(key);

            if (mapType.isEmpty())
            {
                continue;
            }

            KeyframeChannel property = new KeyframeChannel(key, null);

            property.fromData(mapType);

            /* Patch 1.1.1 changes to lighting property */
            if (key.endsWith("lighting") && property.getFactory() == KeyframeFactories.BOOLEAN)
            {
                KeyframeChannel newProperty = new KeyframeChannel(key, KeyframeFactories.FLOAT);

                for (Object keyframe : property.getKeyframes())
                {
                    Keyframe kf = (Keyframe) keyframe;
                    Boolean v = (Boolean) kf.getValue();

                    newProperty.insert(kf.getTick(), v ? 1F : 0F);
                }

                property = newProperty;
            }

            if (property.getFactory() != null)
            {
                this.properties.put(key, property);
                this.add(property);
            }
        }
    }

    @Override
    protected boolean canPersist(BaseValue value)
    {
        if (value instanceof KeyframeChannel<?> channel)
        {
            return !channel.isEmpty();
        }

        return super.canPersist(value);
    }
}
