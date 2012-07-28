package name.justinthomas.flower.analysis.logging;

import java.io.IOException;
import javax.ejb.DependsOn;
import javax.ejb.Stateless;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import name.justinthomas.flower.global.GlobalConfigurationManager;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.FileAppender;
import org.apache.log4j.PatternLayout;

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

        if (globalConfigurationManager == null) {
            try {
                globalConfigurationManager = (GlobalConfigurationManager) InitialContext.doLookup("java:global/Analysis/GlobalConfigurationManager");
            } catch (NamingException e) {
                e.printStackTrace();
            }
        }

        try {
            String pattern = "%d{HH:mm:ss.SSS} - %p - %m %n";
            PatternLayout layout = new PatternLayout(pattern);
            
            BasicConfigurator.configure(new FileAppender(layout, globalConfigurationManager.getBaseDirectory() + "/server.log"));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) {
    }
}
