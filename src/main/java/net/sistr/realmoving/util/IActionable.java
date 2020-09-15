package net.sistr.realmoving.util;

public interface IActionable {

    void setActioning(boolean actioning);

    boolean isActioning();

    void setCrawling(boolean crawling);

    boolean isCrawling();

    boolean isSliding();

    void setClimbing(boolean climbing);

    boolean isClimbing();

    float getClimbHeight();

}
