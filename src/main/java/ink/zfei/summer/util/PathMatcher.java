package ink.zfei.summer.util;

public interface PathMatcher {

    boolean isPattern(String path);

    boolean matchStart(String pattern, String path);

    boolean match(String pattern, String path);
}
