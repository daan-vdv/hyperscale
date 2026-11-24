package dev.daan.hyperscale.hall;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Deterministic resource cost, replica scaling curve, and stable identity key for a compiled ProcessImage.
 */
public final class ImageResources {
    private ImageResources() {
    }

    /**
     * RAM (in GB) reserved per running replica of this image.
     */
    public static int ramGbPerReplica(ProcessImage image) {
        if (image == null) {
            return 2;
        }
        int ioComplexity = (image.inputs().size() + image.outputs().size()) / 2;
        int machineWeight = Math.min(24, image.machineCount() / 2);
        int energyWeight = Math.min(32, image.energyPerCycle() / 2500);
        return Math.max(2, 2 + ioComplexity + machineWeight + energyWeight);
    }

    /**
     * vCPU cores reserved per running replica of this image.
     */
    public static int vcpuPerReplica(ProcessImage image) {
        if (image == null) {
            return 1;
        }
        int machineCores = Math.min(12, image.machineCount() / 4);
        int energyCores = Math.min(16, image.energyPerCycle() / 10000);
        int fastBonus = image.cycleTicks() <= 20 ? 1 : 0;
        return Math.max(1, 1 + machineCores + energyCores + fastBonus);
    }

    /**
     * Organic FE & Heat overhead multiplier as a single deployment scales to high replica counts.
     * No artificial hard cap — high replica counts are sustained through better cooling, power, and silicon.
     */
    public static double replicaOverheadMultiplier(int replicas) {
        if (replicas <= 4) {
            return 1.00D;
        }
        if (replicas <= 16) {
            return 1.15D;
        }
        if (replicas <= 64) {
            return 1.35D;
        }
        double extraDoublings = Math.log(replicas / 64.0D) / Math.log(2.0D);
        return 1.65D + 0.15D * Math.max(0.0D, extraDoublings);
    }

    /**
     * Stable content-addressed key for an image so deployments stay bound even if cards move slots.
     */
    public static String keyOf(ProcessImage image) {
        StringBuilder sb = new StringBuilder();
        sb.append("t=").append(image.cycleTicks())
                .append(";e=").append(image.energyPerCycle())
                .append(";m=").append(image.machineCount())
                .append(";i=");
        for (StackCount in : image.inputs()) {
            sb.append(in.item()).append('*').append(in.count()).append(',');
        }
        sb.append(";o=");
        for (StackCount out : image.outputs()) {
            sb.append(out.item()).append('*').append(out.count()).append(',');
        }
        if (!image.fluidInputs().isEmpty() || !image.fluidOutputs().isEmpty()) {
            sb.append(";fi=");
            for (FluidStackCount fi : image.fluidInputs()) {
                sb.append(fi.fluid()).append('*').append(fi.amount()).append(',');
            }
            sb.append(";fo=");
            for (FluidStackCount fo : image.fluidOutputs()) {
                sb.append(fo.fluid()).append('*').append(fo.amount()).append(',');
            }
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 8);
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(sb.toString().hashCode());
        }
    }
}
