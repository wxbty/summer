package ink.zfei.summer.context.annotation;

import ink.zfei.summer.core.ImportSelector;

/**
 * 延期处理引入的配置类
 * 适合处理带@Conditional的配置类
 */
public interface DeferredImportSelector extends ImportSelector {
}
