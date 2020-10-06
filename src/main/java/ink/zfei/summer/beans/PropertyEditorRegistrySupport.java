package ink.zfei.summer.beans;

import ink.zfei.summer.core.convert.ConversionService;
import ink.zfei.summer.lang.Nullable;
import ink.zfei.summer.util.ClassUtils;
import org.xml.sax.InputSource;

import java.beans.PropertyEditor;
import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.time.ZoneId;
import java.util.*;
import java.util.regex.Pattern;

public class PropertyEditorRegistrySupport implements PropertyEditorRegistry {

    @Nullable
    private ConversionService conversionService;

    private boolean defaultEditorsActive = false;

    private boolean configValueEditorsActive = false;

    @Nullable
    private Map<Class<?>, PropertyEditor> defaultEditors;

    @Nullable
    private Map<Class<?>, PropertyEditor> overriddenDefaultEditors;

    @Nullable
    private Map<Class<?>, PropertyEditor> customEditors;

    @Nullable
    private Map<String, CustomEditorHolder> customEditorsForPath;

    @Nullable
    private Map<Class<?>, PropertyEditor> customEditorCache;


    /**
     * Specify a Spring 3.0 ConversionService to use for converting
     * property values, as an alternative to JavaBeans PropertyEditors.
     */
    public void setConversionService(@Nullable ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    /**
     * Return the associated ConversionService, if any.
     */
    @Nullable
    public ConversionService getConversionService() {
        return this.conversionService;
    }


    //---------------------------------------------------------------------
    // Management of default editors
    //---------------------------------------------------------------------

    /**
     * Activate the default editors for this registry instance,
     * allowing for lazily registering default editors when needed.
     */
    protected void registerDefaultEditors() {
        this.defaultEditorsActive = true;
    }

    /**
     * Activate config value editors which are only intended for configuration purposes,
     * <p>Those editors are not registered by default simply because they are in
     * general inappropriate for data binding purposes. Of course, you may register
     * them individually in any case, through {@link #registerCustomEditor}.
     */
    public void useConfigValueEditors() {
        this.configValueEditorsActive = true;
    }

    /**
     * Override the default editor for the specified type with the given property editor.
     * <p>Note that this is different from registering a custom editor in that the editor
     * semantically still is a default editor. A ConversionService will override such a
     * default editor, whereas custom editors usually override the ConversionService.
     * @param requiredType the type of the property
     * @param propertyEditor the editor to register
     * @see #registerCustomEditor(Class, PropertyEditor)
     */
    public void overrideDefaultEditor(Class<?> requiredType, PropertyEditor propertyEditor) {
        if (this.overriddenDefaultEditors == null) {
            this.overriddenDefaultEditors = new HashMap<>();
        }
        this.overriddenDefaultEditors.put(requiredType, propertyEditor);
    }

    /**
     * Retrieve the default editor for the given property type, if any.
     * <p>Lazily registers the default editors, if they are active.
     * @param requiredType type of the property
     * @return the default editor, or {@code null} if none found
     * @see #registerDefaultEditors
     */
    @Nullable
    public PropertyEditor getDefaultEditor(Class<?> requiredType) {
        if (!this.defaultEditorsActive) {
            return null;
        }
        if (this.overriddenDefaultEditors != null) {
            PropertyEditor editor = this.overriddenDefaultEditors.get(requiredType);
            if (editor != null) {
                return editor;
            }
        }
        if (this.defaultEditors == null) {
            createDefaultEditors();
        }
        return this.defaultEditors.get(requiredType);
    }

    /**
     * Actually register the default editors for this registry instance.
     */
    private void createDefaultEditors() {
        this.defaultEditors = new HashMap<>(64);

        // Simple editors, without parameterization capabilities.
        // The JDK does not contain a default editor for any of these target types.

    }

    /**
     * Copy the default editors registered in this instance to the given target registry.
     * @param target the target registry to copy to
     */
    protected void copyDefaultEditorsTo(PropertyEditorRegistrySupport target) {
        target.defaultEditorsActive = this.defaultEditorsActive;
        target.configValueEditorsActive = this.configValueEditorsActive;
        target.defaultEditors = this.defaultEditors;
        target.overriddenDefaultEditors = this.overriddenDefaultEditors;
    }


    //---------------------------------------------------------------------
    // Management of custom editors
    //---------------------------------------------------------------------

    @Override
    public void registerCustomEditor(Class<?> requiredType, PropertyEditor propertyEditor) {
        registerCustomEditor(requiredType, null, propertyEditor);
    }

    @Override
    public void registerCustomEditor(@Nullable Class<?> requiredType, @Nullable String propertyPath, PropertyEditor propertyEditor) {
        if (requiredType == null && propertyPath == null) {
            throw new IllegalArgumentException("Either requiredType or propertyPath is required");
        }
        if (propertyPath != null) {
            if (this.customEditorsForPath == null) {
                this.customEditorsForPath = new LinkedHashMap<>(16);
            }
            this.customEditorsForPath.put(propertyPath, new CustomEditorHolder(propertyEditor, requiredType));
        }
        else {
            if (this.customEditors == null) {
                this.customEditors = new LinkedHashMap<>(16);
            }
            this.customEditors.put(requiredType, propertyEditor);
            this.customEditorCache = null;
        }
    }

    @Override
    @Nullable
    public PropertyEditor findCustomEditor(@Nullable Class<?> requiredType, @Nullable String propertyPath) {
        Class<?> requiredTypeToUse = requiredType;
        if (propertyPath != null) {
            if (this.customEditorsForPath != null) {
                // Check property-specific editor first.
                PropertyEditor editor = getCustomEditor(propertyPath, requiredType);
                if (editor == null) {
                    List<String> strippedPaths = new ArrayList<>();
//                    addStrippedPropertyPaths(strippedPaths, "", propertyPath);
                    for (Iterator<String> it = strippedPaths.iterator(); it.hasNext() && editor == null;) {
                        String strippedPath = it.next();
                        editor = getCustomEditor(strippedPath, requiredType);
                    }
                }
                if (editor != null) {
                    return editor;
                }
            }
            if (requiredType == null) {
                requiredTypeToUse = getPropertyType(propertyPath);
            }
        }
        // No property-specific editor -> check type-specific editor.
        return getCustomEditor(requiredTypeToUse);
    }


    /**
     * Determine the property type for the given property path.
     * <p>Called by {@link #findCustomEditor} if no required type has been specified,
     * to be able to find a type-specific editor even if just given a property path.
     * <p>The default implementation always returns {@code null}.
     * BeanWrapperImpl overrides this with the standard {@code getPropertyType}
     * method as defined by the BeanWrapper interface.
     * @param propertyPath the property path to determine the type for
     * @return the type of the property, or {@code null} if not determinable
     */
    @Nullable
    protected Class<?> getPropertyType(String propertyPath) {
        return null;
    }

    /**
     * Get custom editor that has been registered for the given property.
     * @param propertyName the property path to look for
     * @param requiredType the type to look for
     * @return the custom editor, or {@code null} if none specific for this property
     */
    @Nullable
    private PropertyEditor getCustomEditor(String propertyName, @Nullable Class<?> requiredType) {
        CustomEditorHolder holder =
                (this.customEditorsForPath != null ? this.customEditorsForPath.get(propertyName) : null);
        return (holder != null ? holder.getPropertyEditor(requiredType) : null);
    }

    /**
     * Get custom editor for the given type. If no direct match found,
     * try custom editor for superclass (which will in any case be able
     * to render a value as String via {@code getAsText}).
     * @param requiredType the type to look for
     * @return the custom editor, or {@code null} if none found for this type
     * @see java.beans.PropertyEditor#getAsText()
     */
    @Nullable
    private PropertyEditor getCustomEditor(@Nullable Class<?> requiredType) {
        if (requiredType == null || this.customEditors == null) {
            return null;
        }
        // Check directly registered editor for type.
        PropertyEditor editor = this.customEditors.get(requiredType);
        if (editor == null) {
            // Check cached editor for type, registered for superclass or interface.
            if (this.customEditorCache != null) {
                editor = this.customEditorCache.get(requiredType);
            }
            if (editor == null) {
                // Find editor for superclass or interface.
                for (Iterator<Class<?>> it = this.customEditors.keySet().iterator(); it.hasNext() && editor == null;) {
                    Class<?> key = it.next();
                    if (key.isAssignableFrom(requiredType)) {
                        editor = this.customEditors.get(key);
                        // Cache editor for search type, to avoid the overhead
                        // of repeated assignable-from checks.
                        if (this.customEditorCache == null) {
                            this.customEditorCache = new HashMap<>();
                        }
                        this.customEditorCache.put(requiredType, editor);
                    }
                }
            }
        }
        return editor;
    }

    /**
     * Guess the property type of the specified property from the registered
     * custom editors (provided that they were registered for a specific type).
     * @param propertyName the name of the property
     * @return the property type, or {@code null} if not determinable
     */
    @Nullable
    protected Class<?> guessPropertyTypeFromEditors(String propertyName) {
        if (this.customEditorsForPath != null) {
            CustomEditorHolder editorHolder = this.customEditorsForPath.get(propertyName);
            if (editorHolder == null) {
                List<String> strippedPaths = new ArrayList<>();
//                addStrippedPropertyPaths(strippedPaths, "", propertyName);
                for (Iterator<String> it = strippedPaths.iterator(); it.hasNext() && editorHolder == null;) {
                    String strippedName = it.next();
                    editorHolder = this.customEditorsForPath.get(strippedName);
                }
            }
            if (editorHolder != null) {
                return editorHolder.getRegisteredType();
            }
        }
        return null;
    }

    /**
     * Holder for a registered custom editor with property name.
     * Keeps the PropertyEditor itself plus the type it was registered for.
     */
    private static final class CustomEditorHolder {

        private final PropertyEditor propertyEditor;

        @Nullable
        private final Class<?> registeredType;

        private CustomEditorHolder(PropertyEditor propertyEditor, @Nullable Class<?> registeredType) {
            this.propertyEditor = propertyEditor;
            this.registeredType = registeredType;
        }

        private PropertyEditor getPropertyEditor() {
            return this.propertyEditor;
        }

        @Nullable
        private Class<?> getRegisteredType() {
            return this.registeredType;
        }

        @Nullable
        private PropertyEditor getPropertyEditor(@Nullable Class<?> requiredType) {
            // Special case: If no required type specified, which usually only happens for
            // Collection elements, or required type is not assignable to registered type,
            // which usually only happens for generic properties of type Object -
            // then return PropertyEditor if not registered for Collection or array type.
            // (If not registered for Collection or array, it is assumed to be intended
            // for elements.)
            if (this.registeredType == null ||
                    (requiredType != null &&
                            (ClassUtils.isAssignable(this.registeredType, requiredType) ||
                                    ClassUtils.isAssignable(requiredType, this.registeredType))) ||
                    (requiredType == null &&
                            (!Collection.class.isAssignableFrom(this.registeredType) && !this.registeredType.isArray()))) {
                return this.propertyEditor;
            }
            else {
                return null;
            }
        }
    }

}
