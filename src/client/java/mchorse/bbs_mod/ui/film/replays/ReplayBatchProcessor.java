package mchorse.bbs_mod.ui.film.replays;

import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.math.IExpression;
import mchorse.bbs_mod.math.MathBuilder;
import mchorse.bbs_mod.utils.RayTracing;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.concurrent.ThreadLocalRandom;

public class ReplayBatchProcessor
{
    public enum Operation
    {
        RANDOM,
        LINE,
        SHIFT,
        SQUARE,
        SQUARE_OUTLINE,
        CIRCLE,
        CIRCLE_OUTLINE,
        CUBE,
        SPHERE
    }

    public enum Error
    {
        NEED_TWO_CHANNELS,
        NEED_THREE_CHANNELS,
        INVALID_EXPRESSION,
        NO_WORLD
    }

    public static class VisibleReplay
    {
        public final Replay replay;
        public final int i;
        public final int o;

        public VisibleReplay(Replay replay, int i, int o)
        {
            this.replay = replay;
            this.i = i;
            this.o = o;
        }
    }

    public static class NormalParams
    {
        public double randomMin;
        public double randomMax;
        public double lineOffset;
        public double size;
        public double shift;
        public boolean fill;
        public boolean fitHeight;
    }

    public static Error applyAdvanced(List<VisibleReplay> selected, List<String> selectedProperties, String expressionText)
    {
        MathBuilder builder = new MathBuilder();

        builder.register("i");
        builder.register("o");
        builder.register("v");
        builder.register("ki");

        IExpression parse;

        try
        {
            parse = builder.parse(expressionText);
        }
        catch (Exception e)
        {
            return Error.INVALID_EXPRESSION;
        }

        for (VisibleReplay replay : selected)
        {
            builder.variables.get("i").set(replay.i);
            builder.variables.get("o").set(replay.o);

            for (String s : selectedProperties)
            {
                KeyframeChannel channel = (KeyframeChannel) replay.replay.keyframes.get(s);

                if (channel == null)
                {
                    continue;
                }

                List keyframes = channel.getKeyframes();

                for (int i = 0; i < keyframes.size(); i++)
                {
                    Keyframe kf = (Keyframe) keyframes.get(i);

                    builder.variables.get("v").set(kf.getFactory().getY(kf.getValue()));
                    builder.variables.get("ki").set(i);

                    kf.setValue(kf.getFactory().yToValue(parse.doubleValue()), true);
                }
            }
        }

        return null;
    }

    public static Error applyNormal(List<VisibleReplay> selected, List<String> selectedProperties, Operation operation, NormalParams params)
    {
        if (operation == Operation.SQUARE || operation == Operation.SQUARE_OUTLINE || operation == Operation.CIRCLE || operation == Operation.CIRCLE_OUTLINE)
        {
            if (selectedProperties.size() < 2)
            {
                return Error.NEED_TWO_CHANNELS;
            }

            String aId = selectedProperties.get(0);
            String bId = selectedProperties.get(1);

            int count = selected.size();

            for (VisibleReplay replay : selected)
            {
                int o = replay.o;
                double a = 0;
                double b = 0;

                if (operation == Operation.SQUARE)
                {
                    int side = (int) Math.ceil(Math.sqrt(count));
                    double half = (side - 1) / 2D;
                    double step = side > 1 ? params.size / (side - 1) : 0D;
                    int col = o % side;
                    int row = o / side;

                    a = (col - half) * step;
                    b = (row - half) * step;
                }
                else if (operation == Operation.SQUARE_OUTLINE)
                {
                    int side = Math.max(2, (int) Math.ceil(count / 4D) + 1);
                    int edge = side - 1;
                    double half = edge / 2D;
                    double step = edge > 0 ? params.size / edge : 0D;
                    int pos = edge == 0 ? 0 : (o % (edge * 4));
                    int x;
                    int y;

                    if (pos < edge)
                    {
                        x = pos;
                        y = 0;
                    }
                    else if (pos < edge * 2)
                    {
                        x = edge;
                        y = pos - edge;
                    }
                    else if (pos < edge * 3)
                    {
                        x = edge - (pos - edge * 2);
                        y = edge;
                    }
                    else
                    {
                        x = 0;
                        y = edge - (pos - edge * 3);
                    }

                    a = (x - half) * step;
                    b = (y - half) * step;
                }
                else if (operation == Operation.CIRCLE)
                {
                    double radius = params.size / 2D;
                    double goldenAngle = Math.PI * (3D - Math.sqrt(5D));
                    double t = (o + 0.5D) / count;
                    double r = Math.sqrt(t) * radius;
                    double angle = o * goldenAngle;

                    a = Math.cos(angle) * r;
                    b = Math.sin(angle) * r;
                }
                else if (operation == Operation.CIRCLE_OUTLINE)
                {
                    double radius = params.size / 2D;
                    double angle = (count == 1 ? 0D : (o / (double) count) * Math.PI * 2D);

                    a = Math.cos(angle) * radius;
                    b = Math.sin(angle) * radius;
                }

                applyDelta(replay.replay, aId, a);
                applyDelta(replay.replay, bId, b);
            }

            return fitHeightToGround(selected, selectedProperties, params);
        }

        if (operation == Operation.CUBE || operation == Operation.SPHERE)
        {
            if (selectedProperties.size() < 3)
            {
                return Error.NEED_THREE_CHANNELS;
            }

            String aId = selectedProperties.get(0);
            String bId = selectedProperties.get(1);
            String cId = selectedProperties.get(2);

            int count = selected.size();

            if (operation == Operation.CUBE)
            {
                int side;
                int[] shellCoords = null;

                if (params.fill)
                {
                    side = (int) Math.ceil(Math.cbrt(count));
                }
                else
                {
                    side = 2;
                    while (count > cubeSurfacePoints(side))
                    {
                        side++;
                    }

                    shellCoords = buildCubeShellCoords(count, side);
                }

                double half = (side - 1) / 2D;
                double step = side > 1 ? params.size / (side - 1) : 0D;

                for (VisibleReplay replay : selected)
                {
                    int o = replay.o;
                    int gx;
                    int gy;
                    int gz;

                    if (params.fill)
                    {
                        gx = o % side;
                        gy = (o / side) % side;
                        gz = o / (side * side);
                    }
                    else
                    {
                        int i = o * 3;
                        gx = shellCoords[i];
                        gy = shellCoords[i + 1];
                        gz = shellCoords[i + 2];
                    }

                    double a = (gx - half) * step;
                    double b = (gy - half) * step;
                    double c = (gz - half) * step;

                    applyDelta(replay.replay, aId, a);
                    applyDelta(replay.replay, bId, b);
                    applyDelta(replay.replay, cId, c);
                }
            }
            else
            {
                double radius = params.size / 2D;
                double goldenAngle = Math.PI * (3D - Math.sqrt(5D));

                for (VisibleReplay replay : selected)
                {
                    int o = replay.o;
                    double t = (o + 0.5D) / count;
                    double theta = goldenAngle * (o + 0.5D);
                    double y = 1D - 2D * t;
                    double rxy = Math.sqrt(1D - y * y);
                    double s = params.fill ? Math.cbrt(t) : 1D;
                    double x = Math.cos(theta) * rxy;
                    double z = Math.sin(theta) * rxy;

                    double a = x * radius * s;
                    double b = y * radius * s;
                    double c = z * radius * s;

                    applyDelta(replay.replay, aId, a);
                    applyDelta(replay.replay, bId, b);
                    applyDelta(replay.replay, cId, c);
                }
            }

            return fitHeightToGround(selected, selectedProperties, params);
        }

        if (operation == Operation.LINE)
        {
            for (VisibleReplay replay : selected)
            {
                double delta = replay.o * params.lineOffset;

                for (String s : selectedProperties)
                {
                    applyDelta(replay.replay, s, delta);
                }
            }

            return fitHeightToGround(selected, selectedProperties, params);
        }

        if (operation == Operation.SHIFT)
        {
            for (VisibleReplay replay : selected)
            {
                for (String s : selectedProperties)
                {
                    applyDelta(replay.replay, s, params.shift);
                }
            }

            return fitHeightToGround(selected, selectedProperties, params);
        }

        if (operation == Operation.RANDOM)
        {
            double minValue = params.randomMin;
            double maxValue = params.randomMax;
            double minRand = Math.min(minValue, maxValue);
            double maxRand = Math.max(minValue, maxValue);
            long seed = ThreadLocalRandom.current().nextLong();

            for (VisibleReplay replay : selected)
            {
                long replaySeed = mix64(seed + (long) replay.o * 0x9e3779b97f4a7c15L);

                for (int ci = 0; ci < selectedProperties.size(); ci++)
                {
                    String s = selectedProperties.get(ci);
                    long channelSeed = mix64(replaySeed + (long) ci * 0xbf58476d1ce4e5b9L);
                    SplittableRandom random = new SplittableRandom(channelSeed);
                    double delta = minRand + random.nextDouble() * (maxRand - minRand);

                    applyDelta(replay.replay, s, delta);
                }
            }

            return fitHeightToGround(selected, selectedProperties, params);
        }

        return null;
    }

    private static Error fitHeightToGround(List<VisibleReplay> selected, List<String> selectedProperties, NormalParams params)
    {
        if (!params.fitHeight)
        {
            return null;
        }

        if (!(selectedProperties.contains("x") && selectedProperties.contains("y") && selectedProperties.contains("z")))
        {
            return null;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        World world = mc.world;

        if (world == null)
        {
            return Error.NO_WORLD;
        }

        Map<Long, Double> groundCache = new HashMap<>();

        for (VisibleReplay replay : selected)
        {
            KeyframeChannel xChannel = (KeyframeChannel) replay.replay.keyframes.get("x");
            KeyframeChannel yChannel = (KeyframeChannel) replay.replay.keyframes.get("y");
            KeyframeChannel zChannel = (KeyframeChannel) replay.replay.keyframes.get("z");

            if (xChannel == null || yChannel == null || zChannel == null)
            {
                continue;
            }

            double x = xChannel.getFactory().getY(xChannel.interpolate(0F));
            double y = yChannel.getFactory().getY(yChannel.interpolate(0F));
            double z = zChannel.getFactory().getY(zChannel.interpolate(0F));
            double groundY = getGroundY(world, groundCache, x, z);

            if (!Double.isNaN(groundY))
            {
                applyDelta(replay.replay, "y", groundY - y);
            }
        }

        return null;
    }

    private static double getGroundY(World world, Map<Long, Double> cache, double x, double z)
    {
        int bx = (int) Math.floor(x);
        int bz = (int) Math.floor(z);
        long key = (((long) bx) << 32) ^ (bz & 0xffffffffL);

        Double cached = cache.get(key);

        if (cached != null)
        {
            return cached;
        }

        double top = world.getTopY() + 5;
        Vec3d pos = new Vec3d(x, top, z);
        BlockHitResult result = RayTracing.rayTrace(world, pos, new Vec3d(0D, -1D, 0D), top - world.getBottomY() + 5D);

        double y = Double.NaN;

        if (result != null && result.getType() != HitResult.Type.MISS)
        {
            y = result.getPos().y;
        }

        cache.put(key, y);

        return y;
    }

    private static void applyDelta(Replay replay, String id, double delta)
    {
        KeyframeChannel channel = (KeyframeChannel) replay.keyframes.get(id);

        if (channel == null || !KeyframeFactories.isNumeric(channel.getFactory()))
        {
            return;
        }

        List keyframes = channel.getKeyframes();

        for (int i = 0; i < keyframes.size(); i++)
        {
            Keyframe kf = (Keyframe) keyframes.get(i);
            double v = kf.getFactory().getY(kf.getValue());

            kf.setValue(kf.getFactory().yToValue(v + delta), true);
        }
    }

    private static long mix64(long z)
    {
        z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
        z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
        return z ^ (z >>> 31);
    }

    private static int cubeSurfacePoints(int side)
    {
        if (side <= 1)
        {
            return 1;
        }

        int inner = Math.max(0, side - 2);

        return side * side * side - inner * inner * inner;
    }

    private static int[] buildCubeShellCoords(int count, int side)
    {
        int[] out = new int[count * 3];
        int i = 0;

        for (int z = 0; z < side && i < count; z++)
        {
            for (int y = 0; y < side && i < count; y++)
            {
                for (int x = 0; x < side && i < count; x++)
                {
                    if (x != 0 && x != side - 1 && y != 0 && y != side - 1 && z != 0 && z != side - 1)
                    {
                        continue;
                    }

                    int j = i * 3;

                    out[j] = x;
                    out[j + 1] = y;
                    out[j + 2] = z;
                    i++;
                }
            }
        }

        return out;
    }
}
