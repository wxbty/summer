package bean.configuration;

import ink.zfei.summer.context.AnnotationConfigApplicationContext;
import ink.zfei.summer.core.ImportSelector;
import ink.zfei.summer.core.type.AnnotationMetadata;


public class MyImportSelect implements ImportSelector {

    @Override
    public String[] selectImports(Class var1) {
        String[] arrs = new String[1];
        arrs[0] = "bean.Water";
        return arrs;
    }

    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
        return new String[0];
    }

}
