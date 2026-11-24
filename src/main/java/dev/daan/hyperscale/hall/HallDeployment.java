package dev.daan.hyperscale.hall;

import net.minecraft.nbt.CompoundTag;

public final class HallDeployment {
    public static final int MAX_REPLICAS = 10_000;

    private final String imageKey;
    private int replicas;
    private boolean paused;
    private ExecutionProfile profile;
    private double progress;
    private DeploymentStatus status;
    private int lastFePerTick;

    public HallDeployment(String imageKey, int replicas) {
        this(imageKey, replicas, false, ExecutionProfile.BALANCED, 0.0D, DeploymentStatus.PAUSED, 0);
    }

    public HallDeployment(
            String imageKey,
            int replicas,
            boolean paused,
            ExecutionProfile profile,
            double progress,
            DeploymentStatus status,
            int lastFePerTick
    ) {
        this.imageKey = imageKey;
        this.replicas = Math.max(1, Math.min(MAX_REPLICAS, replicas));
        this.paused = paused;
        this.profile = profile == null ? ExecutionProfile.BALANCED : profile;
        this.progress = Math.max(0.0D, progress);
        this.status = status == null ? DeploymentStatus.PAUSED : status;
        this.lastFePerTick = Math.max(0, lastFePerTick);
    }

    public String imageKey() {
        return this.imageKey;
    }

    public int replicas() {
        return this.replicas;
    }

    public void setReplicas(int replicas) {
        this.replicas = Math.max(1, Math.min(MAX_REPLICAS, replicas));
    }

    public boolean paused() {
        return this.paused;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
        if (paused) {
            this.status = DeploymentStatus.PAUSED;
            this.lastFePerTick = 0;
        }
    }

    public ExecutionProfile profile() {
        return this.profile;
    }

    public void setProfile(ExecutionProfile profile) {
        this.profile = profile == null ? ExecutionProfile.BALANCED : profile;
    }

    public double progress() {
        return this.progress;
    }

    public void setProgress(double progress) {
        this.progress = Math.max(0.0D, progress);
    }

    public DeploymentStatus status() {
        return this.status;
    }

    public void setStatus(DeploymentStatus status) {
        this.status = status;
    }

    public int lastFePerTick() {
        return this.lastFePerTick;
    }

    public void setLastFePerTick(int fePerTick) {
        this.lastFePerTick = Math.max(0, fePerTick);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("ImageKey", this.imageKey);
        tag.putInt("Replicas", this.replicas);
        tag.putBoolean("Paused", this.paused);
        tag.putString("Profile", this.profile.id());
        tag.putDouble("Progress", this.progress);
        tag.putString("Status", this.status.id());
        tag.putInt("LastFe", this.lastFePerTick);
        return tag;
    }

    public static HallDeployment load(CompoundTag tag) {
        String key = tag.getString("ImageKey");
        if (key.isEmpty()) {
            return null;
        }
        int replicas = Math.max(1, Math.min(MAX_REPLICAS, tag.getInt("Replicas")));
        boolean paused = tag.getBoolean("Paused");
        ExecutionProfile profile = ExecutionProfile.byId(tag.getString("Profile"));
        double progress = Math.max(0.0D, tag.getDouble("Progress"));
        DeploymentStatus status = DeploymentStatus.byId(tag.getString("Status"));
        int lastFe = Math.max(0, tag.getInt("LastFe"));
        return new HallDeployment(key, replicas, paused, profile, progress, status, lastFe);
    }
}
