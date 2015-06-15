package org.kisst.cordys.caas.template;

import static org.kisst.cordys.caas.main.Environment.debug;

import java.io.File;
import java.util.Map;

import org.kisst.cordys.caas.util.StringUtil;

/**
 * Simple template renderer that will perform variable substitution and including of other template files.
 *
 */
public class SimpleRendererImpl extends Renderer
{
    
    @Override
    public String render(Map<String, String> vars, String template, File templateFolder)
    {
        // First we need to include the files that are to be included using the CAAS-specific
        // ${include:file=} or ${include:folder=;pattern=*.xml}
        template = processIncludeFiles(template, templateFolder);
        
        // Now substitute the parameters.
        if (vars != null)
        {
            template = StringUtil.substitute(template, vars);
        }

        // Escape the $ sign.
        return template.replace("${dollar}", "$");
    }
}
