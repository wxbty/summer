package ink.zfei.summer.beans.factory;

import ink.zfei.summer.core.ResolvableType;
import ink.zfei.summer.lang.Nullable;
import ink.zfei.summer.util.StringUtils;

import java.util.Arrays;
import java.util.Collection;

public class NoUniqueBeanDefinitionException extends NoSuchBeanDefinitionException {

    private final int numberOfBeansFound;

    @Nullable
    private final Collection<String> beanNamesFound;


    /**
     * Create a new {@code NoUniqueBeanDefinitionException}.
     * @param type required type of the non-unique bean
     * @param numberOfBeansFound the number of matching beans
     * @param message detailed message describing the problem
     */
    public NoUniqueBeanDefinitionException(Class<?> type, int numberOfBeansFound, String message) {
        super(type, message);
        this.numberOfBeansFound = numberOfBeansFound;
        this.beanNamesFound = null;
    }

    /**
     * Create a new {@code NoUniqueBeanDefinitionException}.
     * @param type required type of the non-unique bean
     * @param beanNamesFound the names of all matching beans (as a Collection)
     */
    public NoUniqueBeanDefinitionException(Class<?> type, Collection<String> beanNamesFound) {
        super(type, "expected single matching bean but found " + beanNamesFound.size() + ": " +
                StringUtils.collectionToCommaDelimitedString(beanNamesFound));
        this.numberOfBeansFound = beanNamesFound.size();
        this.beanNamesFound = beanNamesFound;
    }

    /**
     * Create a new {@code NoUniqueBeanDefinitionException}.
     * @param type required type of the non-unique bean
     * @param beanNamesFound the names of all matching beans (as an array)
     */
    public NoUniqueBeanDefinitionException(Class<?> type, String... beanNamesFound) {
        this(type, Arrays.asList(beanNamesFound));
    }

    /**
     * Create a new {@code NoUniqueBeanDefinitionException}.
     * @param type required type of the non-unique bean
     * @param beanNamesFound the names of all matching beans (as a Collection)
     * @since 5.1
     */
    public NoUniqueBeanDefinitionException(ResolvableType type, Collection<String> beanNamesFound) {
        super(type, "expected single matching bean but found " + beanNamesFound.size() + ": " +
                StringUtils.collectionToCommaDelimitedString(beanNamesFound));
        this.numberOfBeansFound = beanNamesFound.size();
        this.beanNamesFound = beanNamesFound;
    }

    /**
     * Create a new {@code NoUniqueBeanDefinitionException}.
     * @param type required type of the non-unique bean
     * @param beanNamesFound the names of all matching beans (as an array)
     * @since 5.1
     */
    public NoUniqueBeanDefinitionException(ResolvableType type, String... beanNamesFound) {
        this(type, Arrays.asList(beanNamesFound));
    }


    @Override
    public int getNumberOfBeansFound() {
        return this.numberOfBeansFound;
    }


    @Nullable
    public Collection<String> getBeanNamesFound() {
        return this.beanNamesFound;
    }

}
