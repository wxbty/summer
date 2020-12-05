package ink.zfei.summer.context.annotation;

import ink.zfei.summer.beans.InstantiationAwareBeanPostProcessor;
import ink.zfei.summer.core.Ordered;
import ink.zfei.summer.core.PriorityOrdered;

public class CommonAnnotationBeanPostProcessor implements InstantiationAwareBeanPostProcessor, PriorityOrdered {

    private int order = Ordered.LOWEST_PRECEDENCE;

    @Override
    public int getOrder() {
        return this.order;
    }
}
