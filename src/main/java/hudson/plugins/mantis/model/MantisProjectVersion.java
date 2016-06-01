/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hudson.plugins.mantis.model;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Date;

/**
 *
 * @author csibug
 */
public class MantisProjectVersion implements Serializable {
    
    private static final long serialVersionUID = 1L;

    private final BigInteger projectId;
    private BigInteger id;
    private String version;
    private Date dateOrder;
    private String description;
    private boolean released;
    private boolean obsolete;

    public MantisProjectVersion(BigInteger projectId, BigInteger id, String version, 
            String description, boolean released) {
        this.id = id;
        this.projectId = projectId;
        this.version = version;
        this.dateOrder = new Date();
        this.description = description;
        this.released = released;
        this.obsolete = false;
    }

    public BigInteger getId() {
        return id;
    }
    
    public BigInteger getProjectId() {
        return projectId;
    }

    public String getVersion() {
        return version;
    }

    public Date getDateOrder() {
        return dateOrder;
    }

    public String getDescription() {
        return description;
    }

    public boolean isReleased() {
        return released;
    }

    public boolean isObsolete() {
        return obsolete;
    }

    public void setId(BigInteger id) {
        this.id = id;
    }
    
    public void setVersion(String version) {
        this.version = version;
    }

    public void setDateOrder(Date dateOrder) {
        this.dateOrder = dateOrder;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setReleased(boolean released) {
        this.released = released;
    }

    public void setObsolete(boolean obsolete) {
        this.obsolete = obsolete;
    }

    @Override
    public String toString() {
        return "MantisProjectVersion{" + "projectId=" + projectId + ", id=" + id + ", version=" + version + ", dateOrder=" + dateOrder + ", description=" + description + ", released=" + released + ", obsolete=" + obsolete + '}';
    }
    
    
}
