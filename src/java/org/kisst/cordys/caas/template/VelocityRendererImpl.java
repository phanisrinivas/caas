package org.kisst.cordys.caas.template;

import java.io.File;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.kisst.cordys.caas.exception.CaasRuntimeException;

/**
 * Render a template using the Velocity template Engine.
 * This implementation is not fully compatible with the SimpleRenderer as it will not allow you set the following properties:
 * 
 * a.b=xxx
 * a.b.c=xxx
 * 
 * @author hvdvlier
 */
public class VelocityRendererImpl extends Renderer
{
    @Override
    public String render(Map<String, String> vars, String template, File templateFolder)
    {
        // First we need to include the files that are to be included using the CAAS-specific
        // ${include:file=} or ${include:folder=;pattern=*.xml}
        // Velocity-style includes - once supported - will be handled in Velocity.evaluate()
        template = processIncludeFiles(template, templateFolder);
        
        VelocityContext context = getVelocityContext(vars);
        StringWriter sw = new StringWriter();
        Velocity.evaluate(context, sw, "", template);
        return sw.toString();
    }

    private VelocityContext getVelocityContext(Map<String, String> vars)
    {
        VelocityContext context = new VelocityContext();
        Map<String, Object> multiLevelMap = mapToMultiLevelMap(vars);

        for (String key : multiLevelMap.keySet())
        {
            context.put(key, multiLevelMap.get(key));
        }

        return context;
    }

    private Map<String, Object> mapToMultiLevelMap(Map<String, String> vars)
    {
        Map<String, Object> multiLevelMap = new HashMap<String, Object>();
        for (String key : vars.keySet())
        {
            String value = vars.get(key);
            setValueInMultiLevelMap(vars, key, value, multiLevelMap);
        }
        return multiLevelMap;
    }

    @SuppressWarnings("unchecked")
    private void setValueInMultiLevelMap(Map<String, String> vars, String key, String value, Map<String, Object> multiLevelMap)
    {
        String[] keyValues = key.split("\\.");
        Map<String, Object> currentObject = multiLevelMap;
        for (int i = 0; i < keyValues.length - 1; i++)
        {
            String keyValue = keyValues[i];
            Object childObj = currentObject.get(keyValue);
            if (childObj == null)
            {
                childObj = new HashMap<String, Object>();
                currentObject.put(keyValue, childObj);
            }
            else
            {
                if (childObj instanceof String)
                {
                    throw new CaasRuntimeException("Property " + key + " is already set");
                }
            }
            currentObject = (Map<String, Object>) childObj;
        }

        String k = keyValues[keyValues.length - 1];
        if (currentObject.get(k) == null)
        {
            currentObject.put(keyValues[keyValues.length - 1], value);
        }
        else
        {
            throw new CaasRuntimeException("Property " + key + " is already set");
        }
    }
}