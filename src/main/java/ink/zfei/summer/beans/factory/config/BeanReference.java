package ink.zfei.summer.beans.factory.config;

import ink.zfei.summer.beans.BeanMetadataElement;

public interface BeanReference extends BeanMetadataElement {

    /**
     * Return the target bean name that this reference points to (never {@code null}).
     */
    String getBeanName();

}
