package ink.zfei.summer.core.type.filter;

public abstract class AbstractTypeHierarchyTraversingFilter implements TypeFilter{

    private final boolean considerInherited;

    private final boolean considerInterfaces;

    protected AbstractTypeHierarchyTraversingFilter(boolean considerInherited, boolean considerInterfaces) {
        this.considerInherited = considerInherited;
        this.considerInterfaces = considerInterfaces;
    }


}
