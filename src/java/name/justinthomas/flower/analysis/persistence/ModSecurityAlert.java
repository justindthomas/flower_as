package name.justinthomas.flower.analysis.persistence;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 *
 * @author justin
 */
@Entity
public class ModSecurityAlert implements Serializable {

    private Long id;
    private String accountId;
    private Long date;
    private String sourceAddress;
    private String destinationAddress;
    private Integer sourcePort;
    private Integer destinationPort;

    // mod_security-specific Fields
    private String auditLogHeader;
    private String requestHeaders;
    private String requestBody;
    private String intermediateResponseBody;
    private String finalResponseHeaders;
    private String auditLogTrailer;
    private String requestBodyNoFiles;
    private List<String> matchedRules = new ArrayList();

    public ModSecurityAlert() {

    }

    public ModSecurityAlert(Long date, String sourceAddress, String destinationAddress, Integer sourcePort, Integer destinationPort, String auditLogHeader, String requestHeaders, String requestBody, String intermediateResponseBody, String finalResponseHeaders, String auditLogTrailer, String requestBodyNoFiles, LinkedList<String> matchedRules) {
        this.date = date;
        this.sourceAddress = sourceAddress;
        this.destinationAddress = destinationAddress;
        this.sourcePort = sourcePort;
        this.destinationPort = destinationPort;
        this.auditLogHeader = auditLogHeader;
        this.requestHeaders = requestHeaders;
        this.requestBody = requestBody;
        this.intermediateResponseBody = intermediateResponseBody;
        this.finalResponseHeaders = finalResponseHeaders;
        this.auditLogTrailer = auditLogTrailer;
        this.requestBodyNoFiles = requestBodyNoFiles;
        this.matchedRules = matchedRules;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(date);
        builder.append(":");
        builder.append(sourceAddress);
        builder.append(":");
        builder.append(auditLogHeader);
        return builder.toString();
    }

    public String getAuditLogHeader() {
        return auditLogHeader;
    }

    public void setAuditLogHeader(String auditLogHeader) {
        this.auditLogHeader = auditLogHeader;
    }

    public String getAuditLogTrailer() {
        return auditLogTrailer;
    }

    public void setAuditLogTrailer(String auditLogTrailer) {
        this.auditLogTrailer = auditLogTrailer;
    }

    public Long getDate() {
        return date;
    }

    public void setDate(Long date) {
        this.date = date;
    }

    public String getDestinationAddress() {
        return destinationAddress;
    }

    public void setDestinationAddress(String destinationAddress) {
        this.destinationAddress = destinationAddress;
    }

    public Integer getDestinationPort() {
        return destinationPort;
    }

    public void setDestinationPort(Integer destinationPort) {
        this.destinationPort = destinationPort;
    }

    public String getFinalResponseHeaders() {
        return finalResponseHeaders;
    }

    public void setFinalResponseHeaders(String finalResponseHeaders) {
        this.finalResponseHeaders = finalResponseHeaders;
    }

    @Id
    @GeneratedValue
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIntermediateResponseBody() {
        return intermediateResponseBody;
    }

    public void setIntermediateResponseBody(String intermediateResponseBody) {
        this.intermediateResponseBody = intermediateResponseBody;
    }

    public List<String> getMatchedRules() {
        return matchedRules;
    }

    public void setMatchedRules(List<String> matchedRules) {
        this.matchedRules = matchedRules;
    }

    public String getRequestBody() {
        return requestBody;
    }

    public void setRequestBody(String requestBody) {
        this.requestBody = requestBody;
    }

    public String getRequestBodyNoFiles() {
        return requestBodyNoFiles;
    }

    public void setRequestBodyNoFiles(String requestBodyNoFiles) {
        this.requestBodyNoFiles = requestBodyNoFiles;
    }

    public String getRequestHeaders() {
        return requestHeaders;
    }

    public void setRequestHeaders(String requestHeaders) {
        this.requestHeaders = requestHeaders;
    }

    public String getSourceAddress() {
        return sourceAddress;
    }

    public void setSourceAddress(String sourceAddress) {
        this.sourceAddress = sourceAddress;
    }

    public Integer getSourcePort() {
        return sourcePort;
    }

    public void setSourcePort(Integer sourcePort) {
        this.sourcePort = sourcePort;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }
}
