package org.kisst.cordys.caas.documentor;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.kisst.cordys.caas.Assignment;
import org.kisst.cordys.caas.AuthenticatedUser;
import org.kisst.cordys.caas.Caas;
import org.kisst.cordys.caas.ConnectionPoint;
import org.kisst.cordys.caas.CordysSystem;
import org.kisst.cordys.caas.Dso;
import org.kisst.cordys.caas.DsoType;
import org.kisst.cordys.caas.Machine;
import org.kisst.cordys.caas.Organization;
import org.kisst.cordys.caas.OsProcess;
import org.kisst.cordys.caas.PackageDefinition;
import org.kisst.cordys.caas.ProcessModel;
import org.kisst.cordys.caas.Role;
import org.kisst.cordys.caas.ServiceContainer;
import org.kisst.cordys.caas.ServiceGroup;
import org.kisst.cordys.caas.Team;
import org.kisst.cordys.caas.User;
import org.kisst.cordys.caas.util.FileUtil;

import com.thoughtworks.qdox.JavaDocBuilder;
import com.thoughtworks.qdox.model.AbstractJavaEntity;
import com.thoughtworks.qdox.model.ClassLibrary;
import com.thoughtworks.qdox.model.DocletTag;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaField;
import com.thoughtworks.qdox.model.JavaMethod;
import com.thoughtworks.qdox.model.JavaParameter;

/**
 * This class generates wiki documentation for the object model of Caas.
 * 
 * @author pgussow
 */
public class GenerateDoc
{
    /** Whether or not the HTML macro is enabled in the wiki */
    private static final String HTML_MACRO = "";
    /** Holds the classes that should be documented. */
    private List<String> m_classes = new ArrayList<String>();
    /** Holds the JavaDoc parses that is used. */
    private JavaDocBuilder m_builder;

    /**
     * Instantiates a new generate doc.
     * 
     * @param source The source
     */
    public GenerateDoc(String source)
    {
        //Add the classes that need to be documented
        m_classes.add(Caas.class.getName());
        m_classes.add(CordysSystem.class.getName());
        m_classes.add(Organization.class.getName());
        m_classes.add(DsoType.class.getName());
        m_classes.add(Dso.class.getName());
        m_classes.add(Machine.class.getName());
        m_classes.add(ServiceGroup.class.getName());
        m_classes.add(ServiceContainer.class.getName());
        m_classes.add(ConnectionPoint.class.getName());
        m_classes.add(AuthenticatedUser.class.getName());
        m_classes.add(User.class.getName());
        m_classes.add(Role.class.getName());
        m_classes.add(Team.class.getName());
        m_classes.add(Assignment.class.getName());
        m_classes.add(PackageDefinition.class.getName());
        m_classes.add(org.kisst.cordys.caas.Package.class.getName());
        m_classes.add(ProcessModel.class.getName());
        m_classes.add(OsProcess.class.getName());
        

        m_builder = new JavaDocBuilder();
        m_builder.addSourceTree(new File(source));

        ClassLibrary cl = m_builder.getClassLibrary();
        cl.addDefaultLoader();

        cl.addClassLoader(ClassLoader.getSystemClassLoader());
    }

    /**
     * Main method.
     * 
     * @param saArguments Commandline arguments.
     */
    public static void main(String[] saArguments)
    {
        try
        {
            GenerateDoc gd = new GenerateDoc("./src/java");

            String documentation = gd.document("c:/temp");

            System.out.println(documentation);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     * This method will create the wiki documentation for all the classes.
     * 
     * @return The string for the overview page
     */
    public String document(String outputLoc)
    {
        File output = new File(outputLoc);

        StringBuilder sb = new StringBuilder(1024);

        Map<JavaClass, String> allDocs = new LinkedHashMap<JavaClass, String>();

        for (String clazz : m_classes)
        {
            JavaClass clz = m_builder.getClassByName(clazz);
            String doc = documentClass(clazz);
            allDocs.put(clz, doc);
        }

        // Generate the Table of content with the objects
        sb.append("{anchor:top}\n");
        sb.append("h1. All Caas objects\n");
        sb.append("|| Name || Description ||\n");

        for (JavaClass clz : allDocs.keySet())
        {

            sb.append("| [").append(clz.getName());
            sb.append("|CAASObject ").append(clz.getName());
            sb.append("] | ").append(clz.getComment()).append(" |\n");
        }

        sb.append("\n");

        for (JavaClass clz : allDocs.keySet())
        {
            FileUtil.saveString(new File(output, "CAASObject" + clz.getName() + ".wiki"), allDocs.get(clz));
        }

        FileUtil.saveString(new File(output, "CAAS Object reference.wiki"), sb.toString());

        return sb.toString();
    }

    /**
     * This method returns the documentation for the given class.
     * 
     * @param clazz The class to document.
     * @return The documentation for the given class.
     */
    public String documentClass(String clazz)
    {
        JavaClass clz = m_builder.getClassByName(clazz);
        if (clz == null)
        {
            throw new RuntimeException("Could not find the sources for class " + clazz);
        }

        StringBuilder sb = new StringBuilder(1024);

        sb.append("{anchor:clz").append(clz.getName()).append("}\n");
        sb.append("h1. Object ").append(clz.getName()).append("\n");

        // Dump the properties. Since there is inheritence, we need to also look at the parent classes.
        JavaField[] fields = getFields(clz);
        
        if (fields != null)
        {
            boolean writeHeader = true;

            // Convert to a list for sorting
            List<JavaField> sorted = Arrays.asList(fields);
            Collections.sort(sorted, new AbstractJavaEntitySorter());

            for (JavaField field : sorted)
            {
                if (field.isPublic())
                {
                    if (writeHeader)
                    {
                        sb.append("h2. Properties\n\n");
                        sb.append("|| Name || Description ||\n");
                        writeHeader = false;
                    }

                    // Write the name
                    sb.append("| ");

                    if (field.isStatic())
                    {
                        sb.append(clz.getName()).append(".");
                    }

                    sb.append(field.getName()).append(" | ");

                    // Get the Java doc
                    sb.append(field.getComment()).append(" |\n");
                }
            }

            if (!writeHeader)
            {
                sb.append("\n");
            }
        }

        // Dump the methods
        Map<JavaMethod, String> methodDetail = new LinkedHashMap<JavaMethod, String>();

        JavaMethod[] methods = getMethods(clz);
        if (methods != null)
        {
            boolean writeHeader = true;

            // Convert to a list for sorting
            List<JavaMethod> sorted = Arrays.asList(methods);
            Collections.sort(sorted, new AbstractJavaEntitySorter());

            for (JavaMethod method : sorted)
            {
                if (method.isPublic())
                {
                    if (writeHeader)
                    {
                        sb.append("h2. Methods\n\n");
                        sb.append("|| Method signature ||\n");
                        writeHeader = false;
                    }

                    // Write the name
                    sb.append("| [");

                    if (method.isStatic())
                    {
                        sb.append(clz.getName()).append(".");
                    }

                    sb.append(method.getName()).append(" (");

                    // Add the parameters
                    boolean first = true;
                    JavaParameter[] parameters = method.getParameters();
                    if (parameters != null && parameters.length > 0)
                    {
                        for (JavaParameter p : parameters)
                        {
                            if (!first)
                            {
                                sb.append(", ");
                            }
                            else
                            {
                                first = false;
                            }

                            String name = p.getType().getFullyQualifiedName();
                            if (p.getType().isResolved() && name.startsWith("java.lang."))
                            {
                                name = name.substring(name.lastIndexOf('.') + 1);
                            }
                            sb.append(name);
                            sb.append(" ").append(p.getName());
                        }
                    }

                    sb.append(")|#clz").append(clz.getName()).append("mtd").append(method.getName()).append("] |\n");

                    // Write the documentation details for the method
                    StringBuilder sbm = new StringBuilder(1024);

                    sbm.append("{anchor:").append("clz").append(clz.getName()).append("mtd").append(method.getName())
                            .append("}\n");
                    sbm.append("h3. Method ").append(method.getCallSignature()).append("\n\n");
                    sbm.append(HTML_MACRO).append(method.getComment()).append(HTML_MACRO).append("\n\n");

                    if (parameters != null && parameters.length > 0)
                    {
                        DocletTag[] pt = method.getTagsByName("param");
                        Map<String, String> paramDocs = new LinkedHashMap<String, String>();
                        for (DocletTag dt : pt)
                        {
                            String value = dt.getValue();
                            String pName = "";
                            String pValue = "";
                            if (value.indexOf(' ') > -1)
                            {
                                pName = value.substring(0, value.indexOf(' '));
                                pValue = value.substring(value.indexOf(' ') + 1);
                            }
                            else
                            {
                                pName = value;
                            }
                            paramDocs.put(pName, pValue);
                        }

                        sbm.append("Parameters:\n|| Name || Type || Description ||\n");

                        for (JavaParameter p : parameters)
                        {
                            sbm.append("| ").append(p.getName()).append(" | ").append(p.getType()).append(" | ");

                            String doc = "";
                            if (paramDocs.containsKey(p.getName()))
                            {
                                doc = HTML_MACRO + paramDocs.get(p.getName()) + HTML_MACRO;
                            }
                            sbm.append(doc).append(" |\n");
                        }
                    }

                    sbm.append("[").append(clz.getName()).append("|#clz").append(clz.getName()).append("]");

                    methodDetail.put(method, sbm.toString());
                }
            }

            if (!writeHeader)
            {
                sb.append("\n");
            }
        }

        // Add all the detailed method documentation
        sb.append("\n");

        for (String detail : methodDetail.values())
        {
            sb.append(detail).append("\n\n");
        }

        return sb.toString();
    }

    /**
     * This method gets all the fields applicable for this class. This includes the fields from the parent classes.
     * 
     * @param clz The class to get the fields for.
     * @return All the fields including the parent.
     */
    private JavaField[] getFields(JavaClass clz)
    {
        List<JavaField> retVal = new ArrayList<JavaField>();
        
        JavaClass current = clz;
        
        while (current != null && current.getPackageName().startsWith("org.kisst"))
        {
            JavaField[] tmp = current.getFields();
            for (JavaField jf : tmp)
            {
                retVal.add(jf);
            }
            
            current = current.getSuperJavaClass();
        }
        
        return retVal.toArray(new JavaField[0]);
    }
    
    /**
     * This method gets all the methods applicable for this class. This includes the methods from the parent classes.
     * 
     * @param clz The class to get the methods for.
     * @return All the methods including the parent.
     */
    private JavaMethod[] getMethods(JavaClass clz)
    {
        List<JavaMethod> retVal = new ArrayList<JavaMethod>();
        
        JavaClass current = clz;
        
        while (current != null && current.getPackageName().startsWith("org.kisst"))
        {
            JavaMethod[] tmp = current.getMethods();
            for (JavaMethod jf : tmp)
            {
                retVal.add(jf);
            }
            
            current = current.getSuperJavaClass();
        }
        
        return retVal.toArray(new JavaMethod[0]);
    }

    /**
     * Holds the Class AbstractJavaEntitySorter.
     */
    public class AbstractJavaEntitySorter implements Comparator<AbstractJavaEntity>
    {
        /**
         * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
         */
        @Override
        public int compare(AbstractJavaEntity o1, AbstractJavaEntity o2)
        {
            int retVal = 0;

            String s1 = o1.getName();
            String s2 = o2.getName();

            int n1 = s1.length();
            int n2 = s2.length();
            int min = Math.min(n1, n2);
            for (int i = 0; i < min; i++)
            {
                char c1 = s1.charAt(i);
                char c2 = s2.charAt(i);
                if (c1 != c2)
                {
                    c1 = Character.toUpperCase(c1);
                    c2 = Character.toUpperCase(c2);
                    if (c1 != c2)
                    {
                        c1 = Character.toLowerCase(c1);
                        c2 = Character.toLowerCase(c2);
                        if (c1 != c2)
                        {
                            // No overflow because of numeric promotion
                            retVal = c1 - c2;
                            break;
                        }
                    }
                }
            }

            retVal = n1 - n2;

            // In case of equal method names, sort by parameter count
            // if (retVal == 0 && o1 instanceof JavaMethod && o2 instanceof JavaMethod)
            // {
            // JavaMethod jm1 = (JavaMethod) o1;
            // JavaMethod jm2 = (JavaMethod) o2;
            // if (jm1.getParameters().length < jm2.getParameters().length)
            // {
            // retVal = -1;
            // }
            // else if (jm1.getParameters().length > jm2.getParameters().length)
            // {
            // retVal = 1;
            // }
            // }

            return retVal;
        }
    }

}
