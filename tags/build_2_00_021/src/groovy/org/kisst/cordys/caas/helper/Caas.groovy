package org.kisst.cordys.caas.helper

import org.kisst.cordys.caas.*

class Caas {
    def caas
    def env

    Caas() {
        //Load the property file
        this.env = Class.forName("org.kisst.cordys.caas.main.Environment");
        this.env.get()

        //Load the Caas entry point
        caas = Class.forName("org.kisst.cordys.caas.Caas");
    }

    def getSystem = { systemName ->
        this.caas.getSystem(systemName)
    }
}
