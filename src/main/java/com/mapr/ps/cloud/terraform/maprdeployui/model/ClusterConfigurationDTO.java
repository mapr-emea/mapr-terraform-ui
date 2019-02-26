package com.mapr.ps.cloud.terraform.maprdeployui.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ClusterConfigurationDTO implements Serializable {
    private String customerName;
    private String envPrefix;
    private String clusterName;
    private String privateDomain;
    private AwsRegionDTO awsRegion;
    private String awsAvZone;
    private DefaultClusterLayoutDTO numberNodes;
    private AwsInstanceDTO awsInstanceType;
    private List<NodeLayoutDTO> nodesLayout = new ArrayList<>();

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getEnvPrefix() {
        return envPrefix;
    }

    public void setEnvPrefix(String envPrefix) {
        this.envPrefix = envPrefix;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public String getPrivateDomain() {
        return privateDomain;
    }

    public void setPrivateDomain(String privateDomain) {
        this.privateDomain = privateDomain;
    }


    public String getAwsAvZone() {
        return awsAvZone;
    }

    public void setAwsAvZone(String awsAvZone) {
        this.awsAvZone = awsAvZone;
    }

    public DefaultClusterLayoutDTO getNumberNodes() {
        return numberNodes;
    }

    public void setNumberNodes(DefaultClusterLayoutDTO numberNodes) {
        this.numberNodes = numberNodes;
    }

    public AwsRegionDTO getAwsRegion() {
        return awsRegion;
    }

    public void setAwsRegion(AwsRegionDTO awsRegion) {
        this.awsRegion = awsRegion;
    }

    public AwsInstanceDTO getAwsInstanceType() {
        return awsInstanceType;
    }

    public void setAwsInstanceType(AwsInstanceDTO awsInstanceType) {
        this.awsInstanceType = awsInstanceType;
    }

    public List<NodeLayoutDTO> getNodesLayout() {
        return nodesLayout;
    }

    public void setNodesLayout(List<NodeLayoutDTO> nodesLayout) {
        this.nodesLayout = nodesLayout;
    }

    @Override
    public String toString() {
        return "ClusterConfigurationDTO{" +
                "customerName='" + customerName + '\'' +
                ", envPrefix='" + envPrefix + '\'' +
                ", clusterName='" + clusterName + '\'' +
                ", privateDomain='" + privateDomain + '\'' +
                ", awsRegion=" + awsRegion +
                ", awsAvZone='" + awsAvZone + '\'' +
                ", numberNodes=" + numberNodes +
                ", awsInstanceType=" + awsInstanceType +
                ", nodesLayout=" + nodesLayout +
                '}';
    }
}