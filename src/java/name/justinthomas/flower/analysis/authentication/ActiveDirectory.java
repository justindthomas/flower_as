package name.justinthomas.flower.analysis.authentication;

import java.util.Hashtable;
import java.util.Map;
import javax.naming.AuthenticationException;
import javax.naming.CommunicationException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.Control;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import name.justinthomas.flower.global.GlobalConfigurationManager;
import name.justinthomas.flower.manager.services.CustomerAdministration.Customer;
import org.apache.log4j.Logger;

/**
 *
 * @author justin
 */
class FastBindConnectionControl implements Control {

    @Override
    public byte[] getEncodedValue() {
        return null;
    }

    @Override
    public String getID() {
        return "1.2.840.113556.1.4.1781";
    }

    @Override
    public boolean isCritical() {
        return true;
    }
}

public class ActiveDirectory {

    private static final Logger log = Logger.getLogger(ActiveDirectory.class.getName());
    private Customer customer;
    private String username = null;
    private String bareUsername = null;
    private String password = null;
    private String domain = null;
    private String distinguishedName = null;
    private static GlobalConfigurationManager configurationManager;
    private DirectoryDomainManager directoryDomainManager;

    public ActiveDirectory(Customer customer, String username, String password) {
        this.customer = customer;

        if(configurationManager == null) {
            try {
                configurationManager = (GlobalConfigurationManager) InitialContext.doLookup("java:global/Analysis/GlobalConfigurationManager");
            } catch (NamingException e) {
                log.warn("Could not locate GlobalConfigurationManager: " + e.getExplanation());
            }
        }
        
        this.directoryDomainManager = new DirectoryDomainManager(customer);
        
        this.username = username;
        this.password = password;

        if (username.contains("\\")) {
            this.bareUsername = username.split("\\\\")[1];
        } else if (username.contains("@")) {
            this.bareUsername = username.split("@")[0];
            this.domain = username.split("@")[1];
        } else {
            this.bareUsername = username;
        }
    }

    public AuthenticationToken authenticate() {
        AuthenticationToken token = new AuthenticationToken();
        try {
            LdapContext ctx = getLdapContext();

            if (this.domain != null) {
                token.fullName = getDisplayName(ctx);
                this.distinguishedName = getUserDN(ctx);
                token.distinguishedName = getUserDN(ctx);
                token.authenticated = true;
                token = authorize(ctx, domain, token);
            }

            ctx.close();
        } catch (AuthenticationException e) {
            log.warn(username + " failed authentication: " + e.getExplanation());
        } catch (NamingException e) {
            log.warn(username + " failed authentication: " + e.getExplanation());
        } finally {
            return token;
        }
    }

    public boolean isMember(LdapContext ctx, String group) {
        try {
            SearchControls searchCtls = new SearchControls();
            searchCtls.setSearchScope(SearchControls.SUBTREE_SCOPE);

            String searchFilter = "(&(objectClass=group)(CN=" + group + "))";

            int totalResults = 0;

            String returnedAtts[] = {"member"};
            searchCtls.setReturningAttributes(returnedAtts);

            NamingEnumeration answer = ctx.search(fqdnToBase(domain), searchFilter, searchCtls);

            while (answer.hasMoreElements()) {
                SearchResult sr = (SearchResult) answer.next();

                Attributes attrs = sr.getAttributes();
                if (attrs != null) {
                    for (NamingEnumeration ae = attrs.getAll(); ae.hasMore();) {
                        Attribute attr = (Attribute) ae.next();

                        for (NamingEnumeration e = attr.getAll(); e.hasMore(); totalResults++) {
                            if (distinguishedName.equals(e.next())) {
                                return true;
                            }
                        }
                    }
                }
            }
        } catch (AuthenticationException e) {
            log.info(group + " failed lookup: " + e.getExplanation());
        } catch (NamingException e) {
            log.info(group + " failed lookup: " + e.getExplanation());
        }

        return false;
    }

    private String getDnsServers() throws NamingException {
        Hashtable<String, String> env = new Hashtable();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");
        DirContext ictx = new InitialDirContext(env);

        String dns = (String) ictx.getEnvironment().get("java.naming.provider.url");

        if (dns.contains(" ")) {
            String[] servers = dns.split(" ");
            dns = servers[0];
        }

        log.debug("DNS Server: " + dns);
        return dns;
    }

    private String getLdapServer(String domain) throws NamingException {
        Hashtable<String, String> env = new Hashtable();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");
        env.put(Context.PROVIDER_URL, getDnsServers());

        DirContext ctx = new InitialDirContext(env);
        Attributes attrs = ctx.getAttributes("_ldap._tcp." + domain, new String[]{"SRV"});

        String[] response = attrs.getAll().nextElement().toString().split(" ");
        //String port = response[3];
        String server = response[4];
        if (server.endsWith(".")) {
            StringBuilder buf = new StringBuilder(server);
            buf.setLength(buf.length() - 1);
            server = buf.toString();
        }

        ctx.close();
        log.debug("LDAP Server: " + server);
        return "ldap://" + server;
    }

    private String getDisplayName(LdapContext ctx) throws AuthenticationException, NamingException {
        if (domain == null) {
            return null;
        }

        try {
            SearchControls controls = new SearchControls();
            controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            log.debug("Searching for displayName for: " + bareUsername);
            String searchString = "(&(objectClass=person)(sAMAccountName=" + bareUsername + "))";
            NamingEnumeration directoryEntries = ctx.search(fqdnToBase(domain), searchString, controls);

            if (directoryEntries != null) {
                Attributes attributes = ((SearchResult) directoryEntries.next()).getAttributes();
                NamingEnumeration attributeEnumeration = attributes.getAll();
                while (attributeEnumeration.hasMoreElements()) {
                    Attribute attribute = (Attribute) attributeEnumeration.next();
                    if (attribute.getID().equals("displayName")) {
                        log.debug("Display Name: " + attribute.get());
                        return (String) attribute.get();
                    }
                }
            }
        } catch (NullPointerException e) {
            log.warn("Error attempting to get display name for " + bareUsername + ":" + e.getMessage());
        }
        return null;
    }

    private String getUserDN(LdapContext ctx) throws AuthenticationException, NamingException {
        String subEntry = null;
        try {
            SearchControls controls = new SearchControls();
            controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            String searchString = "(&(objectClass=person)(sAMAccountName=" + bareUsername + "))";
            NamingEnumeration enumer = ctx.search(fqdnToBase(domain), searchString, controls);

            if (enumer != null) {
                subEntry = ((SearchResult) enumer.next()).getName();
                return subEntry + "," + fqdnToBase(domain);
            }
        } catch (NullPointerException e) {
            log.warn("Error attempting to get DN for " + bareUsername + ":" + e.getMessage());
        }
        return null;
    }

    private LdapContext getLdapContext() throws NamingException {
        LdapContext ctx = null;
        Hashtable<String, String> env = new Hashtable();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_CREDENTIALS, password);
        env.put(Context.SECURITY_PROTOCOL, "ssl");
        env.put(Context.REFERRAL, "follow");

        if (domain != null) {
            env.put(Context.SECURITY_PRINCIPAL, username);
            log.debug("Trying: " + username + "@" + domain);
            String url = getLdapServer(domain) + ":636";
            if (url != null) {
                env.put(Context.PROVIDER_URL, url);
                ctx = new InitialLdapContext(env, null);
            }
        } else {
            for (DirectoryDomain item : directoryDomainManager.getDirectoryDomains()) {
                log.debug("Trying: " + username + "@" + domain);
                env.put(Context.SECURITY_PRINCIPAL, username + "@" + item.getDomain());
                String url = getLdapServer(item.getDomain()) + ":636";
                if (url != null) {
                    env.put(Context.PROVIDER_URL, url);
                    try {
                        ctx = new InitialLdapContext(env, null);
                    } catch (AuthenticationException e) {
                        log.warn("Failed: " + item.getDomain());
                        continue;
                    } catch (CommunicationException e) {
                        log.warn("Failed: " + item.getDomain() + "/" + e.getExplanation() + " this is probably due to a certificate error.");

                        if (configurationManager.getUnsafeLdap()) {
                            log.warn("Attempting unencrypted authentication: " + item.getDomain());
                            ctx = tryUnsafe(item.getDomain(), ctx, env);
                        }

                        if (domain != null) {
                            log.warn("Unencrypted authentication was successful against: " + item.getDomain());
                            break;
                        }

                        continue;
                    } catch (NamingException e) {
                        log.warn("Failed: " + item.getDomain() + "/" + e.getExplanation());
                        continue;
                    }
                    this.domain = item.getDomain();
                    log.debug("Authentication seems to have succeeded against: " + item.getDomain());
                    break;
                }
            }
        }
        return ctx;
    }

    private LdapContext tryUnsafe(String domain, LdapContext ctx, Hashtable env) {
        try {
            String url = getLdapServer(domain) + ":389";
            env.put(Context.PROVIDER_URL, url);
            env.remove(Context.SECURITY_PROTOCOL);
            ctx = new InitialLdapContext(env, null);
            this.domain = domain;
        } catch (AuthenticationException e) {
            log.info("Unencrypted authentication failed: " + domain + "/" + e.getExplanation());
        } catch (CommunicationException e) {
            log.warn("Unencrypted authentication failed: " + domain + "/" + e.getExplanation());
        } catch (NamingException e) {
            log.warn("Unencrypted authentication failed: " + domain + "/" + e.getExplanation());
        }
        return ctx;
    }

    private AuthenticationToken authorize(LdapContext ctx, String domain, AuthenticationToken token) {
        log.debug("Getting groups for: " + domain);
        Map<String, Boolean> groups = directoryDomainManager.getDirectoryDomain(domain).getGroups();

        for (String group : groups.keySet()) {
            log.debug("Evaluating: " + group);

            if (isMember(ctx, group)) {
                if (groups.get(group)) {
                    token.administrator = true;
                    token.authorized = true;
                    break;
                }

                token.authorized = true;
            }
        }
        return token;
    }

    private String fqdnToBase(String fqdn) {
        if (fqdn == null) {
            return null;
        }

        String[] parts = fqdn.split("\\.");
        StringBuilder searchBase = new StringBuilder();
        for (int i = 0; i < parts.length;) {
            searchBase.append("DC=").append(parts[i++]);
            if (i < parts.length) {
                searchBase.append(",");
            }
        }

        return searchBase.toString();
    }
}
