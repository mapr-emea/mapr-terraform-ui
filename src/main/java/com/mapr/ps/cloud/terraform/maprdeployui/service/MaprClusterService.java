package com.mapr.ps.cloud.terraform.maprdeployui.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapr.ps.cloud.terraform.maprdeployui.model.AdditionalClusterInfoDTO;
import com.mapr.ps.cloud.terraform.maprdeployui.model.ClusterConfigurationDTO;
import com.mapr.ps.cloud.terraform.maprdeployui.model.DeploymentStatus;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MaprClusterService {
    @Value("${maprdeployui.terraform_project_path}")
    private String terraformProjectPath;
    @Autowired
    private TerraformService terraformService;

    @PostConstruct
    public void init() throws IOException {
        FileUtils.forceMkdir(new File(terraformProjectPath));
        FileUtils.forceMkdir(new File(terraformProjectPath + "/clusterinfo/"));
        FileUtils.forceMkdir(new File(terraformProjectPath + "/clusterinfo/openvpn/"));
        FileUtils.forceMkdir(new File(terraformProjectPath + "/clusterinfo/logs/"));
        FileUtils.forceMkdir(new File(terraformProjectPath + "/clusterinfo/states/"));
        FileUtils.forceMkdir(new File(terraformProjectPath + "/clusterinfo/maprdeployui/"));
        FileUtils.forceMkdir(new File(terraformProjectPath + "/clusterinfo/additionalinfo/"));
        FileUtils.forceMkdir(new File(terraformProjectPath + "/clusterinfo/terraformconfig/"));
    }


    public List<ClusterConfigurationDTO> getMaprClusters() {
        Collection<File> files = FileUtils.listFiles(new File(terraformProjectPath + "/clusterinfo/maprdeployui/"), new String[]{"json"}, false);
        return files.stream().map(this::getClusterConfigurationByFile).collect(Collectors.toList());

    }

    public boolean isPrefixUsed(ClusterConfigurationDTO clusterConfiguration) {
        return new File(terraformProjectPath + "/clusterinfo/maprdeployui/" + clusterConfiguration.getEnvPrefix() + "-maprdeployui.json").exists();
    }

    public void deployCluster(ClusterConfigurationDTO clusterConfiguration) {
        clusterConfiguration.setDeploymentStatus(DeploymentStatus.WAIT_DEPLOY);
        clusterConfiguration.setDeployedAt(new Date());
        saveJson(clusterConfiguration);
        terraformService.deploy(clusterConfiguration);
    }

    public void redeployCluster(ClusterConfigurationDTO clusterConfiguration) throws InvalidClusterStateException {
        checkState(clusterConfiguration, DeploymentStatus.DESTROYED, DeploymentStatus.FAILED);
        clusterConfiguration.setDeploymentStatus(DeploymentStatus.WAIT_DEPLOY);
        clusterConfiguration.setDeployedAt(new Date());
        clusterConfiguration.setDestroyedAt(null);
        saveJson(clusterConfiguration);
        terraformService.deploy(clusterConfiguration);
    }

    public void deleteCluster(ClusterConfigurationDTO clusterConfiguration) throws InvalidClusterStateException {
        checkState(clusterConfiguration, DeploymentStatus.DESTROYED);
        try {
            FileUtils.forceDelete(new File(terraformProjectPath + "/clusterinfo/maprdeployui/" + clusterConfiguration.getEnvPrefix() + "-maprdeployui.json"));
            terraformService.deleteTerraformData(clusterConfiguration);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public ClusterConfigurationDTO getClusterConfigurationByEnvPrefix(String envPrefix) {
        File inputFile = new File(terraformProjectPath + "/clusterinfo/maprdeployui/" + envPrefix + "-maprdeployui.json");
        return getClusterConfigurationByFile(inputFile);
    }

    private ClusterConfigurationDTO getClusterConfigurationByFile(File inputFile) {
        ObjectMapper objectMapper = new ObjectMapper();
        // Sometimes the json file is just written and invalid. Retry, up to ten times
        for (int i = 0; i < 10 ; i++) {
            try {
                return objectMapper.readValue(inputFile, ClusterConfigurationDTO.class);
            } catch (IOException e) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e1) {

                }
            }
        }
        throw new RuntimeException("Was not able to read JSON files.");
    }

    public File getOpenvpnFile(String envPrefix) {
        AdditionalClusterInfoDTO additionalClusterInfo = getAdditionalClusterInfo(envPrefix);
        if(additionalClusterInfo.isDataAvailable()) {
            return new File(terraformProjectPath + "/clusterinfo/openvpn/" + additionalClusterInfo.getOpenvpnFile());
        }
        return null;
    }

    public File getLogFile(String envPrefix) {
        return new File(terraformProjectPath + "/clusterinfo/logs/" + envPrefix + ".log");
    }

    public AdditionalClusterInfoDTO getAdditionalClusterInfo(String envPrefix) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            String additionalInfoPath = terraformProjectPath + "/clusterinfo/additionalinfo/" + envPrefix + "-env.json";
            AdditionalClusterInfoDTO additionalClusterInfoDTO = objectMapper.readValue(new File(additionalInfoPath), AdditionalClusterInfoDTO.class);
            additionalClusterInfoDTO.setDataAvailable(true);
            return additionalClusterInfoDTO;
        } catch (IOException e) {
            AdditionalClusterInfoDTO additionalClusterInfoDTO = new AdditionalClusterInfoDTO();
            additionalClusterInfoDTO.setDbPassword("Not yet available");
            additionalClusterInfoDTO.setDomainName("Not yet available");
            additionalClusterInfoDTO.setEnvPrefix("Not yet available");
            additionalClusterInfoDTO.setMaprPassword("Not yet available");
            additionalClusterInfoDTO.setMaprUser("Not yet available");
            additionalClusterInfoDTO.setMcsUrl("Not yet available");
            additionalClusterInfoDTO.setSshConnection("Not yet available");
            additionalClusterInfoDTO.setDataAvailable(false);
            return additionalClusterInfoDTO;
        }
    }

    public void destroyCluster(ClusterConfigurationDTO clusterConfiguration) throws InvalidClusterStateException {
        checkState(clusterConfiguration, DeploymentStatus.DEPLOYED, DeploymentStatus.FAILED);
        clusterConfiguration.setDeploymentStatus(DeploymentStatus.WAIT_DESTROY);
        clusterConfiguration.setDestroyedAt(new Date());
        saveJson(clusterConfiguration);
        terraformService.destroy(clusterConfiguration);
    }

    private void saveJson(ClusterConfigurationDTO clusterConfiguration) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            objectMapper.writeValue(new File(terraformProjectPath + "/clusterinfo/maprdeployui/" +  clusterConfiguration.getEnvPrefix() + "-maprdeployui.json"), clusterConfiguration);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void checkState(ClusterConfigurationDTO clusterConfiguration, DeploymentStatus ...expectedStates) throws InvalidClusterStateException {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            ClusterConfigurationDTO clusterConfigurationDTO = objectMapper.readValue(new File(terraformProjectPath + "/clusterinfo/maprdeployui/" + clusterConfiguration.getEnvPrefix() + "-maprdeployui.json"), ClusterConfigurationDTO.class);
            for (DeploymentStatus expectedState : expectedStates) {
                if(clusterConfigurationDTO.getDeploymentStatus() == expectedState) {
                    return;
                }
            }
            throw new InvalidClusterStateException("Action aborted. Cluster state was changed and is " + clusterConfigurationDTO.getDeploymentStatus());
        } catch (IOException e) {
            throw new InvalidClusterStateException("Cannot read cluster configuration file. " + e.getMessage());
        }
    }
}