/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package name.justinthomas.flower.analysis.authentication;

/**
 *
 * @author justin
 */
public class FastBindClientExample {

    public static void main(String[] args) {
        run();
    }

    public static void run() {
        UserAction action = new UserAction();

        AuthenticationToken token = action.authenticate("1111", "justin@internal.jdthomas.net", "");
        if (token.authenticated) {
            System.out.println("Authenticated: " + token.fullName);
        } else {
            System.out.println("Authentication Failed.");
        }

        if (token.authorized) {
            if(token.internal) {
                System.out.println("Authorized: " + token.fullName);
            } else {
                System.out.println("Authorized: " + token.distinguishedName);
            }
        }

        if (token.administrator) {
            if(token.internal) {
                System.out.println("Privileged: " + token.fullName);
            } else {
                System.out.println("Privileged: " + token.distinguishedName);
            }
        }

    }
}
