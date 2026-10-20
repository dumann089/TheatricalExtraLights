
package com.github.dumann089.theatricalextralights.blockentities.interfaces;

public interface GelScrollerConfigurable {
    int[] getGelColors();
    int getGelCount();
    int[] getGelColorAtIndex(int index);
    float getGelSize();
    float getScrollerSpeed();
    boolean isHorizontalGelScroller();
    void setGelConfiguration(int[] colors, float size, float speed);
}
