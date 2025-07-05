package com.uni.ea_autoscaler.ga.nsga3;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class ReferencePointsGenerator {

    public List<double[]> generate(int numberOfObjectives, int divisions) {
        List<double[]> points = new ArrayList<>();
        double[] point = new double[numberOfObjectives];
        generateRecursive(points, point, divisions, divisions, 0);

        log.info("🎯 Generated {} reference points for {} objectives ({} divisions)", points.size(), numberOfObjectives, divisions);
        return points;
    }

    private void generateRecursive(List<double[]> points, double[] point, int left, int total, int depth) {
        if (depth == point.length - 1) {
            point[depth] = (double) left / total;
            points.add(point.clone());
            return;
        }

        for (int i = 0; i <= left; i++) {
            point[depth] = (double) i / total;
            generateRecursive(points, point, left - i, total, depth + 1);
        }
    }
}
