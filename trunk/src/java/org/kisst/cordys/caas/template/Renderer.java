package org.kisst.cordys.caas.template;

import java.io.File;
import java.util.Map;

/**
 * Interface for rendering a template.
 * 
 * @author hvdvlier
 *
 */
public interface Renderer
{
    public String render(Map<String, String> vars, String template, File templateFolder);
}
