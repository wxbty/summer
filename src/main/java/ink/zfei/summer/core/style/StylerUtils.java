package ink.zfei.summer.core.style;

public abstract class StylerUtils {

    /**
     * Default ValueStyler instance used by the {@code style} method.
     * Also available for the {@link ToStringCreator} class in this package.
     */
    static final ValueStyler DEFAULT_VALUE_STYLER = new DefaultValueStyler();

    /**
     * Style the specified value according to default conventions.
     * @param value the Object value to style
     * @return the styled String
     * @see DefaultValueStyler
     */
    public static String style(Object value) {
        return DEFAULT_VALUE_STYLER.style(value);
    }

}
