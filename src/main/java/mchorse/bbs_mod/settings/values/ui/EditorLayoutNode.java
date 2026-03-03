package mchorse.bbs_mod.settings.values.ui;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.utils.MathUtils;

import java.util.List;
import java.util.Map;

public abstract class EditorLayoutNode
{
    public static final String TYPE_SPLITTER = "splitter";
    public static final String TYPE_PANEL = "panel";
    public static final String DIR_V = "v";
    public static final String DIR_H = "h";

    /** Drop zone edges for split (left/right = vertical split, top/bottom = horizontal). */
    public static final int EDGE_LEFT = 0;
    public static final int EDGE_RIGHT = 1;
    public static final int EDGE_TOP = 2;
    public static final int EDGE_BOTTOM = 3;

    public abstract BaseType toData();

    /** Fill panel id -> normalized bounds (x, y, w, h in 0..1) relative to parent. */
    public abstract void computeBounds(float x, float y, float w, float h, Map<String, float[]> out);

    /** Return a new tree with panel ids id1 and id2 swapped. */
    public abstract EditorLayoutNode copyWithSwappedIds(String id1, String id2);

    public static EditorLayoutNode fromData(BaseType data)
    {
        if (data == null || !data.isMap())
        {
            return defaultFilmLayout();
        }

        MapType map = data.asMap();
        String type = map.getString("type", "");

        if (TYPE_SPLITTER.equals(type))
        {
            String dir = map.getString("dir", DIR_V);
            float ratio = MathUtils.clamp(map.getFloat("ratio", 0.5F), 0.05F, 0.95F);
            EditorLayoutNode first = fromData(map.get("first"));
            EditorLayoutNode second = fromData(map.get("second"));

            if (first == null || second == null)
            {
                return defaultFilmLayout();
            }

            return new SplitterNode(DIR_H.equals(dir), ratio, first, second);
        }

        if (TYPE_PANEL.equals(type))
        {
            String id = map.getString("id", "");
            if (id.isEmpty())
            {
                return null;
            }
            return new PanelNode(id);
        }

        return defaultFilmLayout();
    }

    /** Default: vertical 0.66 -> main | (horizontal 0.5 -> preview / editArea). */
    public static EditorLayoutNode defaultFilmLayout()
    {
        return new SplitterNode(
            false,
            0.66F,
            new PanelNode("main"),
            new SplitterNode(
                true,
                0.5F,
                new PanelNode("preview"),
                new PanelNode("editArea")
            )
        );
    }

    /** Returns a new tree with the leaf panelId removed; parent splitter is collapsed to its other child. */
    public static EditorLayoutNode copyWithRemovedLeaf(EditorLayoutNode root, String panelId)
    {
        if (root == null)
        {
            return null;
        }
        if (root instanceof PanelNode)
        {
            return ((PanelNode) root).getPanelId().equals(panelId) ? null : root;
        }
        SplitterNode s = (SplitterNode) root;
        if (s.first instanceof PanelNode && ((PanelNode) s.first).getPanelId().equals(panelId))
        {
            return s.second;
        }
        if (s.second instanceof PanelNode && ((PanelNode) s.second).getPanelId().equals(panelId))
        {
            return s.first;
        }
        EditorLayoutNode f2 = copyWithRemovedLeaf(s.first, panelId);
        if (f2 != s.first)
        {
            return new SplitterNode(s.horizontal, s.ratio, f2, s.second);
        }
        EditorLayoutNode s2 = copyWithRemovedLeaf(s.second, panelId);
        if (s2 != s.second)
        {
            return new SplitterNode(s.horizontal, s.ratio, s.first, s2);
        }
        return root;
    }

    /** Returns a new tree with the first leaf matching leafId replaced by newNode. */
    public static EditorLayoutNode copyWithReplacedLeaf(EditorLayoutNode root, String leafId, EditorLayoutNode newNode)
    {
        if (root == null || newNode == null)
        {
            return root;
        }
        if (root instanceof PanelNode)
        {
            return ((PanelNode) root).getPanelId().equals(leafId) ? newNode : root;
        }
        SplitterNode s = (SplitterNode) root;
        EditorLayoutNode f2 = copyWithReplacedLeaf(s.first, leafId, newNode);
        if (f2 != s.first)
        {
            return new SplitterNode(s.horizontal, s.ratio, f2, s.second);
        }
        EditorLayoutNode s2 = copyWithReplacedLeaf(s.second, leafId, newNode);
        if (s2 != s.second)
        {
            return new SplitterNode(s.horizontal, s.ratio, s.first, s2);
        }
        return root;
    }

    /** Returns a new tree with droppedPanel moved to split at edge of targetPanel. */
    public static EditorLayoutNode copyWithInsertSplitAt(EditorLayoutNode root, String targetPanelId, String droppedPanelId, int edge)
    {
        EditorLayoutNode root2 = copyWithRemovedLeaf(root, droppedPanelId);
        if (root2 == null)
        {
            return root;
        }
        boolean horizontal = (edge == EDGE_TOP || edge == EDGE_BOTTOM);
        boolean droppedFirst = (edge == EDGE_LEFT || edge == EDGE_TOP);
        EditorLayoutNode first = droppedFirst ? new PanelNode(droppedPanelId) : new PanelNode(targetPanelId);
        EditorLayoutNode second = droppedFirst ? new PanelNode(targetPanelId) : new PanelNode(droppedPanelId);
        SplitterNode newSplit = new SplitterNode(horizontal, 0.5F, first, second);
        return copyWithReplacedLeaf(root2, targetPanelId, newSplit);
    }

    /** Collect all SplitterNodes in pre-order. */
    public static void collectSplitters(EditorLayoutNode node, List<SplitterNode> out)
    {
        if (node instanceof SplitterNode)
        {
            SplitterNode s = (SplitterNode) node;
            out.add(s);
            collectSplitters(s.first, out);
            collectSplitters(s.second, out);
        }
    }

    /** Info for one splitter handle: normalized handle rect (hx,hy,hw,hh), parent rect (px,py,pw,ph), and direction. */
    public static class SplitterHandleInfo
    {
        public final float hx, hy, hw, hh;
        public final float px, py, pw, ph;
        public final boolean horizontal;

        public SplitterHandleInfo(float hx, float hy, float hw, float hh, float px, float py, float pw, float ph, boolean horizontal)
        {
            this.hx = hx;
            this.hy = hy;
            this.hw = hw;
            this.hh = hh;
            this.px = px;
            this.py = py;
            this.pw = pw;
            this.ph = ph;
            this.horizontal = horizontal;
        }
    }

    private static final float SPLITTER_HANDLE_MARGIN = 0.003F;
    /** Minimum thickness (normalized) so horizontal and vertical handles have comparable grab size. */
    private static final float SPLITTER_HANDLE_MIN_THICKNESS = 0.02F;

    /** Fill list with handle bounds and parent rect for each splitter (normalized 0..1). */
    public static void computeSplitterHandles(EditorLayoutNode root, float x, float y, float w, float h, List<SplitterHandleInfo> out)
    {
        if (!(root instanceof SplitterNode))
        {
            return;
        }
        SplitterNode s = (SplitterNode) root;
        float thickness = Math.max(2F * SPLITTER_HANDLE_MARGIN, SPLITTER_HANDLE_MIN_THICKNESS);
        if (s.horizontal)
        {
            float h1 = h * s.ratio;
            float hy = y + h1 - thickness * 0.5F;
            float hh = thickness;
            out.add(new SplitterHandleInfo(x, hy, w, hh, x, y, w, h, true));
            computeSplitterHandles(s.first, x, y, w, h1, out);
            computeSplitterHandles(s.second, x, y + h1, w, h - h1, out);
        }
        else
        {
            float w1 = w * s.ratio;
            float hw = thickness;
            float hx = x + w1 - thickness * 0.5F;
            out.add(new SplitterHandleInfo(hx, y, hw, h, x, y, w, h, false));
            computeSplitterHandles(s.first, x, y, w1, h, out);
            computeSplitterHandles(s.second, x + w1, y, w - w1, h, out);
        }
    }

    public static class SplitterNode extends EditorLayoutNode
    {
        /** true = horizontal (split by height), false = vertical (split by width). */
        private final boolean horizontal;
        private float ratio;
        private final EditorLayoutNode first;
        private final EditorLayoutNode second;

        public SplitterNode(boolean horizontal, float ratio, EditorLayoutNode first, EditorLayoutNode second)
        {
            this.horizontal = horizontal;
            this.ratio = MathUtils.clamp(ratio, 0.05F, 0.95F);
            this.first = first;
            this.second = second;
        }

        public boolean isHorizontal()
        {
            return this.horizontal;
        }

        public float getRatio()
        {
            return this.ratio;
        }

        public void setRatio(float ratio)
        {
            this.ratio = MathUtils.clamp(ratio, 0.05F, 0.95F);
        }

        public EditorLayoutNode getFirst()
        {
            return this.first;
        }

        public EditorLayoutNode getSecond()
        {
            return this.second;
        }

        @Override
        public BaseType toData()
        {
            MapType map = new MapType();
            map.putString("type", TYPE_SPLITTER);
            map.putString("dir", this.horizontal ? DIR_H : DIR_V);
            map.putFloat("ratio", this.ratio);
            map.put("first", this.first.toData());
            map.put("second", this.second.toData());
            return map;
        }

        @Override
        public void computeBounds(float x, float y, float w, float h, Map<String, float[]> out)
        {
            if (this.horizontal)
            {
                float h1 = h * this.ratio;
                float h2 = h * (1F - this.ratio);
                this.first.computeBounds(x, y, w, h1, out);
                this.second.computeBounds(x, y + h1, w, h2, out);
            }
            else
            {
                float w1 = w * this.ratio;
                float w2 = w * (1F - this.ratio);
                this.first.computeBounds(x, y, w1, h, out);
                this.second.computeBounds(x + w1, y, w2, h, out);
            }
        }

        @Override
        public EditorLayoutNode copyWithSwappedIds(String id1, String id2)
        {
            return new SplitterNode(
                this.horizontal,
                this.ratio,
                this.first.copyWithSwappedIds(id1, id2),
                this.second.copyWithSwappedIds(id1, id2)
            );
        }
    }

    public static class PanelNode extends EditorLayoutNode
    {
        private final String panelId;

        public PanelNode(String panelId)
        {
            this.panelId = panelId;
        }

        public String getPanelId()
        {
            return this.panelId;
        }

        @Override
        public BaseType toData()
        {
            MapType map = new MapType();
            map.putString("type", TYPE_PANEL);
            map.putString("id", this.panelId);
            return map;
        }

        @Override
        public void computeBounds(float x, float y, float w, float h, Map<String, float[]> out)
        {
            out.put(this.panelId, new float[] {x, y, w, h});
        }

        @Override
        public EditorLayoutNode copyWithSwappedIds(String id1, String id2)
        {
            String id = this.panelId.equals(id1) ? id2 : this.panelId.equals(id2) ? id1 : this.panelId;
            return new PanelNode(id);
        }
    }
}
