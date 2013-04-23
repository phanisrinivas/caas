package org.kisst.cordys.caas.main;

import java.util.LinkedHashMap;
import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;
import org.kisst.cordys.caas.util.StringUtil;

/**
 * Holds the Class StringUtil_Test.
 */
public class StringUtil_Test
{
    /**
     * This test case will test the substitution of the variables in strings.
     */
    @Test
    public void testSubstitute()
    {
        Map<String, String> replacements = new LinkedHashMap<String, String>();
        replacements.put("sub.val.1", "MyNiceValue");
        replacements.put("sub.val.2", "OtherValue");

        String tmp = StringUtil.substitute(
                "My\nsub.val.1: ${sub.val.1}\nsub.val.2: ${sub.val.2}\nNon existing: ${sub.val.3}tricky}", replacements);
        Assert.assertEquals("My\nsub.val.1: MyNiceValue\nsub.val.2: OtherValue\nNon existing: ${sub.val.3}tricky}", tmp);
    }

    /**
     * This test case tests if the reverse substitution works.
     */
    @Test
    public void testReverseSubstitute()
    {
        Map<String, String> replacements = new LinkedHashMap<String, String>();
        replacements.put("sub.val.1", "MyNiceValue");
        replacements.put("sub.val.2", "OtherValue");

        String tmp = StringUtil.reverseSubstitute(
                "My\nsub.val.1: MyNiceValue\nsub.val.2: OtherValue\nNon existing: ${sub.val.3}tricky}", replacements);
        Assert.assertEquals("My\nsub.val.1: ${sub.val.1}\nsub.val.2: ${sub.val.2}\nNon existing: ${sub.val.3}tricky}", tmp);
    }
}
