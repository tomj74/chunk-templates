package com.x5.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Hashtable;

public class DataCapsuleReader
{
	// For efficiency, keep around instances of DataCapsuleReader objects
	// in a static hashtable so we don't have to wait for slow reflection
	// after the first time...
	private static Hashtable<String, DataCapsuleReader> readerCache
		= new Hashtable<String, DataCapsuleReader>();
	
	private String[] labels;
	private String[] bareLabels;
	private String[] methodNames;
	private Method[] methods;
	
	@SuppressWarnings("rawtypes")
	private Class capsuleClass;
	
	public static DataCapsuleReader getReader(DataCapsule[] dataCapsules)
	{
		DataCapsuleReader reader = getReaderFromCache(dataCapsules);
		if (reader == null) {
			reader = new DataCapsuleReader(dataCapsules);
			readerCache.put(reader.getDataClassName(), reader);
		}
		return reader;
	}
	
	public static DataCapsuleReader getReader(DataCapsule dataCapsule)
	{
		if (dataCapsule == null) return null;
		
		DataCapsuleReader reader = getReaderFromCache(dataCapsule);
		if (reader == null) {
			reader = new DataCapsuleReader(new DataCapsule[]{dataCapsule});
			readerCache.put(reader.getDataClassName(), reader);
		}
		return reader;
	}
	
	private static DataCapsuleReader getReaderFromCache(DataCapsule[] dataCapsules)
	{
		for (int i=0; i<dataCapsules.length; i++) {
			DataCapsule x = dataCapsules[i];
			if (x != null) {
				String className = x.getClass().getName();
				return readerCache.get(className);
			}
		}
		return null;
	}
	
	private static DataCapsuleReader getReaderFromCache(DataCapsule dataCapsule)
	{
		String className = dataCapsule.getClass().getName();
		return readerCache.get(className);
	}
	
	public DataCapsuleReader(DataCapsule[] dataCapsules)
	{
		extractLegend(dataCapsules);
	}
	
	private void extractLegend(DataCapsule[] dataCapsules)
	{
		for (int i=0; i<dataCapsules.length; i++) {
			DataCapsule x = dataCapsules[i];
			if (x != null) {
				this.capsuleClass = x.getClass();
				extractLegend(x);
				break;
			}
		}
	}
	
	private void extractLegend(DataCapsule dataCapsule)
	{
		String[] exports = dataCapsule.getExports();
		String exportPrefix = dataCapsule.getExportPrefix();

		labels = new String[exports.length];
		bareLabels = new String[exports.length];
		methodNames = new String[exports.length];
		
		for (int i=0; i<exports.length; i++) {
			parseExportMap(i,exportPrefix,exports[i]);
		}
	}
	
	private void parseExportMap(int i, String exportPrefix, String directive)
	{
		int spacePos = directive.indexOf(' ');
		String exportName = directive;
		String methodName;
		
		if (spacePos > -1) {
			// use specified name
			exportName = directive.substring(spacePos+1).trim();
			methodName = directive.substring(0,spacePos);
		} else {
			// transmogrify method name
			methodName = directive;
			exportName = transmogrify(directive);
		}

		String prefixedName = exportName;
		if (exportPrefix != null) {
			prefixedName = exportPrefix + "_" + exportName;
		}
		
		labels[i] = prefixedName;
		bareLabels[i] = exportName;
		methodNames[i] = methodName;
	}

	// transmogrify converts getDatePretty to date_pretty
	private static String transmogrify(String s)
	{
		String spaced = splitCamelCase(s);
		return spaced.toLowerCase().replaceFirst("^get_", "");
	}
	
	// splitCamelCase converts SimpleXMLStuff to Simple_XML_Stuff
	private static String splitCamelCase(String s)
	{
	   return s.replaceAll(
	      String.format("%s|%s|%s",
	         "(?<=[A-Z])(?=[A-Z][a-z])",
	         "(?<=[^A-Z])(?=[A-Z])",
	         "(?<=[A-Za-z])(?=[^A-Za-z])"
	      ),
	      "_"
	   );
	}
	
	public String[] getColumnLabels(String altPrefix)
	{
		if (altPrefix == null) return getColumnLabels();
		
		String[] altLabels = new String[bareLabels.length];
		for (int i=0; i<altLabels.length; i++) {
		    altLabels[i] = altPrefix + "." + labels[i];
		}
		return altLabels;
	}
	
	public String[] getColumnLabels()
	{
		return labels;
	}
	
	public void overrideColumnLabels(String[] newLabels)
	{
		this.labels = newLabels;
	}
	
	public Object[] extractData(DataCapsule data)
	{
		if (methods == null) {
			methods = grokMethods(data);
		}
		Object[] rawOutput = new Object[methods.length];
		for (int i=0; i<methods.length; i++) {
			Method m = methods[i];
			if (m != null) {
				// only no-arg methods are allowed
				try {
					rawOutput[i] = m.invoke(data, (Object[])null);
				} catch (IllegalArgumentException e) {
					e.printStackTrace(System.err);
				} catch (IllegalAccessException e) {
					e.printStackTrace(System.err);
				} catch (InvocationTargetException e) {
					e.printStackTrace(System.err);
				}
			}
		}
		return rawOutput;
	}
	
	public String getDataClassName()
	{
		return capsuleClass.getName();
	}
	
	@SuppressWarnings("unchecked")
	private Method[] grokMethods(DataCapsule data)
	{
		Method[] methods = new Method[methodNames.length];
		for (int i=0; i<methods.length; i++) {
			try {
				methods[i] = capsuleClass.getMethod(methodNames[i], (Class[])null);
			} catch (NoSuchMethodException e) {
				System.err.println("Class " + capsuleClass.getName() + " does not provide method "+methodNames[i]+"() as described in getExports() !!");
				e.printStackTrace(System.err);
			}
		}
		return methods;
	}
	
	@SuppressWarnings("unused")
	private Method[] grokSimpleMethods(DataCapsule data)
	{
        /**
         * If you wanted to just grab all the methods that
         * match the call signature of no args, returning
         * a string, you could do this...
         * 
         * you'd have to camel-case the method names and
         * store them somewhere...
         * 
         * this might be handy in a more framework-y scenario
         */

	    Method[] allMethods = capsuleClass.getMethods();
	    boolean[] isMatch = new boolean[allMethods.length];
	    int matchCount = 0;
	    
        for (int i=0; i<allMethods.length; i++) {
            Method m = allMethods[i];
            if (m.getReturnType() == String.class) {
                if (m.getParameterTypes() == null) {
                    String simpleMethodName = m.getName();
                    // add to available methods?
                    isMatch[i] = true;
                    matchCount++;
                }
            }
        }
        
        Method[] simpleMethods = new Method[matchCount];
        
	    for (int i=allMethods.length-1; i>=0 && matchCount > 0; i--) {
	        if (isMatch[i]) {
	            matchCount--;
	            simpleMethods[matchCount] = allMethods[i];
	        }
	    }
	    
	    return simpleMethods;
	}
}
