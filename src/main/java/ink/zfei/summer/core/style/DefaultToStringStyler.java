package ink.zfei.summer.core.style;

import ink.zfei.summer.lang.Nullable;
import ink.zfei.summer.util.Assert;
import ink.zfei.summer.util.ClassUtils;
import ink.zfei.summer.util.ObjectUtils;

public class DefaultToStringStyler implements ToStringStyler {

    private final ValueStyler valueStyler;


    /**
     * Create a new DefaultToStringStyler.
     * @param valueStyler the ValueStyler to use
     */
    public DefaultToStringStyler(ValueStyler valueStyler) {
        Assert.notNull(valueStyler, "ValueStyler must not be null");
        this.valueStyler = valueStyler;
    }

    /**
     * Return the ValueStyler used by this ToStringStyler.
     */
    protected final ValueStyler getValueStyler() {
        return this.valueStyler;
    }


    @Override
    public void styleStart(StringBuilder buffer, Object obj) {
        if (!obj.getClass().isArray()) {
            buffer.append('[').append(ClassUtils.getShortName(obj.getClass()));
            styleIdentityHashCode(buffer, obj);
        }
        else {
            buffer.append('[');
            styleIdentityHashCode(buffer, obj);
            buffer.append(' ');
            styleValue(buffer, obj);
        }
    }

    private void styleIdentityHashCode(StringBuilder buffer, Object obj) {
        buffer.append('@');
        buffer.append(ObjectUtils.getIdentityHexString(obj));
    }

    @Override
    public void styleEnd(StringBuilder buffer, Object o) {
        buffer.append(']');
    }

    @Override
    public void styleField(StringBuilder buffer, String fieldName, @Nullable Object value) {
        styleFieldStart(buffer, fieldName);
        styleValue(buffer, value);
        styleFieldEnd(buffer, fieldName);
    }

    protected void styleFieldStart(StringBuilder buffer, String fieldName) {
        buffer.append(' ').append(fieldName).append(" = ");
    }

    protected void styleFieldEnd(StringBuilder buffer, String fieldName) {
    }

    @Override
    public void styleValue(StringBuilder buffer, @Nullable Object value) {
        buffer.append(this.valueStyler.style(value));
    }

    @Override
    public void styleFieldSeparator(StringBuilder buffer) {
        buffer.append(',');
    }

}
