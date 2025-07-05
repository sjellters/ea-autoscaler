package com.uni.ea_autoscaler.k8s.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class DeploymentStatusCheck {

    private boolean deploymentExists;
    private boolean hpaExists;
    private int expectedReplicas;
    private List<String> readyPods = new ArrayList<>();
    private List<String> terminatingPods = new ArrayList<>();
    private List<String> errorPods = new ArrayList<>();
}
