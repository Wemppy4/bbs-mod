package mchorse.bbs_mod.forms.renderers.utils;

import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.colors.Color;

public class FormColorBlend
{
    public static void blend(Color base, Color overlay, boolean additive)
    {
        if (base == null || overlay == null)
        {
            return;
        }

        float a = MathUtils.clamp(overlay.a, 0F, 1F);
        float r = MathUtils.clamp(overlay.r, 0F, 1F);
        float g = MathUtils.clamp(overlay.g, 0F, 1F);
        float b = MathUtils.clamp(overlay.b, 0F, 1F);

        if (additive)
        {
            /*
             * In this pipeline blend is applied as a texture multiplier, so additive mode
             * must allow values > 1.0 to visibly brighten and wash the texture to white.
             */
            float boost = 8F;

            base.r *= 1F + r * a * boost;
            base.g *= 1F + g * a * boost;
            base.b *= 1F + b * a * boost;
        }
        else
        {
            base.r *= r;
            base.g *= g;
            base.b *= b;
        }

        base.a *= a;
    }
}