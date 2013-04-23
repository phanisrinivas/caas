package org.kisst.cordys.caas.main;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TryReplacement
{
    /**
     * Main method.
     * 
     * @param saArguments Commandline arguments.
     */
    public static void main(String[] saArguments)
    {
        try
        {
            Map<String, String> replacements = new LinkedHashMap<String, String>();
            replacements.put("jan.iets", "MyNiceValue");
            
            
            String output = replaceTokens("Bla blaajdhskja sdhakjsd hasd kja sd\n\n\n\njashd\njan.\niets\njahdjasd\n${jan.iets}\nTrailing", replacements);
            System.out.println("\n====\n"+ output + "\n====\n");
            
            //Tricky:
            replacements.put("other.val", "${jan.iets}");
            output = replaceTokens("${other.val}\n\njashd\njan.\niets\njahdjasd\n${jan.iets}\nTrailing", replacements);
            System.out.println("\n====\n"+ output + "\n====\n");
            
            output = replaceTokens("Nothing to repalce!", replacements);
            System.out.println("\n====\n"+ output + "\n====\n");
            
            output = replaceTokens("${unknown.var}!", replacements);
            System.out.println("\n====\n"+ output + "\n====\n");
            
            replacements.put("unknown.var", "WithAValue");
            output = replaceTokens("${unknown.var}!", replacements);
            System.out.println("\n====\n"+ output + "\n====\n");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static String replaceTokens(String text, Map<String, String> replacements)
    {
        Pattern pattern = Pattern.compile("\\$\\{(.+?)\\}");
        Matcher matcher = pattern.matcher(text);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find())
        {
            String replacement = replacements.get(matcher.group(1));
            if (replacement != null)
            {
                matcher.appendReplacement(buffer, "");
                buffer.append(replacement);
            }
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }
}
