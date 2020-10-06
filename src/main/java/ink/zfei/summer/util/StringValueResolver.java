package ink.zfei.summer.util;

import ink.zfei.summer.lang.Nullable;

@FunctionalInterface
public interface StringValueResolver {

    /**
     * Resolve the given String value, for example parsing placeholders.
     * @param strVal the original String value (never {@code null})
     * @return the resolved String value (may be {@code null} when resolved to a null
     * value), possibly the original String value itself (in case of no placeholders
     * to resolve or when ignoring unresolvable placeholders)
     * @throws IllegalArgumentException in case of an unresolvable String value
     */
    @Nullable
    String resolveStringValue(String strVal);

}
