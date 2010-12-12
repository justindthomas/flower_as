/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package name.justinthomas.flower.analysis.servlets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import name.justinthomas.flower.analysis.persistence.AlertManager;
import name.justinthomas.flower.analysis.persistence.PersistentAlert;

/**
 *
 * @author justin
 */
@WebServlet(name = "modsecLogReceiver", urlPatterns = {"/modsecLogReceiver"})
public class modsecLogReceiver extends HttpServlet {

    private Pattern any = Pattern.compile("^--[a-f0-9]{8}-[ABCDEFGHIJKZ]--$");

    /** 
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        System.out.println("modsecLogReceiver called");
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        try {
            AlertManager alertManager = new AlertManager();
            alertManager.addAlert(this.getAlert(request));
        } finally {
            out.close();
        }
    }

    private PersistentAlert getAlert(HttpServletRequest request) {
        StringBuilder auditLogHeader = new StringBuilder();
        StringBuilder requestHeaders = new StringBuilder();
        StringBuilder requestBody = new StringBuilder();
        StringBuilder intermediateResponseBody = new StringBuilder();
        StringBuilder finalResponseHeaders = new StringBuilder();
        StringBuilder auditLogTrailer = new StringBuilder();
        StringBuilder requestBodyNoFiles = new StringBuilder();
        LinkedList<String> matchedRules = new LinkedList();

        try {
            BufferedReader reader = request.getReader();

            String line = reader.readLine();
            while ((line != null) && reader.ready()) {
                if (any.matcher(line).matches()) {
                    switch (line.charAt(11)) {
                        case 'A':
                            while (!(any.matcher(line = reader.readLine()).matches())) {
                                auditLogHeader.append(line);
                            }
                            break;
                        case 'B':
                            while (!(any.matcher(line = reader.readLine()).matches())) {
                                requestHeaders.append(line);
                            }
                            break;
                        case 'C':
                            while (!(any.matcher(line = reader.readLine()).matches())) {
                                requestBody.append(line);
                            }
                            break;
                        case 'E':
                            while (!(any.matcher(line = reader.readLine()).matches())) {
                                intermediateResponseBody.append(line);
                            }
                            break;
                        case 'F':
                            while (!(any.matcher(line = reader.readLine()).matches())) {
                                finalResponseHeaders.append(line);
                            }
                            break;
                        case 'H':
                            while (!(any.matcher(line = reader.readLine()).matches())) {
                                auditLogTrailer.append(line);
                            }
                            break;
                        case 'I':
                            while (!(any.matcher(line = reader.readLine()).matches())) {
                                requestBodyNoFiles.append(line);
                            }
                            break;
                        case 'K':
                            while (!(any.matcher(line = reader.readLine()).matches())) {
                                matchedRules.add(line);
                            }
                            break;
                        default:
                            line = reader.readLine();
                            break;
                    }
                } else {
                    line = reader.readLine();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        String dateString = auditLogHeader.substring(1, 28);

        // source address, source port, destination address, destination port
        String[] networkInformation = auditLogHeader.substring(55).split(" ");

        DateFormat formatter = new SimpleDateFormat("dd/MMM/yyyy:HH:mm:ss -Z");
        Date date = null;

        try {
            date = formatter.parse(dateString);
        } catch (ParseException pe) {
            pe.printStackTrace();
        }

        PersistentAlert palert = new PersistentAlert(
                date.getTime() / 1000,
                networkInformation[0],
                networkInformation[2],
                Integer.valueOf(networkInformation[1]),
                Integer.valueOf(networkInformation[3]),
                auditLogHeader.toString(),
                requestHeaders.toString(),
                requestBody.toString(),
                intermediateResponseBody.toString(),
                finalResponseHeaders.toString(),
                auditLogTrailer.toString(),
                requestBodyNoFiles.toString(),
                matchedRules);

        System.out.println("Date: " + date.toString() + " Source: " + networkInformation[0] + " Data: " + auditLogHeader + " Length: " + request.getContentLength());
        return palert;
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /** 
     * Handles the HTTP <code>GET</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /** 
     * Handles the HTTP <code>POST</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /** 
     * Returns a short description of the servlet.
     * @return a String containing servlet description
     */
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>
}
