package com.x5.util;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ObjectDataMap
 * 
 * Box POJO/Bean/DataCapsule inside a Map.  When accessed, pry into object
 * using reflection/introspection/capsule-export and pull out all public
 * member fields/properties.
 * Convert field names from camelCase to lower_case_with_underscores
 * Convert bean properties from getSomeProperty() to some_property
 *  or isVeryHappy() to is_very_happy
 * 
 * Values returned are copies, frozen at time of first access.
 * 
 */
@SuppressWarnings("rawtypes")
public class ObjectDataMap implements Map
{
    private Map<String,Object> pickle = null;
    private Object object;
    private boolean isBean = false;
    
    private static final Map<String,Object> EMPTY_MAP = new HashMap<String,Object>();
    
    private static final HashSet<Class<?>> WRAPPER_TYPES = getWrapperTypes();

    private static HashSet<Class<?>> getWrapperTypes()
    {
        HashSet<Class<?>> ret = new HashSet<Class<?>>();
        ret.add(Boolean.class);
        ret.add(Character.class);
        ret.add(Byte.class);
        ret.add(Short.class);
        ret.add(Integer.class);
        ret.add(Long.class);
        ret.add(Float.class);
        ret.add(Double.class);
        ret.add(Void.class);
        return ret;
    }
    
    public static boolean isWrapperType(Class<?> clazz)
    {
        return WRAPPER_TYPES.contains(clazz);
    }

    public ObjectDataMap(Object pojo)
    {
        this.object = pojo;
    }
    
    private void init()
    {
        if (pickle == null) {
            pickle = mapify(object);
            // prevent multiple expensive calls to mapify
            // when result is null
            if (pickle == null) {
                pickle = EMPTY_MAP;
            }
        }
    }
    
    public static ObjectDataMap wrapBean(Object bean)
    {
        if (bean == null) return null;
        ObjectDataMap boxedBean = new ObjectDataMap(bean);
        boxedBean.isBean = true;
        
        return boxedBean;
    }
    
    private Map<String,Object> mapify(Object pojo)
    {
        if (pojo instanceof DataCapsule) {
            return mapifyCapsule((DataCapsule)pojo);
        } else if (isBean) {
            try {
                return mapifyBean(pojo);
            } catch (java.beans.IntrospectionException e) {
                // hmm, not a bean after all...
            }
        }
        
        Field[] fields = pojo.getClass().getDeclaredFields();
        Map<String,Object> pickle = null;
        
        for (int i=0; i<fields.length; i++) {
            Field field = fields[i];
            String paramName = field.getName();
            Class paramClass = field.getType();
            
            // force access
            int mods = field.getModifiers();
            if (!Modifier.isPrivate(mods) && !Modifier.isProtected(mods)) {
                field.setAccessible(true);
            }
            
            Object paramValue = null;
            try {
                paramValue = field.get(pojo);
            } catch (IllegalAccessException e) {
            }
                
            if (paramValue != null) {
                if (pickle == null) pickle = new HashMap<String,Object>();
                // convert isActive to is_active
                paramName = splitCamelCase(paramName);
                storeValue(pickle, paramClass, paramName, paramValue);
            }
        }
        
        return pickle;
    }
    
    private Map<String,Object> mapifyBean(Object bean)
    throws java.beans.IntrospectionException
    {
        BeanInfo beanInfo = Introspector.getBeanInfo(bean.getClass());
        PropertyDescriptor[] properties = beanInfo.getPropertyDescriptors();
        
        if (properties == null) return null;
        
        Map<String,Object> pickle = null;
        
        // copy properties into hashtable
        for (PropertyDescriptor property : properties) {
            Class paramClass = property.getPropertyType();
            Method getter = property.getReadMethod();
            try {
                Object paramValue = getter.invoke(bean, (Object[])null);

                if (paramValue != null) {
                    // converts isActive() to is_active
                    // converts getBookTitle() to book_title
                    String paramName = property.getName();
                    paramName = splitCamelCase(paramName);
                    if (paramValue instanceof Boolean) {
                        paramName = "is_"+paramName;
                    }
                    
                    if (pickle == null) pickle = new HashMap<String,Object>();
                    
                    storeValue(pickle, paramClass, paramName, paramValue);
                }
            } catch (InvocationTargetException e) {
            } catch (IllegalAccessException e) {
            }
        }
        
        return pickle;
    }

    private Map<String,Object> mapifyCapsule(DataCapsule capsule)
    {
        DataCapsuleReader reader = DataCapsuleReader.getReader(capsule);
        
        String[] tags = reader.getColumnLabels(null);
        Object[] data = reader.extractData(capsule);
        
        pickle = new HashMap<String,Object>();
        for (int i=0; i<tags.length; i++) {
            Object val = data[i];
            if (val == null) continue;
            if (val instanceof String) {
                pickle.put(tags[i], val);
            } else if (val instanceof DataCapsule) {
                pickle.put(tags[i], new ObjectDataMap(val));
            } else {
                pickle.put(tags[i], val.toString());
            }
        }
        
        return pickle;
    }
    
    private void storeValue(Map<String,Object> pickle, Class paramClass,
                            String paramName, Object paramValue)
    {
        if (paramClass.isArray() || paramValue instanceof List) {
            pickle.put(paramName, paramValue);
        } else if (paramClass == String.class) {
            pickle.put(paramName, paramValue);
        } else if (paramValue instanceof Boolean) {
            if (((Boolean)paramValue).booleanValue()) {
                pickle.put(paramName, "TRUE");
            }
        } else if (paramClass.isPrimitive() || isWrapperType(paramClass)) {
            pickle.put(paramName, paramValue.toString());
        } else if (paramValue == this) {
            // tiny optimization
            pickle.put(paramName, this);
        } else {
            // box all non-primitive object member fields
            // in their own ObjectDataMap wrapper.
            // lazy init guarantees no infinite recursion here.
            ObjectDataMap boxedParam = isBean ? wrapBean(paramValue) : new ObjectDataMap(paramValue);
            pickle.put(paramName, boxedParam);
        }
        
    }
    
    // splitCamelCase converts SimpleXMLStuff to Simple_XML_Stuff
    public static String splitCamelCase(String s)
    {
       return s.replaceAll(
          String.format("%s|%s|%s",
             "(?<=[A-Z])(?=[A-Z][a-z])",
             "(?<=[^A-Z])(?=[A-Z])",
             "(?<=[A-Za-z])(?=[^A-Za-z])"
          ),
          "_"
       ).toLowerCase();
    }
    
    public int size()
    {
        init();
        return pickle.size();
    }

    public boolean isEmpty()
    {
        init();
        return pickle.isEmpty();
    }

    public boolean containsKey(Object key)
    {
        init();
        return pickle.containsKey(key);
    }

    public boolean containsValue(Object value)
    {
        init();
        return pickle.containsValue(value);
    }

    public Object get(Object key)
    {
        init();
        return pickle.get(key);
    }

    public Object put(Object key, Object value)
    {
        // unsupported
        return null;
    }

    public Object remove(Object key)
    {
        // unsupported
        return null;
    }

    public void putAll(Map m)
    {
        // unsupported
    }

    public void clear()
    {
        // unsupported
    }

    public Set keySet()
    {
        init();
        return pickle.keySet();
    }

    public Collection values()
    {
        init();
        return pickle.values();
    }

    public Set entrySet()
    {
        init();
        return pickle.entrySet();
    }
}
