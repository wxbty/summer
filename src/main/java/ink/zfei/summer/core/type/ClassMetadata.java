package ink.zfei.summer.core.type;

public interface ClassMetadata {

    /**
     * Return the name of the underlying class.
     */
    String getClassName();

    /**
     * Return whether the underlying class represents an interface.
     */
    boolean isInterface();

    /**
     * Return whether the underlying class represents an annotation.
     * @since 4.1
     */
    boolean isAnnotation();

    /**
     * Return whether the underlying class is marked as abstract.
     */
    boolean isAbstract();
}
