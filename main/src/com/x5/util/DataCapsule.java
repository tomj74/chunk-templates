package com.x5.util;

public interface DataCapsule
{
    /**
     * getExports() returns the names of all exported methods
     * that will be available to a template through tags like
     * {$[export_prefix]_[lowercase_export_name]} when the
     * DataCapsule object is placed directly into a Chunk's
     * tag table:
     *  Chunk c = theme.makeChunk("template_name");
     *  someObj x = fetchObj();
     *  c.addData(x);
     *
     * For now, only very simple methods may be exported.
     * Use this pattern:
     *
     *     String getSomeValue() { ... }
     *
     * ie, to be exported the method must accept no arguments and
     * must return a String.
     *
     * Implementing DataCapsule can save quite a bit of programming.
     * For example, exporting "getSomeValue" results in the tag
     * {$prefix_some_value} automatically becoming available to the template.
     *
     * Export the method as "getSomeValue my_val" to change the tag
     * name to {$prefix_my_val}
     *
     * "prefix" can be whatever you want, it is the String returned
     * by getExportPrefix()
     *
     */
    public String[] getExports();
    public String getExportPrefix();

}
