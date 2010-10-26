/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package name.justinthomas.flower.analysis.authentication;

import java.util.HashMap;
import java.util.Hashtable;
import javax.naming.AuthenticationException;
import javax.naming.CommunicationException;
import javax.naming.Context;
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
import name.justinthomas.flower.analysis.persistence.ConfigurationManager;

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

    private String username = null;
    private String bareUsername = null;
    private String password = null;
    private String domain = null;
    private String distinguishedName = null;
    private ConfigurationManager configurationManager = null;
    /*
    private void populateGroups() {
    this.directoryDomains = new HashMap();

    HashMap<String, Boolean> groups = new HashMap();
    groups.put("Flower", false);
    groups.put("Flower-Privileged", true);

    this.directoryDomains.put("internal.jdthomas.net", groups);
    System.out.println("DirectoryDomains has: " + directoryDomains.get("internal.jdthomas.net").keySet().size() + " groups");
    }
     *
     */

    public ActiveDirectory(String username, String password) {
        this.configurationManager = ConfigurationManager.getConfigurationManager();
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
            System.out.println(username + " failed authentication");
            e.printStackTrace();
        } catch (NamingException e) {
            System.out.println(username + " failed authentication");
            e.printStackTrace();
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
                //System.out.println(">>>" + sr.getName());

                Attributes attrs = sr.getAttributes();
                if (attrs != null) {
                    for (NamingEnumeration ae = attrs.getAll(); ae.hasMore();) {
                        Attribute attr = (Attribute) ae.next();
                        //System.out.println("Attribute: " + attr.getID());
                        for (NamingEnumeration e = attr.getAll(); e.hasMore(); totalResults++) {
                            if (distinguishedName.equals(e.next())) {
                                return true;
                            }
                        }
                    }
                }
            }

            //System.out.println("Total members: " + totalResults);
        } catch (AuthenticationException e) {
            e.printStackTrace();
        } catch (NamingException e) {
            e.printStackTrace();
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

        System.out.println("DNS Server: " + dns);
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
        System.out.println("LDAP Server: " + server);
        return "ldap://" + server;
    }

    private String getDisplayName(LdapContext ctx) throws AuthenticationException, NamingException {
        if (domain == null) {
            return null;
        }

        try {
            SearchControls controls = new SearchControls();
            controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            //System.out.println("Searching for displayName for: " + bareUsername);
            String searchString = "(&(objectClass=person)(sAMAccountName=" + bareUsername + "))";
            NamingEnumeration directoryEntries = ctx.search(fqdnToBase(domain), searchString, controls);

            if (directoryEntries != null) {
                Attributes attributes = ((SearchResult) directoryEntries.next()).getAttributes();
                NamingEnumeration attributeEnumeration = attributes.getAll();
                while (attributeEnumeration.hasMoreElements()) {
                    Attribute attribute = (Attribute) attributeEnumeration.next();
                    if (attribute.getID().equals("displayName")) {
                        //System.out.println("Display Name: " + attribute.get());
                        return (String) attribute.get();
                    }
                }
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
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
            e.printStackTrace();
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
            System.out.println("Trying: " + username + "@" + domain);
            String url = getLdapServer(domain) + ":636";
            if (url != null) {
                env.put(Context.PROVIDER_URL, url);
                ctx = new InitialLdapContext(env, null);
            }
        } else {
            for (String item : configurationManager.getDirectoryDomains().keySet()) {
                System.out.println("Trying: " + username + "@" + item);
                env.put(Context.SECURITY_PRINCIPAL, username + "@" + item);
                String url = getLdapServer(item) + ":636";
                if (url != null) {
                    env.put(Context.PROVIDER_URL, url);
                    try {
                        ctx = new InitialLdapContext(env, null);
                    } catch (AuthenticationException e) {
                        System.out.println("Failed: " + item);
                        continue;
                    } catch (CommunicationException e) {
                        System.err.println("Failed: " + item + " / " + e.getExplanation());

                        if (configurationManager.getConfiguration().unsafeLdap) {
                            System.err.println("Attempting unencrypted authentication...");
                            ctx = tryUnsafe(item, ctx, env);
                        }

                        if (domain != null) {
                            System.err.println("Unencrypted authentication was successful against: " + item);
                            break;
                        }

                        continue;
                    } catch (NamingException e) {
                        System.out.println("Failed: " + item + " / " + e.getExplanation());
                        continue;
                    }
                    this.domain = item;
                    System.out.println("Authentication seems to have succeeded against: " + item);
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
            System.out.println("Failed: " + domain);
        } catch (CommunicationException e) {
            System.out.println("Failed: " + domain + " / " + e.getExplanation());
        } catch (NamingException e) {
            System.out.println("Failed: " + domain + " / " + e.getExplanation());
        }
        return ctx;
    }

    private AuthenticationToken authorize(LdapContext ctx, String domain, AuthenticationToken token) {
        //System.out.println("Getting groups for: " + domain);
        HashMap<String, Boolean> groups = configurationManager.getDirectoryDomains().get(domain);

        for (String group : groups.keySet()) {
            //System.out.println("\tEvaluating: " + group);

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
