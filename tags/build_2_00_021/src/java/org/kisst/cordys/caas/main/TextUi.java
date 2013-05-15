package org.kisst.cordys.caas.main;

import org.kisst.cordys.caas.cm.Objective;

public class TextUi implements Objective.Ui
{

    public void error(Objective obj, String msg)
    {
        Environment.error("TEXTUI ERROR:" + msg);
    }

    public void info(Objective obj, String msg)
    {
        Environment.error("TEXTUI INFO :" + msg);
    }

    public void warn(Objective obj, String msg)
    {
        Environment.error("TEXTUI WARN :" + msg);
    }

    public void checking(Objective obj)
    {
        Environment.error("TEXTUI checking :" + obj);
    }

    public void configuring(Objective obj)
    {
        Environment.error("TEXTUI configuring:" + obj);
    }

    public void readyWith(Objective obj)
    {
    }

}
