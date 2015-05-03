package com.cjl.poetryfan.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewPager 书页效果
 * <p/>
 * 不要设置pager 的 Left and right padding
 *
 * @author CJL
 * @since 2015-04-23
 */
public class BookLikePager extends ViewPager {
    public interface IBookLikePagerAdapter {
        /**
         * 根据ItemView 对象获取其对于在adapter中的位置
         * <p/>
         * 建议使用tag存储其位置
         *
         * @param itemView adapter 的一个itemVIew
         * @return 位置
         */
        int getPositionByItemView(View itemView);

        /**
         * 获取ItemView 相同的背景Drawable 请new一个Drawable，不要使用某个item的background
         */
        Drawable getItemViewBackground();
    }

    private int mMaxEdgeWidth;
    private int mMaxEdgeCount = 3;
    private Drawable mEdgeDrawable;

    private float mItemEdgeWidth;
    private float mItemScaleHeight;
    private float mMinAlpha = 0.5f;
    private float mAlphaItem;
    List<EdgeProperties> mExtraEdges;


    public BookLikePager(Context context) {
        super(context);
        init(context);
    }

    public BookLikePager(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        float density = context.getResources().getDisplayMetrics().density;

        mMaxEdgeWidth = (int) (density * 25 + .5);
        mItemScaleHeight = (int) (density * 15 + .5);

        setPageTransformer(false, null); // set the default PageTransformer
        setClipToPadding(false);
        resetSize();
    }

    public void setMaxPageEdgeCount(int count) {
        if (count <= 2) {
            throw new IllegalArgumentException("count can't be less than 2");
        }
        this.mMaxEdgeCount = count;
        resetSize();
    }

    public void setMaxPageEdgeWidth(int width) {
        mMaxEdgeWidth = width;
        resetSize();
    }

    public void setItemScaleHeight(int height) {
        mItemScaleHeight = height;
        invalidate();
    }

    public void setMinAlpha(float alpha) {
        mMinAlpha = alpha;
        resetSize();
    }

    private void resetSize() {
        int padding = (int) (mMaxEdgeWidth * (mMaxEdgeCount + 1) / mMaxEdgeCount / 2f);
        mItemEdgeWidth = mMaxEdgeWidth * 1f / mMaxEdgeCount;
        mAlphaItem = (1 - mMinAlpha) / (mMaxEdgeCount - 1);
        setPadding(padding, getPaddingTop(), padding, getPaddingBottom());
    }

    @Override
    public void setAdapter(PagerAdapter adapter) {
        if (!(adapter instanceof IBookLikePagerAdapter)) {
            throw new IllegalArgumentException("adapter must implements IBookLikePagerAdapter");
        }
        mEdgeDrawable = ((IBookLikePagerAdapter) adapter).getItemViewBackground();
        if (mEdgeDrawable == null || mEdgeDrawable.getCallback() != null) {
            throw new IllegalArgumentException("请 new 一个新的Drawable !");
        }

        super.setAdapter(adapter);
    }

    @Override
    public void setPageTransformer(boolean reverseDrawingOrder, final PageTransformer transformer) {
        PageTransformer ptf = new PageTransformer() {
            @Override
            public void transformPage(View page, float position) {
                int idx = ((IBookLikePagerAdapter) getAdapter()).getPositionByItemView(page);
                position = (page.getLeft() - getPaddingLeft() - getScrollX()) * 1f / page.getWidth();

                onPageTransform(page, idx, position);

                if (transformer != null) {
                    transformer.transformPage(page, position);
                }
            }
        };

        super.setPageTransformer(reverseDrawingOrder, ptf);
    }

    /**
     * @param view   page
     * @param idx    page 在adapter 中的idx
     * @param offset transformPage position
     *               从屏幕左边进-1->0, 出0->-1
     *               屏幕右边进 1->0, 出0->1
     * @return ViewPager 偏移位置
     */
    private void onPageTransform(View view, int idx, float offset) {
        view.setTranslationX(calcTranslationX(view, idx, offset));
        view.setScaleY(calcScaleY(view, idx, offset));
        view.setAlpha(calcAlpha(idx, offset));

        if (idx == getCurrentItem()) {
            setExtraEdge(view, idx, offset);
        }
    }

    private void setExtraEdge(View view, int cIdx, float offset) {
        if (mExtraEdges == null) {
            mExtraEdges = new ArrayList<>(mMaxEdgeCount - 2);
        } else {
            mExtraEdges.clear();
        }

        if (cIdx > 1) {
            int count = Math.min(mMaxEdgeCount - 2, cIdx - 1);
            int start = cIdx - count - 1;
            if (offset > 0 && cIdx - mMaxEdgeCount >= 0) {
                count++;
                start--;
            }

            int idx;
            float from, to, height;
            for (int i = 0; i < count; i++) {
                idx = start + i;
                EdgeProperties ep = new EdgeProperties();

                if (offset <= 0) {
                    from = getDefaultX(idx, cIdx);
                    to = getDefaultX(idx - 1, cIdx);
                    ep.x = from + (to - from) * Math.abs(offset);

                    from = view.getHeight() - mItemScaleHeight * (cIdx - idx);
                    to = from - mItemScaleHeight;
                    height = from + (to - from) * Math.abs(offset);
                    ep.y = (view.getHeight() - height) / 2;
                } else {
                    from = getDefaultX(idx, cIdx);
                    if (cIdx < mMaxEdgeCount) {
                        to = getDefaultX(idx, cIdx - 1);
                    } else {
                        to = getDefaultX(idx + 1, cIdx);
                    }
                    ep.x = from + (to - from) * Math.abs(offset);

                    from = view.getHeight() - mItemScaleHeight * (cIdx - idx);
                    to = from + mItemScaleHeight;
                    height = from + (to - from) * Math.abs(offset);
                    ep.y = (view.getHeight() - height) / 2;
                }

                ep.width = view.getWidth();
                ep.height = (int) height;

                from = 1 - mAlphaItem * (cIdx - idx);
                to = from + mAlphaItem;
                ep.alpha = (int) ((from + (to - from) * Math.abs(offset)) * 255);

                mExtraEdges.add(ep);
            }
        }
    }

    private float calcAlpha(int idx, float offset) {
        final int cIdx = getCurrentItem();

        if ((idx == cIdx && offset <= 0) || cIdx - idx == 1) {
            float from = 1;
            float to = from - mAlphaItem;
            return from + (to - from) * Math.abs(offset);
        }
        return 1;
    }

    private float calcScaleY(View view, int idx, float offset) {
        final int cIdx = getCurrentItem();
        final int viewH = view.getHeight();

        if ((idx == cIdx && offset <= 0) || cIdx - idx == 1) {
            float from = viewH;
            float to = from - mItemScaleHeight;
            return (from + (to - from) * Math.abs(offset)) / viewH;
        }
        return 1;
    }

    /**
     * @param idx    page 在adapter 中的idx
     * @param offset transformPage position
     * @return ViewPager 偏移位置
     */
    private float calcTranslationX(View view, int idx, float offset) {
        final int cIdx = getCurrentItem();

        float from, to;

        if (idx == cIdx + 1) {
            if (offset >= 1) {
                return getPaddingRight();
            }
            from = getDefaultX(idx, cIdx + 1);
            to = getDefaultX(idx, cIdx);
        } else if (idx > cIdx + 1) {
            return getPaddingLeft();
        } else if (idx == cIdx) {
            if (offset <= 0) {
                from = getDefaultX(idx, cIdx);
                to = getDefaultX(idx, cIdx + 1);
            } else {
                from = getDefaultX(idx, cIdx);
                to = getDefaultX(idx + 1, cIdx);
            }
        } else if (cIdx - idx == 1) {
            if (offset < -1) {
                from = getDefaultX(idx, cIdx);
                to = getDefaultX(idx, cIdx + 1);
                offset++;
            } else {
                from = getDefaultX(idx, idx);
                to = getDefaultX(idx, idx + 1);
            }
        } else {
            return getPaddingLeft();
        }

        float toX = from + (to - from) * Math.abs(offset);
        return toX - view.getLeft() + getScrollX();
    }

    /**
     * 获取指定页应该所处的x位置(pager 静止空闲状态)
     *
     * @param idx 页id
     * @param ci  当前默认页ID
     * @return 指定页x位置
     */
    private float getDefaultX(int idx, int ci) {
        if (idx > ci) { // 大于当前显示页，应该是在屏幕右边看不见状态
            return getWidth() * (idx - ci);
        } else if (ci - idx > mMaxEdgeCount) { // 相隔超过最大页数，在屏幕左边
            return -getWidth() * (ci - idx - mMaxEdgeCount + 1);
        } else if (ci < mMaxEdgeCount - 1) {//小于最大页数，所有页一起居中显示，计算偏差
//            float delta = getPaddingLeft() - (ci + 2) * mItemEdgeWidth / 2;
//            return delta + (idx + 1) * mItemEdgeWidth;
            return getPaddingLeft() + (idx - ci / 2f) * mItemEdgeWidth;
        } else {
            return (mMaxEdgeCount + idx - ci) * mItemEdgeWidth;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (getAdapter() != null) {
            drawEdge(canvas);
        }
    }

    private void drawEdge(Canvas canvas) {
        if (mExtraEdges == null) {
            return;
        }
        int clipSaveCount = canvas.save();
        canvas.clipRect(mItemEdgeWidth + getScrollX(), 0, mMaxEdgeCount * mItemEdgeWidth + getScrollX(), getHeight());
        for (EdgeProperties ep : mExtraEdges) {
            mEdgeDrawable.setBounds(0, 0, ep.width, ep.height);
            mEdgeDrawable.setAlpha(ep.alpha);
            canvas.save();
            canvas.translate(ep.x + getScrollX(), ep.y);
            mEdgeDrawable.draw(canvas);
            canvas.restore();
        }
        canvas.restoreToCount(clipSaveCount);
    }



    private class EdgeProperties {
        float x;
        float y;
        int alpha;
        int width;
        int height;
    }
}
