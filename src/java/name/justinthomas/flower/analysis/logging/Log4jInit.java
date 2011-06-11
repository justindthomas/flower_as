/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package name.justinthomas.flower.analysis.logging;

import java.io.IOException;
import javax.ejb.Stateless;
import javax.ejb.DependsOn;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import name.justinthomas.flower.global.GlobalConfigurationManager;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.FileAppender;
import org.apache.log4j.SimpleLayout;

/**
 *
 * @author justin
 */
@Stateless
@DependsOn("GlobalConfigurationManager")
public class Log4jInit extends HttpServlet {

    private static GlobalConfigurationManager globalConfigurationManager;
    
    @Override
    public void init() {
        
        if(globalConfigurationManager == null) {
            try {
                globalConfigurationManager = (GlobalConfigurationManager) InitialContext.doLookup("java:global/Analysis/GlobalConfigurationManager");
            } catch (NamingException e) {
                e.printStackTrace();
            }
        }
        
        try {
            BasicConfigurator.configure(new FileAppender(new SimpleLayout(), globalConfigurationManager.getBaseDirectory() + "/server.log"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }
    
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) {
    }
}
